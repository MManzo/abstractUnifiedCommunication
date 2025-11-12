package com.unifieddto.testservice.hexagonal.usecases;

import com.unifieddto.api.user.CreateUserRequest;
import com.unifieddto.api.user.CreateUserResponse;
import com.unifieddto.framework.hexagonal.domain.BusinessException;
import com.unifieddto.framework.hexagonal.domain.UseCase;
import com.unifieddto.testservice.hexagonal.ports.UserRepository;
import com.unifieddto.testservice.hexagonal.ports.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Use case for creating a new user.
 * This class contains pure business logic without any dependencies on frameworks or protocols.
 */
@Component
public class CreateUserUseCase implements UseCase<CreateUserRequest, CreateUserResponse> {

    private static final Logger log = LoggerFactory.getLogger(CreateUserUseCase.class);

    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    public CreateUserUseCase(UserRepository userRepository, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CreateUserResponse execute(CreateUserRequest request) throws BusinessException {
        log.info("Executing CreateUserUseCase for username: {}", request.getUsername());

        // Business rule: Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USER_ALREADY_EXISTS", 
                    "A user with username '" + request.getUsername() + "' already exists");
        }

        // Business rule: Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", 
                    "A user with email '" + request.getEmail() + "' already exists");
        }

        // Generate new user ID
        String newUserId = UUID.randomUUID().toString();

        // Save user (this would typically involve more complex domain logic)
        userRepository.save(newUserId, request.getUsername(), request.getEmail());

        // Publish domain event
        eventPublisher.publishUserCreated(newUserId, request.getUsername(), request.getEmail());

        log.info("User created successfully with ID: {}", newUserId);

        return CreateUserResponse.newBuilder()
                .setId(newUserId)
                .setStatusMessage("User '" + request.getUsername() + "' created successfully.")
                .build();
    }
}