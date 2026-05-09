package com.finrisk.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public sealed interface Transaction permits BuyTransaction, SellTransaction {

    Long id();

    Long accountId();

    Long assetId();

    int quantity();

    BigDecimal unitPrice();

    LocalDateTime transactionDate();

    TransactionType type();

    default BigDecimal totalAmount() {
        return unitPrice().multiply(BigDecimal.valueOf(quantity())).setScale(4, RoundingMode.HALF_UP);
    }

    Account applyTo(Account account);
}
