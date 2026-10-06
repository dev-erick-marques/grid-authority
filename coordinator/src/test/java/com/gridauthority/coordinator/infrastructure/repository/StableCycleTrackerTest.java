package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StableCycleTrackerTest {

    private StableCycleTracker tracker;
    private static final int CAP = 3;

    @BeforeEach
    void setUp() {
        tracker = new StableCycleTracker();
    }

    @Test
    void getStableCycles_shouldReturnZero_forUnknownDevice() {
        assertThat(tracker.getStableCycles("unknown")).isEqualTo(0);
    }

    @Test
    void record_shouldIncrementOnKeepRunning_untilCap() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(3);
    }

    @Test
    void record_shouldNotExceedCap() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP); // extra

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(CAP);
    }

    @Test
    void record_shouldResetOnShutdown() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.SHUTDOWN, CAP);

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(0);
    }

    @Test
    void record_shouldResetOnRestart() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.RESTART, CAP);

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(0);
    }

    @Test
    void record_shouldAccumulateAgainAfterReset() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.SHUTDOWN, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(2);
    }

    @Test
    void record_shouldIsolateCountersPerDevice() {
        tracker.record("device-A", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-A", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-B", DeviceCommand.KEEP_RUNNING, CAP);

        assertThat(tracker.getStableCycles("device-A")).isEqualTo(2);
        assertThat(tracker.getStableCycles("device-B")).isEqualTo(1);
    }

    @Test
    void record_shutdownOnNewDevice_shouldNotThrow() {
        tracker.record("device-new", DeviceCommand.SHUTDOWN, CAP);
        assertThat(tracker.getStableCycles("device-new")).isEqualTo(0);
    }

    @Test
    void reset_shouldRemoveDeviceCounter() {
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.record("device-1", DeviceCommand.KEEP_RUNNING, CAP);
        tracker.reset("device-1");

        assertThat(tracker.getStableCycles("device-1")).isEqualTo(0);
    }

    @Test
    void reset_onUnknownDevice_shouldNotThrow() {
        tracker.reset("never-seen");
        assertThat(tracker.getStableCycles("never-seen")).isEqualTo(0);
    }
}