package com.gridauthority.device.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "device.simulation")
public class DeviceSimulationProperties {
    private String id;
    private String name;
    private Voltage voltage = new Voltage();
    private Schedule schedule = new Schedule();
    private Surge surge = new Surge();

    @Getter
    @Setter
    public static class Voltage {
        private double base = 220.0;
        private double variation = 4.0;
        private double surgeVariation = 80.0;
    }

    @Getter
    @Setter
    public static class Schedule {
        private long ms = 1000;
    }

    @Getter
    @Setter
    public static class Surge {
        private long durationMs = 15000;
        private long intervalMs = 30000;
        private boolean enabled = false;
    }
}