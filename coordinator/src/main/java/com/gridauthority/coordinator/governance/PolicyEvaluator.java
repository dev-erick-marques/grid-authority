package com.gridauthority.coordinator.governance;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Service;

@Service
public class PolicyEvaluator {
    public boolean allowed(PolicyDTO p, DeviceCommand c) {
        return p.allowedActions().contains(c.name());
    }
}
