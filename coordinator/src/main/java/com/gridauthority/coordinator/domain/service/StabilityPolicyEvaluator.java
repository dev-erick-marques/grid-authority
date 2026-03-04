package com.gridauthority.coordinator.domain.service;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StabilityPolicyEvaluator {

    @Value("${policy.threshold-cv:12.5}")
    private double thresholdCv;

    @Value("${policy.stable-cycles-required:10}")
    private int stableCyclesRequired;

    public DeviceCommand evaluate(double cv, DeviceState currentStatus, int stableCycles) {
        if (cv > thresholdCv) {
            return DeviceCommand.SHUTDOWN;
        }

        if (currentStatus == DeviceState.SHUTDOWN && stableCycles >= stableCyclesRequired) {
            return DeviceCommand.RESTART;
        }

        return DeviceCommand.KEEP_RUNNING;
    }
}