package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public sealed interface Asset permits Stock, ETF, Bond, CryptoAsset {

    Long id();

    String symbol();

    String name();

    BigDecimal currentPrice();

    RiskLevel riskLevel();

    LocalDateTime createdAt();

    AssetType type();

    RiskLevel calculateRiskLevel();
}
