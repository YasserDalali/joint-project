package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ETF(
        Long id,
        String symbol,
        String name,
        BigDecimal currentPrice,
        RiskLevel riskLevel,
        LocalDateTime createdAt,
        String issuer,
        BigDecimal expenseRatio)
        implements Asset {

    @Override
    public AssetType type() {
        return AssetType.ETF;
    }

    @Override
    public RiskLevel calculateRiskLevel() {
        return RiskLevel.MEDIUM;
    }
}
