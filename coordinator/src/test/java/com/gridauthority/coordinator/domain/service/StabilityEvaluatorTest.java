package com.gridauthority.coordinator.domain.service;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class StabilityEvaluatorTest {

    private StabilityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new StabilityEvaluator();
        ReflectionTestUtils.setField(evaluator, "stableCyclesRequired", 60);
    }

    // --- SHUTDOWN ---

    @Test
    void evaluate_shouldReturnShutdown_whenCvExceedsThreshold() {
        DeviceCommand result = evaluator.evaluate(10.1, DeviceState.ACTIVE, 0);
        assertThat(result).isEqualTo(DeviceCommand.SHUTDOWN);
    }

    @Test
    void evaluate_shouldReturnShutdown_whenCvFarExceedsThreshold() {
        DeviceCommand result = evaluator.evaluate(99.0, DeviceState.ACTIVE, 100);
        assertThat(result).isEqualTo(DeviceCommand.SHUTDOWN);
    }

    @Test
    void evaluate_shouldReturnShutdown_whenCvExceedsThreshold_andDeviceAlreadyShutdown() {
        // CV spike overrides state — always SHUTDOWN when unstable
        DeviceCommand result = evaluator.evaluate(15.0, DeviceState.SHUTDOWN, 100);
        assertThat(result).isEqualTo(DeviceCommand.SHUTDOWN);
    }

    // --- KEEP_RUNNING ---

    @Test
    void evaluate_shouldReturnKeepRunning_whenCvAtExactThreshold() {
        // EN 50160: threshold is > 10.0, so exactly 10.0 is safe
        DeviceCommand result = evaluator.evaluate(10.0, DeviceState.ACTIVE, 0);
        assertThat(result).isEqualTo(DeviceCommand.KEEP_RUNNING);
    }

    @Test
    void evaluate_shouldReturnKeepRunning_whenCvBelowThreshold_andDeviceActive() {
        DeviceCommand result = evaluator.evaluate(5.0, DeviceState.ACTIVE, 0);
        assertThat(result).isEqualTo(DeviceCommand.KEEP_RUNNING);
    }

    @Test
    void evaluate_shouldReturnKeepRunning_whenCvBelowThreshold_andDeviceShutdown_butStableCyclesInsufficient() {
        DeviceCommand result = evaluator.evaluate(5.0, DeviceState.SHUTDOWN, 59);
        assertThat(result).isEqualTo(DeviceCommand.KEEP_RUNNING);
    }

    @Test
    void evaluate_shouldReturnKeepRunning_whenCvBelowThreshold_andDeviceShutdown_zeroStableCycles() {
        DeviceCommand result = evaluator.evaluate(0.0, DeviceState.SHUTDOWN, 0);
        assertThat(result).isEqualTo(DeviceCommand.KEEP_RUNNING);
    }

    // --- RESTART ---

    @Test
    void evaluate_shouldReturnRestart_whenCvBelowThreshold_andDeviceShutdown_andStableCyclesMet() {
        DeviceCommand result = evaluator.evaluate(5.0, DeviceState.SHUTDOWN, 60);
        assertThat(result).isEqualTo(DeviceCommand.RESTART);
    }

    @Test
    void evaluate_shouldReturnRestart_whenStableCyclesExceedRequired() {
        DeviceCommand result = evaluator.evaluate(1.0, DeviceState.SHUTDOWN, 200);
        assertThat(result).isEqualTo(DeviceCommand.RESTART);
    }

    @Test
    void evaluate_shouldNotReturnRestart_whenDeviceIsAlreadyActive() {
        // RESTART only applies when device is SHUTDOWN
        DeviceCommand result = evaluator.evaluate(5.0, DeviceState.ACTIVE, 60);
        assertThat(result).isEqualTo(DeviceCommand.KEEP_RUNNING);
    }

    // --- getThreshold ---

    @Test
    void getThreshold_shouldReturnEN50160Standard() {
        var threshold = evaluator.getThreshold();
        assertThat(threshold.thresholdCV()).isEqualTo(10.0);
        assertThat(threshold.standard()).isEqualTo("EN 50160");
        assertThat(threshold.description()).isNotBlank();
    }
}
