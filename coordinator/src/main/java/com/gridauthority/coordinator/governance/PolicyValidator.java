package com.gridauthority.coordinator.governance;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import org.springframework.stereotype.Service;

@Service
public class PolicyValidator {
    public void validate(PolicyDTO p) {
        if (p == null || p.policyVersion() < 1 || p.gridId() == null || p.gridId().isBlank()) throw new IllegalArgumentException("Invalid policy identity");
        if (p.nominalVoltage() <= 0 || p.horizonSeconds() <= 0 || p.maxRateOfChange() <= 0) throw new IllegalArgumentException("Invalid physical/prediction limits");
        if (!between01(p.voltageWarning()) || !between01(p.voltageCritical()) || !between01(p.minimumConfidence())
                || !between01(p.preemptiveRisk()) || !between01(p.criticalRisk()) || !between01(p.warningRisk()) || !between01(p.recoveryRisk())) {
            throw new IllegalArgumentException("Policy ratios must be between 0 and 1");
        }
        if (p.voltageWarning() >= p.voltageCritical() || p.warningRisk() >= p.preemptiveRisk() || p.preemptiveRisk() >= p.criticalRisk()) {
            throw new IllegalArgumentException("Policy thresholds must be ordered");
        }
        if (p.cooldownSeconds() < 0 || p.maxAutomaticActionsPerHour() < 1 || p.recoveryStableSeconds() < 1) {
            throw new IllegalArgumentException("Invalid governance constraints");
        }
        if (p.allowedActions() == null || p.allowedActions().isEmpty()) throw new IllegalArgumentException("Policy must authorize at least one action");
    }

    private boolean between01(double value) { return Double.isFinite(value) && value >= 0 && value <= 1; }
}
