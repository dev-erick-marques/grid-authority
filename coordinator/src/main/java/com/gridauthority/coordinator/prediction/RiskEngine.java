package com.gridauthority.coordinator.prediction;

import org.springframework.stereotype.Service;

@Service
public class RiskEngine {

    /** Weights of the independent risk signals; must be non-negative and sum to 1. */
    public record RiskWeights(double cv, double trend, double deviation, double ttt, double acceleration) {
        public static final RiskWeights DEFAULT = new RiskWeights(.15, .20, .25, .25, .15);
        public RiskWeights {
            if (cv < 0 || trend < 0 || deviation < 0 || ttt < 0 || acceleration < 0)
                throw new IllegalArgumentException("Risk weights must not be negative");
            if (Math.abs(cv + trend + deviation + ttt + acceleration - 1.0) > 1e-6)
                throw new IllegalArgumentException("Risk weights must sum to 1");
        }
    }

    private final RiskWeights weights;

    public RiskEngine() { this(RiskWeights.DEFAULT); }

    public RiskEngine(RiskWeights weights) { this.weights = weights; }

    public double score(double cvRisk, double trendRisk, double deviationRisk, double tttRisk, double accelerationRisk) {
        double r = weights.cv() * clamp(cvRisk) + weights.trend() * clamp(trendRisk)
                + weights.deviation() * clamp(deviationRisk) + weights.ttt() * clamp(tttRisk)
                + weights.acceleration() * clamp(accelerationRisk);
        return clamp(r);
    }

    /** @param forecastReliability 0..1 quality of the statistical fit (see {@link #fitQuality}) */
    public double confidence(double observability, double forecastReliability) {
        return clamp(.70 * observability + .30 * forecastReliability);
    }

    /** 1 when the prediction at the horizon is tight relative to the critical band, 0 when it is noise-dominated. */
    public double fitQuality(double predictionStdErrorVolts, double criticalBandVolts) {
        if (!Double.isFinite(predictionStdErrorVolts) || criticalBandVolts <= 0) return 0;
        return clamp(1 - predictionStdErrorVolts / criticalBandVolts);
    }

    private double clamp(double x) { return Math.max(0, Math.min(1, x)); }
}