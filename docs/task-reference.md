# Task Reference

This is a compact starting point for the public task DSL. It lists the intended
job-facing methods, required fields and important defaults. Detailed error
catalogues and larger examples can be added later.

## SqlExecutor

Purpose: execute one or more SQL files against one database.

DSL methods:

- `database(String jdbcUrl)`
- `database(String jdbcUrl, String username, String password)`
- `sqlFiles(Object... paths)`
- `sqlParameters(Map<String, ?> parameters)`
- `sqlParameterSets(Map<String, ?>... parameterSets)`

Required:

- `database(...)`
- at least one SQL file via `sqlFiles(...)`

Semantics and defaults:

- The whole task runs in one database transaction.
- Without parameters, the SQL file list is executed once.
- With `sqlParameters`, the SQL file list is executed once with that parameter map.
- With `sqlParameterSets`, every parameter set executes the complete SQL file
  list in order.
- Use either `sqlParameters` or `sqlParameterSets`, not both.
- Password is `@Internal` and not a normal Gradle task input.

## DuckDbSqlExecutor

Purpose: execute SQL files in a prepared DuckDB federation session.

DSL methods:

- `database(Object file)`
- `inMemoryDatabase()`
- `installExtensions(boolean value)`
- `sources { postgres(alias) { ... }; gpkg(alias) { ... }; csv(alias) { ... } }`
- `targets { postgres(alias) { ... } }`
- `exports { gpkg(name) { ... }; parquet(name) { ... }; xlsx(name) { ... }; postgres(name) { ... } }`
- `sqlFiles(Object... paths)`
- `sqlParameters(Map<String, ?> parameters)`
- `sqlParameterSets(Map<String, ?>... parameterSets)`

Required:

- exactly one of `database file(...)` or `inMemoryDatabase()`
- at least one SQL file via `sqlFiles(...)`

Semantics and defaults:

- `mode = "view"` is the default for sources and source objects.
- `mode = "materialize"` copies the configured table/layer into DuckDB before
  user SQL runs.
- PostgreSQL sources are attached read-only.
- CSV sources expose one logical object `<alias>.<table>` backed by DuckDB
  `read_csv(...)`.
- PostgreSQL targets are writable and are exposed under their target alias.
- PostgreSQL exports require `mode = "append"`, `mode = "truncate"` or
  `mode = "replace"`.
- PostgreSQL export `writePath = "jdbc"` is the default and supports controlled
  PostGIS geometry writes.
- PostgreSQL export `writePath = "duckdb"` uses the DuckDB PostgreSQL extension
  directly and is intended for scalar/simple writes.
- PostgreSQL export `create = false` by default; `mode = "replace"` requires
  `create = true`.
- `installExtensions = false` by default; production Docker images should
  preinstall DuckDB extensions.
- GeoPackage and Parquet exports write to temporary files first and then move
  them to the configured target path.
- XLSX exports also write to temporary files first and then move them to the
  configured target path.
- `overwrite = false` by default for exports.
- PostgreSQL passwords are `@Internal` and not normal Gradle task inputs.
- SQL parameters and parameter sets behave like `SqlExecutor`.

## Ili2duckdbImportSchema

Purpose: import an INTERLIS schema into a DuckDB database with `ili2duckdb`.

DSL methods:

- `databaseFile(Object path)`
- `modelNames(String... names)`
- `modelDirectories(String... entries)`
- `schema(String name)`
- `iliFile(Object path)`
- `iliMetaAttrsFile(Object path)`
- `logFile(Object path)`

Required:

- `databaseFile(...)`
- at least one of `modelNames(...)` or `iliFile(...)`

Semantics and defaults:

- DuckDB JDBC stays on the modular repo baseline classpath; the task does not
  bring its own second DuckDB driver.
- `strokeArcs` is enabled internally for the DuckDB flavour and is not exposed
  as a public toggle.
