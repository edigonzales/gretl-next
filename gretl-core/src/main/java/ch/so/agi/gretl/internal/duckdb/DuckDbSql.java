package ch.so.agi.gretl.internal.duckdb;

import java.util.regex.Pattern;

public final class DuckDbSql {
    private static final Pattern SIMPLE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private DuckDbSql() {
    }

    public static void requireSimpleIdentifier(String value, String label) {
        if (value == null || !SIMPLE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " must be a simple SQL identifier: " + value);
        }
    }

    public static String quoteIdentifier(String identifier) {
        requireSimpleIdentifier(identifier, "identifier");
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public static String quotePhysicalIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("identifier must not be null or blank");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public static String quoteLiteral(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    public static String stripTrailingSemicolon(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be null or blank");
        }
        String result = query.trim();
        while (result.endsWith(";")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        return result;
    }

    public static QualifiedTable parseQualifiedTable(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("table name must not be null or blank");
        }
        int dot = value.indexOf('.');
        if (dot <= 0 || dot != value.lastIndexOf('.') || dot == value.length() - 1) {
            throw new IllegalArgumentException("PostgreSQL table names must be fully qualified as schema.table: " + value);
        }
        return new QualifiedTable(value.substring(0, dot), value.substring(dot + 1));
    }

    public record QualifiedTable(String schema, String table) {
        public QualifiedTable {
            if (schema == null || schema.isBlank()) {
                throw new IllegalArgumentException("schema must not be null or blank");
            }
            if (table == null || table.isBlank()) {
                throw new IllegalArgumentException("table must not be null or blank");
            }
        }

        public String toSql() {
            return quotePhysicalIdentifier(schema) + "." + quotePhysicalIdentifier(table);
        }
    }
}
