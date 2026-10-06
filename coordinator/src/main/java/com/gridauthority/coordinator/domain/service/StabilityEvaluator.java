package com.gridauthority.coordinator.domain.service;

import com.gridauthority.coordinator.application.dto.StabilityThreshold;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Getter
public class StabilityEvaluator {

    // Proactive instability threshold (fixed, not user-configurable)
    // 10% Coefficient of Variation (CV) over short sliding window (default: 10 seconds)
    // Chosen conservatively to trigger protection early — before a sustained violation of
    // EN 50160 limits (±10% of nominal on 10-minute rms averages for 95% of a week).
    private static final double THRESHOLD_CV = 10.0;

    @Value("${stability.stable-cycles-required}")
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