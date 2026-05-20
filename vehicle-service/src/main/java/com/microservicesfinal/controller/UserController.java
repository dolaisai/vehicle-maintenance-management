package com.microservicesfinal.controller;

import com.microservicesfinal.dto.requests.CreateUserRequest;
import com.microservicesfinal.dto.requests.PatchUserRequest;
import com.microservicesfinal.dto.responses.UserResponse;
import com.microservicesfinal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(
        origins = "http://localhost:3000",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PostMapping
    public ResponseEntity<UserResponse> saveUser(@RequestBody CreateUserRequest createUserRequest) {
        return new ResponseEntity<>(
                userService.saveUser(createUserRequest),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @RequestBody PatchUserRequest patchUserRequest,
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(userService.patchUser(patchUserRequest, userId));
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteUserById(@PathVariable Integer userId){
        userService.deleteUserById(userId);
    }

}
