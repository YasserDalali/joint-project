package com.finrisk.dto.response;

import com.finrisk.model.AssetType;
import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Bond-flavored {@link AssetResponse} variant including coupon-oriented metadata fields. */
public record BondResponse(
        long id,
        String symbol,
        String name,
        AssetType assetType,
        BigDecimal currentPrice,
        String currency,
        RiskLevel defaultRiskLevel,
        LocalDateTime createdAt,
        BigDecimal interestRate,
        LocalDate maturityDate,
        String issuer)
        implements AssetResponse {}
