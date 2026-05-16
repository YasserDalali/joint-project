package com.finrisk.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Represents deposits or withdrawals adjusting only the cash leg of an account. */
public record CashMovementRequest(
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount) {}
