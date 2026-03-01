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
    private Coordinator coordinator = new Coordinator();

    @Getter
    @Setter
    public static class Voltage {
        private double base = 220.0;
        private double variation = 4.0;
    }

    @Getter
    @Setter
    public static class Schedule {
        private long ms = 1000;
    }

    @Getter
    @Setter
    public static class Coordinator {
        private String url = "http://localhost:8080";
    }
}