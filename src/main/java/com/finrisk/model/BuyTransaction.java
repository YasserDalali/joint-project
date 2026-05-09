package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BuyTransaction(
        Long id,
        Long accountId,
        Long assetId,
        int quantity,
        BigDecimal unitPrice,
        LocalDateTime transactionDate)
        implements Transaction {

    @Override
    public TransactionType type() {
        return TransactionType.BUY;
    }

    @Override
    public Account applyTo(Account a) {
        return a.withCashBalance(a.cashBalance().subtract(totalAmount()));
    }
}
