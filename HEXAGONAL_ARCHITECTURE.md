# Hexagonal Architecture Implementation

## Overview

This document describes the improved hexagonal architecture implementation that addresses the separation of concerns between business logic and communication protocols.

## Architecture Principles

### 1. Business Logic Independence
- **Use Cases** contain pure business logic without any knowledge of communication protocols
- **Domain Objects** (Protobuf messages) represent the core data structures
- **Business Exceptions** handle domain-specific errors

### 2. Port and Adapter Pattern
- **Primary Ports** (UseCase interface) define what the application can do
- **Secondary Ports** (Repository, EventPublisher interfaces) define what the application needs
- **Adapters** implement the ports and handle protocol-specific concerns

### 3. Dependency Inversion
- Business logic depends only on abstractions (interfaces)
- Infrastructure adapters depend on business logic interfaces
- Framework components are isolated from business rules

## Key Components

### Domain Layer (`com.unifieddto.framework.hexagonal.domain`)

```java
// Primary port - defines what the application can do
public interface UseCase<REQUEST, RESPONSE> {
    RESPONSE execute(REQUEST request) throws BusinessException;
}

// Domain exceptions
public class BusinessException extends Exception { ... }
public class ValidationException extends BusinessException { ... }
```

### Ports Layer (`com.unifieddto.framework.hexagonal.ports`)

```java
// Secondary port - defines what the application needs
public interface InputValidator {
    void validate(Object input) throws ValidationException;
}
```

### Adapters Layer (`com.unifieddto.framework.hexagonal.adapters`)

- **UseCaseExecutor**: Orchestrates validation and use case execution
- **UseCaseRegistry**: Discovers and manages use case instances
- **HexagonalRestControllerRegistrar**: Bridges HTTP to use cases
- **HexagonalRabbitListenerRegistrar**: Bridges RabbitMQ to use cases
- **ProtobufInputValidator**: Validates Protocol Buffer messages

## Benefits Achieved

### 1. Protocol Agnostic Business Logic

The same business logic can be exposed through multiple protocols:

```java
@Component
public class CreateUserUseCase implements UseCase<CreateUserRequest, CreateUserResponse> {
    
    @Override
    public CreateUserResponse execute(CreateUserRequest request) throws BusinessException {
        // Pure business logic - no knowledge of HTTP, RabbitMQ, gRPC, etc.
        // ...
    }
}
```

This use case can be automatically exposed via:
- **REST API**: `POST /users` 
- **RabbitMQ**: Messages on `user.create` queue
- **gRPC**: `CreateUser` service method
- **Direct calls**: For testing or internal usage

### 2. Loose Coupling

Business logic depends only on interfaces:

```java
public class CreateUserUseCase implements UseCase<CreateUserRequest, CreateUserResponse> {
    
    private final UserRepository userRepository;        // Interface, not implementation
    private final EventPublisher eventPublisher;       // Interface, not implementation
    
    // Business logic is completely decoupled from:
    // - Database technology (MySQL, PostgreSQL, MongoDB, etc.)
    // - Messaging system (RabbitMQ, Kafka, etc.)
    // - Communication protocol (REST, gRPC, messaging)
}
```

### 3. Easy Testing

Business logic can be tested in isolation:

```java
@Test
void testCreateUser() throws BusinessException {
    // Mock the dependencies
    UserRepository mockRepo = mock(UserRepository.class);
    EventPublisher mockPublisher = mock(EventPublisher.class);
    
    // Test pure business logic
    CreateUserUseCase useCase = new CreateUserUseCase(mockRepo, mockPublisher);
    CreateUserResponse response = useCase.execute(request);
    
    // No need for HTTP servers, message brokers, or databases
}
```

### 4. Dynamic Protocol Support

The framework automatically discovers use cases and exposes them through configured protocols:

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

## Implementation Example

### 1. Define Use Case

```java
@Component
public class CreateUserUseCase implements UseCase<CreateUserRequest, CreateUserResponse> {
    
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    
    public CreateUserUseCase(UserRepository userRepository, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }
    
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

### 2. Implement Secondary Ports

```java
@Repository
public class InMemoryUserRepository implements UserRepository {
    // Implementation details hidden from business logic
}

@Component  
public class RabbitMQEventPublisher implements EventPublisher {
    // Implementation details hidden from business logic
}
```

### 3. Configure Protocols

The framework automatically:
1. Discovers the `CreateUserUseCase`
2. Registers it for `CreateUserRequest` messages
3. Exposes it via configured protocols (REST, RabbitMQ, etc.)
4. Handles validation, error management, and response formatting

## Migration from Old Architecture

### Before (Tightly Coupled)
```java
@RestController
public class UserController {
    
    @PostMapping("/users")
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
        // Business logic mixed with HTTP concerns
        // Hard to test, hard to reuse for other protocols
    }
}
```

### After (Hexagonal Architecture)
```java
// Business logic (protocol-agnostic)
@Component
public class CreateUserUseCase implements UseCase<CreateUserRequest, CreateUserResponse> {
    // Pure business logic
}

// Configuration (declarative)
dynamic:
  rest:
    endpoints:
      create-user:
        path: "/users"
        http-method: POST
        dto-class-name: "com.unifieddto.api.user.CreateUserRequest"
```

## Testing Strategy

### Unit Tests
Test use cases in isolation with mocked dependencies:

```java
@Test
void testCreateUserSuccess() throws BusinessException {
    // Given
    UserRepository mockRepo = mock(UserRepository.class);
    EventPublisher mockPublisher = mock(EventPublisher.class);
    CreateUserUseCase useCase = new CreateUserUseCase(mockRepo, mockPublisher);
    
    // When
    CreateUserResponse response = useCase.execute(request);
    
    // Then
    assertNotNull(response);
    verify(mockRepo).save(anyString(), eq("username"), eq("email"));
    verify(mockPublisher).publishUserCreated(anyString(), eq("username"), eq("email"));
}
```

### Integration Tests
Test the complete flow through specific adapters:

```java
@SpringBootTest
@TestPropertySource(properties = {
    "dynamic.rest.endpoints.create-user.path=/users",
    "dynamic.rest.endpoints.create-user.http-method=POST"
})
class RestIntegrationTest {
    
    @Test
    void testCreateUserViaRest() {
        // Test HTTP -> UseCase -> Repository flow
    }
}
```

## Conclusion

This hexagonal architecture implementation provides:

1. **True separation** between business logic and communication protocols
2. **Easy multi-protocol support** for the same business functionality  
3. **Improved testability** through dependency inversion
4. **Better maintainability** through clear architectural boundaries
5. **Framework flexibility** while preserving business logic integrity

The business logic is now completely protocol-agnostic and can be easily exposed through any communication mechanism without modification.