# DuckDbSqlExecutor CSV Example

This example is self-contained. It reads the included CSV through a logical
schema and exports the transformed result to Parquet and Excel.

Run from this directory:

```bash
gradle convert
```

Outputs:

- `build/analyse.parquet`
- `build/analyse.xlsx`

The example sets `installExtensions true` for local development. Production
GRETL Docker images should preinstall DuckDB extensions and use the default
`installExtensions false`.
