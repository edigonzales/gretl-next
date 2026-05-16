package ch.so.agi.gretl.internal.duckdb;

import java.util.List;

public record PostgresExportSpec(
        String name,
        String target,
        String query,
        String table,
        PostgresWriteMode mode,
        PostgresWritePath writePath,
        boolean create,
        List<GeometryOverrideSpec> geometries
) implements DuckDbExportSpec {
    public PostgresExportSpec {
        DuckDbSql.requireSimpleIdentifier(name, "export name");
        DuckDbSql.requireSimpleIdentifier(target, "postgres export target");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("postgres export query must not be null or blank");
        }
        DuckDbSql.parseQualifiedTable(table);
        if (mode == null) {
            throw new IllegalArgumentException("postgres export mode must be configured");
        }
        if (writePath == null) {
            writePath = PostgresWritePath.JDBC;
        }
        if (mode == PostgresWriteMode.REPLACE && !create) {
            throw new IllegalArgumentException("postgres export mode 'replace' requires create = true");
        }
        geometries = List.copyOf(geometries == null ? List.of() : geometries);
    }

    @Override
    public List<String> requiredExtensions() {
        return writePath == PostgresWritePath.DUCKDB ? List.of("postgres") : List.of("spatial");
    }

    @Override
    public String inputSignature() {
        return "postgres|" + name + "|" + target + "|" + query + "|" + table + "|"
                + mode.dslName() + "|" + writePath.dslName() + "|" + create + "|" + geometries;
    }
}
