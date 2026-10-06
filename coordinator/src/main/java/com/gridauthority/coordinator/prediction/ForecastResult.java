package com.gridauthority.coordinator.prediction;

public record ForecastResult(
        double trend,
        double acceleration,
        double baselineDeviation,
        double forecast,
        double riskScore,
        double confidence,
        Double timeToThreshold,
        double observabilityScore) {
}
