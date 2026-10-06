package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/** Remembers which devices are currently in a mitigated state, so RESTORE_GRID is only sent when there is something to restore. */
@Component
public class MitigationStateTracker {
    private final ConcurrentHashMap<String, DeviceCommand> active = new ConcurrentHashMap<>();

    public void activate(String deviceId, DeviceCommand action) { active.put(deviceId, action); }

    public void clear(String deviceId) { active.remove(deviceId); }

    public boolean isActive(String deviceId) { return active.containsKey(deviceId); }
}
