package com.gridauthority.coordinator.observability;

import org.springframework.stereotype.Service;

@Service
public class ObservabilityService {

    private final FreshnessCalculator freshness;
    private final CoverageCalculator coverage;
    private final SignalQuality quality;

    public ObservabilityService(FreshnessCalculator f, CoverageCalculator c, SignalQuality q) {
        freshness = f;
        coverage = c;
        quality = q;
    }

    public double score(double[] values, long received, long expected, long ageMs) {
        double F = freshness.calculate(ageMs, 5000), C = coverage.calculate(received, expected), Q = quality.calculate(values);
        return .30 * F + .25 * C + .30 * Q + .15 * (values != null && values.length >= 5 ? 1 : 0.5);
    }
}
