package com.gridauthority.coordinator.prediction;

import org.springframework.stereotype.Service;

@Service
public class TrendEstimator {

    public double slope(double[] values) {
        if (values == null || values.length < 2) return 0;
        double n = values.length, sx = 0, sy = 0, sxy = 0, sx2 = 0;
        for (int i = 0; i < values.length; i++) {
            sx += i;
            sy += values[i];
            sxy += i * values[i];
            sx2 += i * i;
        }
        double den = n * sx2 - sx * sx;
        return den == 0 ? 0 : (n * sxy - sx * sy) / den;
    }

    public double acceleration(double[] values) {
        if (values == null || values.length < 6) return 0;
        int mid = values.length / 2;
        double a = slope(java.util.Arrays.copyOfRange(values, 0, mid));
        double b = slope(java.util.Arrays.copyOfRange(values, mid, values.length));
        return (b - a) / mid;
    }
}
