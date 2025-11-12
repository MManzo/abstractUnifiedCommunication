package com.unifieddto.framework.hexagonal.domain;

/**
 * Marker interface for all use cases in the hexagonal architecture.
 * Use cases represent the primary ports of the hexagon - the entry points
 * for business logic that are independent of any specific technology or framework.
 * 
 * @param <REQUEST> The input type for the use case
 * @param <RESPONSE> The output type for the use case
 */
public interface UseCase<REQUEST, RESPONSE> {
    
    /**
     * Executes the use case with the given request.
     * 
     * @param request The input data for the use case
     * @return The result of executing the use case
     * @throws BusinessException if business rules are violated
     */
    RESPONSE execute(REQUEST request) throws BusinessException;
}