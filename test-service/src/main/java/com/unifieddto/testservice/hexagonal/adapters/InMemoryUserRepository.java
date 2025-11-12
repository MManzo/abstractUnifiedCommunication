package com.unifieddto.testservice.hexagonal.adapters;

import com.unifieddto.api.user.User;
import com.unifieddto.testservice.hexagonal.ports.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of UserRepository for testing and demonstration purposes.
 * In a production system, this would be replaced with a database adapter.
 */
@Repository
public class InMemoryUserRepository implements UserRepository {
    
    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> usernameToId = new ConcurrentHashMap<>();
    private final Map<String, String> emailToId = new ConcurrentHashMap<>();
    
    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        String id = usernameToId.get(username);
        return id != null ? Optional.ofNullable(usersById.get(id)) : Optional.empty();
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        String id = emailToId.get(email);
        return id != null ? Optional.ofNullable(usersById.get(id)) : Optional.empty();
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return usernameToId.containsKey(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return emailToId.containsKey(email);
    }
    
    @Override
    public void save(String id, String username, String email) {
        User user = User.newBuilder()
                .setId(id)
                .setUsername(username)
                .setEmail(email)
                .build();
        
        usersById.put(id, user);
        usernameToId.put(username, id);
        emailToId.put(email, id);
    }
    
    @Override
    public void update(User user) {
        if (usersById.containsKey(user.getId())) {
            // Remove old mappings
            User oldUser = usersById.get(user.getId());
            usernameToId.remove(oldUser.getUsername());
            emailToId.remove(oldUser.getEmail());
            
            // Add new mappings
            usersById.put(user.getId(), user);
            usernameToId.put(user.getUsername(), user.getId());
            emailToId.put(user.getEmail(), user.getId());
        }
    }
    
    @Override
    public boolean deleteById(String id) {
        User user = usersById.remove(id);
        if (user != null) {
            usernameToId.remove(user.getUsername());
            emailToId.remove(user.getEmail());
            return true;
        }
        return false;
    }
    
    /**
     * Utility method to get the current number of users (for testing).
     */
    public int size() {
        return usersById.size();
    }
    
    /**
     * Utility method to clear all users (for testing).
     */
    public void clear() {
        usersById.clear();
        usernameToId.clear();
        emailToId.clear();
    }
}