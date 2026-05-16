package ch.so.agi.gretl.internal.duckdb;

import java.nio.file.Path;
import java.util.List;

public record ParquetExportSpec(
        String name,
        String query,
        Path file,
        boolean overwrite
) implements DuckDbExportSpec {
    public ParquetExportSpec {
        DuckDbSql.requireSimpleIdentifier(name, "export name");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("parquet export query must not be null or blank");
        }
        if (file == null) {
            throw new IllegalArgumentException("parquet export file must not be null");
        }
    }

    @Override
    public List<String> requiredExtensions() {
        return List.of();
    }

    @Override
    public String inputSignature() {
        return "parquet|" + name + "|" + query + "|" + file + "|" + overwrite;
    }
}
