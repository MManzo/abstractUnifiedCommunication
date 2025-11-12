package com.unifieddto.framework.hexagonal.domain;

import java.util.List;
import java.util.ArrayList;

/**
 * Exception thrown when input validation fails.
 * Contains detailed information about validation errors.
 */
public class ValidationException extends BusinessException {
    
    private final List<ValidationError> validationErrors;
    
    public ValidationException(List<ValidationError> validationErrors) {
        super("VALIDATION_ERROR", "Input validation failed");
        this.validationErrors = new ArrayList<>(validationErrors);
    }
    
    public ValidationException(String field, String message) {
        super("VALIDATION_ERROR", "Input validation failed");
        this.validationErrors = List.of(new ValidationError(field, message));
    }
    
    public List<ValidationError> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
    
    public static class ValidationError {
        private final String field;
        private final String message;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return field + ": " + message;
        }
    }
}