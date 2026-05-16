package com.finrisk.dto.response;

import com.finrisk.model.RiskLevel;

import java.util.List;

/** Packages an account-level risk score plus supporting breakdown rows for transparency. */
public record RiskScoreResponse(
        long accountId,
        double score,
        RiskLevel level,
        String strategy,
        List<RiskBreakdownItem> breakdown) {

    /** Uses the default volatility Strategy implementation label when none is provided. */
    public RiskScoreResponse(long accountId, double score, RiskLevel level, List<RiskBreakdownItem> breakdown) {
        this(accountId, score, level, "VOLATILITY", breakdown);
    }
}