- Common ili2db options are lazy Gradle properties on the task, for example
  `createBasketCol`, `createDatasetCol`, `coalesceJson`, `createFk`,
  `createGeomIdx` or `defaultSrsCode`.
- `pluginFolder` and PostgreSQL-specific flags such as `setupPgExt` are not
  part of this slice.

## Ili2duckdbImport

Purpose: import INTERLIS transfer files or `ilidata:` repository ids into a
DuckDB database with `ili2duckdb`.

DSL methods:

- `databaseFile(Object path)`
- `modelNames(String... names)`
- `modelDirectories(String... entries)`
- `schema(String name)`
- `transferFiles(Object... paths)`
- `repositoryDataIds(String... ids)`
- `datasetNames(String... names)`
- `datasetNamesFromTransferFiles()`
- `datasetNamesFromFiles(Object... paths)`
- `datasetNameSlice(int start)`
- `datasetNameSlice(int start, int endExclusive)`
- `logFile(Object path)`

Required:

- `databaseFile(...)`
- local `transferFiles(...)` or `repositoryDataIds(...)`

Semantics and defaults:

- Use either local `transferFiles(...)` or `repositoryDataIds(...)`, not both.
- Dataset naming is explicit:
  `datasetNames(...)`, `datasetNamesFromTransferFiles()` or
  `datasetNamesFromFiles(...)`.
- `datasetNameSlice(...)` applies only to derived dataset names.
- `failOnException = false` stops the task successfully after the first handled
  `Ili2dbException` and does not continue with later transfer files.
- For import, one shared text log file is aggregated across multi-file runs when
  `logFile(...)` is configured.

## Ili2duckdbExport

Purpose: export INTERLIS transfer files from a DuckDB database with
`ili2duckdb`.

DSL methods:

- `databaseFile(Object path)`
- `modelNames(String... names)`
- `modelDirectories(String... entries)`
- `schema(String name)`
- `dataFiles(Object... paths)`
- `datasetNames(String... names)`
- `datasetNamesFromTransferFiles()`
- `datasetNamesFromFiles(Object... paths)`
- `datasetNameSlice(int start)`
- `datasetNameSlice(int start, int endExclusive)`
- `logFile(Object path)`
- `exportModels(String... names)`

Required:

- `databaseFile(...)`
- local `dataFiles(...)`

Semantics and defaults:

- `repositoryDataIds(...)` are not supported for export.
- `export3 = false` by default.
- `exportModels(...)` limits the export to specific INTERLIS models.
- Dataset naming rules are the same as in `Ili2duckdbImport`.

## Db2Db

Purpose: copy rows selected from a source database into a target table.

DSL methods:

- `sourceDatabase(String jdbcUrl)`
- `sourceDatabase(String jdbcUrl, String username, String password)`
- `targetDatabase(String jdbcUrl)`
- `targetDatabase(String jdbcUrl, String username, String password)`
- `transfer(Object sqlFile, String targetTable, boolean deleteAllRows, String... geometryColumns)`
- `transfer { sqlFile(...); targetTable(...); deleteAllRows(...); geometryColumns(...) }`
- `sqlParameters(Map<String, ?> parameters)`
- `sqlParameterSets(Map<String, ?>... parameterSets)`
- `batchSize(int value)`
- `fetchSize(int value)`

Required:

- `sourceDatabase(...)`
- `targetDatabase(...)`
- at least one `transfer(...)`

Semantics and defaults:

- The whole task runs in one target-database transaction.
- Source database access is read-only from the task perspective.
- `deleteAllRows` runs in the same target transaction as the inserts.
- With `sqlParameterSets`, every parameter set executes the complete transfer
  list in order.
- Default `batchSize`: `5000`
- Default `fetchSize`: `5000`
- Source and target passwords are `@Internal`.
- Geometry column definitions use `column:TYPE` or `column:TYPE:SRID`.

## Gzip

Purpose: write a gzip-compressed copy of one file.

DSL methods:

- `dataFile(Object path)`
- `gzipFile(Object path)`

Required:

- `dataFile(...)`
- `gzipFile(...)`

