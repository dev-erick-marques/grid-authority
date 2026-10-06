package com.gridauthority.coordinator.prediction;

import org.springframework.stereotype.Service;

@Service
public class RiskEngine {
    public double score(double cvRatio, double trendRisk, double deviationRisk, double rateRisk, double tttRisk) {
        double r = .20 * clamp(cvRatio) + .25 * clamp(trendRisk) + .15 * clamp(deviationRisk) + .15 * clamp(rateRisk) + .25 * clamp(tttRisk);
        return clamp(r);
    }

    public double confidence(double observability, double stabilityOfTrend) {
        return clamp(.70 * observability + .30 * stabilityOfTrend);
    }

    private double clamp(double x) {
        return Math.clamp(x, 0, 1);
    }
}
