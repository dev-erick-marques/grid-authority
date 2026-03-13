package com.gridauthority.coordinator.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import com.gridauthority.coordinator.domain.model.DeviceSurgeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

class MetricsHistoryRepositoryTest {

    private MetricsHistoryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MetricsHistoryRepository();
        ReflectionTestUtils.setField(repository, "HISTORY_SIZE", 3);
    }

    @Test
    void shouldAddAndRetrieveByDevice() {
        DeviceMetricsDTO dto = buildDto("device-1", 1);

        repository.add(dto);

        List<DeviceMetricsDTO> history = repository.getByDevice("device-1");

        assertEquals(1, history.size());
        assertEquals("device-1", history.get(0).deviceId());
        assertEquals(1.0, history.get(0).mean());
    }

    @Test
    void shouldReturnEmptyListWhenDeviceNotFound() {
        List<DeviceMetricsDTO> history = repository.getByDevice("unknown");

        assertNotNull(history);
        assertTrue(history.isEmpty());
    }

    @Test
    void shouldRespectHistorySizeLimitAndEvictOldest() {
        repository.add(buildDto("device-1", 1));
        repository.add(buildDto("device-1", 2));
        repository.add(buildDto("device-1", 3));
        repository.add(buildDto("device-1", 4));

        List<DeviceMetricsDTO> history = repository.getByDevice("device-1");

        assertEquals(3, history.size());


        assertFalse(history.stream().anyMatch(d -> d.mean() == 1.0));
        assertTrue(history.stream().anyMatch(d -> d.mean() == 2.0));
        assertTrue(history.stream().anyMatch(d -> d.mean() == 3.0));
        assertTrue(history.stream().anyMatch(d -> d.mean() == 4.0));
    }

    @Test
    void shouldMaintainIndependentHistoriesPerDevice() {
        repository.add(buildDto("device-1", 1));
        repository.add(buildDto("device-2", 10));

        List<DeviceMetricsDTO> history1 = repository.getByDevice("device-1");
        List<DeviceMetricsDTO> history2 = repository.getByDevice("device-2");

        assertEquals(1, history1.size());
        assertEquals(1.0, history1.get(0).mean());

        assertEquals(1, history2.size());
        assertEquals(10.0, history2.get(0).mean());
    }

    @Test
    void shouldReturnSnapshotFromGetAll() {
        repository.add(buildDto("device-1", 1));
        repository.add(buildDto("device-2", 2));

        Map<String, List<DeviceMetricsDTO>> all = repository.getAll();

        assertEquals(2, all.size());
        assertTrue(all.containsKey("device-1"));
        assertTrue(all.containsKey("device-2"));
    }

    @Test
    void getAllShouldReturnDefensiveCopy() {
        repository.add(buildDto("device-1", 1));

        Map<String, List<DeviceMetricsDTO>> snapshot = repository.getAll();
        snapshot.get("device-1").clear();

        List<DeviceMetricsDTO> history = repository.getByDevice("device-1");
        assertEquals(1, history.size());
    }

    private DeviceMetricsDTO buildDto(String deviceId, double mean) {
        return new DeviceMetricsDTO(
                deviceId,
                "Device-" + deviceId,
                mean,
                0.1,
                0.01,
                DeviceState.SHUTDOWN,
                DeviceSurgeState.INACTIVE,
                1,
                10,
                Instant.now()
        );
    }
}