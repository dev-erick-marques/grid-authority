package com.gridauthority.device.aplication.service;

import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurgeModeService {

    private final DeviceSimulationProperties properties;
    private final TaskScheduler taskScheduler;
    private final AtomicBoolean surgeActive = new AtomicBoolean(false);
    private ScheduledFuture<?> cycleHandle;

    public boolean isSurgeActive() {
        return surgeActive.get();
    }

    public void forceSurge() {
        cancelCycle();
        activateSurge();
        log.warn("[SURGE] Surge forced manually");
    }

    public void forceNormal() {
        cancelCycle();
        deactivateSurge();
        log.info("[SURGE] Return to normal state forced manually");
    }

    public void startAutoCycle() {
        cancelCycle();
        log.info("[SURGE] Automatic cycle started — duration={}ms interval={}ms",
                properties.getSurge().getDurationMs(),
                properties.getSurge().getIntervalMs());
        scheduleNextSurge();
    }

    public void stopAutoCycle() {
        cancelCycle();
        deactivateSurge();
        log.info("[SURGE] Automatic cycle stopped");
    }

    private void scheduleNextSurge() {
        long intervalMs = properties.getSurge().getIntervalMs();
        cycleHandle = taskScheduler.schedule(() -> {
            activateSurge();
            scheduleSurgeEnd();
        }, Instant.now().plusMillis(intervalMs));
    }

    private void scheduleSurgeEnd() {
        long durationMs = properties.getSurge().getDurationMs();
        taskScheduler.schedule(() -> {
            deactivateSurge();
            scheduleNextSurge();
        }, Instant.now().plusMillis(durationMs));
    }

    private void activateSurge() {
        surgeActive.set(true);
        log.warn("[SURGE] ACTIVE — unstable voltage for {}ms", properties.getSurge().getDurationMs());
    }

    private void deactivateSurge() {
        surgeActive.set(false);
        log.info("[SURGE] INACTIVE — voltage back to normal");
    }

    private void cancelCycle() {
        if (cycleHandle != null && !cycleHandle.isDone()) {
            cycleHandle.cancel(false);
        }
    }
}