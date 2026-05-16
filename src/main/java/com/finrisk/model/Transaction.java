package com.finrisk.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Sealed transaction hierarchy expressing buys and sells applied against accounts (domain-rich interface). */
public sealed interface Transaction permits BuyTransaction, SellTransaction {

    /** Provides the persistent identifier for this ledger movement. */
    Long id();

    /** Points to the brokerage account participating in the trade. */
    Long accountId();

    /** References which {@link Asset} changed hands. */
    Long assetId();

    /** Count of whole units exchanged in this transaction. */
    int quantity();

    /** Execution price per unit at the time of the trade. */
    BigDecimal unitPrice();

    /** Timestamp capturing when the trade occurred. */
    LocalDateTime transactionDate();

    /** Indicates whether this transaction increases or decreases holdings. */
    TransactionType type();

    /** Calculates currency impact by multiplying unit price by quantity. */
    default BigDecimal totalAmount() {
        return unitPrice().multiply(BigDecimal.valueOf(quantity())).setScale(4, RoundingMode.HALF_UP);
    }

    /** Applies this transaction's cash effect to an {@link Account} snapshot. */
    Account applyTo(Account account);
}
