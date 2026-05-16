package com.finrisk.dto.response;

import com.finrisk.model.AssetType;
import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Crypto specialization of {@link AssetResponse} annotating the backing blockchain network. */
public record CryptoResponse(
        long id,
        String symbol,
        String name,
        AssetType assetType,
        BigDecimal currentPrice,
        String currency,
        RiskLevel defaultRiskLevel,
        LocalDateTime createdAt,
        String blockchain)
        implements AssetResponse {}
