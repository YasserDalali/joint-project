package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        long accountId,
        String currency,
        BigDecimal cashBalance,
        List<Holding> holdings,
        BigDecimal totalHoldingsValue,
        BigDecimal totalAccountValue) {

    public PortfolioResponse(
            long accountId,
            BigDecimal cashBalance,
            List<Holding> holdings,
            BigDecimal totalHoldingsValue,
            BigDecimal totalAccountValue) {
        this(accountId, "USD", cashBalance, holdings, totalHoldingsValue, totalAccountValue);
    }
}
