package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fixed-income asset capturing coupon-like attributes for conservative portfolios. */
public record Bond(
        Long id,
        String symbol,
        String name,
        BigDecimal currentPrice,
        RiskLevel riskLevel,
        LocalDateTime createdAt,
        BigDecimal interestRate,
        LocalDate maturityDate,
        String issuer)
        implements Asset {

    /** Identifies this instrument as {@link AssetType#BOND}. */
    @Override
    public AssetType type() {
        return AssetType.BOND;
    }

    /** Declares bonds as comparatively low risk within the teaching model. */
    @Override
    public RiskLevel calculateRiskLevel() {
        return RiskLevel.LOW;
    }
}
