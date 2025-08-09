# Unified DTO Microservice Architecture

This project is a proof-of-concept demonstrating how to use a single DTO definition across multiple communication protocols (REST, gRPC, and messaging platforms like Kafka/RabbitMQ) in a Java microservices environment.

The core principle is to use **Protocol Buffers (Protobuf)** as the Interface Definition Language (IDL) and the single source of truth for all data contracts.

## Architecture

The project is structured as a multi-module Maven project and follows a clean, layered architecture within the `user-service`.

### Project Modules

-   `unified-dto-parent`: The parent POM that manages common dependencies and build configurations.
-   `api`: This is the **contract module**. It contains only `.proto` files that define our DTOs (as `message`s) and gRPC services. The `protobuf-maven-plugin` is configured here to generate Java classes from the `.proto` files.
-   `user-service`: A Spring Boot microservice that **consumes** the `api` module as a dependency. It exposes both a gRPC endpoint and a RESTful endpoint, both of which use the same DTO classes generated from the `api` module.

### Layered Architecture in `user-service`

To ensure a clean separation of concerns, the `user-service` is structured into layers:

1.  **Dynamic Interface Layer**: This layer is responsible for handling communication with the outside world. It consists of generic, reusable "registrar" components that dynamically create endpoints based on configuration. This avoids writing boilerplate controller and consumer classes for each business action.
    -   `DynamicRestControllerRegistrar`: Creates REST endpoints.
    -   `DynamicRabbitListenerRegistrar`: Creates RabbitMQ listeners.
    -   `UserServiceImpl` (gRPC): gRPC remains explicit due to its contract-first nature.
    -   `KafkaUserConsumer` (Kafka): The Kafka consumer remains explicit for this demo, but could be made dynamic using the same pattern as RabbitMQ.

2.  **Business Logic Layer**: This is the core of the service.
    -   `UserBusinessService`: A single, centralized service that contains the core logic, with overloaded `execute` methods for different commands.

This design provides a powerful, configuration-driven approach to exposing business logic, drastically reducing boilerplate code.

## How it Works: The Dynamic Framework

The key to this architecture is the dynamic registration of endpoints at application startup.

1.  **Configuration as Blueprint**: The `application.properties` file now defines the API surface. It contains sections for `app.rabbitmq.bindings.*` and `app.rest.endpoints.*` that map queues and HTTP paths to specific business services and DTOs.
2.  **Registrars as Engines**: At startup, the `DynamicRabbitListenerRegistrar` and `DynamicRestControllerRegistrar` read this configuration.
3.  **Programmatic Registration**:
    -   The RabbitMQ registrar programmatically creates and registers a `MessageListenerContainer` for each binding, wiring it to a generic handler.
    -   The REST registrar programmatically registers a handler method with Spring's `RequestMappingHandlerMapping` for each endpoint.
4.  **Generic Handlers**: These handlers contain the reusable logic.
    -   The RabbitMQ handler deserializes the message to the configured DTO type and calls the business service.
    -   The REST handler is more complex, dynamically populating the DTO from path variables, query parameters, headers, and the request body based on the configuration before calling the business service.

This creates a system where new business logic can be exposed over REST and RabbitMQ simply by adding configuration entries, without writing any new controller or consumer Java code.

## How to Run

### 1. Build the Project

First, build the entire project. This is unchanged.

```bash
mvn clean install
```

### 2. Run the User Service

Navigate to the `user-service` directory and run the Spring Boot application.

```bash
cd user-service
mvn spring-boot:run
```

The service will start up.
-   The REST API will be available on port `8080`.
-   The gRPC server will be available on port `9090`.

### 3. Test the Endpoints

You can now test the dynamically and explicitly configured endpoints.

#### A) Testing the Dynamic REST Interface

The following endpoints are created dynamically from `application.properties`.

**1. Get User (dynamic):**
This endpoint demonstrates mapping a path variable to a DTO field.
```bash
curl http://localhost:8080/api/v2/users/789
```
*Response:*
```json
{
  "id": "789",
  "username": "command-pattern-user",
  "email": "command.user@example.com"
}
```

**2. Create User (dynamic):**
This endpoint takes the DTO from the request body. Note that this call will also trigger the RabbitMQ and Kafka producers.
```bash
curl -X POST http://localhost:8080/api/v2/users \
-H "Content-Type: application/json" \
-d '{"username": "new-dynamic-rest-user", "email": "dynamic@example.com"}'
```
*Response (ID is random):*
```json
{
  "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "statusMessage": "User 'new-dynamic-rest-user' created successfully."
}
```

#### B) Testing the gRPC Interface (Explicit)

The gRPC interface remains explicitly defined for type safety and clarity. The commands are unchanged.

**1. Get User:**
```bash
grpcurl -plaintext -d '{"id": "456"}' \
localhost:9090 com.unifieddto.api.user.UserService/GetUser
```

**2. Create User:**
```bash
grpcurl -plaintext -d '{"username": "new-grpc-user", "email": "new-grpc@example.com"}' \
localhost:9090 com.unifieddto.api.user.UserService/CreateUser
```

#### C) Verifying Messaging Consumption

**Prerequisites:** You need running instances of Kafka and RabbitMQ.

**1. Trigger a Message:**
Call the "Create User" REST or gRPC endpoint. This will publish a `CreateUserRequest` message to both Kafka and RabbitMQ.

**2. Check the Logs:**
Look at the logs of the running `user-service`. You will see output from both the dynamic RabbitMQ consumer and the explicit Kafka consumer.

*Example Log Output:*
```
# From the REST call to /api/v2/users
... INFO c.u.u.s.UserBusinessService      : Executing business logic for: CreateUserRequest with username new-dynamic-rest-user
... INFO c.u.u.m.KafkaUserProducer        : Producing Kafka message for CreateUserRequest: new-dynamic-rest-user
... INFO c.u.u.m.RabbitUserProducer       : Producing RabbitMQ message for CreateUserRequest: new-dynamic-rest-user

# From the consumers processing the messages
... INFO c.u.u.m.KafkaUserConsumer        : Consumed Kafka message for CreateUserRequest: new-dynamic-rest-user
... INFO c.u.u.s.UserBusinessService      : Executing business logic for: CreateUserRequest with username new-dynamic-rest-user
... INFO m.d.GenericMessageHandler        : Generic handler received message of type CreateUserRequest, invoking UserBusinessService.execute()
... INFO c.u.u.s.UserBusinessService      : Executing business logic for: CreateUserRequest with username new-dynamic-rest-user
```
Notice how the `GenericMessageHandler` now handles the RabbitMQ message, while the explicit `KafkaUserConsumer` handles the other. Both ultimately call the same business logic, successfully demonstrating the power and flexibility of the new architecture.
