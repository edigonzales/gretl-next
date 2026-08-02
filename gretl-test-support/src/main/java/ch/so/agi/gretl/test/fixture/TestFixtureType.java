package ch.so.agi.gretl.test.fixture;

import java.util.Locale;

public enum TestFixtureType {
    HTTP,
    FTP,
    S3,
    POSTGIS,
    DUCKDB_EXTENSIONS;

    public static TestFixtureType fromYaml(String value) {
        if (value == null) throw new IllegalArgumentException("fixture type must not be null");
        try {
            return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown fixture type: " + value, e);
        }
    }
}
