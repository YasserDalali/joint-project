package com.finrisk.dto.response;

import java.util.List;

public record DrawdownResponse(
        long accountId,
        Double maxDrawdown,
        Double currentDrawdown,
        List<HoldingDrawdown> holdings) {}
