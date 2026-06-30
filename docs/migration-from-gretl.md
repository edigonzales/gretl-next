# Migration From Original GRETL

GRETL Modular keeps the Gradle task model, but it deliberately removes the old
public implementation layer from build scripts. A job should describe what a
task should do; Step classes and low-level transfer objects are now internal
implementation details.

## What Is No Longer Public DSL

Do not use these patterns in new modular jobs:

- `Step` classes such as `SqlExecutorStep`, `Db2DbStep`, `GzipStep` or
  `XslTransformerStep`
- `Connector`
- `TransferSet`
- `database = [...]`
- `sourceDb.set(...)`
- `targetDb.set(...)`
- `transferSets.set(...)`
- worker `steps` terminology in GeoTools jobs

The replacement is a small task DSL made of typed Gradle task properties and
helper methods.

## SqlExecutor

Before:

```groovy
tasks.register('executeSql', SqlExecutor) {
    database = [url: 'jdbc:postgresql://localhost/edit', user: 'ddluser', password: dbPassword]
    sqlFiles = files('sql/schema.sql', 'sql/data.sql')
    sqlParameters = [schema: 'public']
}
```

After:

```groovy
tasks.register('executeSql', SqlExecutor) {
    database 'jdbc:postgresql://localhost/edit', 'ddluser', dbPassword
    sqlFiles 'sql/schema.sql', 'sql/data.sql'
    sqlParameters([schema: 'public'])
}
```

For multiple runs of the same SQL file list:

```groovy
tasks.register('loadYears', SqlExecutor) {
    database 'jdbc:postgresql://localhost/edit', 'ddluser', dbPassword
    sqlFiles 'sql/load-year.sql', 'sql/analyze.sql'
    sqlParameterSets([year: 2024], [year: 2025])
}
```

`SqlExecutor` runs the whole task in one database transaction. With
`sqlParameterSets`, every parameter set runs all configured SQL files in order.
If any file or parameter set fails, the whole task is rolled back.

Database passwords are not normal Gradle task inputs. They are intentionally
kept out of Gradle input metadata and debug output.

## Db2Db

Before:

```groovy
def source = new Connector('jdbc:postgresql://localhost/source', 'reader', sourcePassword)
def target = new Connector('jdbc:postgresql://localhost/target', 'writer', targetPassword)

tasks.register('copyRows', Db2Db) {
    sourceDb.set(source)
    targetDb.set(target)
    transferSets.set([
        new TransferSet('sql/select-colors.sql', 'public.colors', true)
    ])
}
```

After:

```groovy
tasks.register('copyRows', Db2Db) {
    sourceDatabase 'jdbc:postgresql://localhost/source', 'reader', sourcePassword
    targetDatabase 'jdbc:postgresql://localhost/target', 'writer', targetPassword
    transfer 'sql/select-colors.sql', 'public.colors', true
}
```

For the expanded transfer syntax:

```groovy
tasks.register('copyParcels', Db2Db) {
    sourceDatabase 'jdbc:postgresql://localhost/source', 'reader', sourcePassword
    targetDatabase 'jdbc:postgresql://localhost/target', 'writer', targetPassword

    transfer {
        sqlFile 'sql/select-parcels.sql'
        targetTable 'public.parcels'
        deleteAllRows true
        geometryColumns 'geom:WKB:2056', 'label_point:WKT:2056'
    }

    batchSize 5000
    fetchSize 5000
}
```

`Db2Db` runs the whole task in one target-database transaction. A failure in any
transfer or any parameter set rolls back every target write, including
`deleteAllRows`.

With `sqlParameterSets`, every parameter set runs the complete transfer list in
order. If source and target are the same database connection target, later
transfers can see earlier writes from the same task transaction.

The geometry column syntax remains `column:TYPE` or `column:TYPE:SRID`, for
example `geom:WKT`, `geom:WKB:2056` or `geom:GEOJSON`. It is configuration on
the `transfer` block, not a public `TransferSet` type.

## Gzip

Before:

```groovy
tasks.register('compressXml', Gzip) {
    dataFile = file('data/input.xml')
    gzipFile = file("$buildDir/out/input.xml.gz")
}
```

After:

```groovy
tasks.register('compressXml', Gzip) {
    dataFile 'data/input.xml'
    gzipFile layout.buildDirectory.file('out/input.xml.gz')
}
```

The task creates the output parent directory when needed.

## Ili2duckdb

Before:

```groovy
tasks.register('ili2duckdbschemaimport', Ili2duckdbImportSchema) {
    dbfile = file('my_gb2av.duckdb')
    models = 'GB2AV'
    modeldir = rootProject.projectDir.toString() + ';http://models.interlis.ch'
    dbschema = 'gb2av'
    coalesceJson = true
    createBasketCol = true
}
```

After:

```groovy
tasks.register('schemaImport', Ili2duckdbImportSchema) {
    databaseFile layout.buildDirectory.file('db/my_gb2av.duckdb')
    modelNames 'GB2AV'
    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
    schema 'gb2av'
    coalesceJson.set(true)
    createBasketCol.set(true)
}
```

For data import:

