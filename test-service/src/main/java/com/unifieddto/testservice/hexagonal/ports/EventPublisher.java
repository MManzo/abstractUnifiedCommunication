package com.unifieddto.testservice.hexagonal.ports;

/**
 * Secondary port for publishing domain events.
 * This interface defines the contract for event publishing
 * without depending on any specific messaging technology.
 */
public interface EventPublisher {
    
    /**
     * Publishes a user created event.
     * 
     * @param userId The ID of the created user
     * @param username The username of the created user
     * @param email The email of the created user
     */
    void publishUserCreated(String userId, String username, String email);
    
    /**
     * Publishes a user updated event.
     * 
     * @param userId The ID of the updated user
     * @param username The username of the updated user
     * @param email The email of the updated user
     */
    void publishUserUpdated(String userId, String username, String email);
    
    /**
     * Publishes a user deleted event.
     * 
     * @param userId The ID of the deleted user
     */
    void publishUserDeleted(String userId);
}