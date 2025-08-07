package com.unifieddto.userservice;

import com.unifieddto.api.user.GetUserRequest;
import com.unifieddto.api.user.User;
import com.unifieddto.api.user.UserServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    @Override
    public void getUser(GetUserRequest request, StreamObserver<User> responseObserver) {
        // In a real application, you would fetch the user from a database.
        // For this example, we'll return a hardcoded user.
        User user = User.newBuilder()
                .setId(request.getId())
                .setUsername("testuser")
                .setEmail("testuser@example.com")
                .build();

        responseObserver.onNext(user);
        responseObserver.onCompleted();
    }
}
