package com.finrisk.strategy.risk;

import com.finrisk.model.Asset;
import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/** Concrete Strategy estimating risk via log-return volatility with configurable sample thresholds. */
public class VolatilityRiskStrategy implements RiskCalculationStrategy {

    private final int minSamples;

    /** Captures how many price observations the strategy expects before trusting sigma math. */
    public VolatilityRiskStrategy(int minSamples) {
        this.minSamples = minSamples;
    }

    /** Calculates sample standard deviation of log returns across consecutive prices. */
    @Override
    public double sigmaFromPrices(List<BigDecimal> chronologicalAscending) {
        if (chronologicalAscending == null || chronologicalAscending.size() < 2) {
            return Double.NaN;
        }
        int n = chronologicalAscending.size() - 1;
        double sum = 0;
        double[] r = new double[n];
        for (int i = 1; i < chronologicalAscending.size(); i++) {
            BigDecimal p0 = chronologicalAscending.get(i - 1);
            BigDecimal p1 = chronologicalAscending.get(i);
            double ri =
                    Math.log(p1.divide(p0, MathContext.DECIMAL128).doubleValue());
            r[i - 1] = ri;
            sum += ri;
        }
        double mean = sum / n;
        double varSum = 0;
        for (double v : r) {
            double d = v - mean;
            varSum += d * d;
        }
        double variance = n <= 1 ? 0 : varSum / (n - 1);
        return Math.sqrt(variance);
    }

    /** Buckets a numeric sigma into {@link RiskLevel} via fixed instructional thresholds. */
    @Override
    public RiskLevel levelForSigma(double dailySigma) {
        if (Double.isNaN(dailySigma)) {
            return RiskLevel.MEDIUM;
        }
        if (dailySigma < 0.01) {
            return RiskLevel.LOW;
        }
        if (dailySigma < 0.03) {
            return RiskLevel.MEDIUM;
        }
        if (dailySigma < 0.06) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.VERY_HIGH;
    }

    /** Returns intrinsic asset-class risk when volatility cannot be computed. */
    @Override
    public RiskLevel fallbackLevel(Asset asset) {
        return asset.calculateRiskLevel();
    }

    /** Exposes the configured minimum sample expectation for diagnostics or future guards. */
    public int minSamples() {
        return minSamples;
    }
}
