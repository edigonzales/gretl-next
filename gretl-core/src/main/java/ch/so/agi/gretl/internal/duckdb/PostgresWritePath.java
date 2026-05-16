package ch.so.agi.gretl.internal.duckdb;

import java.util.Locale;

public enum PostgresWritePath {
    JDBC("jdbc"),
    DUCKDB("duckdb");

    private final String dslName;

    PostgresWritePath(String dslName) {
        this.dslName = dslName;
    }

    public String dslName() {
        return dslName;
    }

    public static PostgresWritePath parse(String value) {
        if (value == null || value.isBlank()) {
            return JDBC;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (PostgresWritePath path : values()) {
            if (path.dslName.equals(normalized)) {
                return path;
            }
        }
        throw new IllegalArgumentException("postgres export writePath must be jdbc or duckdb: " + value);
    }
}
