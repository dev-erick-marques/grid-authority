package com.gridauthority.coordinator.decision;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Service;

@Service
public class EscalationEngine {
    public boolean isEmergency(double voltage, double critical) {
        return voltage <= critical;
    }

    public DeviceCommand emergencyAction(boolean physicalLimit) {
        return physicalLimit ? DeviceCommand.EMERGENCY_PROTECTION : DeviceCommand.PREPARE_BACKUP;
    }
}
