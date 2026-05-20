package com.microservicesfinal.dto.requests;

import com.microservicesfinal.model.enums.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatchAlertRequest {

    private AlertStatus status;
}
