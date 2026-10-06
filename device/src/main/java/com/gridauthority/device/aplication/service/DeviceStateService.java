package com.gridauthority.device.aplication.service;

import com.gridauthority.device.domain.model.DeviceCommand;
import com.gridauthority.device.domain.model.DeviceState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class DeviceStateService {
    public enum OperatingMode { NORMAL, BACKUP_PREPARED, GENERATOR_STARTING, GENERATOR_ACTIVE, UPS_ACTIVE, LOAD_REDUCED, PROTECTED }

    private final AtomicReference<OperatingMode> mode = new AtomicReference<>(OperatingMode.NORMAL);

    public DeviceState current() { return DeviceState.ACTIVE; }
    public OperatingMode operatingMode() { return mode.get(); }

    public OperatingMode execute(DeviceCommand command) {
        OperatingMode next = switch (command) {
            case OBSERVE, WARN -> mode.get();
            case PREPARE_BACKUP -> OperatingMode.BACKUP_PREPARED;
            case START_GENERATOR -> OperatingMode.GENERATOR_STARTING;
            case SWITCH_TO_GENERATOR, TRANSFER_PRIORITY_LOAD -> OperatingMode.GENERATOR_ACTIVE;
            case SWITCH_TO_UPS -> OperatingMode.UPS_ACTIVE;
            case REDUCE_LOAD, SHED_NON_CRITICAL_LOAD, LOAD_SHED -> OperatingMode.LOAD_REDUCED;
            case EMERGENCY_PROTECTION -> OperatingMode.PROTECTED;
            case RESTORE_GRID -> OperatingMode.NORMAL;
        };
        mode.set(next);
        log.info("[DEVICE] action={} operatingMode={}", command, next);
        return next;
    }
}
