package com.gridauthority.coordinator.decision;
import com.gridauthority.coordinator.application.dto.PolicyDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import org.springframework.stereotype.Service;

@Service
public class DecisionEngine {

    /** Fraction of the prediction horizon below which the generator must already be starting. */
    static final double START_LEAD_FRACTION = 0.5;

    /**
     * Predictive decision. The time-to-breach drives preventive escalation directly, because a weighted
     * risk score alone only becomes large when the limit is already close.
     *
     * @param ttt seconds until the forecast leaves the critical band; null if no credible trend
     */
    public DeviceCommand decide(double risk,double confidence,Double ttt,boolean emergency,PolicyDTO p){
        if(emergency) return DeviceCommand.EMERGENCY_PROTECTION;
        boolean breachImminent = ttt!=null && ttt<=p.horizonSeconds();
        boolean breachClose = ttt!=null && ttt<=p.horizonSeconds()*START_LEAD_FRACTION;
        if(risk < p.warningRisk() && !breachImminent) return DeviceCommand.OBSERVE;
        if(confidence < p.minimumConfidence()) return DeviceCommand.WARN;
        if(breachClose || (breachImminent && risk >= p.criticalRisk())) return DeviceCommand.START_GENERATOR;
        if(breachImminent || risk >= p.preemptiveRisk()) return DeviceCommand.PREPARE_BACKUP;
        return DeviceCommand.WARN;
    }

    /**
     * A device is "stable" only when nothing indicates a problem: no emergency, no predicted breach within the
     * horizon, voltage inside the warning band, calm signal and a trustworthy prediction.
     */
    public boolean isStableForRecovery(double risk,double cvPercent,double slope,double confidence,
                                       double deviation,Double ttt,boolean emergency,PolicyDTO p){
        return !emergency
                && (ttt==null || ttt>p.horizonSeconds())
                && deviation<=p.voltageWarning()
                && risk<=p.recoveryRisk()
                && cvPercent/100.0<=p.cvWarning()
                && Math.abs(slope)<=p.maxRateOfChange()*0.25
                && confidence>=p.minimumConfidence();
    }

    /**
     * RESTORE_GRID replaces the decision only when it is safe and meaningful: never during an emergency or a
     * preventive action, and only if a mitigation is actually active.
     */
    public DeviceCommand applyRecovery(DeviceCommand desired,boolean emergency,boolean mitigationActive,
                                       long stableDurationMs,PolicyDTO p){
        if(emergency||!mitigationActive) return desired;
        if(desired!=DeviceCommand.OBSERVE && desired!=DeviceCommand.WARN) return desired;
        return stableDurationMs>=p.recoveryStableSeconds()*1000L ? DeviceCommand.RESTORE_GRID : desired;
    }
}
