package com.finrisk.dto.response;

import com.finrisk.model.AssetType;

import java.math.BigDecimal;

public record Holding(
        long assetId,
        String symbol,
        String name,
        AssetType assetType,
        int quantity,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        String currency) {

    public Holding(
            long assetId,
            String symbol,
            String name,
            AssetType assetType,
            int quantity,
            BigDecimal currentPrice,
            BigDecimal currentValue) {
        this(assetId, symbol, name, assetType, quantity, currentPrice, currentValue, "USD");
    }
}
