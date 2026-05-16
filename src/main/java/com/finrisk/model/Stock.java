package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Equity asset specialization carrying exchange metadata used in stock-specific workflows. */
public record Stock(
        Long id,
        String symbol,
        String name,
        BigDecimal currentPrice,
        RiskLevel riskLevel,
        LocalDateTime createdAt,
        String sector,
        String exchange)
        implements Asset {

    /** Marks this record as a {@link AssetType#STOCK} entry. */
    @Override
    public AssetType type() {
        return AssetType.STOCK;
    }

    /** Supplies FinRisk's simplified intrinsic risk score for individual equities. */
    @Override
    public RiskLevel calculateRiskLevel() {
        return RiskLevel.HIGH;
    }
}
