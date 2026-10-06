package com.gridauthority.coordinator.prediction;

import org.springframework.stereotype.Service;

@Service
public class ForecastEngine {

    public double forecast(double current, double slope, int horizonSeconds) {
        return current + slope * horizonSeconds;
    }

    public Double timeToThreshold(double current, double slope, double threshold) {
        if (Math.abs(slope) < 1e-9) return null;
        double t = (threshold - current) / slope;
        return t >= 0 ? t : null;
    }
}
