package com.unifieddto.framework.hexagonal.adapters;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.unifieddto.framework.hexagonal.domain.BusinessException;
import com.unifieddto.framework.hexagonal.domain.UseCase;
import com.unifieddto.framework.hexagonal.domain.ValidationException;
import com.unifieddto.framework.messaging.DynamicRabbitMQBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Hexagonal architecture version of the dynamic RabbitMQ listener registrar.
 * This adapter bridges RabbitMQ messages to use cases while maintaining proper separation of concerns.
 */
@Component
public class HexagonalRabbitListenerRegistrar implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(HexagonalRabbitListenerRegistrar.class);

    private final DynamicRabbitMQBindings bindings;
    private final AmqpAdmin amqpAdmin;
    private final SimpleRabbitListenerContainerFactory containerFactory;
    private final UseCaseRegistry useCaseRegistry;
    private final UseCaseExecutor useCaseExecutor;

    public HexagonalRabbitListenerRegistrar(DynamicRabbitMQBindings bindings,
                                          AmqpAdmin amqpAdmin,
                                          SimpleRabbitListenerContainerFactory containerFactory,
                                          UseCaseRegistry useCaseRegistry,
                                          UseCaseExecutor useCaseExecutor) {
        this.bindings = bindings;
        this.amqpAdmin = amqpAdmin;
        this.containerFactory = containerFactory;
        this.useCaseRegistry = useCaseRegistry;
        this.useCaseExecutor = useCaseExecutor;
    }

    @Override
    public void afterPropertiesSet() {
        bindings.getBindings().forEach((bindingName, props) -> {
            log.info("Registering hexagonal RabbitMQ listener for binding: {}", bindingName);
            try {
                register(props);
            } catch (Exception e) {
                log.error("Failed to register hexagonal listener for binding: {}", bindingName, e);
            }
        });
    }

    private void register(DynamicRabbitMQBindings.BindingProperties props) throws Exception {
        // 1. Declare the queue, exchange, and binding
        Queue queue = new Queue(props.getQueueName(), true, false, false);
        Exchange exchange = new TopicExchange(props.getExchangeName(), true, false);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(props.getRoutingKey()).noargs();
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareBinding(binding);

        // 2. Get the request class
        Class<?> requestClass = Class.forName(props.getDtoClassName());

        // 3. Create the hexagonal message handler
        HexagonalMessageHandler handler = new HexagonalMessageHandler(requestClass);

        // 4. Create and configure the listener container
        SimpleMessageListenerContainer container = containerFactory.createListenerContainer();
        container.setQueueNames(props.getQueueName());
        container.setMessageListener(new MessageListenerAdapter(handler, "handleMessage"));

        // 5. Start the container
        container.start();
        log.info("Successfully started hexagonal listener for queue: {}", props.getQueueName());
    }

    /**
     * A hexagonal message handler that deserializes messages and delegates to use cases.
     */
    public class HexagonalMessageHandler {
        private final Class<?> requestClass;
        private final Parser<?> dtoParser;

        public HexagonalMessageHandler(Class<?> requestClass) throws Exception {
            this.requestClass = requestClass;
            // Get the static 'parser()' method from the generated Protobuf class
            Method parserMethod = requestClass.getMethod("parser");
            this.dtoParser = (Parser<?>) parserMethod.invoke(null);
        }

        /**
         * Handles incoming messages by delegating to the appropriate use case.
         * Returns a byte array for RPC support, or null for fire-and-forget messages.
         */
        public byte[] handleMessage(byte[] messageBody) {
            try {
                // Parse the message
                Message requestDto = (Message) dtoParser.parseFrom(messageBody);
                log.info("Hexagonal handler received message of type: {}", requestDto.getClass().getSimpleName());

                // Find the appropriate use case
                @SuppressWarnings("unchecked")
                Optional<UseCase<Message, Message>> useCase = useCaseRegistry.findUseCaseForRequest((Class<Message>) requestClass);

                if (useCase.isEmpty()) {
                    log.error("No use case found for request type: {}", requestClass.getSimpleName());
                    return createErrorResponse("USE_CASE_NOT_FOUND", "No use case found for request type");
                }

                // Execute the use case
                Message responseDto = useCaseExecutor.execute(useCase.get(), requestDto);

                // Return response as byte array for RPC
                if (responseDto != null) {
                    return responseDto.toByteArray();
                }

            } catch (ValidationException e) {
                log.warn("Validation error in message handler: {}", e.getMessage());
                return createErrorResponse("VALIDATION_ERROR", e.getMessage());
            } catch (BusinessException e) {
                log.warn("Business error in message handler: {}", e.getMessage());
                return createErrorResponse(e.getErrorCode(), e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error in message handler", e);
                return createErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
            }

            return null; // No response for fire-and-forget messages
        }

        private byte[] createErrorResponse(String errorCode, String message) {
            // In a real implementation, you might have a standard error response protobuf message
            // For now, we'll return a simple JSON-like string as bytes
            String errorJson = String.format("{\"error\":\"%s\",\"message\":\"%s\"}", errorCode, message);
            return errorJson.getBytes();
        }
    }
}