package com.finrisk.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.finrisk.model.AssetType;

import java.math.BigDecimal;

/** Polymorphic request body for catalog inserts; Jackson selects implementations via {@code assetType}. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "assetType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StockCreateRequest.class, name = "STOCK"),
    @JsonSubTypes.Type(value = EtfCreateRequest.class, name = "ETF"),
    @JsonSubTypes.Type(value = BondCreateRequest.class, name = "BOND"),
    @JsonSubTypes.Type(value = CryptoCreateRequest.class, name = "CRYPTO")
})
public sealed interface AssetCreateRequest permits StockCreateRequest, EtfCreateRequest, BondCreateRequest, CryptoCreateRequest {

    /** Names which concrete payload Jackson deserialized for Factory routing. */
    AssetType assetType();

    /** Supplies the ticker symbol that must stay unique across FinRisk's catalog. */
    String symbol();

    /** Captures the descriptive title stored beside the symbol column. */
    String name();

    /** Carries the seed price inserted during asset onboarding. */
    BigDecimal currentPrice();
}
