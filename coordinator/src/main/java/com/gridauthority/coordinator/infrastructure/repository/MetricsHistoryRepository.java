package com.gridauthority.coordinator.infrastructure.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

@Repository
public class MetricsHistoryRepository {

    @Value("${metrics.history.size:10}")
    private int HISTORY_SIZE;

    private final Cache<String, ArrayBlockingQueue<DeviceMetricsDTO>> cache =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .build();


    public void add(DeviceMetricsDTO metrics) {
        ArrayBlockingQueue<DeviceMetricsDTO> history = cache.get(
                metrics.deviceId(),
                k -> new ArrayBlockingQueue<>(HISTORY_SIZE)
        );

        assert history != null;
        if (!history.offer(metrics)) {
            history.poll();
            history.offer(metrics);
        }
    }

    public Map<String, List<DeviceMetricsDTO>> getAll() {
        Map<String, List<DeviceMetricsDTO>> snapshot = new HashMap<>();
        cache.asMap().forEach((deviceId, queue) ->
                snapshot.put(deviceId, new ArrayList<>(queue))
        );
        return snapshot;
    }

    public List<DeviceMetricsDTO> getByDevice(String deviceId) {
        ArrayBlockingQueue<DeviceMetricsDTO> history = cache.getIfPresent(deviceId);
        return history != null ? new ArrayList<>(history) : List.of();
    }
}