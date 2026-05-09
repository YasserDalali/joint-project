package com.finrisk.dto.response;

import com.finrisk.model.AssetType;
import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EtfResponse(
        long id,
        String symbol,
        String name,
        AssetType assetType,
        BigDecimal currentPrice,
        String currency,
        RiskLevel defaultRiskLevel,
        LocalDateTime createdAt,
        String issuer,
        BigDecimal expenseRatio)
        implements AssetResponse {}
