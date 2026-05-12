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

@Service
public class TransactionService {

    private static final String ASSET_NOT_FOUND = "Asset not found";

    private final AccountDao accountDao;
    private final AssetDao assetDao;
    private final TransactionDao transactionDao;

    public TransactionService(AccountDao accountDao, AssetDao assetDao, TransactionDao transactionDao) {
        this.accountDao = accountDao;
        this.assetDao = assetDao;
        this.transactionDao = transactionDao;
    }

    public TransactionResponse buy(TradeRequest req) {
        requireAccount(req.accountId());
        requireAsset(req.assetId());
        transactionDao.executeBuyProcedure(
                req.accountId(), req.assetId(), req.quantity(), req.unitPrice());
        return loadResponse(req, TransactionType.BUY);
    }

    public TransactionResponse sell(TradeRequest req) {
        requireAccount(req.accountId());
        requireAsset(req.assetId());
        transactionDao.executeSellProcedure(
                req.accountId(), req.assetId(), req.quantity(), req.unitPrice());
        return loadResponse(req, TransactionType.SELL);
    }

    private TransactionResponse loadResponse(TradeRequest req, TransactionType type) {
        Transaction t = transactionDao.findLatest(req.accountId(), req.assetId(), type);
        String sym = transactionDao.findSymbol(req.assetId());
        return TransactionMapper.toResponse(t, sym);
    }

    private void requireAccount(long id) {
        Account a = accountDao.findById(id);
        if (a == null) {
            throw new AccountNotFoundException("Account not found");
        }
    }

    private void requireAsset(long id) {
        Asset a = assetDao.findById(id);
        if (a == null) {
            throw new AssetNotFoundException(ASSET_NOT_FOUND);
        }
    }

    public Page<TransactionResponse> listForAccount(TransactionPageQuery query) {
        requireAccount(query.accountId());
        int safeSize = Math.min(Math.max(query.size(), 1), 100);
        int safePage = Math.max(query.page(), 0);
        Page<Transaction> p =
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
        return new Page<>(
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages(),
                p.first(),
                p.last(),
                p.content().stream()
                        .map(
                                tr ->
                                        TransactionMapper.toResponse(
                                                tr, transactionDao.findSymbol(tr.assetId())))
                        .toList());
    }
}
