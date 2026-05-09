package com.finrisk.dto.request;

import com.finrisk.model.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StockCreateRequest(
        @NotNull AssetType assetType,
        @NotBlank @Size(max = 20) String symbol,
        @NotBlank @Size(max = 150) String name,
        @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal currentPrice,
        @NotBlank String sector,
        @NotBlank String exchange)
        implements AssetCreateRequest {}
