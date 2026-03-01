package com.gridauthority.device.infrastructure.scheduler;

import com.gridauthority.device.aplication.service.TelemetryDispatchService;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryScheduler {

    private final TelemetryDispatchService telemetryDispatchService;
    private final DeviceSimulationProperties properties;
    private final TaskScheduler taskScheduler;

    @PostConstruct
    public void start() {
        taskScheduler.scheduleAtFixedRate(
                this::tick,
                Duration.ofMillis(properties.getSchedule().getMs())
        );
    }

    private void tick() {
        try {
            telemetryDispatchService.dispatch();
        } catch (Exception e) {
            log.error("Failed to dispatch telemetry: {}", e.getMessage());
        }
    }
}