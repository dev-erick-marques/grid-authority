package com.gridauthority.device.aplication.dto;

public record DeviceCommandDTO(
        String deviceId,
        String command  // "SHUTDOWN" | "RESTART"
) {}