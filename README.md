# Unified DTO Microservice Architecture

This project is a proof-of-concept demonstrating how to use a single DTO definition across multiple communication protocols (REST, gRPC, and messaging platforms like Kafka/RabbitMQ) in a Java microservices environment.

The core principle is to use **Protocol Buffers (Protobuf)** as the Interface Definition Language (IDL) and the single source of truth for all data contracts.

## Architecture

The project is structured as a multi-module Maven project:

-   `unified-dto-parent`: The parent POM that manages common dependencies and build configurations.
-   `api`: This is the **contract module**. It contains only `.proto` files that define our DTOs (as `message`s) and gRPC services. The `protobuf-maven-plugin` is configured here to generate Java classes from the `.proto` files.
-   `user-service`: A Spring Boot microservice that **consumes** the `api` module as a dependency. It exposes both a gRPC endpoint and a RESTful endpoint, both of which use the same DTO classes generated from the `api` module.

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

You can now test both endpoints using standard tools.

#### Testing the REST Endpoint

Open a new terminal and use `curl` to send a request to the REST API:

```bash
curl http://localhost:8080/api/users/123
```

You should receive a JSON response like this:
```json
{
  "id": "123",
  "username": "testuser-rest",
  "email": "testuser-rest@example.com"
}
```

#### Testing the gRPC Endpoint

You can use a tool like `grpcurl` to test the gRPC endpoint. First, list the available services (this works because we enabled gRPC reflection).

```bash
# List services
grpcurl -plaintext localhost:9090 list

# List methods for our service
grpcurl -plaintext localhost:9090 list com.unifieddto.api.user.UserService
```

Now, call the `GetUser` method:

```bash
# Call the GetUser method
grpcurl -plaintext -d '{"id": "456"}' localhost:9090 com.unifieddto.api.user.UserService/GetUser
```

You should receive a response like this:
```json
{
  "id": "456",
  "username": "testuser",
  "email": "testuser@example.com"
}
```

You have now successfully called two different services (REST and gRPC) that use the exact same DTO definition.
