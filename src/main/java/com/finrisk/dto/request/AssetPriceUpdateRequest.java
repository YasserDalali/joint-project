package com.finrisk.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Wraps the lone numeric field accepted when administrators refresh an asset's quoted price. */
public record AssetPriceUpdateRequest(
        @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal price) {}
