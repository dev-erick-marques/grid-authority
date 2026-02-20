package com.gridauthority.coordinator.application.dto;

import java.time.Instant;

public record DeviceTelemetryDTO(
        String deviceId,
        String deviceName,
        double voltage,
        Instant timestamp,
        String deviceConfigHash

) {}