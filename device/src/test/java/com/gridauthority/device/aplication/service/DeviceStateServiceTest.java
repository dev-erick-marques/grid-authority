package com.gridauthority.device.aplication.service;

import com.gridauthority.device.domain.exception.DeviceAlreadyActiveException;
import com.gridauthority.device.domain.exception.DeviceAlreadyShutdownException;
import com.gridauthority.device.domain.model.DeviceState;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeviceStateServiceTest {

    private DeviceStateService service;

    @BeforeEach
    void setUp() {
        DeviceSimulationProperties properties = mock(DeviceSimulationProperties.class);
        when(properties.getId()).thenReturn("device-test");
        service = new DeviceStateService(properties);
    }

    @Test
    void initialStateShouldBeActive() {
        assertThat(service.current()).isEqualTo(DeviceState.ACTIVE);
    }

    @Test
    void shutdownShouldTransitionToShutdown() {
        DeviceState result = service.shutdown();
        assertThat(result).isEqualTo(DeviceState.SHUTDOWN);
        assertThat(service.current()).isEqualTo(DeviceState.SHUTDOWN);
    }

    @Test
    void restartShouldTransitionToActive() {
        service.shutdown();
        DeviceState result = service.restart();
        assertThat(result).isEqualTo(DeviceState.ACTIVE);
        assertThat(service.current()).isEqualTo(DeviceState.ACTIVE);
    }

    @Test
    void shutdownWhenAlreadyShutdownShouldThrow() {
        service.shutdown();
        assertThatThrownBy(() -> service.shutdown())
                .isInstanceOf(DeviceAlreadyShutdownException.class)
                .hasMessageContaining("device-test");
    }

    @Test
    void restartWhenAlreadyActiveShouldThrow() {
        assertThatThrownBy(() -> service.restart())
                .isInstanceOf(DeviceAlreadyActiveException.class)
                .hasMessageContaining("device-test");
    }

    @Test
    void shutdownThenRestartShouldCycleCorrectly() {
        assertThat(service.current()).isEqualTo(DeviceState.ACTIVE);
        service.shutdown();
        assertThat(service.current()).isEqualTo(DeviceState.SHUTDOWN);
        service.restart();
        assertThat(service.current()).isEqualTo(DeviceState.ACTIVE);
    }
}