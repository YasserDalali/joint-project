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

    private static Account map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Account(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("account_name"),
                rs.getBigDecimal("cash_balance"),
                ts == null ? null : ts.toLocalDateTime());
    }

    @Override
    public Account findById(Long id) {
        return Db.findOne(FIND_BY_ID, AccountDaoJdbc::map, id).orElse(null);
    }

    @Override
    public List<Account> findAll() {
        return Db.findMany(FIND_ALL, AccountDaoJdbc::map);
    }

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

    @Override
    public void update(Account entity) {
        Db.update(
                UPDATE_ACCOUNT,
                entity.userId(),
                entity.accountName(),
                entity.cashBalance(),
                entity.id());
    }

    @Override
    public void delete(Long id) {
        Db.exec(DELETE_ACCOUNT, id);
    }

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
                AccountDaoJdbc::map,
                page,
                size,
                ps -> ps.setLong(1, userId),
                ps -> {
                    ps.setLong(1, userId);
                    ps.setInt(2, page * size);
                    ps.setInt(3, size);
                });
    }

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
