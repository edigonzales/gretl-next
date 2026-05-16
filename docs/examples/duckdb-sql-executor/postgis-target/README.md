# DuckDbSqlExecutor PostGIS Target Example

This example creates a small DuckDB result table and writes it to a
PostgreSQL/PostGIS target table with the controlled JDBC export path.

Run from this directory:

```bash
gradle exportToPostgis \
  -PpgUrl=jdbc:postgresql://localhost:5432/pub \
  -PpgUser=... \
  -PpgPass=... \
  -PpgSchema=public
```

The task replaces `${pgSchema}.duckdb_executor_points` and creates it with a
`geometry(Point, 2056)` column. The schema must already exist and the configured
database user needs permission to drop and create the table.

The example sets `installExtensions true` for local development. Production
Docker images should preinstall DuckDB extensions and use the default
`installExtensions false`.
