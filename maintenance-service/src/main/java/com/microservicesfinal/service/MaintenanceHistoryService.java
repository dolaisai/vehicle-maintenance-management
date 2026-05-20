package com.microservicesfinal.service;

import com.microservicesfinal.dto.requests.CreateMaintenanceHistoryRequest;
import com.microservicesfinal.dto.responses.MaintenanceHistoryResponse;

import java.util.List;

public interface MaintenanceHistoryService {

    //GET
    public MaintenanceHistoryResponse getMaintenanceHistory(Integer maintenanceHistoryId);
    public List<MaintenanceHistoryResponse> getAllMaintenanceHistory();

    //vehicle specific
    public List<MaintenanceHistoryResponse> getAllRecentMaintenanceHistoryForVehicleWithId(Integer vehicleId);
    public List<MaintenanceHistoryResponse> getAllMaintenanceHistoryByVehicleId(Integer vehicleId);

    //POST
    public MaintenanceHistoryResponse saveMaintenanceHistory(CreateMaintenanceHistoryRequest createMaintenanceHistoryRequest);

    //NOTE: No updates since history will act as internal logging and rule eval reference data
    //NOTE: No deletes as well for the same reason. (Add admin data cleanup in the future for larger scale project)

}
