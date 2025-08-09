package com.unifieddto.testservice.service;

import com.unifieddto.api.user.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserBusinessService {

    private static final Logger log = LoggerFactory.getLogger(UserBusinessService.class);

    /**
     * Business logic to retrieve a user, invoked by passing a GetUserRequest command.
     */
    public User execute(GetUserRequest request) {
        log.info("Executing business logic for: GetUserRequest with ID {}", request.getId());
        // In a real application, this would fetch from a database.
        return User.newBuilder()
                .setId(request.getId())
                .setUsername("command-pattern-user")
                .setEmail("command.user@example.com")
                .build();
    }

    /**
     * Business logic to create a new user, invoked by passing a CreateUserRequest command.
     */
    public CreateUserResponse execute(CreateUserRequest request) {
        log.info("Executing business logic for: CreateUserRequest with username {}", request.getUsername());
        // In a real application, this would save to a database and return the generated ID.
        String newId = UUID.randomUUID().toString();
        log.info("New user created with ID: {}", newId);

        return CreateUserResponse.newBuilder()
                .setId(newId)
                .setStatusMessage("User '" + request.getUsername() + "' created successfully.")
                .build();
    }

    /**
     * Business logic to update a user.
     */
    public UpdateUserResponse execute(UpdateUserRequest request) {
        log.info("Executing business logic for: UpdateUserRequest for ID {}", request.getId());
        // Simulate updating a user in the database
        return UpdateUserResponse.newBuilder()
                .setId(request.getId())
                .setStatusMessage("User " + request.getId() + " updated.")
                .build();
    }

    /**
     * Business logic to delete a user.
     */
    public DeleteUserResponse execute(DeleteUserRequest request) {
        log.info("Executing business logic for: DeleteUserRequest for ID {}", request.getId());
        // Simulate deleting a user
        return DeleteUserResponse.newBuilder()
                .setId(request.getId())
                .setStatusMessage("User " + request.getId() + " deleted.")
                .build();
    }

    /**
     * Business logic to search for users.
     */
    public SearchUsersResponse execute(SearchUsersRequest request) {
        log.info("Executing business logic for: SearchUsersRequest with query '{}'", request.getEmailQuery());
        // Simulate finding users
        User foundUser = User.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setUsername("found-user")
                .setEmail(request.getEmailQuery())
                .build();
        return SearchUsersResponse.newBuilder().addUsers(foundUser).build();
    }
}
