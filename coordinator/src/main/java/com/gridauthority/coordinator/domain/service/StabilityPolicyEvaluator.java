package com.gridauthority.coordinator.domain.service;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StabilityPolicyEvaluator {

    // EN 50160: ±10% nominal voltage variation limit.
    // Normative constant — not configurable by design.
    private static final double THRESHOLD_CV = 10.0;

    @Value("${policy.stable-cycles-required:10}")
    private int stableCyclesRequired;

    public DeviceCommand evaluate(double cv, DeviceState currentStatus, int stableCycles) {
        if (cv > THRESHOLD_CV) {
            return DeviceCommand.SHUTDOWN;
        }

        if (currentStatus == DeviceState.SHUTDOWN && stableCycles >= stableCyclesRequired) {
            return DeviceCommand.RESTART;
        }

        return DeviceCommand.KEEP_RUNNING;
    }
    public SurgePolicyResponse getPolicy() {
        return new SurgePolicyResponse(
                THRESHOLD_CV,
                "EN 50160",
                "Maximum acceptable CV before shutdown is triggered"
        );
    }
    public record SurgePolicyResponse(
            double thresholdCV,
            String standard,
            String description
    ) {}
}