package com.microservicesfinal.repository;

import com.microservicesfinal.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepo extends JpaRepository<Vehicle, Integer> {
    List<Vehicle> findAllByUser_Id(Integer userId);
}
