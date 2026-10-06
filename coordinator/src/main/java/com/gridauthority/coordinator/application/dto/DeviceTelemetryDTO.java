package com.gridauthority.coordinator.application.dto;

import com.gridauthority.coordinator.domain.model.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record DeviceTelemetryDTO(
        @NotBlank(message = "deviceId must not be blank")
        String deviceId,

        @NotBlank(message = "deviceName must not be blank")
        String deviceName,

        @Positive(message = "voltage must be positive")
        double voltage,

        @NotNull(message = "status must not be null")
        DeviceState status,

        @NotNull(message = "timestamp must not be null")
        Instant timestamp,

        @NotNull(message = "sourceUrl must not be null")
        String sourceUrl
) {}