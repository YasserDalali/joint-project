package com.finrisk.dto.response;

public record HoldingBeta(long assetId, String symbol, double beta, double weight) {}
