package com.gridauthority.device.aplication.service;

import com.gridauthority.device.aplication.dto.DeviceTelemetryDTO;
import com.gridauthority.device.domain.model.DeviceState;
import com.gridauthority.device.domain.model.GridVoltageModel;
import com.gridauthority.device.infrastructure.config.DeviceNetworkProperties;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VoltageSimulatorService {

    private final DeviceSimulationProperties properties;
    private final DeviceNetworkProperties networkProperties;
    private final GridVoltageModel gridVoltageModel;
    private long lastNanos = -1;

    public DeviceTelemetryDTO generate(DeviceState state) {
        double voltage = gridVoltageModel.next(elapsedSeconds());

        return new DeviceTelemetryDTO(
                properties.getId(),
                properties.getName(),
                voltage,
                state,
                Instant.now(),
                networkProperties.getSourceUrl()
        );
    }

    /** Real time since the previous sample, so the model stays correct even if the scheduler jitters. */
    private synchronized double elapsedSeconds() {
        long now = System.nanoTime();
        double dt = lastNanos < 0
                ? properties.getSchedule().getMs() / 1000.0
                : (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        return dt;
    }
}
