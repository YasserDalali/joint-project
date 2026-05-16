package com.finrisk.mapper;

import com.finrisk.dto.response.TransactionResponse;
import com.finrisk.model.Transaction;

/** Maps domain {@link Transaction} entities into enriched REST responses carrying ticker symbols. */
public final class TransactionMapper {

    /** Marks this helper as non-instantiable static utilities. */
    private TransactionMapper() {}

    /** Builds a {@link TransactionResponse} merging ledger facts with a supplied asset symbol string. */
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
