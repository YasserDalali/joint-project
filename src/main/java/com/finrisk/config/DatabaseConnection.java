package com.finrisk.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton JDBC pool — raw connections for DAO layer (see {@code uml.md} §6.5).
 */
public final class DatabaseConnection {

    private static final AtomicReference<DataSource> INSTANCE = new AtomicReference<>();

    private DatabaseConnection() {}

    public static DataSource getDataSource() {
        DataSource cur = INSTANCE.get();
        if (cur != null) {
            return cur;
        }
        synchronized (DatabaseConnection.class) {
            cur = INSTANCE.get();
            if (cur != null) {
                return cur;
            }
            HikariDataSource created = buildHikariDataSource();
            INSTANCE.set(created);
            return created;
        }
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /** Visible for integration tests that replace the pool. */
    public static void resetForTests() {
        synchronized (DatabaseConnection.class) {
            DataSource ds = INSTANCE.getAndSet(null);
            if (ds instanceof HikariDataSource hd) {
                hd.close();
            }
        }
    }

    private static HikariDataSource buildHikariDataSource() {
        String host = env("DB_HOST", "localhost");
        String port = env("DB_PORT", "1433");
        String database = env("DB_NAME", "FinRiskDB");
        String user = env("DB_USER", "sa");
        String password = firstNonBlank(
                env("DB_PASSWORD", ""),
                env("SA_PASSWORD", "")
        );

        String jdbcUrl =
                "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + database
                        + ";encrypt=true;trustServerCertificate=true";

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setPoolName("finrisk-hikari");
        return new HikariDataSource(cfg);
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isEmpty()) {
            return v;
        }
        v = System.getProperty(key);
        return v != null && !v.isEmpty() ? v : defaultValue;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
