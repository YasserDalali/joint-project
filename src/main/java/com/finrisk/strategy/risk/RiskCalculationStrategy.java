package com.finrisk.strategy.risk;

import com.finrisk.model.Asset;
import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;
import java.util.List;

public interface RiskCalculationStrategy {

    /** Daily volatility (sigma) from chronological ascending price series (oldest -> newest). */
    double sigmaFromPrices(List<BigDecimal> chronologicalAscending);

    RiskLevel levelForSigma(double dailySigma);

    RiskLevel fallbackLevel(Asset asset);
}
