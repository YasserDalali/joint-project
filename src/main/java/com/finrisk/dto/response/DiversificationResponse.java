package com.finrisk.dto.response;

import java.util.List;

public record DiversificationResponse(
        long accountId,
        double score,
        double hhiIndex,
        int holdingCount,
        int sectorCount,
        int assetTypeCount,
        List<AssetTypeAllocation> assetTypeBreakdown) {}
