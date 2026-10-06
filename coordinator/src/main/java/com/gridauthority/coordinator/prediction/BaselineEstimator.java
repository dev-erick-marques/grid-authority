package com.gridauthority.coordinator.prediction;

import org.springframework.stereotype.Service;

@Service
public class BaselineEstimator {

    public double estimate(double[] values) {
        if (values == null || values.length == 0) return 0;
        int n = Math.max(1, values.length / 3);
        double sum = 0;
        for (int i = 0; i < n; i++) sum += values[i];
        return sum / n;
    }

    public double deviation(double current, double baseline) {
        return baseline == 0 ? 0 : Math.abs(current - baseline) / Math.abs(baseline);
    }
}
