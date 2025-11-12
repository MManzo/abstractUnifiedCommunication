package com.unifieddto.framework.hexagonal.adapters;

import com.google.protobuf.Message;
import com.unifieddto.framework.hexagonal.domain.ValidationException;
import com.unifieddto.framework.hexagonal.ports.InputValidator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of InputValidator for Protocol Buffer messages.
 * This adapter provides validation logic specific to protobuf messages
 * while implementing the framework-agnostic InputValidator interface.
 */
@Component
public class ProtobufInputValidator implements InputValidator {
    
    @Override
    public void validate(Object input) throws ValidationException {
        if (input == null) {
            throw new ValidationException("input", "Input cannot be null");
        }
        
        if (!(input instanceof Message)) {
            throw new ValidationException("input", "Input must be a Protocol Buffer message");
        }
        
        Message message = (Message) input;
        List<ValidationException.ValidationError> errors = new ArrayList<>();
        
        // Perform basic protobuf validation
        validateRequiredFields(message, errors);
        validateStringFields(message, errors);
        
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
    
    @Override
    public boolean isValid(Object input) {
        try {
            validate(input);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }
    
    private void validateRequiredFields(Message message, List<ValidationException.ValidationError> errors) {
        // Basic validation - in a real implementation, you might use protobuf field options
        // or custom annotations to define required fields
        message.getAllFields().forEach((field, value) -> {
            if (field.isRequired() && (value == null || (value instanceof String && ((String) value).trim().isEmpty()))) {
                errors.add(new ValidationException.ValidationError(field.getName(), "Field is required"));
            }
        });
    }
    
    private void validateStringFields(Message message, List<ValidationException.ValidationError> errors) {
        message.getAllFields().forEach((field, value) -> {
            if (value instanceof String) {
                String stringValue = (String) value;
                
                // Example validation rules
                if (field.getName().contains("email") && !isValidEmail(stringValue)) {
                    errors.add(new ValidationException.ValidationError(field.getName(), "Invalid email format"));
                }
                
                if (field.getName().contains("username") && stringValue.length() < 3) {
                    errors.add(new ValidationException.ValidationError(field.getName(), "Username must be at least 3 characters"));
                }
            }
        });
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}