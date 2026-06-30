package com.finrisk.dto.response;

import java.math.BigDecimal;

public record AssetTypeAllocation(String assetType, double weight, BigDecimal value) {}
