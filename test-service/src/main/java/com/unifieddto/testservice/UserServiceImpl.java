package com.unifieddto.testservice;

import com.unifieddto.api.user.*;
import com.unifieddto.testservice.service.UserBusinessService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    @Autowired
    private UserBusinessService userBusinessService;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<User> responseObserver) {
        // Delegate the call to the business logic layer
        User response = userBusinessService.execute(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        // Delegate the call to the business logic layer
        CreateUserResponse response = userBusinessService.execute(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
