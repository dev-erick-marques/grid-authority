package com.gridauthority.coordinator.application.dto;

import java.time.Instant;

public record DeviceMetricsDTO(
        String deviceId,
        String deviceName,
        double mean,
        double std,
        double cv,
        boolean shutdown,
        String decision,
        Instant evaluatedAt
) {}