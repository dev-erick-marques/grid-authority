package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.application.dto.DeviceTelemetryDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import com.gridauthority.coordinator.domain.service.VoltageStatisticsService;
import com.gridauthority.coordinator.infrastructure.repository.MetricsHistoryRepository;
import com.gridauthority.coordinator.infrastructure.repository.VoltageWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class TelemetryIngestionService {

    private final VoltageWindowRepository voltageWindowRepository;
    private final VoltageStatisticsService voltageStatisticsService;
    private final MetricsHistoryRepository metricsHistoryRepository;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public void registerEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
    }

    public void ingest(DeviceTelemetryDTO telemetry) {
        voltageWindowRepository.recordAndGet(telemetry)
                .ifPresent(window -> processWindow(telemetry, window));
    }

    private void processWindow(DeviceTelemetryDTO telemetry, double[] window) {
        VoltageStatisticsService.VoltageStats stats = voltageStatisticsService.compute(window);

        DeviceMetricsDTO metrics = new DeviceMetricsDTO(
                telemetry.deviceId(),
                telemetry.deviceName(),
                stats.mean(),
                stats.std(),
                stats.cv(),
                DeviceState.ACTIVE,
                DeviceCommand.KEEP_RUNNING,
                telemetry.timestamp()
        );
        metricsHistoryRepository.add(metrics);
        broadcast(metrics);
    }

    private void broadcast(DeviceMetricsDTO metrics) {
        List<SseEmitter> dead = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("metrics")
                        .data(metrics));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}