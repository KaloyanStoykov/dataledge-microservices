package org.dataledge.userapi.controller;

import org.dataledge.userapi.data.UserEntity;
import org.dataledge.userapi.data.UserRepository;
import org.dataledge.userapi.dto.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;


    @GetMapping()
    public UserResponse getUser() {
        Optional<UserEntity> userRetrieved = userRepository.findById(1);
        return userRetrieved.map(userEntity -> new UserResponse(userEntity.getFirstName())).orElseThrow();
    }
}
