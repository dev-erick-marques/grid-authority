package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StableCycleTrackerTest {

    private StableCycleTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new StableCycleTracker();
    }

    @Test
    void getStableCycles_shouldReturnZero_forUnknownDevice() {
        assertThat(tracker.getStableCycles("unknown")).isEqualTo(0);
    }

    @Test
    void record_shouldIncrementOnKeepRunning() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(3);
    }

    @Test
    void record_shouldResetOnShutdown() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-1", DeviceCommand.SHUTDOWN);

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(0);
    }

    @Test
    void record_shouldResetOnRestart() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-1", DeviceCommand.RESTART);

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(0);
    }

    @Test
    void record_shouldAccumulateAgainAfterReset() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-1", DeviceCommand.SHUTDOWN);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(2);
    }

    @Test
    void record_shouldIsolateCountersPerDevice() {
        tracker.record("device-A", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-A", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-B", DeviceCommand.KEEP_RUNNING);

        assertThat(tracker.getStableCycles("device-A")).isEqualTo(2);
        assertThat(tracker.getStableCycles("device-B")).isEqualTo(1);
    }

    @Test
    void record_shutdownOnNewDevice_shouldNotThrow() {
        // SHUTDOWN before any KEEP_RUNNING — counter doesn't exist yet, must not throw
        tracker.record("device-new", DeviceCommand.SHUTDOWN);
        assertThat(tracker.getStableCycles("device-new")).isEqualTo(0);
    }

    @Test
    void reset_shouldRemoveDeviceCounter() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING);
        tracker.reset("device-1");

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(0);
    }

    @Test
    void reset_onUnknownDevice_shouldNotThrow() {
        tracker.reset("never-seen");
        assertThat(tracker.getStableCycles("never-seen")).isEqualTo(0);
    }
}
