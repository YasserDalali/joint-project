package com.finrisk.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Whitelisted ORDER BY builder for JDBC queries. */
public final class SqlSort {

    private SqlSort() {}

    public static String orderByClause(List<String> sortSpecs, Map<String, String> whitelist, String defaultExpr) {
        if (sortSpecs == null || sortSpecs.isEmpty()) {
            return defaultExpr;
        }
        StringBuilder sb = new StringBuilder();
        for (String spec : sortSpecs) {
            if (spec == null || spec.isBlank()) {
                continue;
            }
            String[] parts = spec.split(",", 2);
            String field = parts[0].trim();
            String dir =
                    parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
            if (!dir.equals("asc") && !dir.equals("desc")) {
                dir = "asc";
            }
            String column = whitelist.get(field.toLowerCase(Locale.ROOT));
            if (column == null) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(column).append(" ").append(dir.toUpperCase(Locale.ROOT));
        }
        return sb.isEmpty() ? defaultExpr : sb.toString();
    }

    public static Map<String, String> usersWhitelist() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", "id");
        m.put("email", "email");
        m.put("fullname", "full_name");
        m.put("createdat", "created_at");
        return m;
    }

    public static Map<String, String> accountsWhitelist() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", "id");
        m.put("accountname", "account_name");
        m.put("cashbalance", "cash_balance");
        m.put("createdat", "created_at");
        return m;
    }

    public static Map<String, String> assetsWhitelist() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", "id");
        m.put("symbol", "symbol");
        m.put("name", "name");
        m.put("createdat", "created_at");
        return m;
    }

    public static Map<String, String> transactionsWhitelist() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", "id");
        m.put("transactiondate", "transaction_date");
        m.put("quantity", "quantity");
        return m;
    }

    /** Spring passes sort as repeated params -> RestAssured sends sort=a&sort=b */
    public static List<String> normalizeSortParams(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream().filter(s -> s != null && !s.isBlank()).collect(Collectors.toList());
    }
}
