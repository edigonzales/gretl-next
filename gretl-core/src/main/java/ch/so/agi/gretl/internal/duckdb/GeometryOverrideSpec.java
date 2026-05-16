package ch.so.agi.gretl.internal.duckdb;

public record GeometryOverrideSpec(
        String column,
        String alias,
        Integer srid,
        String type,
        String encoding,
        boolean force2d,
        boolean include
) {
    public GeometryOverrideSpec {
        if (column == null || column.isBlank()) {
            throw new IllegalArgumentException("geometry column must not be null or blank");
        }
        if (alias != null && !alias.isBlank()) {
            DuckDbSql.requireSimpleIdentifier(alias, "geometry alias");
        }
        if (encoding != null && !encoding.isBlank()) {
            String normalized = encoding.toLowerCase();
            if (!normalized.equals("auto") && !normalized.equals("postgis")
                    && !normalized.equals("wkb") && !normalized.equals("ewkb")) {
                throw new IllegalArgumentException("geometry encoding must be auto, postgis, wkb or ewkb: " + encoding);
            }
            encoding = normalized;
        } else {
            encoding = "auto";
        }
    }
}
