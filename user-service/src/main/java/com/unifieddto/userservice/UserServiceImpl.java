package com.unifieddto.userservice;

import com.unifieddto.api.user.GetUserRequest;
import com.unifieddto.api.user.User;
import com.unifieddto.api.user.UserServiceGrpc;
import com.unifieddto.userservice.service.UserBusinessService;
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
        User user = userBusinessService.getUserById(request.getId());

        responseObserver.onNext(user);
        responseObserver.onCompleted();
    }
}
