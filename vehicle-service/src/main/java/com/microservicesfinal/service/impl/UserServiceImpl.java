package com.microservicesfinal.service.impl;

import com.microservicesfinal.dto.DtoConversion;
import com.microservicesfinal.dto.requests.CreateUserRequest;
import com.microservicesfinal.dto.requests.PatchUserRequest;
import com.microservicesfinal.dto.responses.UserResponse;
import com.microservicesfinal.exceptions.ResourceNotFoundException;
import com.microservicesfinal.model.User;
import com.microservicesfinal.repository.UserRepo;
import com.microservicesfinal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;

    @Override
    public UserResponse getUserById(Integer userId) {
        log.info("fetching User with id: {}", userId);
        User fetchedUser = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user with ID: " + userId));
        return DtoConversion.mapUsertoDTO(fetchedUser);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        log.info("fetching ALL Users");
        List<User> users = userRepo.findAll();
        return users.stream().map(DtoConversion::mapUsertoDTO).toList();
    }

    @Override
    public UserResponse saveUser(CreateUserRequest createUserRequest) {
        log.info("Creating new user");
        User user = DtoConversion.mapDTOtoUser(createUserRequest);
        return DtoConversion.mapUsertoDTO(userRepo.save(user));
    }

    @Override
    public UserResponse patchUser(PatchUserRequest patchUserRequest, Integer id) {

        log.info("Fetching user with id: {} for potential patch", id);
        User fetchedUser = userRepo.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("user with ID: " + id));

        log.info("Patching user with id: {}", id);
        if(patchUserRequest.getFirstName() != null) {
            fetchedUser.setFirstName(patchUserRequest.getFirstName());
        }
        if(patchUserRequest.getLastName() != null) {
            fetchedUser.setLastName(patchUserRequest.getLastName());
        }
        if(patchUserRequest.getEmail() != null) {
            fetchedUser.setEmail(patchUserRequest.getEmail());
        }
        if(patchUserRequest.getPhoneNumber() != null){
            fetchedUser.setPhoneNumber(patchUserRequest.getPhoneNumber());
        }

        return DtoConversion.mapUsertoDTO(userRepo.save(fetchedUser));
    }

    @Override
    public void deleteUserById(Integer userId) {
        log.info("Deleting user with id: {}", userId);
        try {userRepo.deleteById(userId);}
        catch(EmptyResultDataAccessException exception){
            throw new ResourceNotFoundException("user with ID: " + userId);
        }
    }
}
