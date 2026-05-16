package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Represents a customer's brokerage account with its cash balance for trading simulations. */
public record Account(Long id, Long userId, String accountName, BigDecimal cashBalance, LocalDateTime createdAt) {

    /** Produces a copy of this account with an updated cash balance. */
    public Account withCashBalance(BigDecimal newBalance) {
        return new Account(id, userId, accountName, newBalance, createdAt);
    }
}
