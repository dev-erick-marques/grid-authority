package com.gridauthority.coordinator.domain.model;

public enum DeviceCommand {
    OBSERVE, WARN, PREPARE_BACKUP, START_GENERATOR, SWITCH_TO_GENERATOR,
    SWITCH_TO_UPS, REDUCE_LOAD, SHED_NON_CRITICAL_LOAD, LOAD_SHED,
    TRANSFER_PRIORITY_LOAD, EMERGENCY_PROTECTION, RESTORE_GRID;

    /** Actions that protect equipment or supply; they must never be starved by the routine hourly budget. */
    public boolean isSafetyCritical() {
        return switch (this) {
            case EMERGENCY_PROTECTION, START_GENERATOR, SWITCH_TO_GENERATOR, SWITCH_TO_UPS,
                 SHED_NON_CRITICAL_LOAD, LOAD_SHED, TRANSFER_PRIORITY_LOAD -> true;
            default -> false;
        };
    }

    /** Actions that leave the device in a mitigated state which RESTORE_GRID later has to revert. */
    public boolean isMitigation() {
        return this != OBSERVE && this != WARN && this != RESTORE_GRID;
    }
}
