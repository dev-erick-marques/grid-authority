package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.domain.model.DeviceSurgeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceSurgeStateRepositoryTest {

    private DeviceSurgeStateRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DeviceSurgeStateRepository();
    }

    @Test
    void get_shouldReturnInactive_forUnknownDevice() {
        assertThat(repository.get("unknown")).isEqualTo(DeviceSurgeState.INACTIVE);
    }

    @Test
    void set_andGet_shouldReturnStoredState() {
        repository.set("device-1", DeviceSurgeState.SURGE_ACTIVE);

        assertThat(repository.get("device-1")).isEqualTo(DeviceSurgeState.SURGE_ACTIVE);
    }

    @Test
    void set_shouldOverwriteExistingState() {
        repository.set("device-1", DeviceSurgeState.SURGE_ACTIVE);
        repository.set("device-1", DeviceSurgeState.CYCLE_ACTIVE);

        assertThat(repository.get("device-1")).isEqualTo(DeviceSurgeState.CYCLE_ACTIVE);
    }

    @Test
    void set_shouldResetToInactive() {
        repository.set("device-1", DeviceSurgeState.SURGE_ACTIVE);
        repository.set("device-1", DeviceSurgeState.INACTIVE);

        assertThat(repository.get("device-1")).isEqualTo(DeviceSurgeState.INACTIVE);
    }

    @Test
    void get_shouldIsolateStatesPerDevice() {
        repository.set("device-A", DeviceSurgeState.SURGE_ACTIVE);
        repository.set("device-B", DeviceSurgeState.CYCLE_ACTIVE);

        assertThat(repository.get("device-A")).isEqualTo(DeviceSurgeState.SURGE_ACTIVE);
        assertThat(repository.get("device-B")).isEqualTo(DeviceSurgeState.CYCLE_ACTIVE);
    }
}
