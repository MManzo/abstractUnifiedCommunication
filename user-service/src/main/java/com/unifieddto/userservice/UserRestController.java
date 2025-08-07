package com.unifieddto.userservice;

import com.unifieddto.api.user.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        // In a real application, you would fetch the user from a service layer.
        // For this example, we'll return a hardcoded user, similar to the gRPC service.
        return User.newBuilder()
                .setId(id)
                .setUsername("testuser-rest")
                .setEmail("testuser-rest@example.com")
                .build();
    }
}
