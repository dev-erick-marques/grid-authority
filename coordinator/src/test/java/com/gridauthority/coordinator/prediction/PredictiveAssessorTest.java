package com.gridauthority.coordinator.prediction;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class PredictiveAssessorTest {

    private final PredictiveAssessor assessor = new PredictiveAssessor(
            new TrendEstimator(), new BaselineEstimator(), new ForecastEngine(), new RiskEngine());

    private final PolicyDTO policy = new PolicyDTO(13, "grid-01", 230, .05, .10, .06, .10, 4, 60, .75,
            .55, .75, .20, .35, 30, 20, 20, Set.of(), Map.of());

    private double cv(double[] w) {
        double m = 0;
        for (double x : w) m += x;
        m /= w.length;
        double s = 0;
        for (double x : w) s += (x - m) * (x - m);
        return Math.sqrt(s / (w.length - 1)) / m * 100.0;
    }

    private double[] noisy(double base, double amp, long seed) {
        Random rnd = new Random(seed);
        double[] w = new double[30];
        for (int i = 0; i < w.length; i++) w[i] = base + (rnd.nextDouble() * 2 - 1) * amp;
        return w;
    }

    @Test
    void assess_shouldProjectFromCurrentLevel_notWindowMean() {
        double[] w = new double[30];
        for (int i = 0; i < w.length; i++) w[i] = 230 - 0.5 * i + (i % 2 == 0 ? 0.3 : -0.3);

        ForecastResult r = assessor.assess(w, cv(w), 1.0, 0.97, policy);

        // End of the line is ~215.5 V, so breach of 207 V is ~17 s away (window-mean start would say ~31 s)
        assertThat(r.timeToThreshold()).isNotNull();
        assertThat(r.timeToThreshold()).isGreaterThan(14.0);
        assertThat(r.timeToThreshold()).isLessThan(20.0);
        assertThat(r.forecast()).isCloseTo(215.5 - 0.5 * 60, offset(2.0));
        assertThat(r.trend()).isCloseTo(-0.5, offset(0.05));
    }

    @Test
    void assess_shouldFlagDeviationFromNominal_forSteadySag() {
        double[] w = new double[30];
        for (int i = 0; i < w.length; i++) w[i] = 200 + (i % 2 == 0 ? 0.5 : -0.5);

        ForecastResult r = assessor.assess(w, cv(w), 1.0, 0.97, policy);

        assertThat(r.baselineDeviation()).isGreaterThan(0.12);
        assertThat(r.riskScore()).isGreaterThan(0.2);
    }

    @Test
    void assess_shouldIgnoreTrend_whenSlopeIsJustNoise() {
        ForecastResult r = assessor.assess(noisy(230, 4, 11), 1.0, 1.0, 0.97, policy);

        assertThat(r.trend()).isEqualTo(0.0);
        assertThat(r.timeToThreshold()).isNull();
        assertThat(r.confidence()).isGreaterThan(0.75);
        assertThat(r.riskScore()).isLessThan(0.20);
    }

    @Test
    void assess_shouldLowerConfidence_whenSignalIsNoiseDominated() {
        double[] w = noisy(230, 80, 5);

        ForecastResult r = assessor.assess(w, cv(w), 1.0, 0.97, policy);

        assertThat(r.confidence()).isLessThan(0.75);
    }

    @Test
    void assess_shouldUseAcceleration_whenSagIsSpeedingUp() {
        double[] w = new double[30];
        for (int i = 0; i < w.length; i++) w[i] = 230 - 0.02 * i * i + (i % 2 == 0 ? 0.3 : -0.3);

        ForecastResult r = assessor.assess(w, cv(w), 1.0, 0.97, policy);

        assertThat(r.acceleration()).isLessThan(0.0);
        assertThat(r.timeToThreshold()).isNotNull();
    }

    @Test
    void assess_shouldNotPredictBreach_whenVoltageIsRecoveringTowardNominal() {
        double[] w = new double[30];
        for (int i = 0; i < w.length; i++) w[i] = (i < 20 ? 200 : 229) + (i % 2 == 0 ? 0.3 : -0.3);

        ForecastResult r = assessor.assess(w, cv(w), 1.0, 0.97, policy);

        assertThat(r.timeToThreshold()).isNull();
    }
}
