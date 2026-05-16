package com.finrisk.dto.response;

import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;

/** One contributor row inside {@link RiskScoreResponse#getBreakdown()} explaining portfolio weights. */
public record RiskBreakdownItem(
        long assetId,
        String symbol,
        RiskLevel riskLevel,
        double weight,
        BigDecimal volatility,
        Integer sampleSize) {}
