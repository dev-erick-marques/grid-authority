package com.gridauthority.device.infrastructure.transport;

import com.gridauthority.device.aplication.dto.DeviceTelemetryDTO;

public interface TelemetryTransport {
    void dispatch(DeviceTelemetryDTO telemetry);
}