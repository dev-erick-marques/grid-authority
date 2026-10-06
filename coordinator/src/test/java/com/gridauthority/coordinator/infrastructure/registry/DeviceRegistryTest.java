package com.gridauthority.coordinator.infrastructure.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceRegistryTest {

    private DeviceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DeviceRegistry();
    }

    @Test
    void resolve_shouldReturnEmpty_forUnknownDevice() {
        assertThat(registry.resolve("unknown")).isEmpty();
    }

    @Test
    void register_andResolve_shouldReturnRegisteredUrl() {
        registry.register("device-1", "http://device-1:8081");

        Optional<String> url = registry.resolve("device-1");

        assertThat(url).isPresent().hasValue("http://device-1:8081");
    }

    @Test
    void register_shouldOverwriteExistingUrl() {
        registry.register("device-1", "http://old-url:8081");
        registry.register("device-1", "http://new-url:8081");

        assertThat(registry.resolve("device-1")).hasValue("http://new-url:8081");
    }

    @Test
    void deregister_shouldRemoveDevice() {
        registry.register("device-1", "http://device-1:8081");
        registry.deregister("device-1");

        assertThat(registry.resolve("device-1")).isEmpty();
    }

    @Test
    void deregister_onUnknownDevice_shouldNotThrow() {
        registry.deregister("never-registered");
        assertThat(registry.resolve("never-registered")).isEmpty();
    }

    @Test
    void register_shouldIsolateMultipleDevices() {
        registry.register("device-A", "http://a:8081");
        registry.register("device-B", "http://b:8081");

        assertThat(registry.resolve("device-A")).hasValue("http://a:8081");
        assertThat(registry.resolve("device-B")).hasValue("http://b:8081");
    }

    @Test
    void register_shouldBeThreadSafe() throws InterruptedException {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final String deviceId = "device-" + i;
            executor.submit(() -> {
                try {
                    registry.register(deviceId, "http://" + deviceId + ":8081");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        for (int i = 0; i < threads; i++) {
            assertThat(registry.resolve("device-" + i)).isPresent();
        }
    }
}
