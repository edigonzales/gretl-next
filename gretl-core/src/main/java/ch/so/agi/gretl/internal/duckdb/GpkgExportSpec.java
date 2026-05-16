package ch.so.agi.gretl.internal.duckdb;

import java.nio.file.Path;
import java.util.List;

public record GpkgExportSpec(
        String name,
        String query,
        Path file,
        String layer,
        String srs,
        boolean overwrite
) implements DuckDbExportSpec {
    public GpkgExportSpec {
        DuckDbSql.requireSimpleIdentifier(name, "export name");
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("gpkg export query must not be null or blank");
        }
        if (file == null) {
            throw new IllegalArgumentException("gpkg export file must not be null");
        }
        if (layer == null || layer.isBlank()) {
            layer = name;
        }
        DuckDbSql.requireSimpleIdentifier(layer, "gpkg export layer");
    }

    @Override
    public List<String> requiredExtensions() {
        return List.of("spatial");
    }

    @Override
    public String inputSignature() {
        return "gpkg|" + name + "|" + query + "|" + file + "|" + layer + "|" + srs + "|" + overwrite;
    }
}
