package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Serializable projection of {@link com.finrisk.model.Account} for REST GET responses. */
public record AccountResponse(
        long id,
        long userId,
        String accountName,
        BigDecimal cashBalance,
        String currency,
        LocalDateTime createdAt) {

    /** Supplies USD metadata when internal services track balances without ISO codes. */
    public AccountResponse(long id, long userId, String accountName, BigDecimal cashBalance, LocalDateTime createdAt) {
        this(id, userId, accountName, cashBalance, "USD", createdAt);
    }
}
