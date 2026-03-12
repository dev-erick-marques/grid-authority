package com.gridauthority.coordinator.domain.service;

import com.gridauthority.coordinator.application.dto.StabilityThreshold;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StabilityEvaluator {

    // EN 50160: ±10% nominal voltage variation limit.
    // Normative constant — not configurable by design.
    private static final double THRESHOLD_CV = 10.0;

    @Value("${stability.stable-cycles-required:10}")
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
    public StabilityThreshold getThreshold() {
        return new StabilityThreshold(
                THRESHOLD_CV,
                "EN 50160",
                "Maximum acceptable CV before shutdown is triggered"
        );
    }
}