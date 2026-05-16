package com.finrisk.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Describes how to open a brokerage account including optional starting deposit semantics. */
public record AccountCreateRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 100) String accountName,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal initialDeposit) {

    /** Normalizes missing deposits to zero before validation completes. */
    public AccountCreateRequest {
        if (initialDeposit == null) {
            initialDeposit = BigDecimal.ZERO;
        }
    }
}
