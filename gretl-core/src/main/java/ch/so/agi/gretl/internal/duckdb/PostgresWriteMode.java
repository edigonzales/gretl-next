package ch.so.agi.gretl.internal.duckdb;

import java.util.Locale;

public enum PostgresWriteMode {
    APPEND("append"),
    TRUNCATE("truncate"),
    REPLACE("replace");

    private final String dslName;

    PostgresWriteMode(String dslName) {
        this.dslName = dslName;
    }

    public String dslName() {
        return dslName;
    }

    public static PostgresWriteMode parseRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("postgres export mode must be configured: append, truncate or replace");
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (PostgresWriteMode mode : values()) {
            if (mode.dslName.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("postgres export mode must be append, truncate or replace: " + value);
    }
}
