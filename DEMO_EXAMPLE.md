# Demo Example: Hexagonal Architecture in Action

## Overview

This example demonstrates how the same business logic can be exposed through multiple communication protocols without any changes to the core business logic.

## Scenario: User Management

We have a simple user management system with two operations:
1. **Create User** - Creates a new user with username and email
2. **Get User** - Retrieves a user by ID

## Architecture Components

### 1. Domain Layer (Business Logic)

```java
// Pure business logic - protocol agnostic
@Component
public class CreateUserUseCase implements UseCase<CreateUserRequest, CreateUserResponse> {
    
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    
    @Override
    public CreateUserResponse execute(CreateUserRequest request) throws BusinessException {
        // Business validation
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USER_ALREADY_EXISTS", "Username already exists");
        }
        
        // Business logic
        String userId = UUID.randomUUID().toString();
        userRepository.save(userId, request.getUsername(), request.getEmail());
        eventPublisher.publishUserCreated(userId, request.getUsername(), request.getEmail());
        
        return CreateUserResponse.newBuilder()
                .setId(userId)
                .setStatusMessage("User created successfully")
                .build();
    }
}
```

### 2. Ports (Interfaces)

```java
// Primary port - what the application can do
public interface UseCase<REQUEST, RESPONSE> {
    RESPONSE execute(REQUEST request) throws BusinessException;
}

// Secondary ports - what the application needs
public interface UserRepository {
    void save(String id, String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findById(String id);
}

public interface EventPublisher {
    void publishUserCreated(String userId, String username, String email);
}
```

### 3. Adapters (Infrastructure)

```java
// Secondary adapter - implements what the application needs
@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    @Override
    public void save(String id, String username, String email) {
        User user = User.newBuilder()
                .setId(id)
                .setUsername(username)
                .setEmail(email)
                .build();
        users.put(id, user);
    }
    
    // ... other methods
}

// Primary adapters - expose the application to the outside world
@Component
public class HexagonalRestControllerRegistrar {
    // Automatically exposes use cases as REST endpoints
}

@Component  
public class HexagonalRabbitListenerRegistrar {
    // Automatically exposes use cases as RabbitMQ listeners
}
```

## Multi-Protocol Exposure

### Configuration

```yaml
# application.yml
dynamic:
  rest:
    endpoints:
      create-user:
        path: "/users"
        http-method: POST
        dto-class-name: "com.unifieddto.api.user.CreateUserRequest"
        
  rabbitmq:
    bindings:
      create-user:
        queue-name: "user.create"
        exchange-name: "user.events"
        routing-key: "user.create"
        dto-class-name: "com.unifieddto.api.user.CreateUserRequest"
```

### Result

The same `CreateUserUseCase` business logic is now automatically exposed via:

1. **REST API**: `POST /users`
   ```bash
   curl -X POST http://localhost:8080/users \
     -H "Content-Type: application/json" \
     -d '{"username": "john", "email": "john@example.com"}'
   ```

2. **RabbitMQ**: Messages on `user.create` queue
   ```bash
   # Publish message to RabbitMQ
   rabbitmqadmin publish exchange=user.events routing_key=user.create \
     payload='{"username": "john", "email": "john@example.com"}'
   ```

3. **Direct calls**: For testing or internal usage
   ```java
   @Test
   void testCreateUser() throws BusinessException {
       CreateUserRequest request = CreateUserRequest.newBuilder()
           .setUsername("john")
           .setEmail("john@example.com")
           .build();
           
       CreateUserResponse response = createUserUseCase.execute(request);
       
       assertNotNull(response);
       assertEquals("User created successfully", response.getStatusMessage());
   }
   ```

## Key Benefits Demonstrated

### 1. Protocol Independence
- The business logic (`CreateUserUseCase`) has **zero knowledge** of HTTP, RabbitMQ, or any other protocol
- The same logic works identically across all communication channels
- Adding new protocols (gRPC, Kafka, WebSockets) requires **zero changes** to business logic

### 2. Easy Testing
- Business logic can be tested in complete isolation
- No need for HTTP servers, message brokers, or external dependencies
- Fast, reliable unit tests

### 3. Loose Coupling
- Business logic depends only on interfaces (`UserRepository`, `EventPublisher`)
- Infrastructure can be swapped without affecting business rules
- Database technology, messaging system, etc. are implementation details

### 4. Configuration-Driven Exposure
- New communication channels can be added through configuration
- No code changes required to expose existing use cases via new protocols
- Framework handles all the protocol-specific concerns automatically

## Comparison: Before vs After

### Before (Tightly Coupled)
```java
@RestController
public class UserController {
    
    @PostMapping("/users")
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
        // Business logic mixed with HTTP concerns
        // Hard to test, hard to reuse for other protocols
        // Violates single responsibility principle
    }
}

@RabbitListener(queues = "user.create")
public class UserRabbitListener {
    
    public void handleCreateUser(CreateUserRequest request) {
        // Duplicate business logic
        // Maintenance nightmare
        // Inconsistent behavior across protocols
    }
}
```

### After (Hexagonal Architecture)
```java
// Single source of truth for business logic
@Component
public class CreateUserUseCase implements UseCase<CreateUserRequest, CreateUserResponse> {
    // Pure business logic - protocol agnostic
    // Easy to test, maintain, and extend
    // Consistent behavior across all protocols
}

// Configuration-driven exposure
dynamic:
  rest:
    endpoints:
      create-user: { path: "/users", http-method: POST, ... }
  rabbitmq:
    bindings:
      create-user: { queue-name: "user.create", ... }
```

## Running the Demo

1. **Start the application**:
   ```bash
   mvn spring-boot:run -pl test-service
   ```

2. **Test via REST**:
   ```bash
   curl -X POST http://localhost:8080/users \
     -H "Content-Type: application/json" \
     -d '{"username": "demo", "email": "demo@example.com"}'
   ```

3. **Test via RabbitMQ** (if RabbitMQ is running):
   ```bash
   # The same business logic will be triggered
   rabbitmqadmin publish exchange=user.events routing_key=user.create \
     payload='{"username": "demo", "email": "demo@example.com"}'
   ```

4. **Run unit tests**:
   ```bash
   mvn test
   ```

The same business logic executes identically across all three scenarios, demonstrating the power of hexagonal architecture for protocol-agnostic business logic.