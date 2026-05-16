package com.finrisk.dto.response;

import com.finrisk.model.AssetType;

import java.math.BigDecimal;

/** Describes one line item inside {@link PortfolioResponse} holdings arrays. */
public record Holding(
        long assetId,
        String symbol,
        String name,
        AssetType assetType,
        int quantity,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        String currency) {

    /** Adds USD labeling automatically when domain services only supply numeric valuations. */
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
