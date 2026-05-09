package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SellTransaction(
        Long id,
        Long accountId,
        Long assetId,
        int quantity,
        BigDecimal unitPrice,
        LocalDateTime transactionDate)
        implements Transaction {

    @Override
    public TransactionType type() {
        return TransactionType.SELL;
    }

    @Override
    public Account applyTo(Account a) {
        return a.withCashBalance(a.cashBalance().add(totalAmount()));
    }
}
