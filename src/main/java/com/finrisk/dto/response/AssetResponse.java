package com.finrisk.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.finrisk.model.AssetType;
import com.finrisk.model.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    long id();

    String symbol();

    String name();

    AssetType assetType();

    BigDecimal currentPrice();

    String currency();

    RiskLevel defaultRiskLevel();

    LocalDateTime createdAt();
}
