package com.unifieddto.userservice;

import com.unifieddto.api.user.User;
import com.unifieddto.userservice.messaging.KafkaUserProducer;
import com.unifieddto.userservice.messaging.RabbitUserProducer;
import com.unifieddto.userservice.service.UserBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    @Autowired
    private KafkaUserProducer kafkaUserProducer;

    @Autowired
    private RabbitUserProducer rabbitUserProducer;

    @Autowired
    private UserBusinessService userBusinessService;

    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        // Delegate the call to the business logic layer
        return userBusinessService.getUserById(id);
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publishUser(@RequestBody User user) {
        // This endpoint demonstrates using the same User DTO from a REST request
        // to produce messages to Kafka and RabbitMQ.

        // Spring's ProtobufHttpMessageConverter deserializes the incoming JSON to a User object.
        // We then pass this same object to our producers.
        kafkaUserProducer.sendMessage(user);
        rabbitUserProducer.sendMessage(user);

        return ResponseEntity.ok("User published to Kafka and RabbitMQ: " + user.getId());
    }
}
