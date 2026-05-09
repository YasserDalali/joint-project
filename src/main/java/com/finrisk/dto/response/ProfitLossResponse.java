package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProfitLossResponse(
        long accountId,
        String currency,
        List<HoldingProfitLoss> holdings,
        BigDecimal totalProfitLoss) {

    public ProfitLossResponse(long accountId, List<HoldingProfitLoss> holdings, BigDecimal totalProfitLoss) {
        this(accountId, "USD", holdings, totalProfitLoss);
    }
}
