package com.finrisk.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds safe SQL {@code ORDER BY} fragments from user input using an allow-list (whitelist). */
public final class SqlSort {

    private static final String SORT_ASC = "asc";
    private static final String SORT_DESC = "desc";

    /** Hides the default constructor so this class can only be used via static helpers. */
    private SqlSort() {}

    /** Turns a list of sort strings into a comma-separated {@code ORDER BY} clause. */
    public static String orderByClause(List<String> sortSpecs, Map<String, String> whitelist, String defaultExpr) {
        if (sortSpecs == null || sortSpecs.isEmpty()) {
            return defaultExpr;
        }
        StringBuilder sb = new StringBuilder();
        for (String spec : sortSpecs) {
            appendOneSort(sb, spec, whitelist);
        }
        if (sb.isEmpty()) {
            return defaultExpr;
        }
        return sb.toString();
    }

    /** Appends one validated sort fragment to a growing {@code ORDER BY} string. */
    private static void appendOneSort(StringBuilder sb, String spec, Map<String, String> whitelist) {
        if (spec == null || spec.isBlank()) {
            return;
        }
        String[] parts = spec.split(",", 2);
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : SORT_ASC;
        if (!dir.equals(SORT_ASC) && !dir.equals(SORT_DESC)) {
            dir = SORT_ASC;
        }
        String column = whitelist.get(field.toLowerCase(Locale.ROOT));
        if (column == null) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(", ");
        }
        sb.append(column).append(" ").append(dir.toUpperCase(Locale.ROOT));
    }

    /** Returns the allow-list mapping API sort keys to real {@code users} table columns. */
    public static Map<String, String> usersWhitelist() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "id");
        map.put("email", "email");
        map.put("fullname", "full_name");
        map.put("createdat", "created_at");
        return map;
    }

    /** Returns the allow-list mapping for sorting brokerage accounts. */
    public static Map<String, String> accountsWhitelist() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "id");
        map.put("accountname", "account_name");
        map.put("cashbalance", "cash_balance");
        map.put("createdat", "created_at");
        return map;
    }

    /** Returns the allow-list mapping for sorting tradable assets. */
    public static Map<String, String> assetsWhitelist() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "id");
        map.put("symbol", "symbol");
        map.put("name", "name");
        map.put("createdat", "created_at");
        return map;
    }

    /** Returns the allow-list mapping for sorting ledger transactions. */
    public static Map<String, String> transactionsWhitelist() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "id");
        map.put("transactiondate", "transaction_date");
        map.put("quantity", "quantity");
        return map;
    }

    /** Cleans repeated HTTP {@code sort} query parameters into a plain list of tokens. */
    public static List<String> normalizeSortParams(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : raw) {
            if (value != null && !value.isBlank()) {
                cleaned.add(value);
            }
        }
        return cleaned;
    }
}
