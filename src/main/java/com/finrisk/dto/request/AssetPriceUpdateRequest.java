package com.finrisk.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AssetPriceUpdateRequest(
        @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal price) {}
