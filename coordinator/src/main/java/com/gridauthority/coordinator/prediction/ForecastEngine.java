package com.gridauthority.coordinator.prediction;

import org.springframework.stereotype.Service;

@Service
public class ForecastEngine {


    public double forecast(double current, double slope, double acceleration, int horizonSeconds) {
        return current + slope * horizonSeconds + 0.5 * acceleration * horizonSeconds * (double) horizonSeconds;
    }

    public double forecast(double current, double slope, int horizonSeconds) {
        return forecast(current, slope, 0, horizonSeconds);
    }

    public Double timeToBreach(double current, double slope, double acceleration, double lower, double upper) {
        if (current <= lower || current >= upper) return 0.0;
        Double tUp = firstCrossing(current, slope, acceleration, upper);
        Double tLow = firstCrossing(current, slope, acceleration, lower);
        if (tUp == null) return tLow;
        if (tLow == null) return tUp;
        return Math.min(tUp, tLow);
    }


    private Double firstCrossing(double current, double slope, double acceleration, double bound) {
        double c = current - bound;
        if (Math.abs(acceleration) < 1e-12) {
            if (Math.abs(slope) < 1e-9) return null;
            double t = -c / slope;
            return t > 0 ? t : null;
        }
        double a = 0.5 * acceleration, b = slope;
        double disc = b * b - 4 * a * c;
        if (disc < 0) return null;
        double sq = Math.sqrt(disc);
        double t1 = (-b - sq) / (2 * a), t2 = (-b + sq) / (2 * a);
        double best = Double.POSITIVE_INFINITY;
        if (t1 > 0) best = Math.min(best, t1);
        if (t2 > 0) best = Math.min(best, t2);
        return Double.isInfinite(best) ? null : best;
    }
}