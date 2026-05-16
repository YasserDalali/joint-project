package com.finrisk.dao.impl;

import com.finrisk.dao.AccountDao;
import com.finrisk.dto.response.Page;
import com.finrisk.model.Account;
import com.finrisk.util.Db;
import com.finrisk.util.SqlSort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/** JDBC repository for {@code dbo.accounts} rows modeled as immutable {@link Account} records. */
@Repository
public class AccountDaoJdbc implements AccountDao {

    private static final String FIND_BY_ID =
            """
            SELECT id, user_id, account_name, cash_balance, created_at FROM dbo.accounts WHERE id = ?
            """;

    private static final String FIND_ALL =
            """
            SELECT id, user_id, account_name, cash_balance, created_at FROM dbo.accounts ORDER BY id
            """;

    private static final String INSERT =
            """
            INSERT INTO dbo.accounts (user_id, account_name, cash_balance, created_at) OUTPUT INSERTED.id VALUES (?, ?, ?, SYSUTCDATETIME())
            """;

    private static final String UPDATE_ACCOUNT =
            """
            UPDATE dbo.accounts SET user_id = ?, account_name = ?, cash_balance = ? WHERE id = ?
            """;

    private static final String DELETE_ACCOUNT =
            """
            DELETE FROM dbo.accounts WHERE id = ?
            """;

    /** Maps joined SQL columns into an {@link Account} aggregate. */
    private static Account mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Account(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("account_name"),
                rs.getBigDecimal("cash_balance"),
                ts == null ? null : ts.toLocalDateTime());
    }

    /** Retrieves one brokerage account by primary key or {@code null} when missing. */
    @Override
    public Account findById(Long id) {
        Optional<Account> found = Db.findOne(FIND_BY_ID, rs -> mapRow(rs), id);
        if (found.isPresent()) {
            return found.get();
        }
        return null;
    }

    /** Loads every account sorted by id ascending. */
    @Override
    public List<Account> findAll() {
        return Db.findMany(FIND_ALL, rs -> mapRow(rs));
    }

    /** Persists a fresh account row returning identifiers supplied by SQL Server OUTPUT. */
    @Override
    public Account save(Account entity) {
        long id =
                Db.insertReturning(
                        INSERT,
                        rs -> rs.getLong(1),
                        entity.userId(),
                        entity.accountName(),
                        entity.cashBalance());
        Timestamp created =
                Db.findOne(
                                """
                                SELECT created_at FROM dbo.accounts WHERE id = ?
                                """,
                                rs -> rs.getTimestamp(1),
                                id)
                        .orElse(null);
        return new Account(
                id,
                entity.userId(),
                entity.accountName(),
                entity.cashBalance(),
                created == null ? null : created.toLocalDateTime());
    }

    /** Overwrites mutable columns for an existing account primary key. */
    @Override
    public void update(Account entity) {
        Db.update(
                UPDATE_ACCOUNT,
                entity.userId(),
                entity.accountName(),
                entity.cashBalance(),
                entity.id());
    }

    /** Deletes an account row outright when higher layers authorize removal. */
    @Override
    public void delete(Long id) {
        Db.exec(DELETE_ACCOUNT, id);
    }

    /** Paginates accounts filtered by owning {@code user_id} with ORDER BY whitelist integration. */
    @Override
    public Page<Account> pageByUserId(Long userId, int page, int size, List<String> sortSpecs) {
        String order =
                SqlSort.orderByClause(sortSpecs, SqlSort.accountsWhitelist(), "created_at DESC, id DESC");
        String where = " WHERE user_id = ?";
        String countSql = "SELECT COUNT(1) FROM dbo.accounts" + where;
        String dataSql =
                "SELECT id, user_id, account_name, cash_balance, created_at FROM dbo.accounts"
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
                ps -> ps.setLong(1, userId),
                ps -> {
                    ps.setLong(1, userId);
                    ps.setInt(2, page * size);
                    ps.setInt(3, size);
                });
    }

    /** Performs a targeted balance update without touching unrelated columns. */
    @Override
    public void updateCashBalance(Long id, BigDecimal newBalance) {
        Db.update(
                """
                UPDATE dbo.accounts SET cash_balance = ? WHERE id = ?
                """,
                newBalance,
                id);
    }
}
