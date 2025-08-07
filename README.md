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

1.  **Interface Layer**: These are the components that handle communication with the outside world. They are responsible for protocol-specific tasks (like handling HTTP requests, gRPC calls, or consuming messages) and translating them into calls to the business logic layer.
    -   `UserRestController` (REST)
    -   `UserServiceImpl` (gRPC)
    -   `KafkaUserConsumer` (Kafka)
    -   `RabbitUserConsumer` (RabbitMQ)

2.  **Business Logic Layer**: This is the core of the service, where the actual business rules and operations reside. It is completely decoupled from the communication style.
    -   `UserBusinessService`: A single, centralized service that contains the core logic. All components in the interface layer delegate their calls to this service.

This design ensures that the business logic is written only once and can be reused by any number of interfaces.

## How it Works

1.  **DTO Definition**: The `User` DTO is defined once in `api/src/main/proto/user.proto`.
2.  **Code Generation**: When you build the project with Maven, the `protobuf-maven-plugin` compiles `user.proto` into Java classes (e.g., `User`, `GetUserRequest`, `UserServiceGrpc`) and packages them into a JAR file for the `api` module.
3.  **gRPC Implementation**: The `UserServiceImpl` in the `user-service` module directly implements the generated `UserServiceGrpc.UserServiceImplBase`, using the `User` and `GetUserRequest` classes natively.
4.  **REST Implementation**: The `UserRestController` uses the exact same `User` class as a return type for its endpoints. Spring Boot, thanks to the `com.google.protobuf:protobuf-java-util` dependency, automatically configures a `ProtobufHttpMessageConverter` that serializes the `User` object into JSON for the HTTP response.
5.  **Messaging (Kafka/RabbitMQ)**: Although not fully implemented, the same `User` object can be easily used for messaging. You would serialize it to a byte array for production (`user.toByteArray()`) and deserialize it from a byte array upon consumption (`User.parseFrom(byteArray)`). This is far more efficient than JSON-based serialization.

## How to Run

### 1. Build the Project

First, build the entire project from the root directory. This will compile the `.proto` files and install the artifacts into your local Maven repository.

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

You can now test the various endpoints, which all trigger the same underlying business logic.

#### A) Testing the REST Interface

Open a new terminal.

**1. Get User:**
Send a `POST` request with a `GetUserRequest` body.
```bash
curl -X POST http://localhost:8080/api/users/get \
-H "Content-Type: application/json" \
-d '{"id": "123"}'
```
*Response:*
```json
{
  "id": "123",
  "username": "command-pattern-user",
  "email": "command.user@example.com"
}
```

**2. Create User (and trigger messaging):**
Send a `POST` request with a `CreateUserRequest` body. This endpoint will also trigger the Kafka and RabbitMQ producers.
```bash
curl -X POST http://localhost:8080/api/users/create \
-H "Content-Type: application/json" \
-d '{"username": "new-rest-user", "email": "new-rest@example.com"}'
```
*Response (the user ID will be random):*
```json
{
  "id": "c7a8f2e9-a3b4-4c1d-8e6f-ac72a8d3e5d7",
  "statusMessage": "User 'new-rest-user' created successfully."
}
```

#### B) Testing the gRPC Interface

You can use a tool like `grpcurl`.

**1. List Methods:**
The `UserService` now exposes two methods.
```bash
grpcurl -plaintext localhost:9090 list com.unifieddto.api.user.UserService
```
*Response:*
```
com.unifieddto.api.user.UserService.CreateUser
com.unifieddto.api.user.UserService.GetUser
```

**2. Get User:**
```bash
grpcurl -plaintext -d '{"id": "456"}' \
localhost:9090 com.unifieddto.api.user.UserService/GetUser
```
*Response:*
```json
{
  "id": "456",
  "username": "command-pattern-user",
  "email": "command.user@example.com"
}
```

**3. Create User:**
```bash
grpcurl -plaintext -d '{"username": "new-grpc-user", "email": "new-grpc@example.com"}' \
localhost:9090 com.unifieddto.api.user.UserService/CreateUser
```
*Response (the user ID will be random):*
```json
{
  "id": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "statusMessage": "User 'new-grpc-user' created successfully."
}
```

#### C) Verifying Messaging Consumption

If you run the "Create User" command via either the REST or gRPC endpoint, it will publish a `CreateUserRequest` message to both Kafka and RabbitMQ.

**Prerequisites:** You need running instances of Kafka and RabbitMQ. You can use the Docker commands from the previous version of this README.

**Check the Logs:**
Look at the logs of the running `user-service`. You will see the output from the consumers, showing they received the command and passed it to the business service.

*Example Log Output:*
```
# From the REST call to /api/users/create
... INFO c.u.u.s.UserBusinessService : Executing business logic for: CreateUserRequest with username new-rest-user
... INFO c.u.u.m.KafkaUserProducer   : Producing Kafka message for CreateUserRequest: new-rest-user
... INFO c.u.u.m.RabbitUserProducer  : Producing RabbitMQ message for CreateUserRequest: new-rest-user

# From the consumers processing the messages
... INFO c.u.u.m.KafkaUserConsumer   : Consumed Kafka message for CreateUserRequest: new-rest-user
... INFO c.u.u.s.UserBusinessService : Executing business logic for: CreateUserRequest with username new-rest-user
... INFO c.u.u.m.RabbitUserConsumer  : Consumed RabbitMQ message for CreateUserRequest: new-rest-user
... INFO c.u.u.s.UserBusinessService : Executing business logic for: CreateUserRequest with username new-rest-user
```
Notice how the same business logic (`Executing business logic for: CreateUserRequest...`) is triggered by three different interface points: the initial REST call, the Kafka consumer, and the RabbitMQ consumer. This successfully demonstrates the desired architecture.
