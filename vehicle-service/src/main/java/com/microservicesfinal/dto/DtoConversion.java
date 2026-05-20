package com.microservicesfinal.dto;

import com.microservicesfinal.dto.requests.CreateUserRequest;
import com.microservicesfinal.dto.requests.CreateVehicleRequest;
import com.microservicesfinal.dto.responses.UserResponse;
import com.microservicesfinal.dto.responses.VehicleResponse;
import com.microservicesfinal.model.User;
import com.microservicesfinal.model.Vehicle;

public class DtoConversion {

    //vehicle <-> CreateVehicleRequest
    public static Vehicle mapDTOtoVehicle(CreateVehicleRequest request){
        Vehicle vehicle = new Vehicle();
        vehicle.setMake(request.getMake().trim().toLowerCase());
        vehicle.setModel(request.getModel().trim().toLowerCase());
        vehicle.setMileage(request.getMileage());
        vehicle.setMaintenanceTemplateId(request.getMaintenanceTemplateId());
        //NOTE: set user outside of conversion class!
        return vehicle;
    }

    public static VehicleResponse mapVehicletoDTO(Vehicle vehicle){
        return VehicleResponse.builder()
                 .vehicleId(vehicle.getId())
                 .make(vehicle.getMake())
                 .model(vehicle.getModel())
                 .mileage(vehicle.getMileage())
                 .maintenanceTemplateId(vehicle.getMaintenanceTemplateId())
                 .userId(vehicle.getUser().getId())
                 .createdAt(vehicle.getCreatedAt())
                 .build();
    }

    //User <-> CreateUserRequest
    public static User mapDTOtoUser(CreateUserRequest request){
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhoneNumber(request.getPhoneNumber());

        return user;
    }

    public static UserResponse mapUsertoDTO(User user){
        return UserResponse.builder()
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName((user.getLastName()))
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
