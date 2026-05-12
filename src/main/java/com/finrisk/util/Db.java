package com.finrisk.util;

import com.finrisk.config.DatabaseConnection;
import com.finrisk.dto.response.Page;
import com.finrisk.exception.DaoException;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Single place for JDBC try-with-resources ({@link Connection}, {@link PreparedStatement},
 * {@link ResultSet}) per project conventions.
 */
public final class Db {

    private static final System.Logger LOG = System.getLogger(Db.class.getName());

    private static final String QUERY_FAILED_PREFIX = "query failed: ";
    private static final String UPDATE_FAILED_PREFIX = "update failed: ";
    private static final String EXEC_FAILED_PREFIX = "exec failed: ";
    private static final String INSERT_FAILED_PREFIX = "insert failed: ";
    private static final String INSERT_NO_ROW_PREFIX = "insert returned no row: ";
    private static final String CALL_FAILED_PREFIX = "call failed: ";
    private static final String SCALAR_FAILED_PREFIX = "scalar query failed: ";
    private static final String PAGE_QUERY_FAILED = "paged query failed";
    private static final String TX_FAILED = "transaction failed";

    private Db() {}

    /** Use {@link SqlNull#DECIMAL} where JDBC needs {@code SET NULL} with {@link Types#DECIMAL}. */
    public enum SqlNull {
        DECIMAL
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    @FunctionalInterface
    public interface PreparedStatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    public static <T> Optional<T> findOne(String sql, RowMapper<T> mapper, Object... params) {
        try (var c = DatabaseConnection.getConnection();
                var ps = bind(c.prepareStatement(sql), params);
                var rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapper.map(rs)) : Optional.empty();
        } catch (SQLException e) {
            throw new DaoException(QUERY_FAILED_PREFIX + sql, e);
        }
    }

