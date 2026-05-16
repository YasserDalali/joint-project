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

/** Small JDBC façade used by every DAO to open connections, bind parameters, and map rows safely. */
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

    /** Blocks instantiation because {@code Db} only exposes static helpers. */
    private Db() {}

    /** Sentinel values understood by {@link Db#bind(PreparedStatement, Object...)} when JDBC needs typed SQL NULL. */
    public enum SqlNull {
        /** Marks parameters that must become {@link Types#DECIMAL} NULLs for numeric columns. */
        DECIMAL
    }

    /**

     * Functional Strategy-style hook that converts one JDBC {@link ResultSet} row into a domain object.

     * @param <T> the mapped Java type produced for each row.

     */
    @FunctionalInterface
    public interface RowMapper<T> {
        /** Reads the current row from a {@link ResultSet} into a Java object. */
        T map(ResultSet rs) throws SQLException;
    }

    /** Functional hook that assigns bind variables on a {@link PreparedStatement} used in paging helpers. */
    @FunctionalInterface
    public interface PreparedStatementBinder {
        /** Sets every {@code ?} placeholder needed before executing a prepared statement. */
        void bind(PreparedStatement ps) throws SQLException;
    }

    /** Runs a SQL {@code SELECT} expected to return zero or one row using an internal connection. */
    public static <T> Optional<T> findOne(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = bind(connection.prepareStatement(sql), params);
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return Optional.of(mapper.map(resultSet));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DaoException(QUERY_FAILED_PREFIX + sql, e);
        }
    }

    /** Runs a {@code SELECT} for a single row while reusing an existing JDBC transaction {@link Connection}. */
    public static <T> Optional<T> findOne(
            Connection connection, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement statement = bind(connection.prepareStatement(sql), params);
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return Optional.of(mapper.map(resultSet));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DaoException(QUERY_FAILED_PREFIX + sql, e);
        }
    }

    /** Executes a {@code SELECT} that may return many rows using a freshly borrowed connection. */
    public static <T> List<T> findMany(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = bind(connection.prepareStatement(sql), params);
                ResultSet resultSet = statement.executeQuery()) {
            List<T> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(mapper.map(resultSet));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException(QUERY_FAILED_PREFIX + sql, e);
        }
    }

    /** Executes a multi-row {@code SELECT} on an existing transactional {@link Connection}. */
    public static <T> List<T> findMany(
            Connection connection, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement statement = bind(connection.prepareStatement(sql), params);
                ResultSet resultSet = statement.executeQuery()) {
            List<T> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(mapper.map(resultSet));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException(QUERY_FAILED_PREFIX + sql, e);
        }
    }

    /** Runs an {@code INSERT}/{@code UPDATE}/{@code DELETE} style statement and returns affected row count. */
    public static int update(String sql, Object... params) {
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = bind(connection.prepareStatement(sql), params)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(UPDATE_FAILED_PREFIX + sql, e);
        }
    }

    /** Runs a mutating SQL statement using an existing {@link Connection}. */
    public static int update(Connection connection, String sql, Object... params) {
        try (PreparedStatement statement = bind(connection.prepareStatement(sql), params)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(UPDATE_FAILED_PREFIX + sql, e);
        }
    }

    /** Executes a statement for side effects and ignores the returned update count. */
    public static void exec(String sql, Object... params) {
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = bind(connection.prepareStatement(sql), params)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(EXEC_FAILED_PREFIX + sql, e);
        }
    }

    /** Runs a side-effect statement on a caller-supplied {@link Connection}. */
    public static void exec(Connection connection, String sql, Object... params) {
        try (PreparedStatement statement = bind(connection.prepareStatement(sql), params)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException(EXEC_FAILED_PREFIX + sql, e);
        }
    }

    /** Runs an {@code INSERT ... OUTPUT} (or similar) query returning the inserted projection as {@code T}. */
    public static <T> T insertReturning(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = bind(connection.prepareStatement(sql), params);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new DaoException(INSERT_NO_ROW_PREFIX + sql, null);
            }
            return mapper.map(resultSet);
        } catch (SQLException e) {
            throw new DaoException(INSERT_FAILED_PREFIX + sql, e);
        }
    }

    /** Performs {@link #insertReturning(String, RowMapper, Object...)} using an existing {@link Connection}. */
    public static <T> T insertReturning(
            Connection connection, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement statement = bind(connection.prepareStatement(sql), params);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new DaoException(INSERT_NO_ROW_PREFIX + sql, null);
            }
            return mapper.map(resultSet);
        } catch (SQLException e) {
            throw new DaoException(INSERT_FAILED_PREFIX + sql, e);
        }
    }

    /** Invokes a JDBC {@link CallableStatement} (typically a stored procedure) with bound parameters. */
    public static void call(String sql, Object... params) {
        try (Connection connection = DatabaseConnection.getConnection();
                CallableStatement statement = bindCall(connection.prepareCall(sql), params)) {
            statement.execute();
        } catch (SQLException e) {
            RuntimeException mapped = JdbcSqlExceptionMapper.map(e);
            if (mapped != null) {
                throw mapped;
            }
            throw new DaoException(CALL_FAILED_PREFIX + sql, e);
        }
    }

    /** Runs a scalar {@code SELECT} returning the first column of the first row as a {@code long}. */
    public static long queryLong(String sql, Object... params) {
        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = bind(connection.prepareStatement(sql), params);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return 0L;
            }
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new DaoException(SCALAR_FAILED_PREFIX + sql, e);
        }
    }

    /** Reads a single {@code long} column using an existing transactional {@link Connection}. */
    public static long queryLong(Connection connection, String sql, Object... params) {
        try (PreparedStatement statement = bind(connection.prepareStatement(sql), params);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return 0L;
            }
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new DaoException(SCALAR_FAILED_PREFIX + sql, e);
        }
    }

    /** Loads one page of rows plus total-hit metadata for offset/limit style APIs. */
    public static <T> Page<T> findPage(
            String countSql,
            String dataSql,
            RowMapper<T> mapper,
            int page,
            int size,
            PreparedStatementBinder countBinder,
            PreparedStatementBinder dataBinder) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            long total;
            try (PreparedStatement countStatement = connection.prepareStatement(countSql)) {
                countBinder.bind(countStatement);
                try (ResultSet countResult = countStatement.executeQuery()) {
                    countResult.next();
                    total = countResult.getLong(1);
                }
            }

            int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / (double) size);
            List<T> content = new ArrayList<>();
            try (PreparedStatement dataStatement = connection.prepareStatement(dataSql)) {
                dataBinder.bind(dataStatement);
                try (ResultSet dataResult = dataStatement.executeQuery()) {
                    while (dataResult.next()) {
                        content.add(mapper.map(dataResult));
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

    /** Runs arbitrary DAO work inside a single JDBC transaction with commit/rollback handling. */
    public static <T> T inTx(Function<Connection, T> work) {
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);
            T result = work.apply(connection);
            connection.commit();
            return result;
        } catch (RuntimeException e) {
            rollbackQuietly(connection);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(connection);
            throw new DaoException(TX_FAILED, e);
        } finally {
            resetAutoCommitQuietly(connection);
        }
    }

    /** Attempts to roll back a JDBC transaction without crashing if rollback itself fails. */
    private static void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                LOG.log(System.Logger.Level.DEBUG, "rollback failed", e);
            }
        }
    }

    /** Restores {@code autoCommit=true} and closes the connection after transactional work. */
    private static void resetAutoCommitQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                LOG.log(System.Logger.Level.DEBUG, "reset autocommit/close failed", e);
            }
        }
    }

    /** Assigns Java objects to positional {@code ?} placeholders on a {@link PreparedStatement}. */
    static PreparedStatement bind(PreparedStatement statement, Object... params) throws SQLException {
        int index = 1;
        for (Object param : params) {
            setParam(statement, index, param);
            index++;
        }
        return statement;
    }

    /** Binds parameters onto a {@link CallableStatement} used for stored procedures. */
    private static CallableStatement bindCall(CallableStatement statement, Object... params)
            throws SQLException {
        int index = 1;
        for (Object param : params) {
            setParam(statement, index, param);
            index++;
        }
        return statement;
    }

    /** Writes one JDBC parameter using the correct setter based on runtime Java type. */
    private static void setParam(PreparedStatement statement, int index, Object param)
            throws SQLException {
        if (param == null) {
            statement.setObject(index, null);
            return;
        }
        if (param instanceof SqlNull sqlNull) {
            if (sqlNull == SqlNull.DECIMAL) {
                statement.setNull(index, Types.DECIMAL);
            }
            return;
        }
        if (param instanceof Long longValue) {
            statement.setLong(index, longValue);
            return;
        }
        if (param instanceof Integer intValue) {
            statement.setInt(index, intValue);
            return;
        }
        if (param instanceof String stringValue) {
            statement.setString(index, stringValue);
            return;
        }
        if (param instanceof BigDecimal decimalValue) {
            statement.setBigDecimal(index, decimalValue);
            return;
        }
        if (param instanceof Timestamp timestampValue) {
            statement.setTimestamp(index, timestampValue);
            return;
        }
        if (param instanceof LocalDateTime dateTimeValue) {
            statement.setTimestamp(index, Timestamp.valueOf(dateTimeValue));
            return;
        }
        if (param instanceof LocalDate dateValue) {
            statement.setDate(index, Date.valueOf(dateValue));
            return;
        }
        if (param instanceof Date sqlDateValue) {
            statement.setDate(index, sqlDateValue);
            return;
        }
        if (param instanceof Enum<?> enumValue) {
            statement.setString(index, enumValue.name());
            return;
        }
        if (param instanceof Boolean booleanValue) {
            statement.setBoolean(index, booleanValue);
            return;
        }
        statement.setObject(index, param);
    }
}
