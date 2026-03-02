package com.gridauthority.device.aplication.service;

import com.gridauthority.device.aplication.dto.DeviceTelemetryDTO;
import com.gridauthority.device.domain.model.DeviceState;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class VoltageSimulatorService {

    private final DeviceSimulationProperties properties;
    private final Random random = new Random();

    public DeviceTelemetryDTO generate(DeviceState state) {
        double voltage = properties.getVoltage().getBase()
                + (random.nextDouble() * 2 - 1) * properties.getVoltage().getVariation();

        return new DeviceTelemetryDTO(
                properties.getId(),
                properties.getName(),
                voltage,
                state,
                Instant.now(),
                "sha256:config-" + properties.getId()
        );
    }
}