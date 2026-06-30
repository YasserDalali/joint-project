package com.finrisk.dto.response;

import java.util.List;

public record BetaResponse(
        long accountId,
        double portfolioBeta,
        String benchmark,
        List<HoldingBeta> holdings) {}
