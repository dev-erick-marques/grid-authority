package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.application.dto.DeviceTelemetryDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.service.StabilityPolicyEvaluator;
import com.gridauthority.coordinator.domain.service.VoltageStatisticsService;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import com.gridauthority.coordinator.infrastructure.repository.MetricsHistoryRepository;
import com.gridauthority.coordinator.infrastructure.repository.StableCycleTracker;
import com.gridauthority.coordinator.infrastructure.repository.VoltageWindowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryIngestionService {

    private final VoltageWindowRepository voltageWindowRepository;
    private final VoltageStatisticsService voltageStatisticsService;
    private final StabilityPolicyEvaluator stabilityPolicyEvaluator;
    private final StableCycleTracker stableCycleTracker;
    private final DeviceRegistry deviceRegistry;
    private final MetricsHistoryRepository metricsHistoryRepository;
    private final DeviceCommandDispatcher dispatcher;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final ConcurrentHashMap<String, Object> deviceLocks = new ConcurrentHashMap<>();

    public void registerEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
    }

    public void ingest(DeviceTelemetryDTO telemetry) {
        if (telemetry.sourceUrl() != null && !telemetry.sourceUrl().isBlank()) {
            deviceRegistry.register(telemetry.deviceId(), telemetry.sourceUrl());
        }

        voltageWindowRepository
                .recordAndGet(telemetry)
                .ifPresent(window -> processWindow(telemetry, window));
    }

    private void processWindow(DeviceTelemetryDTO telemetry, double[] window) {
        VoltageStatisticsService.VoltageStats stats = voltageStatisticsService.compute(window);

        Object lock = deviceLocks.computeIfAbsent(telemetry.deviceId(), k -> new Object());

        DeviceMetricsDTO metrics;

        synchronized (lock) {
            int stableCycles = stableCycleTracker.getStableCycles(telemetry.deviceId());

            DeviceCommand command = stabilityPolicyEvaluator.evaluate(
                    stats.cv(),
                    telemetry.status(),
                    stableCycles
            );

            metrics = new DeviceMetricsDTO(
                    telemetry.deviceId(),
                    telemetry.deviceName(),
                    stats.mean(),
                    stats.std(),
                    stats.cv(),
                    telemetry.status(),
                    telemetry.timestamp()
            );
            metricsHistoryRepository.add(metrics);
            stableCycleTracker.record(metrics.deviceId(), command);
            dispatcher.dispatch(metrics, command);
        }

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