package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Common shape for every tradable instrument plus hooks for risk labeling (polymorphism via sealed types). */
public sealed interface Asset permits Stock, ETF, Bond, CryptoAsset {

    /** Returns the persistent identifier assigned by the database. */
    Long id();

    /** Exposes the ticker or symbol clients quote when trading this asset. */
    String symbol();

    /** Human-readable title shown beside the symbol in UI layers. */
    String name();

    /** Latest unit price FinRisk uses for valuation math. */
    BigDecimal currentPrice();

    /** Risk tier persisted alongside the asset row for quick filtering. */
    RiskLevel riskLevel();

    /** Timestamp marking when this catalog row was created. */
    LocalDateTime createdAt();

    /** Declares which concrete subtype this asset represents. */
    AssetType type();

    /** Computes an intrinsic {@link RiskLevel} based on instrument-specific rules. */
    RiskLevel calculateRiskLevel();
}
