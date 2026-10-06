package com.gridauthority.coordinator.prediction;

import com.gridauthority.coordinator.application.dto.PolicyDTO;
import org.springframework.stereotype.Service;

/**
 * Turns an observation window into a forecast and a risk score.
 * <ul>
 *   <li>The trajectory starts at the END of the fitted line (current level), not at the window mean.</li>
 *   <li>A trend is only used when its slope is statistically significant; acceleration only when it is too.</li>
 *   <li>Deviation is measured against the policy nominal voltage, so a steady sag is still a deviation.</li>
 *   <li>Confidence reflects how tight the statistical prediction is, not how fast the slope changes.</li>
 * </ul>
 */
@Service
public class PredictiveAssessor {
    private final TrendEstimator trend;
    private final BaselineEstimator baseline;
    private final ForecastEngine forecast;
    private final RiskEngine riskEngine;

    public PredictiveAssessor(TrendEstimator trend, BaselineEstimator baseline, ForecastEngine forecast, RiskEngine riskEngine) {
        this.trend = trend;
        this.baseline = baseline;
        this.forecast = forecast;
        this.riskEngine = riskEngine;
    }

    public ForecastResult assess(double[] window, double cvPercent, double dtSeconds, double observability, PolicyDTO p) {
        TrendFit fit = trend.fit(window, dtSeconds);
        double lower = p.nominalVoltage() * (1 - p.voltageCritical());
        double upper = p.nominalVoltage() * (1 + p.voltageCritical());

        boolean credible = fit.slopeSignificant(TrendFit.DEFAULT_Z);
        double accel = credible && fit.accelerationSignificant(TrendFit.DEFAULT_Z) ? fit.accelerationPerSecond2() : 0;
        double endSlope = credible ? fit.slopePerSecond() + accel * fit.halfWindowSeconds() : 0;

        double end = fit.endValue();
        double forecastValue = forecast.forecast(end, endSlope, accel, p.horizonSeconds());
        Double ttt = forecast.timeToBreach(end, endSlope, accel, lower, upper);
        // A level converging back toward nominal is not a threat until it passes nominal. This also stops the
        // linear model from reading a recovery step (e.g. 200 V -> 230 V) as a ramp towards the upper limit.
        boolean convergingToNominal = (end - p.nominalVoltage()) * endSlope < 0;
        if (convergingToNominal && ttt != null && ttt > 0) ttt = null;

        double dev = baseline.deviation(end, p.nominalVoltage());
        double cvRisk = cvPercent / 100.0 / Math.max(.01, p.cvCritical());
        double trendRisk = Math.abs(endSlope) / Math.max(.001, p.maxRateOfChange());
        double devRisk = dev / Math.max(.01, p.voltageCritical());
        double tttRisk = ttt == null ? 0 : 1 - ttt / p.horizonSeconds();
        boolean speedingUp = accel != 0 && Math.signum(accel) == Math.signum(endSlope);
        double accelRisk = speedingUp ? Math.abs(accel) * p.horizonSeconds() / Math.max(.001, p.maxRateOfChange()) : 0;

        double quality = riskEngine.fitQuality(fit.predictionStdError(p.horizonSeconds()), upper - lower);
        double conf = riskEngine.confidence(observability, quality);
        double risk = riskEngine.score(cvRisk, trendRisk, devRisk, tttRisk, accelRisk);
        return new ForecastResult(endSlope, accel, dev, forecastValue, risk, conf, ttt, observability);
    }
}
