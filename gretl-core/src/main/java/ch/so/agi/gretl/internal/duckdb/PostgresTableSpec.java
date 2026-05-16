package ch.so.agi.gretl.internal.duckdb;

import java.util.List;

public record PostgresTableSpec(
        String physicalSchema,
        String physicalTable,
        String alias,
        String mode,
        List<String> columns,
        List<GeometryOverrideSpec> geometries
) {
    public PostgresTableSpec {
        if (physicalSchema == null || physicalSchema.isBlank()) {
            throw new IllegalArgumentException("physicalSchema must not be null or blank");
        }
        if (physicalTable == null || physicalTable.isBlank()) {
            throw new IllegalArgumentException("physicalTable must not be null or blank");
        }
        if (alias == null || alias.isBlank()) {
            alias = physicalTable;
        }
        DuckDbSql.requireSimpleIdentifier(alias, "table alias");
        DuckDbMode.parse(mode);
        columns = List.copyOf(columns == null ? List.of() : columns);
        geometries = List.copyOf(geometries == null ? List.of() : geometries);
    }

    public static PostgresTableSpec fromQualifiedName(String name, String alias, String mode,
            List<String> columns, List<GeometryOverrideSpec> geometries) {
        DuckDbSql.QualifiedTable table = DuckDbSql.parseQualifiedTable(name);
        return new PostgresTableSpec(table.schema(), table.table(), alias, mode, columns, geometries);
    }

    public String physicalName() {
        return physicalSchema + "." + physicalTable;
    }

    public String inputSignature() {
        return physicalName() + "|" + alias + "|" + mode + "|" + columns + "|" + geometries;
    }
}
