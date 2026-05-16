package ch.so.agi.gretl.internal.duckdb;

import java.util.Locale;

public enum DuckDbMode {
    VIEW("view"),
    MATERIALIZE("materialize");

    private final String dslName;

    DuckDbMode(String dslName) {
        this.dslName = dslName;
    }

    public String dslName() {
        return dslName;
    }

    public static DuckDbMode parse(String value) {
        if (value == null || value.isBlank()) {
            return VIEW;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (DuckDbMode mode : values()) {
            if (mode.dslName.equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("mode must be either 'view' or 'materialize': " + value);
    }
}
