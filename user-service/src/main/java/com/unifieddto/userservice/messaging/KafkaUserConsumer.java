package com.unifieddto.userservice.messaging;

import com.google.protobuf.InvalidProtocolBufferException;
import com.unifieddto.api.user.User;
import com.unifieddto.userservice.service.UserBusinessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaUserConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaUserConsumer.class);

    @Autowired
    private UserBusinessService userBusinessService;

    @KafkaListener(topics = "${app.kafka.topic.users}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(byte[] message) {
        try {
            // Deserialize the byte array back into a Protobuf User object
            User user = User.parseFrom(message);
            log.info("Consumed Kafka message for user: {}", user.getId());
            // Delegate processing to the business logic layer
            userBusinessService.processUserEvent(user);
        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse Kafka message to User protobuf", e);
        }
    }
}
