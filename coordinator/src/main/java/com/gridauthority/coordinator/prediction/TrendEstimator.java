package com.gridauthority.coordinator.prediction;

import org.springframework.stereotype.Service;


@Service
public class TrendEstimator {

    public double slope(double[] values) {
        if (values == null || values.length < 2) return 0;
        return regress(values).slope;
    }


    public TrendFit fit(double[] values, double dtSeconds) {
        if (values == null || values.length == 0) return TrendFit.flat(0);
        double dt = dtSeconds > 0 ? dtSeconds : 1.0;
        int n = values.length;
        if (n < 3) return TrendFit.flat(values[n - 1]);

        Reg r = regress(values);
        double xMean = (n - 1) / 2.0;
        double sse = 0, sst = 0, yMean = r.yMean;
        for (int i = 0; i < n; i++) {
            double fitted = r.intercept + r.slope * i;
            sse += (values[i] - fitted) * (values[i] - fitted);
            sst += (values[i] - yMean) * (values[i] - yMean);
        }
        double residualStd = Math.sqrt(sse / (n - 2));
        double r2 = sst <= 1e-12 ? 0 : Math.max(0, 1 - sse / sst);
        double seSlopeIdx = r.sxx > 0 ? residualStd / Math.sqrt(r.sxx) : Double.POSITIVE_INFINITY;
        double endValue = r.intercept + r.slope * (n - 1);

        double accel = 0, seAccel = Double.POSITIVE_INFINITY;
        if (n >= 6) {
            int mid = n / 2;
            Reg a = regress(java.util.Arrays.copyOfRange(values, 0, mid));
            Reg b = regress(java.util.Arrays.copyOfRange(values, mid, n));
            double separationSeconds = (n / 2.0) * dt;
            accel = ((b.slope - a.slope) / dt) / separationSeconds;
            double seA = stdErrIdx(values, 0, mid, a);
            double seB = stdErrIdx(values, mid, n, b);
            seAccel = Math.sqrt(seA * seA + seB * seB) / dt / separationSeconds;
        }
        return new TrendFit(r.slope / dt, endValue, r2, seSlopeIdx / dt, residualStd, accel, seAccel,
                xMean * dt, n, r.sxx, dt);
    }

    private double stdErrIdx(double[] v, int from, int to, Reg reg) {
        int m = to - from;
        if (m < 3 || reg.sxx <= 0) return Double.POSITIVE_INFINITY;
        double sse = 0;
        for (int i = 0; i < m; i++) {
            double fitted = reg.intercept + reg.slope * i;
            sse += (v[from + i] - fitted) * (v[from + i] - fitted);
        }
        return Math.sqrt(sse / (m - 2)) / Math.sqrt(reg.sxx);
    }

    private record Reg(double slope, double intercept, double yMean, double sxx) {
    }

    private Reg regress(double[] y) {
        int n = y.length;
        double xMean = (n - 1) / 2.0, yMean = 0;
        for (double v : y) yMean += v;
        yMean /= n;
        double sxy = 0, sxx = 0;
        for (int i = 0; i < n; i++) {
            sxy += (i - xMean) * (y[i] - yMean);
            sxx += (i - xMean) * (i - xMean);
        }
        double slope = sxx == 0 ? 0 : sxy / sxx;
        return new Reg(slope, yMean - slope * xMean, yMean, sxx);
    }
}