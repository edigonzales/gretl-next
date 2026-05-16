package ch.so.agi.gretl.internal.duckdb;

import ch.so.agi.gretl.internal.sql.DatabaseSpec;

import java.util.List;

public record PostgresTargetSpec(
        String alias,
        DatabaseSpec database
) implements DuckDbTargetSpec {
    public PostgresTargetSpec {
        DuckDbSql.requireSimpleIdentifier(alias, "target alias");
        if (database == null) {
            throw new IllegalArgumentException("postgres target database must not be null");
        }
        PostgresJdbcUrl.parse(database.jdbcUrl());
    }

    @Override
    public List<String> requiredExtensions() {
        return List.of("postgres");
    }

    @Override
    public String inputSignature() {
        return "postgres|" + alias + "|" + PostgresJdbcUrl.parse(database.jdbcUrl()).inputSignatureWithoutCredentials()
                + "|" + database.username();
    }
}
