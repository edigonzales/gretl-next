package ch.so.agi.gretl.internal.duckdb;

import ch.so.agi.gretl.internal.sql.DatabaseSpec;

import java.util.List;

public record PostgresSourceSpec(
        String alias,
        DatabaseSpec database,
        String mode,
        boolean autoDetectGeometry,
        List<PostgresTableSpec> tables
) implements DuckDbSourceSpec {
    public PostgresSourceSpec {
        DuckDbSql.requireSimpleIdentifier(alias, "source alias");
        if (database == null) {
            throw new IllegalArgumentException("postgres database must not be null");
        }
        DuckDbMode.parse(mode);
        tables = List.copyOf(tables == null ? List.of() : tables);
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("postgres source must contain at least one table");
        }
    }

    @Override
    public List<String> requiredExtensions() {
        return List.of("postgres", "spatial");
    }

    @Override
    public String inputSignature() {
        return "postgres|" + alias + "|" + PostgresJdbcUrl.parse(database.jdbcUrl()).inputSignatureWithoutCredentials()
                + "|" + database.username()
                + "|" + mode + "|" + autoDetectGeometry + "|" + tables.stream()
                .map(PostgresTableSpec::inputSignature)
                .toList();
    }
}
