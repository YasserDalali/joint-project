package com.finrisk.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

/** Holds a single shared HikariCP {@link DataSource} so every DAO borrows connections from one pool. */
public final class DatabaseConnection {

    private static final AtomicReference<DataSource> INSTANCE = new AtomicReference<>();

    /** Prevents accidental instantiation of this static helper class. */
    private DatabaseConnection() {}

    /** Lazily creates and returns the application-wide JDBC {@link DataSource}. */
    public static DataSource getDataSource() {
        DataSource current = INSTANCE.get();
        if (current != null) {
            return current;
        }
        synchronized (DatabaseConnection.class) {
            current = INSTANCE.get();
            if (current != null) {
                return current;
            }
            HikariDataSource created = buildHikariDataSource();
            INSTANCE.set(created);
            return created;
        }
    }

    /** Borrows a live JDBC {@link Connection} from the shared pool. */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /** Closes the pooled datasource and clears it so the next call rebuilds configuration. */
    public static void resetForTests() {
        synchronized (DatabaseConnection.class) {
            DataSource dataSource = INSTANCE.getAndSet(null);
            if (dataSource instanceof HikariDataSource hikari) {
                hikari.close();
            }
        }
    }

    /** Builds a {@link HikariDataSource} pointed at SQL Server using environment properties. */
    private static HikariDataSource buildHikariDataSource() {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "1433");
        String database = env("DB_NAME", "FinRiskDB");
        String user = env("DB_USER", "sa");
        String password = env("DB_PASSWORD", "");

        String jdbcUrl =
                "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + database
                        + ";encrypt=true;trustServerCertificate=true";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setPoolName("finrisk-hikari");
        return new HikariDataSource(config);
    }

    /** Reads a configuration value from the OS environment with JVM system-property fallback. */
    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }
}
