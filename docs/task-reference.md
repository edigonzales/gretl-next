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

## CsvImport / CsvExport

Purpose: import one CSV file into a database table or export one database table
to a CSV file.

DSL methods:

- `database(String jdbcUrl)`
- `database(String jdbcUrl, String username, String password)`
- `dataFile(Object path)`
- `tableName(String name)`
- `schemaName(String name)`
- `attributes(String... names)` for `CsvExport`
- `firstLineIsHeader(boolean value)`
- `valueDelimiter(String value)`
- `valueSeparator(String value)`
- `encoding(String value)`
- `batchSize(int value)` for `CsvImport`

Required:

- `database(...)`
- `dataFile(...)`
- `tableName(...)`

Semantics and defaults:

- `firstLineIsHeader = true` by default.
- Database passwords are `@Internal`.
- Import and export each run in one database transaction.

## Csv2Excel

Purpose: convert one CSV file into an XLSX workbook.

DSL methods:

- `csvFile(Object path)`
- `outputFile(Object path)`
- `firstLineIsHeader(boolean value)`
- `valueDelimiter(String value)`
- `valueSeparator(String value)`
- `encoding(String value)`
- `models(String value)`
- `modeldir(String value)`

Required:

- `csvFile(...)`
- `outputFile(...)`

Semantics and defaults:

- `firstLineIsHeader = true` by default.
- With `models(...)`, the CSV is read through the INTERLIS-aware CSV reader and
  the XLSX writer receives the compiled model.

## JsonImport / JsonValidator

Purpose: import JSON array/object documents into a database text column, or
validate JSON data with the GRETL JSON reader adapter.

DSL methods:

- `JsonImport`: `database(...)`, `jsonFile(Object path)`,
  `qualifiedTableName(String name)`, `columnName(String name)`,
  `deleteAllRows(boolean value)`
- `JsonValidator`: validator methods from `IliValidator`, using
  `dataFiles(Object... paths)` for JSON files

Required:

- `JsonImport`: `database(...)`, `jsonFile(...)`, `qualifiedTableName(...)`,
  `columnName(...)`
- `JsonValidator`: exactly one JSON file via `dataFiles(...)`

Semantics and defaults:

- `JsonImport.deleteAllRows = false` by default.
- `JsonValidator` accepts JSON arrays and single JSON objects. Missing
  validator attributes such as `@topic`, `@id` and `@bid` are added
  temporarily from `@type`; source files are not modified.

## GpkgImport / GpkgExport / GpkgValidator

Purpose: import/export GeoPackage tables or validate a GeoPackage table with
the GRETL GPKG reader adapter.

DSL methods:

- `database(String jdbcUrl)`
- `database(String jdbcUrl, String username, String password)`
- `dataFile(Object path)`
- `schemaName(String name)`
- `GpkgImport`: `srcTableName(String name)`, `dstTableName(String name)`
- `GpkgExport`: `srcTableName(String... names)`,
  `dstTableName(String... names)`
- `GpkgValidator`: validator methods from `IliValidator` plus
  `tableName(String name)`
- `batchSize(int value)`
- `fetchSize(int value)`

Required:

- `GpkgImport`: `database(...)`, `dataFile(...)`, `srcTableName(...)`,
  `dstTableName(...)`
- `GpkgExport`: `database(...)`, `dataFile(...)`, non-empty matching
  `srcTableName(...)` and `dstTableName(...)`
- `GpkgValidator`: exactly one file via `dataFiles(...)` and `tableName(...)`

Semantics and defaults:

- Database passwords are `@Internal`.
- Import and export each run in one database transaction.
- Multi-table `GpkgExport` rejects source/destination list count mismatches.

## Gpkg2Dxf

Purpose: convert geometry tables in an ili2gpkg GeoPackage to DXF files.

DSL methods:

- `dataFile(Object path)`
- `outputDir(Object path)`

Required:

- `dataFile(...)`
- `outputDir(...)`

Semantics and defaults:

- One DXF file is written per geometry class table.
- The DXF layer is read from ili2db `dxflayer` meta attributes when present;
  otherwise `default` is used.

## Av2ch

Purpose: convert Swiss cadastral ITF files to the federal AV model.

DSL methods:

- `inputFiles(Object... paths)`
- `outputDirectory(Object path)`
- `modeldir(String value)`
- `language(String value)`
- `zip(boolean value)`

Required:

- at least one file via `inputFiles(...)`
- `outputDirectory(...)`

Semantics and defaults:

- `language = "de"` by default. Supported values are `de` and `it`.
- `zip = false` by default.
- File collections are processed in stable path order.

## Av2geobau

Purpose: convert cadastral ITF files to GeoBau DXF files.

DSL methods:

- `itfFiles(Object... paths)`
- `dxfDirectory(Object path)`
- `modeldir(String value)`
- `logFile(Object path)`
- `proxy(String value)`
- `proxyPort(int value)`
- `zip(boolean value)`

Required:

- at least one file via `itfFiles(...)`
- `dxfDirectory(...)`

Semantics and defaults:

- `zip = false` by default.
- When `zip = true`, the DXF file and bundled GeoBau reference PDFs are added
  to a per-input ZIP archive.
- File collections are processed in stable path order.

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
