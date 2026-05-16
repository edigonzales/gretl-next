package ch.so.agi.gretl.internal.duckdb;

import java.util.List;

public record GpkgLayerSpec(
        String layer,
        String alias,
        String mode,
        List<String> columns,
        String srs
) {
    public GpkgLayerSpec {
        if (layer == null || layer.isBlank()) {
            throw new IllegalArgumentException("layer must not be null or blank");
        }
        if (alias == null || alias.isBlank()) {
            alias = layer;
        }
        DuckDbSql.requireSimpleIdentifier(alias, "layer alias");
        DuckDbMode.parse(mode);
        columns = List.copyOf(columns == null ? List.of() : columns);
    }

    public String inputSignature() {
        return layer + "|" + alias + "|" + mode + "|" + columns + "|" + srs;
    }
}
