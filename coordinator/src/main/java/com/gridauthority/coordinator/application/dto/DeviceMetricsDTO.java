package com.gridauthority.coordinator.application.dto;

import com.gridauthority.coordinator.domain.model.DeviceState;
import com.gridauthority.coordinator.domain.model.DeviceSurgeState;
import java.time.Instant;

public record DeviceMetricsDTO(
    String deviceId, String deviceName, double mean, double std, double cv,
    DeviceState state, DeviceSurgeState surgeState, Instant evaluatedAt,
    double trend, double acceleration, double baselineDeviation,
    double forecast, double riskScore, double confidence,
    Double timeToThreshold, double observabilityScore, String action, long policyVersion
) {}
