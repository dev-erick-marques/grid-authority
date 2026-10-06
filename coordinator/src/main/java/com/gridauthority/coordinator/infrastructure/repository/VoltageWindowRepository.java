package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.application.dto.DeviceTelemetryDTO;
import com.gridauthority.coordinator.observability.ObservationWindow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class VoltageWindowRepository {
    @Value("${voltage.repository.window.size:30}")
    private int windowSize;

    @Value("${telemetry.sample-interval-ms:1000}")
    private long expectedIntervalMs;

    private final ConcurrentHashMap<String, ArrayDeque<Sample>> windows = new ConcurrentHashMap<>();

    public Optional<ObservationWindow> recordAndGet(DeviceTelemetryDTO dto) {
        ArrayDeque<Sample> window = windows.computeIfAbsent(dto.deviceId(), k -> new ArrayDeque<>(windowSize));
        synchronized (window) {
            if (window.size() == windowSize) window.removeFirst();
            window.addLast(new Sample(dto.voltage(), dto.timestamp()));
            if (window.size() < windowSize) return Optional.empty();

            Sample first = window.peekFirst();
            Sample latest = window.peekLast();
            long spanMs = Math.max(0, Duration.between(first.timestamp(), latest.timestamp()).toMillis());
            long expected = Math.max(window.size(), spanMs / Math.max(1, expectedIntervalMs) + 1);
            long ageMs = Math.max(0, Duration.between(latest.timestamp(), Instant.now()).toMillis());
            double[] values = window.stream().mapToDouble(Sample::voltage).toArray();
            double intervalSeconds = window.size() > 1 && spanMs > 0
                    ? spanMs / (double) (window.size() - 1) / 1000.0
                    : expectedIntervalMs / 1000.0;
            return Optional.of(new ObservationWindow(values, window.size(), expected, ageMs, intervalSeconds));
        }
    }

    public Optional<double[]> getWindowIfFull(String deviceId) {
        ArrayDeque<Sample> window = windows.get(deviceId);
        if (window == null) return Optional.empty();
        synchronized (window) {
            if (window.size() < windowSize) return Optional.empty();
            return Optional.of(window.stream().mapToDouble(Sample::voltage).toArray());
        }
    }

    public void record(String deviceId, double voltage) {
        ArrayDeque<Sample> window = windows.computeIfAbsent(deviceId, k -> new ArrayDeque<>(windowSize));
        synchronized (window) {
            if (window.size() == windowSize) window.removeFirst();
            window.addLast(new Sample(voltage, Instant.now()));
        }
    }

    private record Sample(double voltage, Instant timestamp) {}
}