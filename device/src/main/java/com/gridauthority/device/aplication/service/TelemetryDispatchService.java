package com.gridauthority.device.aplication.service;

import com.gridauthority.device.aplication.dto.DeviceTelemetryDTO;
import com.gridauthority.device.infrastructure.transport.TelemetryTransport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelemetryDispatchService {

    private final TelemetryTransport telemetryTransport;
    private final VoltageSimulatorService voltageSimulatorService;
    private final DeviceStateService deviceStateService;

    public void dispatch() {
        DeviceTelemetryDTO telemetry = voltageSimulatorService.generate(deviceStateService.current());
        telemetryTransport.dispatch(telemetry);
    }
}