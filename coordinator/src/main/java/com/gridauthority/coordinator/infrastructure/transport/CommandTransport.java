package com.gridauthority.coordinator.infrastructure.transport;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceCommand;

public interface CommandTransport {
    void send(String deviceBaseUrl, String deviceId, DeviceCommand command, SignedCommandPayload payload);
}