package ch.so.agi.gretl.internal.duckdb;

import java.nio.file.Path;
import java.util.List;

public record XlsxExportSpec(
        String name,
        String query,
        Path file,
        String sheet,
        boolean header,
        int sheetRowLimit,
        boolean overwrite
) implements DuckDbFileExportSpec {
    public XlsxExportSpec {
        DuckDbSql.requireSimpleIdentifier(name, "export name");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("xlsx export query must not be null or blank");
        }
        if (file == null) {
            throw new IllegalArgumentException("xlsx export file must not be null");
        }
        if (sheet == null || sheet.isBlank()) {
            sheet = "Sheet1";
        }
        if (sheetRowLimit <= 0) {
            throw new IllegalArgumentException("xlsx export sheetRowLimit must be greater than zero");
        }
    }

    @Override
    public List<String> requiredExtensions() {
        return List.of("excel");
    }

    @Override
    public String inputSignature() {
        return "xlsx|" + name + "|" + query + "|" + file + "|" + sheet + "|" + header + "|" + sheetRowLimit + "|" + overwrite;
    }
}
