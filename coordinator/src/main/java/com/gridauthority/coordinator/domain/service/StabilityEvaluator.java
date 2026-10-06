package com.gridauthority.coordinator.domain.service;
import com.gridauthority.coordinator.application.dto.StabilityThreshold;
import org.springframework.stereotype.Service;
@Service
public class StabilityEvaluator {
 public StabilityThreshold getThreshold(){return new StabilityThreshold(10.0,"CV","CV is an input to predictive risk, not a direct action trigger");}
}
