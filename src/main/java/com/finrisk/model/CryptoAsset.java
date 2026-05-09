package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CryptoAsset(
        Long id,
        String symbol,
        String name,
        BigDecimal currentPrice,
        RiskLevel riskLevel,
        LocalDateTime createdAt,
        String blockchain)
        implements Asset {

    @Override
    public AssetType type() {
        return AssetType.CRYPTO;
    }

    @Override
    public RiskLevel calculateRiskLevel() {
        return RiskLevel.VERY_HIGH;
    }
}
