package com.gridauthority.coordinator.observability;

import java.util.Arrays;

public record ObservationWindow(double[] values, long received, long expected, long latestAgeMs) {
    public double[] values() {
        return Arrays.copyOf(values, values.length);
    }
}
