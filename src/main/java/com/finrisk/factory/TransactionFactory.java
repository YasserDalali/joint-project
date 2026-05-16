package com.finrisk.factory;

import com.finrisk.dto.request.TradeRequest;
import com.finrisk.model.BuyTransaction;
import com.finrisk.model.SellTransaction;
import com.finrisk.model.Transaction;
import com.finrisk.model.TransactionType;

/** Factory Method wrapper producing buy/sell {@link Transaction} shells before persistence timestamps arrive. */
public final class TransactionFactory {

    /** Prevents external instantiation of this static helper. */
    private TransactionFactory() {}

    /** Converts a validated {@link TradeRequest} into the proper transaction subtype. */
    public static Transaction create(TradeRequest req, TransactionType type) {
        if (type == TransactionType.BUY) {
            return new BuyTransaction(
                    null, req.accountId(), req.assetId(), req.quantity(), req.unitPrice(), null);
        }
        if (type == TransactionType.SELL) {
            return new SellTransaction(
                    null, req.accountId(), req.assetId(), req.quantity(), req.unitPrice(), null);
        }
        throw new IllegalArgumentException("Unknown transaction type: " + type);
    }
}
