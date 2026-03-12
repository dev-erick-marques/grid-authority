package com.gridauthority.coordinator.api;

import com.gridauthority.coordinator.application.dto.StabilityThreshold;
import com.gridauthority.coordinator.domain.service.StabilityEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StabilityController {

    private final StabilityEvaluator stabilityEvaluator;

    @GetMapping("/stability/threshold")
    public StabilityThreshold Threshold() {
        return stabilityEvaluator.getThreshold();
    }

}