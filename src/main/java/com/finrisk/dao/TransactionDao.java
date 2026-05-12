package com.finrisk.dao;

import com.finrisk.dto.response.Page;
import com.finrisk.model.Transaction;
import com.finrisk.model.TransactionType;

import java.math.BigDecimal;

public interface TransactionDao extends GenericDao<Transaction, Long> {

    void executeBuyProcedure(long accountId, long assetId, int quantity, BigDecimal unitPrice);

    void executeSellProcedure(long accountId, long assetId, int quantity, BigDecimal unitPrice);

    int ownedQuantity(long accountId, long assetId);

    Page<Transaction> pageForAccount(TransactionPageQuery query);

    String findSymbol(long assetId);

    Transaction findLatest(long accountId, long assetId, TransactionType type);
}
