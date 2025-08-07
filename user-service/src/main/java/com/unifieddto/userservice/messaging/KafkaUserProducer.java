package com.unifieddto.userservice.messaging;

import com.unifieddto.api.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaUserProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaUserProducer.class);

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${app.kafka.topic.users}")
    private String topic;

    public void sendMessage(User user) {
        log.info("Producing Kafka message for user: {}", user.getId());
        // Serialize the Protobuf User object to a byte array
        byte[] userBytes = user.toByteArray();
        kafkaTemplate.send(topic, user.getId(), userBytes);
    }
}
