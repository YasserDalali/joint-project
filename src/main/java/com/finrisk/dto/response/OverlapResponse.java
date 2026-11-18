package com.finrisk.dto.response;

import java.util.List;

public record OverlapResponse(long accountId, List<Pair> pairs, List<String> unresolvedEtfs) {

    public record Pair(String etfA, String etfB, double overlap, List<String> sharedHoldings) {}
}
