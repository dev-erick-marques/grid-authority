package com.gridauthority.coordinator.api;

import com.gridauthority.coordinator.domain.service.StabilityPolicyEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PolicyController {

    private final StabilityPolicyEvaluator policyEvaluator;

    @GetMapping("/policy")
    public StabilityPolicyEvaluator.SurgePolicyResponse getPolicy() {
        return policyEvaluator.getPolicy();
    }

}