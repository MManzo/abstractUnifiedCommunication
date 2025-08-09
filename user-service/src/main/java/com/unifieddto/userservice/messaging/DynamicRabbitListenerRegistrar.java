package com.unifieddto.userservice.messaging;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Component
public class DynamicRabbitListenerRegistrar implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(DynamicRabbitListenerRegistrar.class);

    private final DynamicRabbitMQBindings bindings;
    private final AmqpAdmin amqpAdmin;
    private final SimpleRabbitListenerContainerFactory containerFactory;
    private final ApplicationContext applicationContext;

    public DynamicRabbitListenerRegistrar(DynamicRabbitMQBindings bindings, AmqpAdmin amqpAdmin,
                                          SimpleRabbitListenerContainerFactory containerFactory, ApplicationContext applicationContext) {
        this.bindings = bindings;
        this.amqpAdmin = amqpAdmin;
        this.containerFactory = containerFactory;
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        bindings.getBindings().forEach((bindingName, props) -> {
            log.info("Registering dynamic RabbitMQ listener for binding: {}", bindingName);
            try {
                register(props);
            } catch (Exception e) {
                log.error("Failed to register dynamic listener for binding: {}", bindingName, e);
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

        // 2. Get the service bean and DTO class from the context and classpath
        Object serviceBean = applicationContext.getBean(props.getServiceBeanName());
        Class<?> dtoClass = Class.forName(props.getDtoClassName());

        // 3. Create the generic message handler
        GenericMessageHandler handler = new GenericMessageHandler(serviceBean, dtoClass);

        // 4. Create and configure the listener container
        MessageListenerContainer container = containerFactory.createListenerContainer();
        container.setQueueNames(props.getQueueName());
        container.setMessageListener(new MessageListenerAdapter(handler, "handleMessage"));

        // 5. Start the container
        container.start();
        log.info("Successfully started dynamic listener for queue: {}", props.getQueueName());
    }

    /**
     * A generic handler that takes a byte array, deserializes it to the correct
     * Protobuf DTO, and calls the 'execute' method on the target service.
     */
    public static class GenericMessageHandler {
        private final Object serviceBean;
        private final Method executeMethod;
        private final Parser<?> dtoParser;

        public GenericMessageHandler(Object serviceBean, Class<?> dtoClass) throws Exception {
            this.serviceBean = serviceBean;
            // Find the correct overloaded 'execute' method on the service bean
            this.executeMethod = serviceBean.getClass().getMethod("execute", dtoClass);
            // Get the static 'parser()' method from the generated Protobuf class
            Method parserMethod = dtoClass.getMethod("parser");
            this.dtoParser = (Parser<?>) parserMethod.invoke(null);
        }

        // This is the method the MessageListenerAdapter will call
        public void handleMessage(byte[] messageBody) {
            try {
                Message requestDto = (Message) dtoParser.parseFrom(messageBody);
                log.info("Generic handler received message of type {}, invoking {}.execute()",
                        requestDto.getClass().getSimpleName(), serviceBean.getClass().getSimpleName());
                executeMethod.invoke(serviceBean, requestDto);
            } catch (Exception e) {
                log.error("Error in generic message handler", e);
            }
        }
    }
}
