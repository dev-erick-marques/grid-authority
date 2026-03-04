package com.gridauthority.coordinator.application.dto;

import com.gridauthority.coordinator.domain.model.DeviceState;
import java.time.Instant;

public record DeviceTelemetryDTO(
        String deviceId,
        String deviceName,
        double voltage,
        DeviceState status,
        Instant timestamp,
        String sourceUrl,
        String deviceConfigHash

) {}