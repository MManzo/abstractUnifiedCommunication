package com.unifieddto.userservice.messaging;

import com.unifieddto.api.user.User;
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

    public void sendMessage(User user) {
        log.info("Producing RabbitMQ message for user: {}", user.getId());
        // Serialize the Protobuf User object to a byte array
        byte[] userBytes = user.toByteArray();
        rabbitTemplate.convertAndSend(exchange, routingKey, userBytes);
    }
}
