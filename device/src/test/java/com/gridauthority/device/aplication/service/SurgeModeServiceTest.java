package com.gridauthority.device.aplication.service;

import com.gridauthority.device.domain.model.GridVoltageModel;
import com.gridauthority.device.domain.model.GridVoltageModel.Phase;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SurgeModeServiceTest {

    private TaskScheduler scheduler;
    private ScheduledFuture<?> future;
    private GridVoltageModel model;
    private SurgeModeService service;

    @BeforeEach
    void setUp() {
        scheduler = mock(TaskScheduler.class);
        future = mock(ScheduledFuture.class);
        doReturn(future).when(scheduler).schedule(any(Runnable.class), any(Instant.class));
        model = new GridVoltageModel(
                new GridVoltageModel.Params(230, 1.2, 20, 1.5, 300, 0.25, 0.1), new Random(1));
        service = new SurgeModeService(new DeviceSimulationProperties(), scheduler, model);
    }

    @Test
    void forceSurgeStartsSwellAndForceSagStartsSag() {
        service.forceSurge();
        assertThat(service.isSurgeActive()).isTrue();
        model.next(10);
        assertThat(model.disturbance()).isPositive();

        service.forceSag();
        for (int i = 0; i < 60; i++) model.next(1);
        assertThat(model.disturbance()).isNegative();
    }

    @Test
    void forceNormalRecoversGraduallyInsteadOfJumping() {
        service.forceSurge();
        for (int i = 0; i < 40; i++) model.next(1);
        service.forceNormal();
        assertThat(model.phase()).isEqualTo(Phase.RECOVER);
        assertThat(service.isSurgeActive()).isTrue();
    }

    @Test
    void stoppingCycleCancelsPendingEventAndDoesNotRestartIt() {
        service.startAutoCycle();
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), any(Instant.class));

        service.stopAutoCycle();
        verify(future).cancel(false);

        task.getValue().run();

        assertThat(model.phase()).isEqualTo(Phase.IDLE);
        verify(scheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void activeCycleStartsAnEventAndSchedulesTheNextOne() {
        service.startAutoCycle();
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(task.capture(), any(Instant.class));

        task.getValue().run();

        assertThat(model.phase()).isEqualTo(Phase.RAMP);
        verify(scheduler, times(2)).schedule(any(Runnable.class), any(Instant.class));
    }
}
