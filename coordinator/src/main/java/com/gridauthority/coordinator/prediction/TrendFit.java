package com.gridauthority.coordinator.prediction;

/**
 * Result of a least-squares line fit over a sliding window, with the statistics
 * needed to judge whether the trend is real or just measurement noise.
 *
 * @param slopePerSecond       fitted slope in V/s
 * @param endValue             fitted value at the LAST sample of the window (V)
 * @param r2                   coefficient of determination of the line fit (0..1)
 * @param slopeStdError        standard error of the slope in V/s
 * @param residualStd          standard deviation of the fit residuals (V)
 * @param accelerationPerSecond2 change of slope between the two half-windows (V/s^2)
 * @param accelerationStdError standard error of the acceleration (V/s^2); infinite if not computable
 * @param halfWindowSeconds    time from the window centre to its last sample (s)
 * @param n                    number of samples
 * @param sxx                  sum of squared index deviations (fit geometry)
 * @param dtSeconds            mean sampling interval (s)
 */
public record TrendFit(double slopePerSecond, double endValue, double r2, double slopeStdError,
                       double residualStd, double accelerationPerSecond2, double accelerationStdError,
                       double halfWindowSeconds, int n, double sxx, double dtSeconds) {

    public static final double DEFAULT_Z = 3.0;

    public static TrendFit flat(double value) {
        return new TrendFit(0, value, 0, Double.POSITIVE_INFINITY, 0, 0, Double.POSITIVE_INFINITY, 0, 0, 0, 1);
    }

    public double tStat() {
        if (slopeStdError <= 0) return slopePerSecond == 0 ? 0 : Math.copySign(Double.POSITIVE_INFINITY, slopePerSecond);
        return slopePerSecond / slopeStdError;
    }

    public boolean slopeSignificant(double z) {
        return Math.abs(tStat()) >= z;
    }

    public boolean accelerationSignificant(double z) {
        if (accelerationPerSecond2 == 0) return false;
        if (accelerationStdError <= 0) return true;
        return Double.isFinite(accelerationStdError) && Math.abs(accelerationPerSecond2) / accelerationStdError >= z;
    }

    /** Standard error (V) of the fitted mean at {@code horizonSeconds} after the last sample. */
    public double predictionStdError(double horizonSeconds) {
        if (n < 3 || sxx <= 0) return Double.POSITIVE_INFINITY;
        double xMean = (n - 1) / 2.0;
        double x0 = (n - 1) + horizonSeconds / dtSeconds;
        return residualStd * Math.sqrt(1.0 / n + ((x0 - xMean) * (x0 - xMean)) / sxx);
    }
}