```groovy
tasks.register('importData', Ili2duckdbImport) {
    dependsOn 'schemaImport'
    databaseFile layout.buildDirectory.file('db/my_gb2av.duckdb')
    modelNames 'GB2AV'
    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
    schema 'gb2av'
    transferFiles 'data/VOLLZUG.xml'
    datasetNamesFromTransferFiles()
    datasetNameSlice 8
}
```

Key changes:

- `dbfile`, `models`, `modeldir` and `dbschema` are replaced by the explicit
  helper methods `databaseFile(...)`, `modelNames(...)`, `modelDirectories(...)`
  and `schema(...)`.
- The old overloaded `dataset` / `datasetSubstring` API is gone. Dataset naming
  is now explicit with `datasetNames(...)`, `datasetNamesFromTransferFiles()`,
  `datasetNamesFromFiles(...)` and `datasetNameSlice(...)`.
- `repositoryDataIds(...)` replaces passing `ilidata:` strings through the old
  generic `dataFile` property.
- DuckDB-specific `strokeArcs` is enabled internally. It is not a public task
  option.

## IliValidator

Before:

```groovy
task validate(type: IliValidator) {
    dataFiles = files('Beispiel2a.xtf')
    logFile = file('ilivalidator.log')
}
```

After:

```groovy
tasks.register('validate', IliValidator) {
    dataFiles 'Beispiel2a.xtf'
    modelDirectories projectDir.toString()
    logFile layout.buildDirectory.file('logs/ilivalidator.log')
}
```

For repository-backed or local validator config files:

```groovy
tasks.register('validateNgk', IliValidator) {
    dataFiles 'NGK_SO_Testbeddata.xtf'
    modelDirectories projectDir.toString(), 'http://models.interlis.ch'
    metaConfigFile 'SO_AFU_Naturgefahren_20240515-gretl-meta.ini'
    logFile layout.buildDirectory.file('logs/ilivalidator.log')
}
```

Key changes:

- `dataFiles` is now a helper method instead of assigning a `FileCollection`.
- Validator config is split into explicit local-vs-repository methods:
  `configFile(...)` / `configRepositoryId(...)` and
  `metaConfigFile(...)` / `metaConfigRepositoryId(...)`.
- The built-in custom functions are registered internally; `pluginFolder` is no
  longer public DSL in this slice.

## CsvValidator

Before:

```groovy
task validate(type: CsvValidator) {
    models = 'CsvModel'
    firstLineIsHeader = false
    dataFiles = files('data1.csv')
    logFile = file('csvvalidator.log')
}
```

After:

```groovy
tasks.register('validateCsv', CsvValidator) {
    dataFiles 'data1.csv'
    modelNames 'CsvModel'
    modelDirectories projectDir.toString()
    firstLineIsHeader.set(false)
    logFile layout.buildDirectory.file('logs/csvvalidator.log')
}
```

Key changes:

- `models` becomes `modelNames(...)`.
- `dataFiles` becomes an explicit helper method.
- CSV-specific settings such as `firstLineIsHeader`, `valueSeparator`,
  `valueDelimiter` and `encoding` are lazy Gradle properties.
- Multiple CSV input files are rejected explicitly instead of being interpreted
  implicitly.

## XslTransformer

Before:

```groovy
tasks.register('transformXml', XslTransformer) {
    xmlFile = file('data/input.xml')
    xslFile = file('xsl/transform.xsl')
    outDirectory = file("$buildDir/out")
}
```

After:

```groovy
tasks.register('transformXml', XslTransformer) {
    xmlFiles 'data/input.xml', 'data/other.xml'
    xslFile 'xsl/transform.xsl'
    outDirectory layout.buildDirectory.dir('out')
    fileExtension 'xml'
}
```

For bundled resources:

```groovy
tasks.register('transformWithResource', XslTransformer) {
    xmlFiles 'data/input.xml'
    xslResource 'xslt/eCH0132_to_SO_AGI_SGV_Meldungen_20221109.xsl'
    outDirectory layout.buildDirectory.dir('out')
}
```

Exactly one of `xslFile` and `xslResource` must be configured. `xmlFiles` must
not be empty.

## GeoTools Tasks

GeoTools jobs no longer expose Step terminology. The task builds a typed
operation request; the worker runtime remains internal.

```groovy
tasks.register('readShapefile', ReadShapefile) {
    shapefile 'data/parcels.shp'
    crsCode 'EPSG:2056'
}

tasks.register('vectorizeRaster', Vectorize) {
    inputRaster 'data/input.tif'
    outputGeopackage layout.buildDirectory.file('vectorized/output.gpkg').get().asFile
    band 0
    cellValues 55d, 65d
}

tasks.register('reclassifyRaster', RasterReclassify) {
    inputRaster 'data/input.tif'
    outputRaster layout.buildDirectory.file('reclassified/output.tif').get().asFile
    breaks 0d, 55d, 60d, 65d, 70d, 500d
    noData -100d
}
```

`ReadShapefile`, `Vectorize` and `RasterReclassify` still run through Gradle
Worker API isolation. That is an execution detail, not build-script DSL.
