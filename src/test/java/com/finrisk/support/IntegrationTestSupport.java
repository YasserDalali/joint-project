package com.finrisk.support;

import com.finrisk.config.DatabaseConnection;

public final class IntegrationTestSupport {

    private IntegrationTestSupport() {}

    public static void configureTestDatabase() {
        DatabaseConnection.resetForTests();
        setIfMissing("DB_HOST", "localhost");
        setIfMissing("DB_PORT", "1433");
        setIfMissing("DB_NAME", "FinRiskDB_Test");
        setIfMissing("DB_USER", "sa");
        String pwd = System.getenv("DB_PASSWORD");
        if (pwd == null || pwd.isBlank()) {
            pwd = System.getenv("SA_PASSWORD");
        }
        if (pwd != null && !pwd.isBlank()) {
            System.setProperty("DB_PASSWORD", pwd);
        }
    }

    private static void setIfMissing(String key, String def) {
        String env = System.getenv(key);
        String cur = System.getProperty(key);
        if ((cur == null || cur.isBlank()) && env != null && !env.isBlank()) {
            System.setProperty(key, env);
        }
        if (System.getProperty(key) == null || System.getProperty(key).isBlank()) {
            System.setProperty(key, def);
        }
    }
}
