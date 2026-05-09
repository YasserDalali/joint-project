package com.finrisk.factory;

import com.finrisk.dto.request.TradeRequest;
import com.finrisk.model.BuyTransaction;
import com.finrisk.model.SellTransaction;
import com.finrisk.model.Transaction;
import com.finrisk.model.TransactionType;

public final class TransactionFactory {

    private TransactionFactory() {}

    public static Transaction create(TradeRequest req, TransactionType type) {
        return switch (type) {
            case BUY -> new BuyTransaction(
                    null, req.accountId(), req.assetId(), req.quantity(), req.unitPrice(), null);
            case SELL -> new SellTransaction(
                    null, req.accountId(), req.assetId(), req.quantity(), req.unitPrice(), null);
        };
    }
}
