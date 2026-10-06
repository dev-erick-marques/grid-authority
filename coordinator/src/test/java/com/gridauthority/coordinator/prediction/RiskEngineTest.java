package com.gridauthority.coordinator.prediction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RiskEngineTest {

    private final RiskEngine engine = new RiskEngine();

    @Test
    void score_shouldBeOne_whenAllSignalsSaturated() {
        assertThat(engine.score(1, 1, 1, 1, 1)).isCloseTo(1.0, offset(1e-9));
    }

    @Test
    void score_shouldNotCountSlopeTwice() {
        assertThat(engine.score(0, 1, 0, 0, 0)).isCloseTo(RiskEngine.RiskWeights.DEFAULT.trend(), offset(1e-9));
        assertThat(RiskEngine.RiskWeights.DEFAULT.trend()).isLessThan(0.25);
    }

    @Test
    void score_shouldClampInputs() {
        assertThat(engine.score(5, -3, 2, 9, 7)).isLessThanOrEqualTo(1.0);
        assertThat(engine.score(-1, -1, -1, -1, -1)).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void weights_shouldRejectInvalidConfiguration() {
        assertThatThrownBy(() -> new RiskEngine.RiskWeights(.5, .5, .5, .5, .5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RiskEngine.RiskWeights(-.1, .3, .3, .3, .2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fitQuality_shouldDropAsPredictionUncertaintyGrows() {
        assertThat(engine.fitQuality(0, 23)).isCloseTo(1.0, offset(1e-9));
        assertThat(engine.fitQuality(11.5, 23)).isCloseTo(0.5, offset(1e-9));
        assertThat(engine.fitQuality(30, 23)).isEqualTo(0.0);
        assertThat(engine.fitQuality(Double.POSITIVE_INFINITY, 23)).isEqualTo(0.0);
    }

    @Test
    void confidence_shouldDropWhenFitIsPoor() {
        assertThat(engine.confidence(1.0, 0.0)).isLessThan(engine.confidence(1.0, 1.0));
        assertThat(engine.confidence(1.0, 0.0)).isLessThan(0.75);
    }
}
