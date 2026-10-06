package com.gridauthority.device.domain.model;

import java.util.Random;

/**
 * Pure (framework-free) model of a single-phase grid voltage as seen by a sensor.
 *
 * <p>The signal is the sum of four physically motivated parts:</p>
 * <ul>
 *   <li><b>Wander</b>: a mean-reverting (Ornstein-Uhlenbeck) process, i.e. temporally correlated slow
 *       fluctuation caused by load changes on the feeder. Consecutive samples are close to each other.</li>
 *   <li><b>Load swing</b>: a small, slow sinusoidal drift (daily/shift load pattern, compressed).</li>
 *   <li><b>Disturbance</b>: an optional sag or swell that ramps smoothly to a target, holds, and recovers
 *       smoothly. It is progressive, so there are no voltage jumps between samples.</li>
 *   <li><b>Measurement noise</b>: small gaussian noise, then quantized to the sensor resolution.</li>
 * </ul>
 */
public final class GridVoltageModel {

    public enum Phase { IDLE, RAMP, HOLD, RECOVER }

    public record Params(double nominal, double wanderStd, double wanderTimeConstantSeconds,
                         double loadSwing, double loadPeriodSeconds, double noiseStd, double resolution) {}

    /** A disturbance relative to the nominal voltage: {@code deltaVolts} is negative for a sag, positive for a swell. */
    public record Disturbance(double deltaVolts, double rampSeconds, double holdSeconds, double recoverSeconds) {
        public double totalSeconds() { return rampSeconds + holdSeconds + recoverSeconds; }
    }

    private static final double MAX_DT_SECONDS = 10.0;
    private static final double MAX_OVER_NOMINAL = 1.3;

    private final Params params;
    private final Random random;
    private final double loadPhase;

    private double wander;
    private double clockSeconds;

    private Phase phase = Phase.IDLE;
    private Disturbance event;
    private double phaseElapsed;
    private double rampFrom;
    private double recoverFrom;
    private double offset;

    public GridVoltageModel(Params params, Random random) {
        this.params = params;
        this.random = random;
        this.loadPhase = random.nextDouble() * 2 * Math.PI;
        this.wander = random.nextGaussian() * params.wanderStd();
    }

    /** Starts (or retargets, continuously from the current level) a disturbance. */
    public synchronized void start(Disturbance disturbance) {
        this.rampFrom = offset;
        this.event = disturbance;
        this.phase = Phase.RAMP;
        this.phaseElapsed = 0;
    }

    /** Begins a smooth recovery to the undisturbed level from wherever the disturbance currently is. */
    public synchronized void release() {
        if (phase == Phase.IDLE || phase == Phase.RECOVER) return;
        beginRecover();
    }

    public synchronized Phase phase() { return phase; }

    public synchronized double disturbance() { return offset; }

    /** Advances the model by {@code dtSeconds} and returns the next measured voltage. */
    public synchronized double next(double dtSeconds) {
        double dt = Math.min(Math.max(dtSeconds, 1e-3), MAX_DT_SECONDS);

        double a = Math.exp(-dt / params.wanderTimeConstantSeconds());
        wander = wander * a + params.wanderStd() * Math.sqrt(1 - a * a) * random.nextGaussian();

        clockSeconds += dt;
        double load = params.loadSwing() * Math.sin(2 * Math.PI * clockSeconds / params.loadPeriodSeconds() + loadPhase);

        advanceDisturbance(dt);

        double v = params.nominal() + wander + load + offset + random.nextGaussian() * params.noiseStd();
        v = Math.min(Math.max(v, 0), params.nominal() * MAX_OVER_NOMINAL);
        if (params.resolution() > 0) v = Math.round(v / params.resolution()) * params.resolution();
        return v;
    }

    private void advanceDisturbance(double dt) {
        switch (phase) {
            case IDLE -> offset = 0;
            case RAMP -> {
                phaseElapsed += dt;
                double x = progress(phaseElapsed, event.rampSeconds());
                offset = rampFrom + (event.deltaVolts() - rampFrom) * smoothstep(x);
                if (x >= 1) { phase = Phase.HOLD; phaseElapsed = 0; }
            }
            case HOLD -> {
                phaseElapsed += dt;
                offset = event.deltaVolts();
                if (phaseElapsed >= event.holdSeconds()) beginRecover();
            }
            case RECOVER -> {
                phaseElapsed += dt;
                double x = progress(phaseElapsed, event.recoverSeconds());
                offset = recoverFrom * (1 - smoothstep(x));
                if (x >= 1) { phase = Phase.IDLE; offset = 0; event = null; }
            }
        }
    }

    private void beginRecover() {
        recoverFrom = offset;
        phase = Phase.RECOVER;
        phaseElapsed = 0;
    }

    private static double progress(double elapsed, double duration) {
        return duration <= 0 ? 1 : Math.min(1, elapsed / duration);
    }

    /** Ease-in/ease-out: acceleration is non-zero, so it exercises the quadratic forecast term. */
    private static double smoothstep(double x) { return x * x * (3 - 2 * x); }
}
