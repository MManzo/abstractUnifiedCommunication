package com.unifieddto.testservice.hexagonal.ports;

import com.unifieddto.api.user.User;

import java.util.Optional;

/**
 * Secondary port for user data persistence.
 * This interface defines the contract for user data operations
 * without depending on any specific persistence technology.
 */
public interface UserRepository {
    
    /**
     * Finds a user by their ID.
     * 
     * @param id The user ID
     * @return An optional containing the user if found
     */
    Optional<User> findById(String id);
    
    /**
     * Finds a user by their username.
     * 
     * @param username The username
     * @return An optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Finds a user by their email.
     * 
     * @param email The email address
     * @return An optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Checks if a user exists with the given username.
     * 
     * @param username The username to check
     * @return true if a user exists with this username
     */
    boolean existsByUsername(String username);
    
    /**
     * Checks if a user exists with the given email.
     * 
     * @param email The email to check
     * @return true if a user exists with this email
     */
    boolean existsByEmail(String email);
    
    /**
     * Saves a new user.
     * 
     * @param id The user ID
     * @param username The username
     * @param email The email address
     */
    void save(String id, String username, String email);
    
    /**
     * Updates an existing user.
     * 
     * @param user The user to update
     */
    void update(User user);
    
    /**
     * Deletes a user by ID.
     * 
     * @param id The user ID
     * @return true if the user was deleted, false if not found
     */
    boolean deleteById(String id);
}