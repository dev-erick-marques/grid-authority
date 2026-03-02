package com.gridauthority.device.aplication.service;

import com.gridauthority.device.aplication.dto.DeviceTelemetryDTO;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TelemetryDispatchService {

    private final DeviceSimulationProperties properties;
    private final VoltageSimulatorService voltageSimulatorService;
    private final DeviceStateService deviceStateService;
    private final RestClient restClient;

    public void dispatch() {
        DeviceTelemetryDTO telemetry = voltageSimulatorService.generate(deviceStateService.current());

        restClient.post()
                .uri(properties.getCoordinator().getUrl() + "/api/devices/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .body(telemetry)
                .retrieve()
                .toBodilessEntity();
    }
}