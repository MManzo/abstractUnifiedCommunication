package com.unifieddto.userservice.messaging;

import com.unifieddto.api.user.CreateUserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitUserProducer {

    private static final Logger log = LoggerFactory.getLogger(RabbitUserProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.users}")
    private String exchange;

    @Value("${app.rabbitmq.routingkey.users}")
    private String routingKey;

    public void sendMessage(CreateUserRequest request) {
        log.info("Producing RabbitMQ message for CreateUserRequest: {}", request.getUsername());
        // Serialize the Protobuf CreateUserRequest object to a byte array
        byte[] requestBytes = request.toByteArray();
        rabbitTemplate.convertAndSend(exchange, routingKey, requestBytes);
    }
}
