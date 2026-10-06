package com.gridauthority.coordinator.observability;

import org.springframework.stereotype.Service;

@Service
public class FreshnessCalculator {

    public double calculate(long ageMs, double tauMs) {
        return Math.exp(-Math.max(0, ageMs) / tauMs);
    }
}
