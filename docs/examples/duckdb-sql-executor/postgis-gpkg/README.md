# DuckDbSqlExecutor PostGIS + GeoPackage Example

This example combines a PostgreSQL/PostGIS table with the included GeoPackage.
It expects a PostGIS table `agi_pub.gemeindegrenzen` with a geometry column
named `geometrie` in EPSG:2056.

Run from this directory:

```bash
gradle analyse \
  -PpgUrl=jdbc:postgresql://localhost:5432/pub \
  -PpgUser=... \
  -PpgPass=...
```

Outputs:

- `build/analyse.gpkg`
- `build/analyse.parquet`

The example uses `inMemoryDatabase()` because the final artifacts are exported.
It sets `installExtensions true` for local development; production Docker images
should preinstall DuckDB extensions and use the default `installExtensions false`.
