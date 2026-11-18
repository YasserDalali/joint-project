package com.finrisk.dto.response;

public record HoldingDrawdown(long assetId, String symbol, Double maxDrawdown) {}
