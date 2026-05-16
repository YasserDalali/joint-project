package com.finrisk.service;

import com.finrisk.dao.AccountDao;
import com.finrisk.dao.AssetDao;
import com.finrisk.dao.TransactionDao;
import com.finrisk.dao.TransactionPageQuery;
import com.finrisk.dto.request.TradeRequest;
import com.finrisk.dto.response.Page;
import com.finrisk.dto.response.TransactionResponse;
import com.finrisk.exception.AccountNotFoundException;
import com.finrisk.exception.AssetNotFoundException;
import com.finrisk.mapper.TransactionMapper;
import com.finrisk.model.Account;
import com.finrisk.model.Asset;
import com.finrisk.model.Transaction;
import com.finrisk.model.TransactionType;
import com.finrisk.util.SqlSort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Executes trades via stored procedures and shapes ledger listings for REST responses. */
@Service
public class TransactionService {

    private static final String ASSET_NOT_FOUND = "Asset not found";

    private final AccountDao accountDao;
    private final AssetDao assetDao;
    private final TransactionDao transactionDao;

    /** Injects DAO dependencies covering accounts, assets, and ledger persistence. */
    public TransactionService(AccountDao accountDao, AssetDao assetDao, TransactionDao transactionDao) {
        this.accountDao = accountDao;
        this.assetDao = assetDao;
        this.transactionDao = transactionDao;
    }

    /** Runs the buy stored procedure after verifying references exist. */
    public TransactionResponse buy(TradeRequest req) {
        requireAccount(req.accountId());
        requireAsset(req.assetId());
        transactionDao.executeBuyProcedure(
                req.accountId(), req.assetId(), req.quantity(), req.unitPrice());
        return loadResponse(req, TransactionType.BUY);
    }

    /** Mirrors {@link #buy(TradeRequest)} for liquidation flows using the sell procedure. */
    public TransactionResponse sell(TradeRequest req) {
        requireAccount(req.accountId());
        requireAsset(req.assetId());
        transactionDao.executeSellProcedure(
                req.accountId(), req.assetId(), req.quantity(), req.unitPrice());
        return loadResponse(req, TransactionType.SELL);
    }

    /** Reads the freshly inserted transaction plus symbol for mapper consumption. */
    private TransactionResponse loadResponse(TradeRequest req, TransactionType type) {
        Transaction transaction = transactionDao.findLatest(req.accountId(), req.assetId(), type);
        String symbol = transactionDao.findSymbol(req.assetId());
        return TransactionMapper.toResponse(transaction, symbol);
    }

    /** Ensures an account id resolves before sensitive trading operations proceed. */
    private void requireAccount(long id) {
        Account account = accountDao.findById(id);
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }
    }

    /** Ensures an asset id resolves prior to executing trades. */
    private void requireAsset(long id) {
        Asset asset = assetDao.findById(id);
        if (asset == null) {
            throw new AssetNotFoundException(ASSET_NOT_FOUND);
        }
    }

    /** Paginates transactions for an account using sanitized filters/sorts. */
    public Page<TransactionResponse> listForAccount(TransactionPageQuery query) {
        requireAccount(query.accountId());
        int safeSize = Math.min(Math.max(query.size(), 1), 100);
        int safePage = Math.max(query.page(), 0);
        Page<Transaction> transactionPage =
                transactionDao.pageForAccount(
                        new TransactionPageQuery(
                                query.accountId(),
                                query.type(),
                                query.assetId(),
                                query.fromInclusive(),
                                query.toExclusive(),
                                safePage,
                                safeSize,
                                SqlSort.normalizeSortParams(query.sortSpecs())));

        List<TransactionResponse> responses = new ArrayList<>();
        for (Transaction transaction : transactionPage.content()) {
            String symbol = transactionDao.findSymbol(transaction.assetId());
            responses.add(TransactionMapper.toResponse(transaction, symbol));
        }

        return new Page<>(
                transactionPage.page(),
                transactionPage.size(),
                transactionPage.totalElements(),
                transactionPage.totalPages(),
                transactionPage.first(),
                transactionPage.last(),
                responses);
    }
}
