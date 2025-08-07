package com.unifieddto.userservice.service;

import com.unifieddto.api.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserBusinessService {

    private static final Logger log = LoggerFactory.getLogger(UserBusinessService.class);

    /**
     * Business logic to retrieve a user.
     * In a real application, this would interact with a database or another microservice.
     *
     * @param id The ID of the user to retrieve.
     * @return The User DTO.
     */
    public User getUserById(String id) {
        log.info("Executing business logic: getUserById for ID {}", id);
        // Simulate fetching a user
        return User.newBuilder()
                .setId(id)
                .setUsername("business-logic-user")
                .setEmail("business-logic.user@example.com")
                .build();
    }

    /**
     * Business logic to process a user event from a message queue.
     *
     * @param user The User DTO from the event.
     */
    public void processUserEvent(User user) {
        log.info("Executing business logic: processUserEvent for User ID {}", user.getId());
        // Here you would put logic like:
        // - Storing the user in a database
        // - Sending a welcome email
        // - Triggering other downstream processes
        log.info("Processed event for user: {}", user.getUsername());
    }
}
