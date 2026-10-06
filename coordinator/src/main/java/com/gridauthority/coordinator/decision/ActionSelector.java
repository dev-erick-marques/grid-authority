package com.gridauthority.coordinator.decision;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ActionSelector {
    private final boolean backupAvailable;
    private final boolean priorityLoadAvailable;

    public ActionSelector(
            @Value("${actuator.backup-available:true}") boolean backupAvailable,
            @Value("${actuator.priority-load-available:true}") boolean priorityLoadAvailable) {
        this.backupAvailable = backupAvailable;
        this.priorityLoadAvailable = priorityLoadAvailable;
    }

    public DeviceCommand refine(DeviceCommand base) {
        if (base == DeviceCommand.START_GENERATOR && !backupAvailable && priorityLoadAvailable) {
            return DeviceCommand.SHED_NON_CRITICAL_LOAD;
        }
        if (base == DeviceCommand.PREPARE_BACKUP && !backupAvailable && priorityLoadAvailable) {
            return DeviceCommand.REDUCE_LOAD;
        }
        return base;
    }
}
