package com.gridauthority.device.aplication.service;

import com.gridauthority.device.aplication.dto.DeviceTelemetryDTO;
import com.gridauthority.device.domain.model.DeviceState;
import com.gridauthority.device.infrastructure.config.DeviceNetworkProperties;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class VoltageSimulatorService {

    private final DeviceSimulationProperties properties;
    private final DeviceNetworkProperties networkProperties;
    private final Random random = new Random();
    private final SurgeModeService surgeModeService;

    public DeviceTelemetryDTO generate(DeviceState state) {
        double voltage = generateVoltage();

        return new DeviceTelemetryDTO(
                properties.getId(),
                properties.getName(),
                voltage,
                state,
                Instant.now(),
                networkProperties.getSourceUrl()
        );
    }

    private double generateVoltage() {
        double base = properties.getVoltage().getBase();
        double variation = surgeModeService.isSurgeActive()
                ? properties.getVoltage().getSurgeVariation()
                : properties.getVoltage().getVariation();

        return base + (random.nextDouble() * 2 - 1) * variation;
    }
}