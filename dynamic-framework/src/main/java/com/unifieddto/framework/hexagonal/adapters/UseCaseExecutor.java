package com.unifieddto.framework.hexagonal.adapters;

import com.unifieddto.framework.hexagonal.domain.UseCase;
import com.unifieddto.framework.hexagonal.domain.BusinessException;
import com.unifieddto.framework.hexagonal.domain.ValidationException;
import com.unifieddto.framework.hexagonal.ports.InputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic executor for use cases that handles validation and error management.
 * This class acts as an adapter between the communication layer and the business logic,
 * ensuring proper validation and error handling.
 */
public class UseCaseExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(UseCaseExecutor.class);
    
    private final InputValidator validator;
    
    public UseCaseExecutor(InputValidator validator) {
        this.validator = validator;
    }
    
    /**
     * Executes a use case with proper validation and error handling.
     * 
     * @param useCase The use case to execute
     * @param request The request data
     * @param <REQUEST> The request type
     * @param <RESPONSE> The response type
     * @return The response from the use case
     * @throws BusinessException if business logic fails
     * @throws ValidationException if input validation fails
     */
    public <REQUEST, RESPONSE> RESPONSE execute(UseCase<REQUEST, RESPONSE> useCase, REQUEST request) 
            throws BusinessException, ValidationException {
        
        log.debug("Executing use case: {} with request type: {}", 
                useCase.getClass().getSimpleName(), request.getClass().getSimpleName());
        
        // Validate input
        validator.validate(request);
        
        // Execute use case
        try {
            RESPONSE response = useCase.execute(request);
            log.debug("Use case executed successfully: {}", useCase.getClass().getSimpleName());
            return response;
        } catch (BusinessException e) {
            log.warn("Business exception in use case {}: {}", useCase.getClass().getSimpleName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in use case {}", useCase.getClass().getSimpleName(), e);
            throw new BusinessException("INTERNAL_ERROR", "An unexpected error occurred", e);
        }
    }
}