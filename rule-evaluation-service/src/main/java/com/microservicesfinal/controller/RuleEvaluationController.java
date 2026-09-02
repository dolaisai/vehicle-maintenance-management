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

    //base comment
    @GetMapping("/{vehicleId}")
    public void syneOne(@PathVariable Integer vehicleId){
             ruleEvaluationService.syncOne(vehicleId);
    }

    //base comment
    @GetMapping
    public void syncAll(){
        ruleEvaluationService.syncAll();
    }
}
