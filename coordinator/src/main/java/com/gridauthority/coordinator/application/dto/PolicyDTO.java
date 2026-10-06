package com.gridauthority.coordinator.application.dto;

import java.util.Map;
import java.util.Set;

public record PolicyDTO(
        long policyVersion,
        String gridId,
        double nominalVoltage,
        double voltageWarning,
        double voltageCritical,
        double cvWarning,
        double cvCritical,
        double maxRateOfChange,
        int horizonSeconds,
        double minimumConfidence,
        double preemptiveRisk,
        double criticalRisk,
        double warningRisk,
        double recoveryRisk,
        int cooldownSeconds,
        int maxAutomaticActionsPerHour,
        int recoveryStableSeconds,
        Set<String> allowedActions,
        Map<String, Integer> severity
) {
}
