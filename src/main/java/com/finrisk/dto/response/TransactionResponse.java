package com.finrisk.dto.response;

import com.finrisk.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** REST-safe view of a ledger transaction including derived totals for JSON responses. */
public record TransactionResponse(
        long id,
        long accountId,
        long assetId,
        String assetSymbol,
        TransactionType transactionType,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime transactionDate) {

    /** Builds a response assuming USD reporting currency when callers omit it. */
    public TransactionResponse(
            long id,
            long accountId,
            long assetId,
            String assetSymbol,
            TransactionType transactionType,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalAmount,
            LocalDateTime transactionDate) {
        this(
                id,
                accountId,
                assetId,
                assetSymbol,
                transactionType,
                quantity,
                unitPrice,
                totalAmount,
                "USD",
                transactionDate);
    }
}
