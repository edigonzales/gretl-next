# DuckDbSqlExecutor GeoPackage Example

This example is self-contained. It reads the included GeoPackage through a
logical schema and exports the result.

Run from this directory:

```bash
gradle analyse
```

Outputs:

- `build/work.duckdb`
- `build/analyse.gpkg`
- `build/analyse.parquet`

The example sets `installExtensions true` for local development. Production
GRETL Docker images should preinstall DuckDB extensions and use the default
`installExtensions false`.
