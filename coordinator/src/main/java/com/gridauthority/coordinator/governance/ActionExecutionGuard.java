package com.gridauthority.coordinator.governance;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Rate-limits automatic actions per device.
 * <ul>
 *   <li>Every action has a per-action cooldown.</li>
 *   <li>Routine actions share an hourly budget ({@code maxAutomaticActionsPerHour}).</li>
 *   <li>Safety-critical actions (see {@link DeviceCommand#isSafetyCritical()}) are NOT subject to the hourly
 *       budget, so routine traffic can never prevent an emergency or a generator start.</li>
 * </ul>
 */
@Service
public class ActionExecutionGuard {

    public enum Result { ALLOWED, BLOCKED_COOLDOWN, BLOCKED_HOURLY_BUDGET }

    private static final long HOUR_MS = 3_600_000L;
    private final ConcurrentHashMap<String, DeviceHistory> history = new ConcurrentHashMap<>();

    public boolean allow(String deviceId, DeviceCommand action, PolicyDTO policy, long nowMs) {
        return evaluate(deviceId, action, policy, nowMs) == Result.ALLOWED;
    }

    public synchronized Result evaluate(String deviceId, DeviceCommand action, PolicyDTO policy, long nowMs) {
        if (action == DeviceCommand.OBSERVE || action == DeviceCommand.WARN) return Result.ALLOWED;
        DeviceHistory h = history.computeIfAbsent(deviceId, k -> new DeviceHistory());

        Long last = h.lastByAction.get(action);
        if (last != null && nowMs - last < policy.cooldownSeconds() * 1000L) return Result.BLOCKED_COOLDOWN;

        if (!action.isSafetyCritical()) {
            while (!h.routineTimestamps.isEmpty() && nowMs - h.routineTimestamps.peekFirst() >= HOUR_MS) {
                h.routineTimestamps.removeFirst();
            }
            if (h.routineTimestamps.size() >= policy.maxAutomaticActionsPerHour()) return Result.BLOCKED_HOURLY_BUDGET;
            h.routineTimestamps.addLast(nowMs);
        }
        h.lastByAction.put(action, nowMs);
        return Result.ALLOWED;
    }

    private static final class DeviceHistory {
        private final ConcurrentHashMap<DeviceCommand, Long> lastByAction = new ConcurrentHashMap<>();
        private final ArrayDeque<Long> routineTimestamps = new ArrayDeque<>();
    }
}
