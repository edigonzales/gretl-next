package ch.so.agi.gretl.internal.duckdb;

import java.nio.file.Path;
import java.util.List;

public record CsvSourceSpec(
        String alias,
        Path file,
        String table,
        String mode,
        Boolean header,
        String delimiter,
        boolean allVarchar
) implements DuckDbSourceSpec {
    public CsvSourceSpec {
        DuckDbSql.requireSimpleIdentifier(alias, "source alias");
        if (file == null) {
            throw new IllegalArgumentException("csv file must not be null");
        }
        if (table == null || table.isBlank()) {
            table = "data";
        }
        DuckDbSql.requireSimpleIdentifier(table, "csv source table");
        DuckDbMode.parse(mode);
        if (delimiter != null && delimiter.length() != 1) {
            throw new IllegalArgumentException("csv delimiter must be exactly one character: " + delimiter);
        }
    }

    @Override
    public List<String> requiredExtensions() {
        return List.of();
    }

    @Override
    public String inputSignature() {
        return "csv|" + alias + "|" + file + "|" + table + "|" + mode + "|" + header + "|" + delimiter + "|" + allVarchar;
    }
}
