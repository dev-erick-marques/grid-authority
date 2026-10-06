package com.gridauthority.coordinator.prediction;
import org.springframework.stereotype.Service;

@Service
public class BaselineEstimator {

    public double deviation(double current, double baseline) {
        return baseline == 0 ? 0 : Math.abs(current - baseline) / Math.abs(baseline);
    }
}
