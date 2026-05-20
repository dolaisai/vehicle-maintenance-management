package com.microservicesfinal.service;

import com.microservicesfinal.dto.requests.CreateAlertRequest;
import com.microservicesfinal.dto.requests.PatchAlertRequest;
import com.microservicesfinal.dto.responses.AlertResponse;

import java.util.List;

public interface AlertService {
    //GET
    AlertResponse getAlert(Integer AlertId);
    List<AlertResponse> getAllAlerts();
    List<AlertResponse> getAllActiveAlerts();

    //POST
    AlertResponse saveAlert(CreateAlertRequest createAlertRequest);

    //PUT-PATCH
    AlertResponse patchAlert(PatchAlertRequest patchAlertRequest, Integer alertId);

    //DELETE
    void deleteAlert(Integer alertId);
}
