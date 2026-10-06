package com.gridauthority.coordinator.decision;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Service;

@Service
public class DecisionEngine {
    public DeviceCommand decide(double risk, double confidence, Double ttt, boolean emergency, PolicyDTO p) {

        if (emergency) return DeviceCommand.EMERGENCY_PROTECTION;
        if (risk < p.warningRisk()) return DeviceCommand.OBSERVE;
        if (risk < p.preemptiveRisk()) return DeviceCommand.WARN;
        if (confidence < p.minimumConfidence()) return DeviceCommand.WARN;
        if (risk < p.criticalRisk()) return DeviceCommand.PREPARE_BACKUP;
        if (ttt != null && ttt <= p.horizonSeconds()) return DeviceCommand.START_GENERATOR;
        return DeviceCommand.PREPARE_BACKUP;
    }
}
