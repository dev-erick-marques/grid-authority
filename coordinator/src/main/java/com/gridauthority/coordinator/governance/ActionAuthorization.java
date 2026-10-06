package com.gridauthority.coordinator.governance;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Service;

@Service
public class ActionAuthorization {
    private final PolicyEvaluator evaluator;

    public ActionAuthorization(PolicyEvaluator e) {
        evaluator = e;
    }

    public DeviceCommand authorize(PolicyDTO p, DeviceCommand desired) {
        return evaluator.allowed(p, desired) ? desired : DeviceCommand.OBSERVE;
    }
}
