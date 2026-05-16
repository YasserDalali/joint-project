package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Digital asset specialization noting which blockchain network backs the holding. */
public record CryptoAsset(
        Long id,
        String symbol,
        String name,
        BigDecimal currentPrice,
        RiskLevel riskLevel,
        LocalDateTime createdAt,
        String blockchain)
        implements Asset {

    /** Declares this catalog row as {@link AssetType#CRYPTO}. */
    @Override
    public AssetType type() {
        return AssetType.CRYPTO;
    }

    /** Marks crypto as the most volatile bucket in the starter curriculum. */
    @Override
    public RiskLevel calculateRiskLevel() {
        return RiskLevel.VERY_HIGH;
    }
}
