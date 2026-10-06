package com.gridauthority.coordinator.decision;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class DecisionEngineTest {

    private final DecisionEngine engine = new DecisionEngine();

    // horizon 60 s, minConfidence .75, warning .20, preemptive .55, critical .75
    private final PolicyDTO policy = new PolicyDTO(13, "grid-01", 230, .05, .10, .06, .10, 4, 60, .75,
            .55, .75, .20, .35, 30, 20, 20, Set.of(), Map.of());

    @Test
    void emergency_shouldAlwaysWin() {
        assertThat(engine.decide(0.0, 1.0, null, true, policy)).isEqualTo(DeviceCommand.EMERGENCY_PROTECTION);
    }

    @Test
    void lowRiskAndNoBreach_shouldObserve() {
        assertThat(engine.decide(0.10, 0.9, null, false, policy)).isEqualTo(DeviceCommand.OBSERVE);
        assertThat(engine.decide(0.10, 0.9, 70.0, false, policy)).isEqualTo(DeviceCommand.OBSERVE);
    }

    @Test
    void breachWithinHorizon_shouldPrepareBackup_evenWithLowRisk() {
        assertThat(engine.decide(0.25, 0.9, 50.0, false, policy)).isEqualTo(DeviceCommand.PREPARE_BACKUP);
    }

    @Test
    void breachWithinHalfHorizon_shouldStartGenerator_beforeLimitIsReached() {
        assertThat(engine.decide(0.25, 0.9, 25.0, false, policy)).isEqualTo(DeviceCommand.START_GENERATOR);
    }

    @Test
    void breachWithinHorizonAndCriticalRisk_shouldStartGenerator() {
        assertThat(engine.decide(0.80, 0.9, 50.0, false, policy)).isEqualTo(DeviceCommand.START_GENERATOR);
    }

    @Test
    void lowConfidence_shouldOnlyWarn() {
        assertThat(engine.decide(0.25, 0.5, 25.0, false, policy)).isEqualTo(DeviceCommand.WARN);
    }

    @Test
    void highRiskWithoutCredibleTrend_shouldPrepareBackup() {
        assertThat(engine.decide(0.60, 0.9, null, false, policy)).isEqualTo(DeviceCommand.PREPARE_BACKUP);
    }

    @Test
    void moderateRiskWithoutBreach_shouldWarn() {
        assertThat(engine.decide(0.30, 0.9, null, false, policy)).isEqualTo(DeviceCommand.WARN);
        assertThat(engine.decide(0.30, 0.9, 70.0, false, policy)).isEqualTo(DeviceCommand.WARN);
    }

    // ---- recovery (RESTORE_GRID) ----

    @Test
    void recovery_shouldNotRestore_whenNoMitigationIsActive() {
        assertThat(engine.applyRecovery(DeviceCommand.OBSERVE, false, false, 60_000, policy)).isEqualTo(DeviceCommand.OBSERVE);
    }

    @Test
    void recovery_shouldNeverOverrideEmergency() {
        assertThat(engine.applyRecovery(DeviceCommand.EMERGENCY_PROTECTION, true, true, 60_000, policy))
                .isEqualTo(DeviceCommand.EMERGENCY_PROTECTION);
    }

    @Test
    void recovery_shouldNeverOverridePreventiveActions() {
        assertThat(engine.applyRecovery(DeviceCommand.START_GENERATOR, false, true, 60_000, policy)).isEqualTo(DeviceCommand.START_GENERATOR);
        assertThat(engine.applyRecovery(DeviceCommand.PREPARE_BACKUP, false, true, 60_000, policy)).isEqualTo(DeviceCommand.PREPARE_BACKUP);
    }

    @Test
    void recovery_shouldWaitForStableDuration() {
        assertThat(engine.applyRecovery(DeviceCommand.OBSERVE, false, true, 19_000, policy)).isEqualTo(DeviceCommand.OBSERVE);
        assertThat(engine.applyRecovery(DeviceCommand.OBSERVE, false, true, 20_000, policy)).isEqualTo(DeviceCommand.RESTORE_GRID);
    }

    @Test
    void stable_shouldBeFalse_duringEmergencyEvenWithLowRisk() {
        assertThat(engine.isStableForRecovery(0.0, 0.5, 0.0, 0.9, 0.0, null, true, policy)).isFalse();
    }

    @Test
    void stable_shouldBeFalse_forSteadySagBeyondWarningBand() {
        assertThat(engine.isStableForRecovery(0.22, 0.5, 0.0, 0.9, 0.078, null, false, policy)).isFalse();
    }

    @Test
    void stable_shouldBeFalse_whenBreachIsPredictedWithinHorizon() {
        assertThat(engine.isStableForRecovery(0.1, 0.5, 0.0, 0.9, 0.0, 40.0, false, policy)).isFalse();
    }

    @Test
    void stable_shouldBeTrue_forCalmNominalGrid() {
        assertThat(engine.isStableForRecovery(0.05, 0.5, 0.0, 0.9, 0.01, null, false, policy)).isTrue();
    }
}
