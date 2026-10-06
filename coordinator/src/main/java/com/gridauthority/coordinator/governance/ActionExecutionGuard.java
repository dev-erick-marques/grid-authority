package com.gridauthority.coordinator.governance;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActionExecutionGuard {
    private final ConcurrentHashMap<String, DeviceHistory> history = new ConcurrentHashMap<>();

    public synchronized boolean allow(String deviceId, DeviceCommand action, PolicyDTO policy, long nowMs) {
        if (action == DeviceCommand.OBSERVE || action == DeviceCommand.WARN) return true;
        DeviceHistory h = history.computeIfAbsent(deviceId, k -> new DeviceHistory());
        long cooldownMs = policy.cooldownSeconds() * 1000L;
        Long last = h.lastByAction.get(action);

        if (last != null && nowMs - last < cooldownMs) return false;

        while (!h.timestamps.isEmpty() && nowMs - h.timestamps.peekFirst() >= 3_600_000L) h.timestamps.removeFirst();

        if (h.timestamps.size() >= policy.maxAutomaticActionsPerHour()) return false;

        h.lastByAction.put(action, nowMs);
        h.timestamps.addLast(nowMs);
        return true;
    }

    private static final class DeviceHistory {
        private final ConcurrentHashMap<DeviceCommand, Long> lastByAction = new ConcurrentHashMap<>();
        private final ArrayDeque<Long> timestamps = new ArrayDeque<>();
    }
}
