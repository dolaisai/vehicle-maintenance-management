package com.microservicesfinal.controller;

import com.microservicesfinal.service.RuleEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(
        origins = "http://localhost:3000",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
@RestController
@RequestMapping("/api/rule-eval")
@RequiredArgsConstructor
public class RuleEvaluationController {

    private final RuleEvaluationService ruleEvaluationService;

    //sync vehicle with specific ID passed in with URI
    @GetMapping("/{vehicleId}")
    public void syneOne(@PathVariable Integer vehicleId){
             ruleEvaluationService.syncOne(vehicleId);
    }

    //start sync process for ALL vehicles saved within the system.
    @GetMapping
    public void syncAll(){
        ruleEvaluationService.syncAll();
    }
}
