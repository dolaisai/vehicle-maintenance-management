package com.microservicesfinal.service;

import com.microservicesfinal.dto.requests.CreateUserRequest;
import com.microservicesfinal.dto.requests.PatchUserRequest;
import com.microservicesfinal.dto.responses.UserResponse;
import com.microservicesfinal.model.User;

import java.util.List;

public interface UserService {

    //GET
    UserResponse getUserById(Integer userId);
    List<UserResponse> getAllUsers();

    //POST
    UserResponse saveUser(CreateUserRequest createUserRequest);

    //PUT-PATCH
    UserResponse patchUser(PatchUserRequest patchUserRequest, Integer id);

    //DELETE
    void deleteUserById(Integer userId);
}
