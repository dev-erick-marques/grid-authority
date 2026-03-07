package com.gridauthority.coordinator.infrastructure.repository;

import com.gridauthority.coordinator.application.dto.DeviceTelemetryDTO;
import com.gridauthority.coordinator.domain.model.DeviceState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


class VoltageWindowRepositoryTest {

    private VoltageWindowRepository repository;
    private final DeviceTelemetryDTO baseTelemetry = new DeviceTelemetryDTO("device-1", "",120.0, DeviceState.ACTIVE, Instant.now(),"http://localhost:8080", "");

    @BeforeEach
    void setUp() throws Exception {
        repository = new VoltageWindowRepository();

        Field field = VoltageWindowRepository.class.getDeclaredField("WINDOW_SIZE");
        field.setAccessible(true);
        field.set(repository, 10);
    }

    @Test
    void getWindowIfFull_shouldReturnEmpty_whenWindowIsNotYetFull() {
        repository.record("device-1", 120.0);
        repository.record("device-1", 119.0);

        assertThat(repository.getWindowIfFull("device-1")).isEmpty();
    }

    @Test
    void getWindowIfFull_shouldReturnEmpty_whenDeviceIsUnknown() {
        assertThat(repository.getWindowIfFull("unknown-device")).isEmpty();
    }

    @Test
    void getWindowIfFull_shouldReturnWindow_whenExactlyFull() {
        fillWindow("device-1", 10, 120.0);

        Optional<double[]> result = repository.getWindowIfFull("device-1");

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(10);
    }

    @Test
    void record_shouldEvictOldestEntry_whenWindowIsAlreadyFull() {
        fillWindow("device-1", 10, 120.0);

        repository.record("device-1", 999.0);

        double[] window = repository.getWindowIfFull("device-1").orElseThrow();

        assertThat(window).hasSize(10);
        assertThat(Arrays.stream(window).filter(v -> v == 999.0).count()).isEqualTo(1);
        assertThat(Arrays.stream(window).filter(v -> v == 120.0).count()).isEqualTo(9);
    }

    @Test
    void record_shouldIsolateWindowsPerDevice() {
        fillWindow("device-A", 5, 118.0);
        fillWindow("device-B", 10, 220.0);

        assertThat(repository.getWindowIfFull("device-A")).isEmpty();
        assertThat(repository.getWindowIfFull("device-B")).isPresent();
    }

    @Test
    void recordAndGet_shouldReturnEmpty_whenWindowIsNotYetFull() {
        Optional<double[]> result = repository.recordAndGet(baseTelemetry);

        assertThat(result).isEmpty();
    }

    @Test
    void recordAndGet_shouldReturnWindow_onTenthCall() {
        for (int i = 0; i < 9; i++) {
            repository.recordAndGet(baseTelemetry);
        }

        Optional<double[]> result = repository.recordAndGet(baseTelemetry);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(10);
    }

    @Test
    void recordAndGet_shouldContinueReturningWindow_afterWindowIsFull() {
        fillWindow("device-1", 10, 120.0);

        Optional<double[]> result = repository.recordAndGet(baseTelemetry);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(10);
    }

    @Test
    void record_shouldBeThreadSafe_underConcurrentWrites() throws InterruptedException {
        int threads = 20;
        int recordsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < recordsPerThread; j++) {
                        repository.record("shared-device", 120.0 + (j % 5));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        Optional<double[]> result = repository.getWindowIfFull("shared-device");
        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(10);
    }

    private void fillWindow(String deviceId, int count, double voltage) {
        for (int i = 0; i < count; i++) {
            repository.record(deviceId, voltage);
        }
    }
}