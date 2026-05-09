package com.finrisk.dto.response;

import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;

public record RiskBreakdownItem(
        long assetId,
        String symbol,
        RiskLevel riskLevel,
        double weight,
        BigDecimal volatility,
        Integer sampleSize) {}
