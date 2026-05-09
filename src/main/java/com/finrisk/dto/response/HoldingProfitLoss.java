package com.finrisk.dto.response;

import java.math.BigDecimal;

public record HoldingProfitLoss(
        long assetId,
        String symbol,
        int quantity,
        BigDecimal netInvested,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercent,
        String currency) {

    public HoldingProfitLoss(
            long assetId,
            String symbol,
            int quantity,
            BigDecimal netInvested,
            BigDecimal currentValue,
            BigDecimal profitLoss,
            BigDecimal profitLossPercent) {
        this(assetId, symbol, quantity, netInvested, currentValue, profitLoss, profitLossPercent, "USD");
    }
}
