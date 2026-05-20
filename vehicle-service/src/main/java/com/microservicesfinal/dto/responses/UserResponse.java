package com.microservicesfinal.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class UserResponse {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
}