    public static <T> Optional<T> findOne(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        try (var ps = bind(c.prepareStatement(sql), params);
                var rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapper.map(rs)) : Optional.empty();
        } catch (SQLException e) {
            throw new DaoException(QUERY_FAILED_PREFIX + sql, e);
        }
    }

    public static <T> List<T> findMany(String sql, RowMapper<T> mapper, Object... params) {
        try (var c = DatabaseConnection.getConnection();
                var ps = bind(c.prepareStatement(sql), params);
                var rs = ps.executeQuery()) {
            var list = new ArrayList<T>();
            while (rs.next()) {
                list.add(mapper.map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException(QUERY_FAILED_PREFIX + sql, e);
        }
    }

    public static <T> List<T> findMany(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        try (var ps = bind(c.prepareStatement(sql), params);
                var rs = ps.executeQuery()) {
            var list = new ArrayList<T>();
            while (rs.next()) {
                list.add(mapper.map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException(QUERY_FAILED_PREFIX + sql, e);
        }
    }

    public static int update(String sql, Object... params) {
        try (var c = DatabaseConnection.getConnection();
                var ps = bind(c.prepareStatement(sql), params)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(UPDATE_FAILED_PREFIX + sql, e);
        }
    }

    public static int update(Connection c, String sql, Object... params) {
        try (var ps = bind(c.prepareStatement(sql), params)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(UPDATE_FAILED_PREFIX + sql, e);
        }
    }

    public static void exec(String sql, Object... params) {
        try (var c = DatabaseConnection.getConnection();
                var ps = bind(c.prepareStatement(sql), params)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(EXEC_FAILED_PREFIX + sql, e);
        }
    }

    public static void exec(Connection c, String sql, Object... params) {
        try (var ps = bind(c.prepareStatement(sql), params)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(EXEC_FAILED_PREFIX + sql, e);
        }
    }

    public static <T> T insertReturning(String sql, RowMapper<T> mapper, Object... params) {
        try (var c = DatabaseConnection.getConnection();
                var ps = bind(c.prepareStatement(sql), params);
                var rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new DaoException(INSERT_NO_ROW_PREFIX + sql, null);
            }
            return mapper.map(rs);
        } catch (SQLException e) {
            throw new DaoException(INSERT_FAILED_PREFIX + sql, e);
        }
    }

    public static <T> T insertReturning(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        try (var ps = bind(c.prepareStatement(sql), params);
                var rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new DaoException(INSERT_NO_ROW_PREFIX + sql, null);
            }
            return mapper.map(rs);
        } catch (SQLException e) {
            throw new DaoException(INSERT_FAILED_PREFIX + sql, e);
        }
    }

    public static void call(String sql, Object... params) {
        try (var c = DatabaseConnection.getConnection();
                var cs = bindCall(c.prepareCall(sql), params)) {
            cs.execute();
        } catch (SQLException e) {
            RuntimeException mapped = JdbcSqlExceptionMapper.map(e);
            if (mapped != null) {
                throw mapped;
            }
            throw new DaoException(CALL_FAILED_PREFIX + sql, e);
        }
    }

    public static long queryLong(String sql, Object... params) {
        try (var c = DatabaseConnection.getConnection();
                var ps = bind(c.prepareStatement(sql), params);
                var rs = ps.executeQuery()) {
            if (!rs.next()) {
                return 0L;
            }
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DaoException(SCALAR_FAILED_PREFIX + sql, e);
        }
    }

    public static long queryLong(Connection c, String sql, Object... params) {
        try (var ps = bind(c.prepareStatement(sql), params);
                var rs = ps.executeQuery()) {
            if (!rs.next()) {
                return 0L;
            }
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DaoException(SCALAR_FAILED_PREFIX + sql, e);
        }
    }

    /**
     * Runs {@code countSql} then {@code dataSql}, building a {@link Page}. Caller binds every placeholder
     * (including {@code OFFSET} / {@code FETCH}) via the two binders.
     */
    public static <T> Page<T> findPage(
            String countSql,
            String dataSql,
            RowMapper<T> mapper,
            int page,
            int size,
            PreparedStatementBinder countBinder,
            PreparedStatementBinder dataBinder) {
        try (var c = DatabaseConnection.getConnection()) {
            long total;
            try (var ps = c.prepareStatement(countSql)) {
                countBinder.bind(ps);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / (double) size);
            var content = new ArrayList<T>();
            try (var ps = c.prepareStatement(dataSql)) {
                dataBinder.bind(ps);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        content.add(mapper.map(rs));
                    }
                }
            }
            boolean first = page <= 0;
            boolean last = totalPages == 0 || page >= totalPages - 1;
            return new Page<>(page, size, total, Math.max(totalPages, 0), first, last, content);
        } catch (SQLException e) {
            throw new DaoException(PAGE_QUERY_FAILED, e);
        }
    }

    public static <T> T inTx(Function<Connection, T> work) {
        Connection c = null;
        try {
            c = DatabaseConnection.getConnection();
            c.setAutoCommit(false);
            T out = work.apply(c);
            c.commit();
            return out;
        } catch (RuntimeException e) {
            rollbackQuietly(c);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(c);
            throw new DaoException(TX_FAILED, e);
        } finally {
            resetAutoCommitQuietly(c);
        }
    }

    private static void rollbackQuietly(Connection c) {
        if (c != null) {
            try {
                c.rollback();
            } catch (SQLException e) {
                LOG.log(System.Logger.Level.DEBUG, "rollback failed", e);
            }
        }
    }

    private static void resetAutoCommitQuietly(Connection c) {
        if (c != null) {
            try {
                c.setAutoCommit(true);
                c.close();
            } catch (SQLException e) {
                LOG.log(System.Logger.Level.DEBUG, "reset autocommit/close failed", e);
            }
        }
    }

    static PreparedStatement bind(PreparedStatement ps, Object... params) throws SQLException {
        int i = 1;
        for (Object p : params) {
            setParam(ps, i++, p);
        }
        return ps;
    }

    private static CallableStatement bindCall(CallableStatement cs, Object... params) throws SQLException {
        int i = 1;
        for (Object p : params) {
            setParam(cs, i++, p);
        }
        return cs;
    }

    private static void setParam(PreparedStatement ps, int index, Object param) throws SQLException {
        if (param == null) {
            ps.setObject(index, null);
            return;
        }
        if (param instanceof SqlNull sn) {
            if (sn == SqlNull.DECIMAL) {
                ps.setNull(index, Types.DECIMAL);
            }
            return;
        }
        switch (param) {
            case Long l -> ps.setLong(index, l);
            case Integer in -> ps.setInt(index, in);
            case String s -> ps.setString(index, s);
            case BigDecimal bd -> ps.setBigDecimal(index, bd);
            case Timestamp ts -> ps.setTimestamp(index, ts);
            case LocalDateTime ldt -> ps.setTimestamp(index, Timestamp.valueOf(ldt));
            case LocalDate ld -> ps.setDate(index, Date.valueOf(ld));
            case Date d -> ps.setDate(index, d);
            case Enum<?> en -> ps.setString(index, en.name());
            case Boolean b -> ps.setBoolean(index, b);
            default -> ps.setObject(index, param);
        }
    }
}
