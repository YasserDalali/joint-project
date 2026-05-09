package com.finrisk.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        long id,
        long userId,
        String accountName,
        BigDecimal cashBalance,
        String currency,
        LocalDateTime createdAt) {

    public AccountResponse(long id, long userId, String accountName, BigDecimal cashBalance, LocalDateTime createdAt) {
        this(id, userId, accountName, cashBalance, "USD", createdAt);
    }
}
