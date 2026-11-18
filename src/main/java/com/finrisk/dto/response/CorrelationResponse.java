package com.finrisk.dto.response;

import java.util.List;

public record CorrelationResponse(
        long accountId,
        List<String> symbols,
        List<List<Double>> matrix,
        Double averagePairwiseCorrelation) {}
