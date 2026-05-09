package com.finrisk.mapper;

import com.finrisk.dto.response.TransactionResponse;
import com.finrisk.model.Transaction;

public final class TransactionMapper {

    private TransactionMapper() {}

    public static TransactionResponse toResponse(Transaction t, String assetSymbol) {
        return new TransactionResponse(
                t.id(),
                t.accountId(),
                t.assetId(),
                assetSymbol,
                t.type(),
                t.quantity(),
                t.unitPrice(),
                t.totalAmount(),
                t.transactionDate());
    }
}
