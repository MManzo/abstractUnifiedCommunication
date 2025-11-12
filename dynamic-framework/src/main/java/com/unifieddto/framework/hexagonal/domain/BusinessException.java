package com.unifieddto.framework.hexagonal.domain;

/**
 * Base exception for all business logic violations.
 * This exception is part of the domain layer and should not depend on any framework.
 */
public class BusinessException extends Exception {
    
    private final String errorCode;
    private final Object[] parameters;
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.parameters = new Object[0];
    }
    
    public BusinessException(String errorCode, String message, Object... parameters) {
        super(message);
        this.errorCode = errorCode;
        this.parameters = parameters != null ? parameters : new Object[0];
    }
    
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.parameters = new Object[0];
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object[] getParameters() {
        return parameters;
    }
}