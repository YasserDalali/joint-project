package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Override
    public AssetType type() {
        return AssetType.STOCK;
    }

    @Override
    public RiskLevel calculateRiskLevel() {
        return RiskLevel.HIGH;
    }
}
