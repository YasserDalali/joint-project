package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Override
    public AssetType type() {
        return AssetType.BOND;
    }

    @Override
    public RiskLevel calculateRiskLevel() {
        return RiskLevel.LOW;
    }
}
