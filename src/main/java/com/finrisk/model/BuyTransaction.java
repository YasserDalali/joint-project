package com.finrisk.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Purchase-side ledger entry reducing cash and increasing implied holdings. */
public record BuyTransaction(
        Long id,
        Long accountId,
        Long assetId,
        int quantity,
        BigDecimal unitPrice,
        LocalDateTime transactionDate)
        implements Transaction {

    /** Marks the transaction row as a {@link TransactionType#BUY}. */
    @Override
    public TransactionType type() {
        return TransactionType.BUY;
    }

    /** Subtracts the trade's cash requirement from the supplied {@link Account}. */
    @Override
    public Account applyTo(Account a) {
        return a.withCashBalance(a.cashBalance().subtract(totalAmount()));
    }
}
