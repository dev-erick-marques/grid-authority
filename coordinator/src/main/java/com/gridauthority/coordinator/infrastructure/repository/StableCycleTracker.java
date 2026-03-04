package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class StableCycleTracker {

    private final ConcurrentHashMap<String, Integer> stableCycles = new ConcurrentHashMap<>();

    public int getStableCycles(String deviceId) {
        return stableCycles.getOrDefault(deviceId, 0);
    }

    public void record(String deviceId, DeviceCommand command) {
        switch (command) {
            case KEEP_RUNNING -> stableCycles.merge(deviceId, 1, Integer::sum);
            case SHUTDOWN     -> stableCycles.put(deviceId, 0);
            case RESTART      -> { /* counter unchanged — wait for stable confirmation */ }
        }
    }

    public void reset(String deviceId) {
        stableCycles.remove(deviceId);
    }
}