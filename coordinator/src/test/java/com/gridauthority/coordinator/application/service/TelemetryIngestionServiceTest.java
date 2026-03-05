package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.application.dto.DeviceTelemetryDTO;
import com.gridauthority.coordinator.domain.model.DeviceState;
import com.gridauthority.coordinator.infrastructure.repository.MetricsHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class TelemetryIngestionServiceTest {

    private final TelemetryIngestionService telemetryIngestionService;
    private final MetricsHistoryRepository metricsHistoryRepository;

    TelemetryIngestionServiceTest(
            TelemetryIngestionService telemetryIngestionService,
            MetricsHistoryRepository metricsHistoryRepository
    ) {
        this.telemetryIngestionService = telemetryIngestionService;
        this.metricsHistoryRepository = metricsHistoryRepository;
    }

    private static final String DEVICE_1 = "device-01";
    private static final String DEVICE_2 = "device-02";

    @Test
    void shouldProcessFloodTelemetryForTwoDevices() throws InterruptedException {
        int totalMessages = 50;
        CountDownLatch latch = new CountDownLatch(totalMessages * 2);

        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (int i = 0; i < totalMessages; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    double voltage = 218.0 + (Math.random() * 4.0); // 218~222V
                    telemetryIngestionService.ingest(buildTelemetry(DEVICE_1, "Device Alpha", voltage));
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < totalMessages; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    double voltage = index % 7 == 0
                            ? 500.0 + (Math.random() * 19500.0)  // spike extremo ocasional
                            : 215.0 + (Math.random() * 10.0);    // baseline instável
                    telemetryIngestionService.ingest(buildTelemetry(DEVICE_2, "Device Beta", voltage));
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        Thread.sleep(200);

        List<DeviceMetricsDTO> device1Metrics = metricsHistoryRepository.getByDevice(DEVICE_1);
        List<DeviceMetricsDTO> device2Metrics = metricsHistoryRepository.getByDevice(DEVICE_2);

        System.out.println("=== DEVICE 1 (Stable) ===");
        device1Metrics.forEach(m -> System.out.printf(
                "[%s] mean=%.2f | std=%.4f | cv=%.4f",
                m.deviceId(), m.mean(), m.std(), m.cv()
        ));

        System.out.println("\n=== DEVICE 2 (Unstable/Storm) ===");
        device2Metrics.forEach(m -> System.out.printf(
                "[%s] mean=%.2f | std=%.4f | cv=%.4f",
                m.deviceId(), m.mean(), m.std(), m.cv()
        ));

        assertThat(device1Metrics).isNotEmpty();
        assertThat(device2Metrics).isNotEmpty();

        double maxCvDevice1 = device1Metrics.stream().mapToDouble(DeviceMetricsDTO::cv).max().orElse(0);
        double maxCvDevice2 = device2Metrics.stream().mapToDouble(DeviceMetricsDTO::cv).max().orElse(0);

        System.out.printf("%nMax CV Device 1 (stable): %.4f%n", maxCvDevice1);
        System.out.printf("Max CV Device 2 (storm):  %.4f%n", maxCvDevice2);

        assertThat(maxCvDevice2).isGreaterThan(maxCvDevice1);
    }

    private DeviceTelemetryDTO buildTelemetry(String deviceId, String deviceName, double voltage) {
        return new DeviceTelemetryDTO(
                deviceId,
                deviceName,
                voltage,
                DeviceState.ACTIVE,
                Instant.now(),
                "http://localhost:8080",
                "sha256:config-hash-" + deviceId
        );
    }
}