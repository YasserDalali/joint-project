package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record LookThroughResponse(
        long accountId,
        BigDecimal totalValue,
        List<Exposure> exposures,
        List<String> unresolvedEtfs) {

    public record Exposure(
            String symbol,
            String name,
            double effectiveWeight,
            double directWeight,
            double viaEtfWeight,
            List<Source> sources) {}

    public record Source(String etf, double weight) {}
}
