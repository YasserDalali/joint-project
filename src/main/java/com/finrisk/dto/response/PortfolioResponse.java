package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Snapshot combining cash, holdings, and rolled-up valuations for an account dashboard. */
public record PortfolioResponse(
        long accountId,
        String currency,
        BigDecimal cashBalance,
        List<Holding> holdings,
        BigDecimal totalHoldingsValue,
        BigDecimal totalAccountValue) {

    /** Defaults monetary fields to USD presentation when services omit an ISO currency code. */
    public PortfolioResponse(
            long accountId,
            BigDecimal cashBalance,
            List<Holding> holdings,
            BigDecimal totalHoldingsValue,
            BigDecimal totalAccountValue) {
        this(accountId, "USD", cashBalance, holdings, totalHoldingsValue, totalAccountValue);
    }
}
