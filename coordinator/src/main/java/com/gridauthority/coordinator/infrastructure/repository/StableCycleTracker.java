package com.gridauthority.coordinator.infrastructure.repository;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class StableCycleTracker {
    private final ConcurrentHashMap<String, Long> stableSince = new ConcurrentHashMap<>();

    public void observe(String deviceId, boolean stable, long nowMs) {
        if (stable) stableSince.putIfAbsent(deviceId, nowMs);
        else stableSince.remove(deviceId);
    }

    public long stableDurationMs(String deviceId, long nowMs) {
        Long since = stableSince.get(deviceId);
        return since == null ? 0 : Math.max(0, nowMs - since);
    }

    public void reset(String deviceId) { stableSince.remove(deviceId); }
}
