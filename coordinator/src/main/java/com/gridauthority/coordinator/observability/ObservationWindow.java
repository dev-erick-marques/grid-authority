package com.gridauthority.coordinator.observability;

import java.util.Arrays;

public record ObservationWindow(double[] values, long received, long expected, long latestAgeMs, double sampleIntervalSeconds) {

    public ObservationWindow(double[] values, long received, long expected, long latestAgeMs) {
        this(values, received, expected, latestAgeMs, 1.0);
    }

    public double[] values() {
        return Arrays.copyOf(values, values.length);
    }
}