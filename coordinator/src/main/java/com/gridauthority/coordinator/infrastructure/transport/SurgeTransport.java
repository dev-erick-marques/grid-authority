package com.gridauthority.coordinator.infrastructure.transport;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;

public interface SurgeTransport {
    void send(String deviceBaseUrl, String deviceId, String action, SignedCommandPayload payload);
}