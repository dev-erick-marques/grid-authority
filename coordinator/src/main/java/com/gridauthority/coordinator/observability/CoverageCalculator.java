package com.gridauthority.coordinator.observability;

import org.springframework.stereotype.Service;

@Service
public class CoverageCalculator {

    public double calculate(long received, long expected) {
        return expected <= 0 ? 1 : Math.clamp((double) received / expected, 0, 1);
    }
}
