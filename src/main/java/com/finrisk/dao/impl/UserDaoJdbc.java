package com.finrisk.dao.impl;

import com.finrisk.dao.UserDao;
import com.finrisk.dto.response.Page;
import com.finrisk.exception.DaoException;
import com.finrisk.exception.EmailAlreadyExistsException;
import com.finrisk.model.User;
import com.finrisk.util.Db;
import com.finrisk.util.SqlSort;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoJdbc implements UserDao {

    private static final String FIND_BY_ID =
            """
            SELECT id, full_name, email, created_at FROM dbo.users WHERE id = ?
            """;

    private static final String FIND_ALL =
            """
            SELECT id, full_name, email, created_at FROM dbo.users ORDER BY id
            """;

    private static final String INSERT =
            """
            INSERT INTO dbo.users (full_name, email, created_at) VALUES (?, ?, SYSUTCDATETIME());
            SELECT CAST(SCOPE_IDENTITY() AS BIGINT);
            """;

    private static final String UPDATE_USER =
            """
            UPDATE dbo.users SET full_name = ?, email = ? WHERE id = ?
            """;

    private static final String DELETE_USER =
            """
            DELETE FROM dbo.users WHERE id = ?
            """;

    private static final String FIND_BY_EMAIL =
            """
            SELECT id, full_name, email, created_at FROM dbo.users WHERE email = ?
            """;

    private static User map(ResultSet rs) throws SQLException {
        var ts = rs.getTimestamp("created_at");
        return new User(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                ts == null ? null : ts.toLocalDateTime());
    }

    private static boolean isUniqueViolation(SQLException e) {
        return "23000".equals(e.getSQLState()) || e.getErrorCode() == 2627 || e.getErrorCode() == 2601;
    }

    @Override
    public User findById(Long id) {
        return Db.findOne(FIND_BY_ID, UserDaoJdbc::map, id).orElse(null);
    }

    @Override
    public List<User> findAll() {
        return Db.findMany(FIND_ALL, UserDaoJdbc::map);
    }

    @Override
    public User save(User entity) {
        try {
            long id =
                    Db.insertReturning(
                            INSERT,
                            rs -> rs.getLong(1),
                            entity.fullName(),
                            entity.email());
            LocalDateTime created =
                    Db.findOne(
                                    """
                                    SELECT created_at FROM dbo.users WHERE id = ?
                                    """,
                                    rs -> {
                                        Timestamp ts = rs.getTimestamp(1);
                                        return ts == null ? null : ts.toLocalDateTime();
                                    },
                                    id)
                            .orElse(null);
            return new User(id, entity.fullName(), entity.email(), created);
        } catch (DaoException e) {
            if (e.getCause() instanceof SQLException sql && isUniqueViolation(sql)) {
                throw new EmailAlreadyExistsException("Email already in use");
            }
            throw e;
        }
    }

    @Override
    public void update(User entity) {
        try {
            Db.update(UPDATE_USER, entity.fullName(), entity.email(), entity.id());
        } catch (DaoException e) {
            if (e.getCause() instanceof SQLException sql && isUniqueViolation(sql)) {
                throw new EmailAlreadyExistsException("Email already in use");
            }
            throw e;
        }
    }

    @Override
    public void delete(Long id) {
        Db.exec(DELETE_USER, id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Db.findOne(FIND_BY_EMAIL, UserDaoJdbc::map, email.trim().toLowerCase());
    }

    @Override
    public Page<User> pageUsers(String emailPrefix, int page, int size, List<String> sortSpecs) {
        String order =
                SqlSort.orderByClause(sortSpecs, SqlSort.usersWhitelist(), "created_at DESC, id DESC");
        String where =
                emailPrefix == null || emailPrefix.isBlank()
                        ? ""
                        : " WHERE LOWER(email) LIKE LOWER(?) + N'%' ESCAPE N'\\'";
        String countSql = "SELECT COUNT(1) FROM dbo.users" + where;
        String dataSql =
                "SELECT id, full_name, email, created_at FROM dbo.users"
                        + where
                        + " ORDER BY "
                        + order
                        + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return Db.findPage(
                countSql,
                dataSql,
                UserDaoJdbc::map,
                page,
                size,
                ps -> {
                    int ci = 1;
                    if (emailPrefix != null && !emailPrefix.isBlank()) {
                        ps.setString(ci++, escapeLike(emailPrefix.trim()));
                    }
                },
                ps -> {
                    int idx = 1;
                    if (emailPrefix != null && !emailPrefix.isBlank()) {
                        ps.setString(idx++, escapeLike(emailPrefix.trim()));
                    }
                    ps.setInt(idx++, page * size);
                    ps.setInt(idx, size);
                });
    }

    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
