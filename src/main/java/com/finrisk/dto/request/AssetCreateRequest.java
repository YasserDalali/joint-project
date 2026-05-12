package com.finrisk.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.finrisk.model.AssetType;

import java.math.BigDecimal;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "assetType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = StockCreateRequest.class, name = "STOCK"),
    @JsonSubTypes.Type(value = EtfCreateRequest.class, name = "ETF"),
    @JsonSubTypes.Type(value = BondCreateRequest.class, name = "BOND"),
    @JsonSubTypes.Type(value = CryptoCreateRequest.class, name = "CRYPTO")
})
public sealed interface AssetCreateRequest permits StockCreateRequest, EtfCreateRequest, BondCreateRequest, CryptoCreateRequest {

    AssetType assetType();

    String symbol();

    String name();

    BigDecimal currentPrice();
}
