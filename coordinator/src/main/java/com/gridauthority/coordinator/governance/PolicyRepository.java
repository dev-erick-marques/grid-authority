package com.gridauthority.coordinator.governance;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import lombok.Getter;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Set;

@Repository
@Getter
public class PolicyRepository {
    private static final Set<String> DEMO_ACTIONS = Set.of(
            DeviceCommand.OBSERVE.name(), DeviceCommand.WARN.name(),
            DeviceCommand.PREPARE_BACKUP.name(), DeviceCommand.START_GENERATOR.name(),
            DeviceCommand.SWITCH_TO_GENERATOR.name(), DeviceCommand.SWITCH_TO_UPS.name(),
            DeviceCommand.REDUCE_LOAD.name(), DeviceCommand.SHED_NON_CRITICAL_LOAD.name(),
            DeviceCommand.LOAD_SHED.name(), DeviceCommand.TRANSFER_PRIORITY_LOAD.name(),
            DeviceCommand.EMERGENCY_PROTECTION.name(), DeviceCommand.RESTORE_GRID.name());

    private final PolicyDTO active = new PolicyDTO(
            13, "grid-01", 230, .05, .10, .06, .10, 4, 60, .75, .55, .75, .20, .35, 30, 20, 20,
            DEMO_ACTIONS,
            Map.ofEntries(
                    Map.entry("OBSERVE", 0), Map.entry("WARN", 1),
                    Map.entry("REDUCE_LOAD", 2), Map.entry("PREPARE_BACKUP", 2),
                    Map.entry("SWITCH_TO_GENERATOR", 3), Map.entry("SWITCH_TO_UPS", 3),
                    Map.entry("TRANSFER_PRIORITY_LOAD", 3), Map.entry("SHED_NON_CRITICAL_LOAD", 3),
                    Map.entry("LOAD_SHED", 3), Map.entry("START_GENERATOR", 3),
                    Map.entry("RESTORE_GRID", 2), Map.entry("EMERGENCY_PROTECTION", 4)));

    private final PolicyValidator validator;

    public PolicyRepository(PolicyValidator validator) {
        this.validator = validator;
        validator.validate(active);
    }

}
