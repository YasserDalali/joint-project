package com.finrisk.dto.response;

import com.finrisk.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
