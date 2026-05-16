package com.finrisk.strategy.risk;

import com.finrisk.model.Asset;
import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;
import java.util.List;

/** Strategy interface for turning historical prices into qualitative {@link RiskLevel} assessments. */
public interface RiskCalculationStrategy {

    /** Computes daily volatility (sigma) from an oldest-to-newest price stream. */
    double sigmaFromPrices(List<BigDecimal> chronologicalAscending);

    /** Maps a numeric sigma measurement onto the coarse {@link RiskLevel} ladder. */
    RiskLevel levelForSigma(double dailySigma);

    /** Supplies a deterministic risk tier when analytics lack enough price samples. */
    RiskLevel fallbackLevel(Asset asset);
}
