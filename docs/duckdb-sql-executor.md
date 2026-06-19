# DuckDbSqlExecutor

`DuckDbSqlExecutor` runs SQL files in a DuckDB session that GRETL prepares from
configured sources, writable targets and exports. The current slice supports
federated PostgreSQL and GeoPackage sources plus CSV files exposed as logical
DuckDB schemas.

## Requirements

- GRETL uses `org.duckdb:duckdb_jdbc:1.5.2.0`.
- Production Docker images must preinstall the DuckDB `postgres`, `spatial` and
  `excel` extensions for the Gradle user.
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

## CSV Sources

CSV sources expose one logical DuckDB object under `<alias>.<table>`. The file
is read through DuckDB `read_csv(...)`.

```groovy
sources {
    csv('input') {
        file file('data/input.csv')
        table = 'records'
        delimiter = ';'
        header = true
    }
}
```

User SQL can then query `input.records`.

Optional CSV settings in this slice:

- `table`: logical object name, default `data`.
- `mode`: `view` or `materialize`, default `view`.
- `header`: optional; when omitted DuckDB auto-detects it.
- `delimiter`: optional single-character delimiter.
- `allVarchar`: optional, default `false`.

## Outputs

`database file(...)` creates a DuckDB database file and models it as a Gradle
output. `inMemoryDatabase()` runs without a DuckDB output file and is useful when
all relevant results are exported.

GeoPackage, Parquet and XLSX exports are written through temporary files first.
The final target is replaced only after the DuckDB SQL and export steps
complete.

## PostgreSQL/PostGIS Targets

PostgreSQL targets are writable attachments. They are separate from
`sources.postgres`, which always stays read-only.

```groovy
targets {
    postgres('out') {
        database pgUrl, pgUser, pgPass
    }
}
```

The target alias is visible to user SQL:

```sql
CREATE TABLE out.agi_result.direct_result AS
SELECT * FROM result.analyse;
```

Direct target SQL is the advanced mode. GRETL prepares credentials and the
DuckDB attachment, but the SQL author is responsible for DDL, types and geometry
conversion details.

## PostgreSQL/PostGIS Exports

The recommended PostgreSQL write path is a controlled export:

```groovy
exports {
    postgres('analyse_db') {
        target = 'out'
        query = 'SELECT * FROM result.analyse'
        table = 'agi_result.analyse'
        mode = 'truncate'

        geometry('geom') {
            srid = 2056
            type = 'MULTIPOLYGON'
        }
    }
}
```

PostgreSQL export properties:

- `target`: configured `targets.postgres` alias.
- `query`: DuckDB query whose result is written.
- `table`: fully qualified PostgreSQL target table, `schema.table`.
- `mode`: required, one of `append`, `truncate`, `replace`.
- `writePath`: optional, default `jdbc`; alternative `duckdb`.
- `create`: default `false`; required for `mode = 'replace'`.

`writePath = 'jdbc'` streams the DuckDB query result through GRETL and writes via
PostgreSQL JDBC. This is the default because GRETL can validate target columns,
map geometries with `ST_AsHEXWKB` and PostGIS `ST_GeomFromWKB`, and fail with a
clear message when the mapping is incomplete.

`writePath = 'duckdb'` uses DuckDB's PostgreSQL extension directly. It is useful
for scalar tables and simple cases. Geometry mappings are intentionally rejected
on this path; use `writePath = 'jdbc'` for controlled PostGIS geometry writes.

When `create = true`, scalar column types are inferred from the DuckDB query
metadata. Geometry columns must be declared explicitly because PostGIS table DDL
needs SRID and geometry type.

DuckDB permits only one attached database to be written inside a single
transaction. Jobs that use writable DuckDB PostgreSQL targets therefore use
explicit transaction boundaries around those writes. Controlled JDBC exports are
still rollbacked on the PostgreSQL connection if a later task step fails.

## Runnable Examples

- [GeoPackage only](examples/duckdb-sql-executor/gpkg-only/README.md)
- [CSV to Parquet and XLSX](examples/duckdb-sql-executor/csv-xlsx-parquet/README.md)
- [PostGIS and GeoPackage](examples/duckdb-sql-executor/postgis-gpkg/README.md)
- [PostGIS target export](examples/duckdb-sql-executor/postgis-target/README.md)
