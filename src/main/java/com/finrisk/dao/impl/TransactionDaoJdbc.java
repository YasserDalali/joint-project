package com.finrisk.dao.impl;

import com.finrisk.dao.TransactionDao;
import com.finrisk.dao.TransactionPageQuery;
import com.finrisk.dto.response.Page;
import com.finrisk.model.BuyTransaction;
import com.finrisk.model.SellTransaction;
import com.finrisk.model.Transaction;
import com.finrisk.model.TransactionType;
import com.finrisk.util.Db;
import com.finrisk.util.SqlSort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TransactionDaoJdbc implements TransactionDao {

    private static final String FILTER_KEY_ASSET_ID = "asset";

    private static Transaction mapRow(ResultSet rs) throws SQLException {
        String tt = rs.getString("transaction_type");
        Timestamp ts = rs.getTimestamp("transaction_date");
        LocalDateTime td = ts == null ? null : ts.toLocalDateTime();
        long id = rs.getLong("id");
        long accountId = rs.getLong("account_id");
        long assetId = rs.getLong("asset_id");
        int qty = rs.getInt("quantity");
        BigDecimal unit = rs.getBigDecimal("unit_price");
        return "BUY".equals(tt)
                ? new BuyTransaction(id, accountId, assetId, qty, unit, td)
                : new SellTransaction(id, accountId, assetId, qty, unit, td);
    }

    @Override
    public Transaction findById(Long id) {
        return Db.findOne(
                        """
                        SELECT id, account_id, asset_id, transaction_type, quantity, unit_price, transaction_date FROM dbo.transactions WHERE id = ?
                        """,
                        TransactionDaoJdbc::mapRow,
                        id)
                .orElse(null);
    }

    @Override
    public List<Transaction> findAll() {
        return List.of();
    }

    @Override
    public Transaction save(Transaction entity) {
        throw new UnsupportedOperationException("Use stored procedures for trades");
    }

    @Override
    public void update(Transaction entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void executeBuyProcedure(long accountId, long assetId, int quantity, BigDecimal unitPrice) {
        Db.call("{call dbo.sp_buy_asset(?,?,?,?)}", accountId, assetId, quantity, unitPrice);
    }

    @Override
    public void executeSellProcedure(long accountId, long assetId, int quantity, BigDecimal unitPrice) {
        Db.call("{call dbo.sp_sell_asset(?,?,?,?)}", accountId, assetId, quantity, unitPrice);
    }

    @Override
    public int ownedQuantity(long accountId, long assetId) {
        return Db.findOne(
                        """
                        SELECT COALESCE(SUM(CASE WHEN transaction_type = N'BUY' THEN quantity ELSE -quantity END), 0)
                        FROM dbo.transactions
                        WHERE account_id = ? AND asset_id = ?
                        """,
                        rs -> rs.getBigDecimal(1).intValue(),
                        accountId,
                        assetId)
                .orElse(0);
    }

    @Override
    public Page<Transaction> pageForAccount(TransactionPageQuery q) {
        long accountId = q.accountId();
        TransactionType type = q.type();
        Long assetId = q.assetId();
        LocalDateTime fromInclusive = q.fromInclusive();
        LocalDateTime toExclusive = q.toExclusive();
        int page = q.page();
        int size = q.size();
        List<String> sortSpecs = q.sortSpecs();
        String order =
                SqlSort.orderByClause(sortSpecs, SqlSort.transactionsWhitelist(), "transaction_date DESC, id DESC");
        Map<String, Object> typed = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE account_id = ? ");
        typed.put("accountId", accountId);
        if (type != null) {
            where.append(" AND transaction_type = ? ");
            typed.put("tt", type.name());
        }
        if (assetId != null) {
            where.append(" AND asset_id = ? ");
            typed.put(FILTER_KEY_ASSET_ID, assetId);
        }
        if (fromInclusive != null) {
            where.append(" AND transaction_date >= ? ");
            typed.put("from", Timestamp.valueOf(fromInclusive));
        }
        if (toExclusive != null) {
            where.append(" AND transaction_date < ? ");
            typed.put("to", Timestamp.valueOf(toExclusive));
        }

        String countSql = "SELECT COUNT(1) FROM dbo.transactions " + where;
        String dataSql =
                "SELECT id, account_id, asset_id, transaction_type, quantity, unit_price, transaction_date FROM dbo.transactions"
                        + where
                        + " ORDER BY "
                        + order
                        + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return Db.findPage(
                countSql,
                dataSql,
                TransactionDaoJdbc::mapRow,
                page,
                size,
                ps -> bindTxFilters(ps, typed),
                ps -> {
                    int idx = bindTxFilters(ps, typed);
                    ps.setInt(idx++, page * size);
                    ps.setInt(idx, size);
                });
    }

    private static int bindTxFilters(java.sql.PreparedStatement ps, Map<String, Object> typed) throws SQLException {
        int idx = 1;
        ps.setLong(idx++, (Long) typed.get("accountId"));
        if (typed.containsKey("tt")) {
            ps.setString(idx++, (String) typed.get("tt"));
        }
        if (typed.containsKey(FILTER_KEY_ASSET_ID)) {
            ps.setLong(idx++, (Long) typed.get(FILTER_KEY_ASSET_ID));
        }
        if (typed.containsKey("from")) {
            ps.setTimestamp(idx++, (Timestamp) typed.get("from"));
        }
        if (typed.containsKey("to")) {
            ps.setTimestamp(idx++, (Timestamp) typed.get("to"));
        }
        return idx;
    }

    @Override
    public String findSymbol(long assetId) {
        return Db.findOne(
                        """
                        SELECT symbol FROM dbo.assets WHERE id = ?
                        """,
                        rs -> rs.getString(1),
                        assetId)
                .orElse(null);
    }

    @Override
    public Transaction findLatest(long accountId, long assetId, TransactionType type) {
        return Db.findOne(
                        """
                        SELECT TOP 1 id, account_id, asset_id, transaction_type, quantity, unit_price, transaction_date
                        FROM dbo.transactions
                        WHERE account_id = ? AND asset_id = ? AND transaction_type = ?
                        ORDER BY id DESC
                        """,
                        TransactionDaoJdbc::mapRow,
                        accountId,
                        assetId,
                        type.name())
                .orElse(null);
    }
}
