package com.finrisk.dto.response;

import java.math.BigDecimal;

/** Row-level profit metrics for one holding inside a {@link ProfitLossResponse}. */
public record HoldingProfitLoss(
        long assetId,
        String symbol,
        int quantity,
        BigDecimal netInvested,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercent,
        String currency) {

    /** Assumes USD when formatting percentage gains for teaching portfolios. */
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
