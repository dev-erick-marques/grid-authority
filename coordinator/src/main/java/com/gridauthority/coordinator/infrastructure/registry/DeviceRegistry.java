package com.gridauthority.coordinator.infrastructure.registry;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceRegistry {

    private final ConcurrentHashMap<String, String> deviceUrls = new ConcurrentHashMap<>();

    public void register(String deviceId, String baseUrl) {
        deviceUrls.put(deviceId, baseUrl);
    }

    public Optional<String> resolve(String deviceId) {
        return Optional.ofNullable(deviceUrls.get(deviceId));
    }

    public void deregister(String deviceId) {
        deviceUrls.remove(deviceId);
    }
}
