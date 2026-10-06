package com.gridauthority.coordinator.prediction;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.*;

class TrendEstimatorTest {

    private final TrendEstimator estimator = new TrendEstimator();

    private double[] ramp(double start, double perSample, int n) {
        double[] v = new double[n];
        for (int i = 0; i < n; i++) v[i] = start + perSample * i + (i % 2 == 0 ? 0.2 : -0.2);
        return v;
    }

    @Test
    void fit_shouldReturnSlopeInVoltsPerSecond_usingSamplingInterval() {
        TrendFit fit = estimator.fit(ramp(230, -1.0, 30), 2.0);

        assertThat(fit.slopePerSecond()).isCloseTo(-0.5, offset(0.01));
        assertThat(fit.r2()).isGreaterThan(0.99);
        assertThat(fit.slopeSignificant(TrendFit.DEFAULT_Z)).isTrue();
    }

    @Test
    void fit_shouldStartFromEndOfFittedLine_notFromWindowMean() {
        double[] w = ramp(230, -1.0, 30);
        double mean = 0;
        for (double x : w) mean += x;
        mean /= w.length;

        TrendFit fit = estimator.fit(w, 1.0);

        assertThat(fit.endValue()).isCloseTo(201.0, offset(0.5));
        assertThat(mean - fit.endValue()).isGreaterThan(10.0);
    }

    @Test
    void fit_shouldNotFlagSignificantTrend_forPureNoise() {
        Random rnd = new Random(42);
        double[] w = new double[30];
        for (int i = 0; i < w.length; i++) w[i] = 220 + (rnd.nextDouble() * 2 - 1) * 4;

        TrendFit fit = estimator.fit(w, 1.0);

        assertThat(fit.slopeSignificant(TrendFit.DEFAULT_Z)).isFalse();
        assertThat(fit.r2()).isLessThan(0.3);
        assertThat(fit.slopeStdError()).isGreaterThan(0.0);
    }

    @Test
    void fit_shouldReturnZeroTrend_forConstantSeries() {
        double[] w = new double[30];
        java.util.Arrays.fill(w, 230.0);

        TrendFit fit = estimator.fit(w, 1.0);

        assertThat(fit.slopePerSecond()).isCloseTo(0.0, offset(1e-9));
        assertThat(fit.slopeSignificant(TrendFit.DEFAULT_Z)).isFalse();
    }

    @Test
    void fit_shouldDetectSignificantAcceleration_forQuadraticSag() {
        double[] w = new double[30];
        for (int i = 0; i < w.length; i++) w[i] = 230 - 0.01 * i * i;

        TrendFit fit = estimator.fit(w, 1.0);

        assertThat(fit.accelerationPerSecond2()).isCloseTo(-0.02, offset(0.002));
        assertThat(fit.accelerationSignificant(TrendFit.DEFAULT_Z)).isTrue();
    }

    @Test
    void fit_shouldReturnFlat_whenTooFewSamples() {
        TrendFit fit = estimator.fit(new double[]{230.0, 229.0}, 1.0);

        assertThat(fit.slopePerSecond()).isEqualTo(0.0);
        assertThat(fit.slopeSignificant(TrendFit.DEFAULT_Z)).isFalse();
    }

    @Test
    void predictionStdError_shouldGrowWithHorizon() {
        Random rnd = new Random(3);
        double[] w = new double[30];
        for (int i = 0; i < w.length; i++) w[i] = 230 + (rnd.nextDouble() * 2 - 1) * 4;

        TrendFit fit = estimator.fit(w, 1.0);

        assertThat(fit.predictionStdError(60)).isGreaterThan(fit.predictionStdError(0));
    }
}
