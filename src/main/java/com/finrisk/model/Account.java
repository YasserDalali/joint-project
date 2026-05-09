package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Account(Long id, Long userId, String accountName, BigDecimal cashBalance, LocalDateTime createdAt) {

    public Account withCashBalance(BigDecimal newBalance) {
        return new Account(id, userId, accountName, newBalance, createdAt);
    }
}
