package com.gridauthority.coordinator.observability;

import org.springframework.stereotype.Service;

@Service
public class SignalQuality {

    public double calculate(double[] v) {
        if (v == null || v.length < 2) return 0;
        for (double x : v) if (!Double.isFinite(x) || x < 0) return .1;
        return 1.0;
    }
}