Semantics and defaults:

- The output parent directory is created when needed.
- There are no task-specific defaults.

## IliValidator

Purpose: validate INTERLIS transfer files with `ilivalidator`.

DSL methods:

- `dataFiles(Object... paths)`
- `modelNames(String... names)`
- `modelDirectories(String... entries)`
- `configFile(Object path)`
- `configRepositoryId(String id)`
- `metaConfigFile(Object path)`
- `metaConfigRepositoryId(String id)`
- `logFile(Object path)`
- `xtfLogFile(Object path)`

Required:

- at least one file via `dataFiles(...)`

Semantics and defaults:

- An empty resolved `dataFiles(...)` set is a no-op, matching the legacy task
  behavior.
- Use either `configFile(...)` or `configRepositoryId(...)`, not both.
- Use either `metaConfigFile(...)` or `metaConfigRepositoryId(...)`, not both.
- Built-in SOGIS and Geowerkstatt/NGK custom functions are registered
  automatically; no public `pluginFolder` is used in this slice.
- `failOnError = true` by default.
- `validationOk` remains available as an execution result when
  `failOnError = false`.

## CsvValidator

Purpose: validate one CSV file with `ilivalidator` and the GRETL CSV reader
adapter.

DSL methods:

- `dataFiles(Object... paths)`
- `modelNames(String... names)`
- `modelDirectories(String... entries)`
- `configFile(Object path)`
- `configRepositoryId(String id)`
- `metaConfigFile(Object path)`
- `metaConfigRepositoryId(String id)`
- `logFile(Object path)`
- `xtfLogFile(Object path)`

Required:

- exactly one file via `dataFiles(...)`

Semantics and defaults:

- `firstLineIsHeader = true` by default.
- `valueSeparator`, `valueDelimiter` and `encoding` are lazy Gradle properties
  on the task.
- Multiple CSV input files are rejected explicitly instead of being processed
  ambiguously.
- `validationOk` behaves like in `IliValidator`.

## XslTransformer

Purpose: transform one or more XML files with one XSLT stylesheet.

DSL methods:

- `xslFile(Object path)`
- `xslResource(String resourceName)`
- `xmlFiles(Object... paths)`
- `outDirectory(Object path)`
- `fileExtension(String fileExtension)`

Required:

- exactly one of `xslFile(...)` or `xslResource(...)`
- at least one XML file via `xmlFiles(...)`
- `outDirectory(...)`

Semantics and defaults:

- The stylesheet is compiled once per task execution.
- Every XML input file is transformed into the output directory.
- Default `fileExtension`: `xtf`

## ReadShapefile

Purpose: read a shapefile through the GeoTools worker runtime and log basic
diagnostics.

DSL methods:

- `shapefile(Object path)`
- `crsCode(String crsCode)`

Required:

- `shapefile(...)`

Semantics and defaults:

- `crsCode(...)` is optional.
- Execution happens in the GeoTools worker runtime.

## Vectorize

Purpose: vectorize selected raster cell values into a GeoPackage.

DSL methods:

- `inputRaster(Object path)`
- `outputGeopackage(Object path)`
- `band(int band)`
- `cellValues(Number... values)`

Required:

- `inputRaster(...)`
- `outputGeopackage(...)`
- non-empty `cellValues(...)`

Semantics and defaults:

- Default `band`: `0`
- `cellValues` must not be empty and must not contain null values.
- Execution happens in the GeoTools worker runtime.

## RasterReclassify

Purpose: reclassify raster values into a new raster.

DSL methods:

- `inputRaster(Object path)`
- `outputRaster(Object path)`
- `breaks(Number... values)`
- `noData(Number value)`

Required:

- `inputRaster(...)`
- `outputRaster(...)`

Semantics and defaults:

- Default `breaks`: `0, 55, 60, 65, 70, 500`
- Default `noData`: `-100`
- Breaks must contain at least two strictly increasing values.
- Execution happens in the GeoTools worker runtime.
