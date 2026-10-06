package com.gridauthority.coordinator.prediction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ForecastEngineTest {

    private final ForecastEngine engine = new ForecastEngine();

    @Test
    void forecast_shouldExtrapolateLinearly() {
        assertThat(engine.forecast(220, -0.5, 60)).isCloseTo(190.0, offset(1e-9));
    }

    @Test
    void forecast_shouldIncludeAcceleration() {
        assertThat(engine.forecast(220, -0.2, -0.01, 60)).isCloseTo(220 - 12 - 18, offset(1e-9));
    }

    @Test
    void timeToBreach_shouldReturnSecondsToLowerBound() {
        assertThat(engine.timeToBreach(220, -0.5, 0, 207, 253)).isCloseTo(26.0, offset(1e-9));
    }

    @Test
    void timeToBreach_shouldReturnZero_whenAlreadyOutsideBand() {
        assertThat(engine.timeToBreach(200, 0.0, 0, 207, 253)).isEqualTo(0.0);
        assertThat(engine.timeToBreach(260, -1.0, 0, 207, 253)).isEqualTo(0.0);
    }

    @Test
    void timeToBreach_shouldReturnNull_whenFlat() {
        assertThat(engine.timeToBreach(230, 0.0, 0, 207, 253)).isNull();
    }

    @Test
    void timeToBreach_shouldSolveQuadratic_whenAccelerating() {
        // 220 - 0.2t - 0.005t^2 = 207
        double expected = (-0.2 + Math.sqrt(0.04 + 4 * 0.005 * 13)) / (2 * 0.005);

        assertThat(engine.timeToBreach(220, -0.2, -0.01, 207, 253)).isCloseTo(expected, offset(1e-6));
    }

    @Test
    void timeToBreach_shouldIgnoreBoundThatIsNeverReached_whenDecelerating() {
        // 220 - 0.1t + 0.005t^2 bottoms out at 219.5 V (never reaches 207), then climbs to 253 V
        double expected = (0.1 + Math.sqrt(0.01 + 4 * 0.005 * 33)) / (2 * 0.005);

        assertThat(engine.timeToBreach(220, -0.1, 0.01, 207, 253)).isCloseTo(expected, offset(1e-6));
    }
}
