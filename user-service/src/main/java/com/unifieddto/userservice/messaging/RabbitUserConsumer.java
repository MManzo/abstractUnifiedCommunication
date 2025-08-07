package com.unifieddto.userservice.messaging;

import com.google.protobuf.InvalidProtocolBufferException;
import com.unifieddto.api.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitUserConsumer {

    private static final Logger log = LoggerFactory.getLogger(RabbitUserConsumer.class);

    @RabbitListener(queues = "${app.rabbitmq.queue.users}")
    public void receiveMessage(byte[] message) {
        try {
            // Deserialize the byte array back into a Protobuf User object
            User user = User.parseFrom(message);
            log.info("Consumed RabbitMQ message -> User ID: {}, Username: {}", user.getId(), user.getUsername());
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse RabbitMQ message to User protobuf", e);
        }
    }
}
