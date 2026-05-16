package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Sell-side ledger entry returning proceeds to the cash balance. */
public record SellTransaction(
        Long id,
        Long accountId,
        Long assetId,
        int quantity,
        BigDecimal unitPrice,
        LocalDateTime transactionDate)
        implements Transaction {

    /** Tags this movement as {@link TransactionType#SELL}. */
    @Override
    public TransactionType type() {
        return TransactionType.SELL;
    }

    /** Credits cash equal to {@link #totalAmount()} on the working {@link Account}. */
    @Override
    public Account applyTo(Account a) {
        return a.withCashBalance(a.cashBalance().add(totalAmount()));
    }
}
