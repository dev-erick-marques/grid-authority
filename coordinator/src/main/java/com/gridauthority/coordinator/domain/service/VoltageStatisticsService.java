package com.gridauthority.coordinator.domain.service;

import com.gridauthority.coordinator.domain.exceptions.InsufficientVoltageSamplesException;
import com.gridauthority.coordinator.domain.exceptions.InvalidVoltageInputException;
import com.gridauthority.coordinator.domain.exceptions.InvalidVoltageValueException;
import com.gridauthority.coordinator.domain.exceptions.VoltageSensorFailureException;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class VoltageStatisticsService {

    public VoltageStats compute(double[] voltages) {
        validateVoltages(voltages);
        double mean = calculateMean(voltages);
        double std = calculateStd(voltages, mean);
        double cv = calculateCv(std, mean);
        return new VoltageStats(mean, std, cv);
    }

    private void validateVoltages(double[] voltages) {
        if (voltages == null) {
            throw new InvalidVoltageInputException(
                    "Voltage array must not be null"
            );
        }
        if (voltages.length == 0) {
            throw new InvalidVoltageInputException(
                    "Voltage array must not be empty"
            );
        }
        if (voltages.length < 2) {
            throw new InsufficientVoltageSamplesException(
                    "At least two voltage samples are required to compute standard deviation"
            );
        }
        if (Arrays.stream(voltages).anyMatch(v -> v < 0)) {
            throw new InvalidVoltageValueException(
                    "Voltage values must not be negative"
            );
        }
    }

    private double calculateMean(double[] voltages) {
        double mean = Arrays.stream(voltages).summaryStatistics().getAverage();
        if (mean == 0.0) {
            throw new VoltageSensorFailureException("Mean voltage is zero — possible sensor failure");
        }
        return mean;
    }

    private double calculateStd(double[] voltages, double mean) {
        double variance = Arrays.stream(voltages)
                .map(v -> Math.pow(v - mean, 2))
                .sum() / (voltages.length - 1);
        return Math.sqrt(variance);
    }

    private double calculateCv(double std, double mean) {
        return (std / mean) * 100.0;
    }

    public record VoltageStats(double mean, double std, double cv) {}
}
