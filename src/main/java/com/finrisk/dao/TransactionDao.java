package com.finrisk.dao;

import com.finrisk.dto.response.Page;
import com.finrisk.model.Transaction;
import com.finrisk.model.TransactionType;

import java.math.BigDecimal;

/** Ledger DAO bridging Java domain transactions with SQL stored procedures and paging queries. */
public interface TransactionDao extends GenericDao<Transaction, Long> {

    /** Calls the server-side buy routine enforcing balances and inventory rules atomically. */
    void executeBuyProcedure(long accountId, long assetId, int quantity, BigDecimal unitPrice);

    /** Calls the complementary sell routine verifying sufficient holdings. */
    void executeSellProcedure(long accountId, long assetId, int quantity, BigDecimal unitPrice);

    /** Computes how many units an account currently owns for one asset. */
    int ownedQuantity(long accountId, long assetId);

    /** Applies rich filters to transactions for one account with stable sorting. */
    Page<Transaction> pageForAccount(TransactionPageQuery query);

    /** Looks up the ticker symbol for an asset id during response enrichment. */
    String findSymbol(long assetId);

    /** Finds the newest transaction of a given type for an account/asset pair. */
    Transaction findLatest(long accountId, long assetId, TransactionType type);
}
