package com.gridauthority.coordinator.application.dto;

import com.gridauthority.coordinator.domain.model.DeviceState;
import com.gridauthority.coordinator.domain.model.DeviceSurgeState;

import java.time.Instant;

public record DeviceMetricsDTO(
        String deviceId,
        String deviceName,
        double mean,
        double std,
        double cv,
        DeviceState state,
        DeviceSurgeState surgeState,
        Instant evaluatedAt
) {}