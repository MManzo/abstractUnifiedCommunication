package com.unifieddto.testservice.hexagonal.adapters;

import com.unifieddto.testservice.hexagonal.ports.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of EventPublisher.
 * This adapter publishes domain events to RabbitMQ exchanges.
 */
@Component
public class RabbitMQEventPublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventPublisher.class);
    
    private final RabbitTemplate rabbitTemplate;
    
    // Exchange names for different event types
    private static final String USER_EVENTS_EXCHANGE = "user.events";
    
    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    @Override
    public void publishUserCreated(String userId, String username, String email) {
        String routingKey = "user.created";
        String message = String.format(
                "{\"eventType\":\"USER_CREATED\",\"userId\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"timestamp\":\"%s\"}",
                userId, username, email, java.time.Instant.now().toString()
        );
        
        try {
            rabbitTemplate.convertAndSend(USER_EVENTS_EXCHANGE, routingKey, message);
            log.info("Published USER_CREATED event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish USER_CREATED event for user: {}", userId, e);
            // In a production system, you might want to implement retry logic or dead letter queues
        }
    }
    
    @Override
    public void publishUserUpdated(String userId, String username, String email) {
        String routingKey = "user.updated";
        String message = String.format(
                "{\"eventType\":\"USER_UPDATED\",\"userId\":\"%s\",\"username\":\"%s\",\"email\":\"%s\",\"timestamp\":\"%s\"}",
                userId, username, email, java.time.Instant.now().toString()
        );
        
        try {
            rabbitTemplate.convertAndSend(USER_EVENTS_EXCHANGE, routingKey, message);
            log.info("Published USER_UPDATED event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish USER_UPDATED event for user: {}", userId, e);
        }
    }
    
    @Override
    public void publishUserDeleted(String userId) {
        String routingKey = "user.deleted";
        String message = String.format(
                "{\"eventType\":\"USER_DELETED\",\"userId\":\"%s\",\"timestamp\":\"%s\"}",
                userId, java.time.Instant.now().toString()
        );
        
        try {
            rabbitTemplate.convertAndSend(USER_EVENTS_EXCHANGE, routingKey, message);
            log.info("Published USER_DELETED event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish USER_DELETED event for user: {}", userId, e);
        }
    }
}