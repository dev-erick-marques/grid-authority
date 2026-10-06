package com.gridauthority.coordinator.infrastructure.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StableCycleTrackerTest {
    @Test
    void stableDurationStartsWhenSignalBecomesStable() {
        StableCycleTracker tracker = new StableCycleTracker();
        tracker.observe("device-1", true, 1_000);
        assertThat(tracker.stableDurationMs("device-1", 4_500)).isEqualTo(3_500);
    }

    @Test
    void instabilityClearsRecoveryWindow() {
        StableCycleTracker tracker = new StableCycleTracker();
        tracker.observe("device-1", true, 1_000);
        tracker.observe("device-1", false, 2_000);
        assertThat(tracker.stableDurationMs("device-1", 5_000)).isZero();
    }

    @Test
    void stableWindowIsIndependentPerDevice() {
        StableCycleTracker tracker = new StableCycleTracker();
        tracker.observe("a", true, 1_000);
        tracker.observe("b", true, 2_000);
        assertThat(tracker.stableDurationMs("a", 4_000)).isEqualTo(3_000);
        assertThat(tracker.stableDurationMs("b", 4_000)).isEqualTo(2_000);
    }
}
