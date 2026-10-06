package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.domain.model.DeviceSurgeState;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DeviceSurgeStateRepository {

    private final ConcurrentHashMap<String, DeviceSurgeState> states = new ConcurrentHashMap<>();

    public DeviceSurgeState get(String deviceId) {
        return states.getOrDefault(deviceId, DeviceSurgeState.INACTIVE);
    }

    public void set(String deviceId, DeviceSurgeState state) {
        states.put(deviceId, state);
    }
}