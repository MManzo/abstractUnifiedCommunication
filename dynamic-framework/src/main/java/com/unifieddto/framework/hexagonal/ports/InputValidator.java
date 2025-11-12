package com.unifieddto.framework.hexagonal.ports;

import com.unifieddto.framework.hexagonal.domain.ValidationException;

/**
 * Secondary port for input validation.
 * This interface defines the contract for validating input data
 * without depending on any specific validation framework.
 */
public interface InputValidator {
    
    /**
     * Validates the given input object.
     * 
     * @param input The object to validate
     * @throws ValidationException if validation fails
     */
    void validate(Object input) throws ValidationException;
    
    /**
     * Validates the given input object and returns whether it's valid.
     * 
     * @param input The object to validate
     * @return true if valid, false otherwise
     */
    boolean isValid(Object input);
}