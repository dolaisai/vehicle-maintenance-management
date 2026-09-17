package com.microservicesfinal.dto;

import com.microservicesfinal.dto.requests.CreateUserRequest;
import com.microservicesfinal.dto.requests.CreateVehicleRequest;
import com.microservicesfinal.model.User;
import com.microservicesfinal.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Fixture for the CI pipeline spike (SCRUM-10).
 *
 * These tests give the pipeline something to gate on. They need no Spring
 * context, no database and no discovery server, so a red build here means a
 * test failed and nothing else. They are not test coverage for this service.
 */
class DtoConversionTest {

    @Test
    @DisplayName("mapDTOtoUser trims and lowercases the email")
    //test to see if email normalizer works as intended. Should make any email all lowercase.
    void mapDTOtoUserNormalizesEmail() {
        CreateUserRequest request = CreateUserRequest.builder()
                .firstName("Isaiah")
                .lastName("Dolan")
                .email("  ISAIAH@Example.COM  ")
                .phoneNumber("555-0100")
                .build();

        User user = DtoConversion.mapDTOtoUser(request);

        assertEquals("isaiah@example.com", user.getEmail());
        assertEquals("Isaiah", user.getFirstName());
    }

    @Test
    @DisplayName("mapDTOtoVehicle trims and lowercases make and model")
    void mapDTOtoVehicleNormalizesMakeAndModel() {
        CreateVehicleRequest request = CreateVehicleRequest.builder()
                .make("  Toyota ")
                .model(" Camry  ")
                .mileage(42000)
                .maintenanceTemplateId(3)
                .build();

        Vehicle vehicle = DtoConversion.mapDTOtoVehicle(request);

        assertEquals("toyota", vehicle.getMake());
        assertEquals("camry", vehicle.getModel());
        assertEquals(42000, vehicle.getMileage());
    }
}
