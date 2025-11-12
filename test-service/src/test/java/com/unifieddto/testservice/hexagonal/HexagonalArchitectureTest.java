package com.unifieddto.testservice.hexagonal;

import com.unifieddto.api.user.CreateUserRequest;
import com.unifieddto.api.user.CreateUserResponse;
import com.unifieddto.api.user.GetUserRequest;
import com.unifieddto.api.user.User;
import com.unifieddto.framework.hexagonal.adapters.UseCaseExecutor;
import com.unifieddto.framework.hexagonal.domain.BusinessException;
import com.unifieddto.framework.hexagonal.domain.ValidationException;
import com.unifieddto.testservice.hexagonal.adapters.InMemoryUserRepository;
import com.unifieddto.testservice.hexagonal.adapters.RabbitMQEventPublisher;
import com.unifieddto.testservice.hexagonal.usecases.CreateUserUseCase;
import com.unifieddto.testservice.hexagonal.usecases.GetUserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * Test class demonstrating the hexagonal architecture in action.
 * This test shows how business logic is completely isolated from communication protocols.
 */
@ExtendWith(MockitoExtension.class)
class HexagonalArchitectureTest {

    private InMemoryUserRepository userRepository;
    
    @Mock
    private RabbitMQEventPublisher eventPublisher;
    
    private CreateUserUseCase createUserUseCase;
    private GetUserUseCase getUserUseCase;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        createUserUseCase = new CreateUserUseCase(userRepository, eventPublisher);
        getUserUseCase = new GetUserUseCase(userRepository);
    }

    @Test
    void testCreateUserUseCase_Success() throws BusinessException {
        // Given
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail("test@example.com")
                .build();

        // When
        CreateUserResponse response = createUserUseCase.execute(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("User 'testuser' created successfully.", response.getStatusMessage());
        
        // Verify the user was saved
        assertEquals(1, userRepository.size());
        assertTrue(userRepository.existsByUsername("testuser"));
        assertTrue(userRepository.existsByEmail("test@example.com"));
        
        // Verify event was published
        verify(eventPublisher).publishUserCreated(anyString(), anyString(), anyString());
    }

    @Test
    void testCreateUserUseCase_DuplicateUsername() {
        // Given
        CreateUserRequest firstRequest = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail("test1@example.com")
                .build();
        
        CreateUserRequest secondRequest = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail("test2@example.com")
                .build();

        // When & Then
        assertDoesNotThrow(() -> createUserUseCase.execute(firstRequest));
        
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> createUserUseCase.execute(secondRequest));
        
        assertEquals("USER_ALREADY_EXISTS", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("testuser"));
    }

    @Test
    void testGetUserUseCase_Success() throws BusinessException {
        // Given - Create a user first
        CreateUserRequest createRequest = CreateUserRequest.newBuilder()
                .setUsername("testuser")
                .setEmail("test@example.com")
                .build();
        CreateUserResponse createResponse = createUserUseCase.execute(createRequest);
        
        GetUserRequest getRequest = GetUserRequest.newBuilder()
                .setId(createResponse.getId())
                .build();

        // When
        User user = getUserUseCase.execute(getRequest);

        // Then
        assertNotNull(user);
        assertEquals(createResponse.getId(), user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    void testGetUserUseCase_UserNotFound() {
        // Given
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId("non-existent-id")
                .build();

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> getUserUseCase.execute(request));
        
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("non-existent-id"));
    }

    @Test
    void testGetUserUseCase_EmptyId() {
        // Given
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId("")
                .build();

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> getUserUseCase.execute(request));
        
        assertEquals("INVALID_USER_ID", exception.getErrorCode());
    }

    /**
     * This test demonstrates the key benefit of hexagonal architecture:
     * The same business logic can be tested without any knowledge of
     * HTTP, RabbitMQ, or any other communication protocol.
     */
    @Test
    void testBusinessLogicIsProtocolAgnostic() throws BusinessException {
        // The use cases work with pure domain objects (Protobuf messages)
        // They have no knowledge of:
        // - HTTP requests/responses
        // - RabbitMQ messages
        // - gRPC calls
        // - Database specifics
        // - Spring framework details
        
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setUsername("protocol-agnostic-user")
                .setEmail("agnostic@example.com")
                .build();

        CreateUserResponse response = createUserUseCase.execute(request);
        
        // This same business logic can be exposed via:
        // 1. REST API (via HexagonalRestControllerRegistrar)
        // 2. RabbitMQ (via HexagonalRabbitListenerRegistrar)  
        // 3. gRPC (via a future HexagonalGrpcServiceRegistrar)
        // 4. Direct method calls (as in this test)
        // 5. Any other communication protocol
        
        assertNotNull(response);
        assertTrue(response.getStatusMessage().contains("protocol-agnostic-user"));
    }
}