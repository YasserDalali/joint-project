package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Basket-style fund asset combining issuer data with expense ratio for education scenarios. */
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

    /** Tags this row as {@link AssetType#ETF}. */
    @Override
    public AssetType type() {
        return AssetType.ETF;
    }

    /** Places ETFs between bonds and single stocks on the simplified risk ladder. */
    @Override
    public RiskLevel calculateRiskLevel() {
        return RiskLevel.MEDIUM;
    }
}
