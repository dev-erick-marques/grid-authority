package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class StableCycleTracker {

    private final ConcurrentHashMap<String, AtomicInteger> stableCycles = new ConcurrentHashMap<>();

    public int getStableCycles(String deviceId) {
        AtomicInteger counter = stableCycles.get(deviceId);
        return counter != null ? counter.get() : 0;
    }

    public void record(String deviceId, DeviceCommand command, int cap) {
        switch (command) {
            case KEEP_RUNNING -> {
                AtomicInteger counter = stableCycles.computeIfAbsent(deviceId, k -> new AtomicInteger(0));
                counter.getAndUpdate(v -> v < cap ? v + 1 : v);
            }
            case SHUTDOWN, RESTART -> {
                AtomicInteger counter = stableCycles.get(deviceId);
                if (counter != null) counter.set(0);
            }
        }
    }

    public void reset(String deviceId) {
        stableCycles.remove(deviceId);
    }
}