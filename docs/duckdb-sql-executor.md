# DuckDbSqlExecutor

`DuckDbSqlExecutor` runs SQL files in a DuckDB session that GRETL prepares from
configured sources and exports. The MVP focuses on live federation: PostgreSQL
and GeoPackage sources stay external and are exposed as logical DuckDB schemas.

## Requirements

- GRETL uses `org.duckdb:duckdb_jdbc:1.5.2.0`.
- Production Docker images must preinstall the DuckDB `postgres` and `spatial`
  extensions for the Gradle user.
- Local development examples may set `installExtensions true`; production jobs
  should leave the default `false`.

DuckDB SQL is trusted build code. It can read and write files, load extensions,
and access configured external systems. Do not treat SQL files as sandboxed user
input.

## Minimal Example

```groovy
tasks.register('analyse', DuckDbSqlExecutor) {
    database file('build/work.duckdb')

    sources {
        gpkg('input') {
            file file('build/input.gpkg')
            layer('perimeter') {
                alias = 'perimeter'
            }
        }
    }

    sqlFiles 'sql/analyse.sql'

    exports {
        gpkg('analyse') {
            query = 'SELECT * FROM result.analyse'
            file file('build/analyse.gpkg')
            layer = 'analyse'
            srs = 'EPSG:2056'
            overwrite = true
        }
    }
}
```

The SQL file uses only logical schemas:

```sql
CREATE SCHEMA IF NOT EXISTS result;

CREATE TABLE result.analyse AS
SELECT id, ST_Area(geom) AS area_m2
FROM input.perimeter;
```

## PostgreSQL/PostGIS Sources

PostgreSQL tables are configured with fully qualified physical names. The source
alias becomes the logical DuckDB schema.

```groovy
sources {
    postgres('pub') {
        database pgUrl, pgUser, pgPass

        table('agi_pub.gemeindegrenzen') {
            alias = 'gemeinden'
        }
    }
}
```

User SQL can then query `pub.gemeinden`.

GRETL discovers PostGIS geometry and geography columns through PostgreSQL
metadata. Geometry values are projected through PostGIS as EWKB and converted in
DuckDB with spatial functions. Explicit geometry configuration is only needed
for overrides:

```groovy
table('agi_pub.objekt') {
    geometry('labelpunkt') {
        include = false
    }
}
```

If a discovered geometry cannot be mapped safely, the task fails before user SQL
runs.

## View vs. Materialize

Default `mode = "view"` keeps sources live. DuckDB reads PostgreSQL or
GeoPackage data when the user query runs.

Use `mode = "materialize"` when repeated access, expensive remote reads, or
large spatial joins are more important than staying live:

```groovy
postgres('pub') {
    mode = 'materialize'
    database pgUrl, pgUser, pgPass
    table('agi_pub.gemeindegrenzen')
}
```

The mode can also be set per table or layer.

## Outputs

`database file(...)` creates a DuckDB database file and models it as a Gradle
output. `inMemoryDatabase()` runs without a DuckDB output file and is useful when
all relevant results are exported.

GeoPackage and Parquet exports are written through temporary files first. The
final target is replaced only after the DuckDB SQL and export steps complete.

## Runnable Examples

- [GeoPackage only](examples/duckdb-sql-executor/gpkg-only/README.md)
- [PostGIS and GeoPackage](examples/duckdb-sql-executor/postgis-gpkg/README.md)
