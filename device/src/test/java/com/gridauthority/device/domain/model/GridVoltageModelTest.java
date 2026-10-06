package com.gridauthority.device.domain.model;

import com.gridauthority.device.domain.model.GridVoltageModel.Disturbance;
import com.gridauthority.device.domain.model.GridVoltageModel.Params;
import com.gridauthority.device.domain.model.GridVoltageModel.Phase;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GridVoltageModelTest {

    private static final double NOMINAL = 230.0;
    private static final double WARNING_LOW = NOMINAL * 0.95;
    private static final double CRITICAL_LOW = NOMINAL * 0.90;

    private static GridVoltageModel model(long seed) {
        return new GridVoltageModel(new Params(NOMINAL, 1.2, 20, 1.5, 300, 0.25, 0.1), new Random(seed));
    }

    @Test
    void healthyGridStaysInsideWarningBandAndIsSmooth() {
        GridVoltageModel m = model(42);
        double prev = m.next(1), maxDev = 0, maxStep = 0, sum = 0;
        int n = 3600;
        for (int i = 0; i < n; i++) {
            double v = m.next(1);
            maxDev = Math.max(maxDev, Math.abs(v - NOMINAL));
            maxStep = Math.max(maxStep, Math.abs(v - prev));
            sum += v - NOMINAL;
            prev = v;
        }
        assertThat(maxDev).isLessThan(NOMINAL * 0.05);
        assertThat(maxStep).isLessThan(3.5);
        assertThat(Math.abs(sum / n)).isLessThan(1.0);
        assertThat(m.phase()).isEqualTo(Phase.IDLE);
    }

    @Test
    void samplesAreQuantizedToSensorResolution() {
        GridVoltageModel m = model(1);
        for (int i = 0; i < 200; i++) {
            double v = m.next(1);
            assertThat(Math.abs(v * 10 - Math.round(v * 10))).isLessThan(1e-6);
        }
    }

    @Test
    void sagIsProgressiveCrossesWarningBeforeCriticalAndHasNoJumps() {
        GridVoltageModel m = model(42);
        for (int i = 0; i < 30; i++) m.next(1);
        m.start(new Disturbance(-32.2, 40, 25, 30));

        double prev = m.next(1), maxStep = 0;
        int warningAt = -1, criticalAt = -1;
        for (int t = 1; t <= 120; t++) {
            double v = m.next(1);
            maxStep = Math.max(maxStep, Math.abs(v - prev));
            prev = v;
            if (warningAt < 0 && v < WARNING_LOW) warningAt = t;
            if (criticalAt < 0 && v < CRITICAL_LOW) criticalAt = t;
        }
        assertThat(maxStep).isLessThan(3.5);
        assertThat(warningAt).isPositive();
        assertThat(criticalAt - warningAt).isGreaterThanOrEqualTo(5);
        assertThat(m.phase()).isEqualTo(Phase.IDLE);
        assertThat(Math.abs(prev - NOMINAL)).isLessThan(8);
    }

    @Test
    void eventWalksThroughRampHoldRecoverIdle() {
        GridVoltageModel m = model(7);
        m.start(new Disturbance(27.6, 10, 5, 10));
        assertThat(m.phase()).isEqualTo(Phase.RAMP);
        for (int i = 0; i < 10; i++) m.next(1);
        assertThat(m.phase()).isEqualTo(Phase.HOLD);
        assertThat(m.disturbance()).isEqualTo(27.6);
        for (int i = 0; i < 5; i++) m.next(1);
        assertThat(m.phase()).isEqualTo(Phase.RECOVER);
        for (int i = 0; i < 10; i++) m.next(1);
        assertThat(m.phase()).isEqualTo(Phase.IDLE);
        assertThat(m.disturbance()).isZero();
    }

    @Test
    void releaseMidRampRecoversFromCurrentLevelWithoutJump() {
        GridVoltageModel m = model(5);
        m.start(new Disturbance(27.6, 40, 25, 30));
        for (int i = 0; i < 20; i++) m.next(1);
        double before = m.disturbance();

        m.release();
        m.next(1);

        assertThat(m.phase()).isEqualTo(Phase.RECOVER);
        assertThat(Math.abs(m.disturbance() - before)).isLessThan(1.0);
    }

    @Test
    void retargetingContinuesFromCurrentLevel() {
        GridVoltageModel m = model(9);
        m.start(new Disturbance(27.6, 20, 5, 10));
        for (int i = 0; i < 10; i++) m.next(1);
        double before = m.disturbance();

        m.start(new Disturbance(-32.2, 20, 5, 10));
        m.next(1);

        assertThat(Math.abs(m.disturbance() - before)).isLessThan(1.0);
    }

    @Test
    void outputIsClampedToPhysicalLimits() {
        GridVoltageModel high = model(3);
        high.start(new Disturbance(1e6, 1, 1, 1));
        GridVoltageModel low = model(3);
        low.start(new Disturbance(-1e6, 1, 1, 1));
        for (int i = 0; i < 10; i++) {
            assertThat(high.next(1)).isLessThanOrEqualTo(NOMINAL * 1.3 + 1e-6);
            assertThat(low.next(1)).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Test
    void releaseWhenIdleIsNoOp() {
        GridVoltageModel m = model(2);
        m.release();
        assertThat(m.phase()).isEqualTo(Phase.IDLE);
    }
}
