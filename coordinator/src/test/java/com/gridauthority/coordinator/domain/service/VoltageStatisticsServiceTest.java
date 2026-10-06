package com.gridauthority.coordinator.domain.service;

import com.gridauthority.coordinator.domain.exceptions.InsufficientVoltageSamplesException;
import com.gridauthority.coordinator.domain.exceptions.InvalidVoltageInputException;
import com.gridauthority.coordinator.domain.exceptions.InvalidVoltageValueException;
import com.gridauthority.coordinator.domain.exceptions.VoltageSensorFailureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.*;

class VoltageStatisticsServiceTest {

    private final VoltageStatisticsService service = new VoltageStatisticsService();

    @Test
    void compute_shouldReturnCorrectStats() {
        double[] voltages = {118.0, 120.0, 121.0, 119.0, 122.0, 120.0, 118.0, 121.0, 119.0, 120.0};

        VoltageStatisticsService.VoltageStats stats = service.compute(voltages);

        assertThat(stats.mean()).isCloseTo(119.8, offset(0.01));
        assertThat(stats.std()).isPositive();
        assertThat(stats.cv()).isPositive();
    }

    @Test
    void compute_shouldReturnZeroStdAndCv_whenAllVoltagesAreEqual() {
        VoltageStatisticsService.VoltageStats stats = service.compute(
                new double[]{120.0, 120.0, 120.0, 120.0, 120.0}
        );

        assertThat(stats.std()).isCloseTo(0.0, offset(0.0001));
        assertThat(stats.cv()).isCloseTo(0.0, offset(0.0001));
    }

    @Test
    void compute_shouldReturnCorrectCv_whenStdAndMeanAreKnown() {
        double[] voltages = {98.0, 100.0, 102.0};

        VoltageStatisticsService.VoltageStats stats = service.compute(voltages);

        assertThat(stats.mean()).isCloseTo(100.0, offset(0.01));
        assertThat(stats.cv()).isCloseTo(2.0, offset(0.01));
    }

    @Test
    void compute_shouldThrow_whenVoltagesIsNull() {
        assertThatThrownBy(() -> service.compute(null))
                .isInstanceOf(InvalidVoltageInputException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void compute_shouldThrow_whenVoltagesIsEmpty() {
        assertThatThrownBy(() -> service.compute(new double[]{}))
                .isInstanceOf(InvalidVoltageInputException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void compute_shouldThrow_whenOnlyOneSampleIsProvided() {
        assertThatThrownBy(() -> service.compute(new double[]{120.0}))
                .isInstanceOf(InsufficientVoltageSamplesException.class)
                .hasMessageContaining("At least two voltage samples");
    }

    @Test
    void compute_shouldThrow_whenAnyVoltageIsNegative() {
        assertThatThrownBy(() -> service.compute(new double[]{120.0, -5.0, 118.0}))
                .isInstanceOf(InvalidVoltageValueException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void compute_shouldThrow_whenMeanIsZero() {
        assertThatThrownBy(() -> service.compute(new double[]{0.0, 0.0, 0.0}))
                .isInstanceOf(VoltageSensorFailureException.class)
                .hasMessageContaining("zero");
    }
}