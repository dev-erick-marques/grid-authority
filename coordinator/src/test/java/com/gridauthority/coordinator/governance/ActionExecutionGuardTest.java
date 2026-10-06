package com.gridauthority.coordinator.governance;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ActionExecutionGuardTest {

    private final ActionExecutionGuard guard = new ActionExecutionGuard();

    // cooldown 30 s, 20 routine actions/hour
    private final PolicyDTO policy = new PolicyDTO(13, "grid-01", 230, .05, .10, .06, .10, 4, 60, .75,
            .55, .75, .20, .35, 30, 20, 20, Set.of(), Map.of());

    private void exhaustRoutineBudget(String device, long start) {
        for (int i = 0; i < policy.maxAutomaticActionsPerHour(); i++) {
            long now = start + i * 31_000L; // just past the cooldown each time
            assertThat(guard.evaluate(device, DeviceCommand.RESTORE_GRID, policy, now)).isEqualTo(ActionExecutionGuard.Result.ALLOWED);
        }
    }

    @Test
    void routineActions_shouldBeBlockedByHourlyBudget() {
        exhaustRoutineBudget("d1", 0);
        long now = policy.maxAutomaticActionsPerHour() * 31_000L + 31_000L;

        assertThat(guard.evaluate("d1", DeviceCommand.RESTORE_GRID, policy, now))
                .isEqualTo(ActionExecutionGuard.Result.BLOCKED_HOURLY_BUDGET);
        assertThat(guard.evaluate("d1", DeviceCommand.PREPARE_BACKUP, policy, now))
                .isEqualTo(ActionExecutionGuard.Result.BLOCKED_HOURLY_BUDGET);
    }

    @Test
    void safetyCriticalActions_shouldNeverBeStarvedByExhaustedRoutineBudget() {
        exhaustRoutineBudget("d1", 0);
        long now = policy.maxAutomaticActionsPerHour() * 31_000L + 31_000L;

        assertThat(guard.evaluate("d1", DeviceCommand.EMERGENCY_PROTECTION, policy, now)).isEqualTo(ActionExecutionGuard.Result.ALLOWED);
        assertThat(guard.evaluate("d1", DeviceCommand.START_GENERATOR, policy, now)).isEqualTo(ActionExecutionGuard.Result.ALLOWED);
    }

    @Test
    void criticalActions_shouldNotConsumeRoutineBudget() {
        for (int i = 0; i < 50; i++) {
            guard.evaluate("d1", DeviceCommand.EMERGENCY_PROTECTION, policy, i * 31_000L);
        }
        assertThat(guard.evaluate("d1", DeviceCommand.RESTORE_GRID, policy, 50 * 31_000L)).isEqualTo(ActionExecutionGuard.Result.ALLOWED);
    }

    @Test
    void cooldown_shouldStillApplyPerActionToCriticalActions() {
        assertThat(guard.evaluate("d1", DeviceCommand.EMERGENCY_PROTECTION, policy, 0)).isEqualTo(ActionExecutionGuard.Result.ALLOWED);
        assertThat(guard.evaluate("d1", DeviceCommand.EMERGENCY_PROTECTION, policy, 10_000))
                .isEqualTo(ActionExecutionGuard.Result.BLOCKED_COOLDOWN);
        assertThat(guard.evaluate("d1", DeviceCommand.EMERGENCY_PROTECTION, policy, 30_000)).isEqualTo(ActionExecutionGuard.Result.ALLOWED);
    }

    @Test
    void budget_shouldBeTrackedPerDevice() {
        exhaustRoutineBudget("d1", 0);

        assertThat(guard.evaluate("d2", DeviceCommand.RESTORE_GRID, policy, 700_000)).isEqualTo(ActionExecutionGuard.Result.ALLOWED);
    }

    @Test
    void budget_shouldRecoverAfterOneHour() {
        exhaustRoutineBudget("d1", 0);
        long afterHour = policy.maxAutomaticActionsPerHour() * 31_000L + 3_600_000L;

        assertThat(guard.evaluate("d1", DeviceCommand.RESTORE_GRID, policy, afterHour)).isEqualTo(ActionExecutionGuard.Result.ALLOWED);
    }

    @Test
    void observeAndWarn_shouldAlwaysBeAllowed() {
        assertThat(guard.allow("d1", DeviceCommand.OBSERVE, policy, 0)).isTrue();
        assertThat(guard.allow("d1", DeviceCommand.WARN, policy, 0)).isTrue();
    }
}
