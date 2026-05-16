package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Aggregated profit-and-loss figures per account for REST consumers. */
public record ProfitLossResponse(
        long accountId,
        String currency,
        List<HoldingProfitLoss> holdings,
        BigDecimal totalProfitLoss) {

    /** Supplies USD as the reporting currency when services compute plain monetary totals. */
    public ProfitLossResponse(long accountId, List<HoldingProfitLoss> holdings, BigDecimal totalProfitLoss) {
        this(accountId, "USD", holdings, totalProfitLoss);
    }
}
