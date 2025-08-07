package com.unifieddto.userservice;

import com.unifieddto.api.user.CreateUserRequest;
import com.unifieddto.api.user.CreateUserResponse;
import com.unifieddto.api.user.GetUserRequest;
import com.unifieddto.api.user.User;
import com.unifieddto.userservice.messaging.KafkaUserProducer;
import com.unifieddto.userservice.messaging.RabbitUserProducer;
import com.unifieddto.userservice.service.UserBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    @Autowired
    private UserBusinessService userBusinessService;

    @Autowired
    private KafkaUserProducer kafkaUserProducer;

    @Autowired
    private RabbitUserProducer rabbitUserProducer;


    @PostMapping("/get")
    public User getUser(@RequestBody GetUserRequest request) {
        // Delegate the call to the business logic layer
        return userBusinessService.execute(request);
    }

    @PostMapping("/create")
    public CreateUserResponse createUser(@RequestBody CreateUserRequest request) {
        // This endpoint demonstrates a command-based approach.
        // 1. Execute the business logic command
        CreateUserResponse response = userBusinessService.execute(request);

        // 2. Publish the command to message queues for other services to consume
        kafkaUserProducer.sendMessage(request);
        rabbitUserProducer.sendMessage(request);

        return response;
    }
}
