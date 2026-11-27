package com.ieltspath.assessment.infrastructure.scheduler;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Inserts test rows into real tables: the caller names the columns that matter and every other NOT NULL column
 * without a default gets a plain value of its type. Used to seed data for account-deletion tests across the schema.
 */
final class RowSeeder {
    private final JdbcTemplate jdbc;

    RowSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    record Column(String name, String type, boolean required) { }

    List<Column> columns(String table) {
        return jdbc.query("""
                SELECT a.attname, format_type(a.atttypid, a.atttypmod) AS type, a.attnotnull AND NOT a.atthasdef AS required
                FROM pg_attribute a
                WHERE a.attrelid = ?::regclass AND a.attnum > 0 AND NOT a.attisdropped AND a.attgenerated = ''
                ORDER BY a.attnum
                """, (rs, row) -> new Column(rs.getString(1), rs.getString(2), rs.getBoolean(3)), table);
    }

    /** Inserts one row and returns the value written to {@code id} when the table has one. */
    UUID insert(String table, Map<String, Object> given) {
        Map<String, Object> values = new LinkedHashMap<>(given);
        Map<String, String> types = new LinkedHashMap<>();
        UUID id = null;
        Map<String, String> allowed = allowedValues(table);
        for (Column column : columns(table)) {
            types.put(column.name(), column.type());
            if (!values.containsKey(column.name()) && (column.required() || column.name().equals("id") && column.type().equals("uuid"))) {
                values.put(column.name(), allowed.getOrDefault(column.name(), String.valueOf(plainValue(column.type()))));
            }
        }
        if (values.get("id") instanceof UUID uuid) id = uuid;
        else if (values.get("id") instanceof String text && types.getOrDefault("id", "").equals("uuid")) id = UUID.fromString(text);
        List<String> names = new ArrayList<>(values.keySet());
        String placeholders = String.join(", ", names.stream()
                .map(name -> values.get(name) == null ? "NULL" : "CAST(? AS " + types.get(name) + ")").toList());
        List<Object> args = names.stream().filter(name -> values.get(name) != null).map(name -> (Object) String.valueOf(values.get(name))).toList();
        try {
            jdbc.update("INSERT INTO " + table + " (" + String.join(", ", names) + ") VALUES (" + placeholders + ")", args.toArray());
        } catch (RuntimeException rejected) {
            throw new IllegalStateException("Cannot seed " + table + "; its checks are " + checks(table), rejected);
        }
        return id;
    }

    private static final java.util.regex.Pattern ONE_OF = java.util.regex.Pattern.compile(
            "\\(\\(?(\\w+)\\)?(?:::text)? = ANY \\(\\(?ARRAY\\['([^']+)'");

    /** The first value each {@code column IN (...)} check accepts, so enum-like columns get a valid value. */
    Map<String, String> allowedValues(String table) {
        Map<String, String> allowed = new LinkedHashMap<>();
        for (String check : checks(table)) {
            var matcher = ONE_OF.matcher(check);
            while (matcher.find()) allowed.putIfAbsent(matcher.group(1), matcher.group(2));
        }
        return allowed;
    }

    List<String> checks(String table) {
        return jdbc.queryForList("SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = ?::regclass AND contype = 'c'",
                String.class, table);
    }

    static Object plainValue(String type) {
        if (type.endsWith("[]")) return "{}";
        if (type.equals("uuid")) return UUID.randomUUID();
        if (type.startsWith("timestamp")) return "2026-11-01T08:00:00Z";
        if (type.equals("date")) return "2026-11-01";
        if (type.startsWith("time")) return "08:00";
        if (type.equals("interval")) return "1 hour";
        if (type.equals("boolean")) return "false";
        if (type.equals("jsonb") || type.equals("json")) return "{}";
        if (type.startsWith("integer") || type.startsWith("bigint") || type.startsWith("smallint")
                || type.startsWith("numeric") || type.startsWith("double") || type.startsWith("real")) return "1";
        // Distinct text, cut to the column length, so unique columns accept several seeded rows.
        String text = "x" + UUID.randomUUID().toString().replace("-", "");
        var length = java.util.regex.Pattern.compile("\\((\\d+)\\)").matcher(type);
        return length.find() ? text.substring(0, Math.min(text.length(), Integer.parseInt(length.group(1)))) : text;
    }

    /** Every table with one of {@code userColumns}, mapped to the matching columns. */
    Map<String, List<String>> userColumns(Set<String> userColumns) {
        Map<String, List<String>> tables = new TreeMap<>();
        jdbc.query("""
                SELECT table_name, column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND data_type = 'uuid' AND column_name = ANY (?)
                ORDER BY 1, 2
                """, rs -> {
            tables.computeIfAbsent(rs.getString(1), key -> new ArrayList<>()).add(rs.getString(2));
        }, (Object) userColumns.toArray(String[]::new));
        return tables;
    }

    /** Rows of each table that mention {@code userId} in one of its user columns. */
    Map<String, Integer> rowsMentioning(Map<String, List<String>> tables, UUID userId) {
        Map<String, Integer> counts = new TreeMap<>();
        tables.forEach((table, cols) -> {
            String where = String.join(" OR ", cols.stream().map(col -> col + " = ?").toList());
            Object[] args = cols.stream().map(col -> (Object) userId).toArray();
            Integer count = jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + where, Integer.class, args);
            if (count != null && count > 0) counts.put(table, count);
        });
        return counts;
    }
}
