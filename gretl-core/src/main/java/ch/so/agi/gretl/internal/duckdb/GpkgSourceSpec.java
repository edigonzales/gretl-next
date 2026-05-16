package ch.so.agi.gretl.internal.duckdb;

import java.nio.file.Path;
import java.util.List;

public record GpkgSourceSpec(
        String alias,
        Path file,
        String mode,
        List<GpkgLayerSpec> layers
) implements DuckDbSourceSpec {
    public GpkgSourceSpec {
        DuckDbSql.requireSimpleIdentifier(alias, "source alias");
        if (file == null) {
            throw new IllegalArgumentException("gpkg file must not be null");
        }
        DuckDbMode.parse(mode);
        layers = List.copyOf(layers == null ? List.of() : layers);
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("gpkg source must contain at least one layer");
        }
    }

    @Override
    public List<String> requiredExtensions() {
        return List.of("spatial");
    }

    @Override
    public String inputSignature() {
        return "gpkg|" + alias + "|" + file + "|" + mode + "|" + layers.stream()
                .map(GpkgLayerSpec::inputSignature)
                .toList();
    }
}
