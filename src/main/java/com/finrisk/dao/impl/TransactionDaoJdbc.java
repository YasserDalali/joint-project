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
import java.util.Optional;

/** JDBC {@link TransactionDao} bridging ledger rows and SQL stored procedures for trades. */
@Repository
public class TransactionDaoJdbc implements TransactionDao {

    private static final String FILTER_KEY_ASSET_ID = "asset";

    /** Hydrates either {@link BuyTransaction} or {@link SellTransaction} based on stored type column. */
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

    /** Loads a transaction row by numeric primary key. */
    @Override
    public Transaction findById(Long id) {
        Optional<Transaction> found =
                Db.findOne(
                        """
                        SELECT id, account_id, asset_id, transaction_type, quantity, unit_price, transaction_date FROM dbo.transactions WHERE id = ?
                        """,
                        rs -> mapRow(rs),
                        id);
        if (found.isPresent()) {
            return found.get();
        }
        return null;
    }

    /** Returns an empty list intentionally because bulk scans are unsupported for trades. */
    @Override
    public List<Transaction> findAll() {
        return List.of();
    }

    /** Blocks naive inserts because trading flows must call stored procedures enforcing constraints. */
    @Override
    public Transaction save(Transaction entity) {
        throw new UnsupportedOperationException("Use stored procedures for trades");
    }

    /** Disallows direct updates for the same integrity reasons as {@link #save(Transaction)}. */
    @Override
    public void update(Transaction entity) {
        throw new UnsupportedOperationException();
    }

    /** Prevents DAO-driven deletes to protect immutable audit expectations. */
    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    /** Executes {@code dbo.sp_buy_asset} with JDBC callable statement semantics. */
    @Override
    public void executeBuyProcedure(long accountId, long assetId, int quantity, BigDecimal unitPrice) {
        Db.call("{call dbo.sp_buy_asset(?,?,?,?)}", accountId, assetId, quantity, unitPrice);
    }

    /** Executes {@code dbo.sp_sell_asset} analogously to buy flows. */
    @Override
    public void executeSellProcedure(long accountId, long assetId, int quantity, BigDecimal unitPrice) {
        Db.call("{call dbo.sp_sell_asset(?,?,?,?)}", accountId, assetId, quantity, unitPrice);
    }

    /** Computes net owned quantity by summing buys minus sells straight from SQL aggregates. */
    @Override
    public int ownedQuantity(long accountId, long assetId) {
        Optional<Integer> found =
                Db.findOne(
                        """
                        SELECT COALESCE(SUM(CASE WHEN transaction_type = N'BUY' THEN quantity ELSE -quantity END), 0)
                        FROM dbo.transactions
                        WHERE account_id = ? AND asset_id = ?
                        """,
                        rs -> rs.getBigDecimal(1).intValue(),
                        accountId,
                        assetId);
        if (found.isPresent()) {
            return found.get();
        }
        return 0;
    }

    /** Paginates ledger rows with dynamic filters for account/type/asset/date windows plus ORDER BY whitelist. */
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
                rs -> mapRow(rs),
                page,
                size,
                ps -> bindTxFilters(ps, typed),
                ps -> {
                    int idx = bindTxFilters(ps, typed);
                    ps.setInt(idx++, page * size);
                    ps.setInt(idx, size);
                });
    }

    /** Applies WHERE bind variables accumulated during {@link #pageForAccount}. */
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

    /** Looks up ticker text for decorating transaction responses. */
    @Override
    public String findSymbol(long assetId) {
        Optional<String> found =
                Db.findOne(
                        """
                        SELECT symbol FROM dbo.assets WHERE id = ?
                        """,
                        rs -> rs.getString(1),
                        assetId);
        if (found.isPresent()) {
            return found.get();
        }
        return null;
    }

    /** Retrieves the newest matching ledger row for account/asset/type tuple ordered by id desc. */
    @Override
    public Transaction findLatest(long accountId, long assetId, TransactionType type) {
        Optional<Transaction> found =
                Db.findOne(
                        """
                        SELECT TOP 1 id, account_id, asset_id, transaction_type, quantity, unit_price, transaction_date
                        FROM dbo.transactions
                        WHERE account_id = ? AND asset_id = ? AND transaction_type = ?
                        ORDER BY id DESC
                        """,
                        rs -> mapRow(rs),
                        accountId,
                        assetId,
                        type.name());
        if (found.isPresent()) {
            return found.get();
        }
        return null;
    }
}
