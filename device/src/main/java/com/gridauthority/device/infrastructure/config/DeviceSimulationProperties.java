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

    /** Healthy-grid behaviour. Must match the coordinator policy nominal voltage. */
    @Getter
    @Setter
    public static class Voltage {
        private double base = 230.0;
        /** Stationary standard deviation (V) of the slow, correlated fluctuation. */
        private double wanderStd = 1.2;
        private double wanderTimeConstantSeconds = 20.0;
        private double loadSwing = 1.5;
        private double loadPeriodSeconds = 300.0;
        /** Sensor measurement noise (V). */
        private double noiseStd = 0.25;
        /** Sensor resolution (V); 0 disables quantization. */
        private double resolution = 0.1;
    }

    @Getter
    @Setter
    public static class Schedule {
        private long ms = 1000;
    }

    /** Progressive sag/swell events. Percentages are relative to the nominal voltage. */
    @Getter
    @Setter
    public static class Surge {
        private double swellPercent = 12.0;
        private double sagPercent = 14.0;
        private double rampSeconds = 40.0;
        private double holdSeconds = 25.0;
        private double recoverSeconds = 30.0;
        /** Random +/- variation applied to magnitude and durations of auto-cycle events. */
        private double cycleVariationPercent = 25.0;
        /** Quiet time between the end of an auto-cycle event and the start of the next one. */
        private long intervalMs = 90000;
    }
}
