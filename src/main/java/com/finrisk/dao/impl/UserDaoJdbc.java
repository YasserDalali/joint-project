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

/** JDBC-backed {@link UserDao} translating rows in {@code dbo.users} into immutable {@link User} records. */
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
            INSERT INTO dbo.users (full_name, email, created_at) OUTPUT INSERTED.id VALUES (?, ?, SYSUTCDATETIME())
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

    /** Converts one JDBC {@link ResultSet} row into a {@link User} value object. */
    private static User mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new User(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                ts == null ? null : ts.toLocalDateTime());
    }

    /** Detects SQL Server unique constraint violations based on state/error codes. */
    private static boolean isUniqueViolation(SQLException e) {
        return "23000".equals(e.getSQLState()) || e.getErrorCode() == 2627 || e.getErrorCode() == 2601;
    }

    /** Loads a user by numeric primary key returning {@code null} when absent (DAO convention). */
    @Override
    public User findById(Long id) {
        Optional<User> found = Db.findOne(FIND_BY_ID, rs -> mapRow(rs), id);
        if (found.isPresent()) {
            return found.get();
        }
        return null;
    }

    /** Retrieves every user ordered by id for administrative dumps. */
    @Override
    public List<User> findAll() {
        return Db.findMany(FIND_ALL, rs -> mapRow(rs));
    }

    /** Inserts a user row and reconstructs the entity including generated keys/timestamps. */
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
            if (e.getCause() instanceof SQLException sqlException && isUniqueViolation(sqlException)) {
                throw new EmailAlreadyExistsException("Email already in use");
            }
            throw e;
        }
    }

    /** Updates mutable columns on an existing user record. */
    @Override
    public void update(User entity) {
        try {
            Db.update(UPDATE_USER, entity.fullName(), entity.email(), entity.id());
        } catch (DaoException e) {
            if (e.getCause() instanceof SQLException sqlException && isUniqueViolation(sqlException)) {
                throw new EmailAlreadyExistsException("Email already in use");
            }
            throw e;
        }
    }

    /** Deletes the specified user row when foreign keys permit removal. */
    @Override
    public void delete(Long id) {
        Db.exec(DELETE_USER, id);
    }

    /** Looks up a user by normalized email address. */
    @Override
    public Optional<User> findByEmail(String email) {
        return Db.findOne(FIND_BY_EMAIL, rs -> mapRow(rs), email.trim().toLowerCase());
    }

    /** Applies LIKE-filtered pagination with ORDER BY fragments derived from {@link SqlSort}. */
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
                rs -> mapRow(rs),
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

    /** Escapes LIKE wildcard characters before embedding user-provided prefixes into SQL patterns. */
    private static String escapeLike(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
