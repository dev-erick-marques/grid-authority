package com.gridauthority.device.aplication.service;

import com.gridauthority.device.domain.model.GridVoltageModel;
import com.gridauthority.device.domain.model.GridVoltageModel.Disturbance;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

/**
 * Triggers progressive sag/swell events on the voltage model. The model itself owns the event lifecycle
 * (ramp, hold, recover), so there are no "end" tasks to leak or restart the cycle after a stop.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SurgeModeService {

    private final DeviceSimulationProperties properties;
    private final TaskScheduler taskScheduler;
    private final GridVoltageModel model;
    private final Random random = new Random();
    private ScheduledFuture<?> cycleHandle;
    private boolean cycleActive;

    public boolean isSurgeActive() {
        return model.phase() != GridVoltageModel.Phase.IDLE;
    }

    /** Gradual overvoltage (swell). */
    public synchronized void forceSurge() {
        stopCycle();
        Disturbance d = build(+1, 1, 1);
        model.start(d);
        log.warn("[SURGE] SWELL started — target={}V ramp={}s", round(d.deltaVolts()), d.rampSeconds());
    }

    /** Gradual undervoltage (sag / brownout). */
    public synchronized void forceSag() {
        stopCycle();
        Disturbance d = build(-1, 1, 1);
        model.start(d);
        log.warn("[SURGE] SAG started — target={}V ramp={}s", round(d.deltaVolts()), d.rampSeconds());
    }

    /** Smooth recovery to the healthy level (no instant jump). */
    public synchronized void forceNormal() {
        stopCycle();
        model.release();
        log.info("[SURGE] Recovery started — voltage returning to nominal");
    }

    public synchronized void startAutoCycle() {
        stopCycle();
        cycleActive = true;
        log.info("[SURGE] Auto-cycle started — interval={}ms", properties.getSurge().getIntervalMs());
        scheduleNext(properties.getSurge().getIntervalMs());
    }

    public synchronized void stopAutoCycle() {
        stopCycle();
        model.release();
        log.info("[SURGE] Auto-cycle stopped");
    }

    private void scheduleNext(long delayMs) {
        cycleHandle = taskScheduler.schedule(this::runCycleEvent, Instant.now().plusMillis(delayMs));
    }

    private synchronized void runCycleEvent() {
        if (!cycleActive) return;
        double v = properties.getSurge().getCycleVariationPercent() / 100.0;
        Disturbance d = build(random.nextBoolean() ? 1 : -1, jitter(v), jitter(v));
        model.start(d);
        log.warn("[SURGE] Cycle event — delta={}V total={}s", round(d.deltaVolts()), d.totalSeconds());
        scheduleNext((long) (d.totalSeconds() * 1000) + properties.getSurge().getIntervalMs());
    }

    private void stopCycle() {
        cycleActive = false;
        if (cycleHandle != null && !cycleHandle.isDone()) {
            cycleHandle.cancel(false);
        }
        cycleHandle = null;
    }

    private Disturbance build(int sign, double magnitudeFactor, double timeFactor) {
        DeviceSimulationProperties.Surge s = properties.getSurge();
        double percent = sign > 0 ? s.getSwellPercent() : s.getSagPercent();
        double delta = sign * properties.getVoltage().getBase() * percent / 100.0 * magnitudeFactor;
        return new Disturbance(delta, s.getRampSeconds() * timeFactor,
                s.getHoldSeconds() * timeFactor, s.getRecoverSeconds() * timeFactor);
    }

    private double jitter(double variation) {
        return 1 + (random.nextDouble() * 2 - 1) * variation;
    }

    private static double round(double v) { return Math.round(v * 10) / 10.0; }
}
