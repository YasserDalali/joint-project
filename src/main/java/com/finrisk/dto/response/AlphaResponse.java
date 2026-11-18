package com.finrisk.dto.response;

public record AlphaResponse(
        long accountId,
        Double alpha,
        Double portfolioReturn,
        Double benchmarkReturn,
        double beta,
        double riskFree,
        String benchmark) {}
