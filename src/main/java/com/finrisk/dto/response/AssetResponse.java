package com.finrisk.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.finrisk.model.AssetType;
import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Polymorphic JSON contract for listing assets; Jackson picks concrete shapes via {@code assetType}. */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "assetType",
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StockResponse.class, name = "STOCK"),
    @JsonSubTypes.Type(value = EtfResponse.class, name = "ETF"),
    @JsonSubTypes.Type(value = BondResponse.class, name = "BOND"),
    @JsonSubTypes.Type(value = CryptoResponse.class, name = "CRYPTO")
})
public sealed interface AssetResponse permits StockResponse, EtfResponse, BondResponse, CryptoResponse {

    /** Returns the persisted surrogate key for this catalog entry. */
    long id();

    /** Exposes the ticker clients quote when placing simulated trades. */
    String symbol();

    /** Provides the marketing-friendly asset title alongside the ticker. */
    String name();

    /** Names which subclass discriminator applies (mirrors {@link com.finrisk.model.AssetType}). */
    AssetType assetType();

    /** Shows the latest valuation FinRisk quotes for this instrument. */
    BigDecimal currentPrice();

    /** Indicates which fiat/crypto denomination wraps {@link #currentPrice()}. */
    String currency();

    /** Surfaces the persisted coarse risk tier carried from the database row. */
    RiskLevel defaultRiskLevel();

    /** Records catalog creation time for freshness indicators. */
    LocalDateTime createdAt();
}
