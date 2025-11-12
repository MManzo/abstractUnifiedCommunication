package com.unifieddto.testservice.hexagonal.usecases;

import com.unifieddto.api.user.GetUserRequest;
import com.unifieddto.api.user.User;
import com.unifieddto.framework.hexagonal.domain.BusinessException;
import com.unifieddto.framework.hexagonal.domain.UseCase;
import com.unifieddto.testservice.hexagonal.ports.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Use case for retrieving a user by ID.
 * This class contains pure business logic without any dependencies on frameworks or protocols.
 */
@Component
public class GetUserUseCase implements UseCase<GetUserRequest, User> {

    private static final Logger log = LoggerFactory.getLogger(GetUserUseCase.class);

    private final UserRepository userRepository;

    public GetUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User execute(GetUserRequest request) throws BusinessException {
        log.info("Executing GetUserUseCase for user ID: {}", request.getId());

        // Business rule: ID must not be empty
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            throw new BusinessException("INVALID_USER_ID", "User ID cannot be empty");
        }

        // Retrieve user from repository
        Optional<User> user = userRepository.findById(request.getId());

        if (user.isEmpty()) {
            throw new BusinessException("USER_NOT_FOUND", 
                    "User with ID '" + request.getId() + "' not found");
        }

        log.info("User retrieved successfully: {}", user.get().getUsername());
        return user.get();
    }
}