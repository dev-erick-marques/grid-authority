package com.gridauthority.device.aplication.dto;

import com.gridauthority.device.domain.model.DeviceCommand;

public record DeviceCommandDTO(
        String deviceId,
        DeviceCommand command
) {}