package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.application.dto.DeviceTelemetryDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class VoltageWindowRepository {

    @Value("${voltage.repository.window.size:10}")
    private int WINDOW_SIZE;

    private final ConcurrentHashMap<String, ArrayBlockingQueue<Double>> windows =
            new ConcurrentHashMap<>();


    public void record(String deviceId, double voltage) {
        ArrayBlockingQueue<Double> window = windows
                .computeIfAbsent(deviceId, k -> new ArrayBlockingQueue<>(WINDOW_SIZE));

        if (!window.offer(voltage)) {
            window.poll();
            window.offer(voltage);
        }
    }

    public Optional<double[]> getWindowIfFull(String deviceId) {
        ArrayBlockingQueue<Double> window = windows.get(deviceId);
        if (window == null || window.size() < WINDOW_SIZE) {
            return Optional.empty();
        }
        return Optional.of(window.stream().mapToDouble(Double::doubleValue).toArray());
    }

    public Optional<double[]> recordAndGet(DeviceTelemetryDTO dto) {
        record(dto.deviceId(), dto.voltage());
        return getWindowIfFull(dto.deviceId());
    }
}
