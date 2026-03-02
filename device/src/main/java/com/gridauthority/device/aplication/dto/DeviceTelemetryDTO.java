package com.gridauthority.device.aplication.dto;

import com.gridauthority.device.domain.model.DeviceState;

import java.time.Instant;

public record DeviceTelemetryDTO(
        String deviceId,
        String deviceName,
        double voltage,
        DeviceState status,
        Instant timestamp,
        String deviceConfigHash
) {}