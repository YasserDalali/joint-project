package com.finrisk.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Client-submitted parameters for simulated buy/sell transactions hitting stored procedures. */
public record TradeRequest(
        @NotNull Long accountId,
        @NotNull Long assetId,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal unitPrice) {}
