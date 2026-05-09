package com.finrisk.dto.response;

import com.finrisk.model.RiskLevel;

import java.util.List;

public record RiskScoreResponse(
        long accountId,
        double score,
        RiskLevel level,
        String strategy,
        List<RiskBreakdownItem> breakdown) {

    public RiskScoreResponse(long accountId, double score, RiskLevel level, List<RiskBreakdownItem> breakdown) {
        this(accountId, score, level, "VOLATILITY", breakdown);
    }
}
