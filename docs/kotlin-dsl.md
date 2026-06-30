# Kotlin DSL Examples

Kotlin builds need explicit imports for GRETL task types. Use `mapOf("key" to
value)` for SQL parameters and normal Kotlin varargs for helper methods such as
`sqlFiles(...)`, `geometryColumns(...)`, `cellValues(...)` and `breaks(...)`.

Core GRETL file and directory helpers accept `layout.buildDirectory` providers
directly, for example:

```kotlin
layout.buildDirectory.file("out/result.gpkg")
layout.buildDirectory.dir("out")
```

GeoTools examples still pass `File` objects until that module is aligned with
the same provider-friendly helpers.

## SqlExecutor

```kotlin
import ch.so.agi.gretl.tasks.SqlExecutor

plugins {
    id("ch.so.agi.gretl")
}

tasks.register<SqlExecutor>("loadYears") {
    database("jdbc:postgresql://localhost/edit", "ddluser", providers.gradleProperty("dbPassword").get())
    sqlFiles("sql/schema.sql", "sql/load-year.sql", "sql/analyze.sql")
    sqlParameterSets(
        mapOf("year" to 2024),
        mapOf("year" to 2025)
    )
}
```

## Db2Db Simple Transfer

```kotlin
import ch.so.agi.gretl.tasks.Db2Db

plugins {
    id("ch.so.agi.gretl")
}

tasks.register<Db2Db>("copyColors") {
    sourceDatabase("jdbc:sqlite:/tmp/source.db")
    targetDatabase("jdbc:sqlite:/tmp/target.db")
    transfer("sql/select-colors.sql", "colors", true)
}
```

## Db2Db Transfer Block

```kotlin
import ch.so.agi.gretl.tasks.Db2Db

plugins {
    id("ch.so.agi.gretl")
}

tasks.register<Db2Db>("copyParcels") {
    sourceDatabase("jdbc:postgresql://localhost/source", "reader", providers.gradleProperty("sourcePassword").get())
    targetDatabase("jdbc:postgresql://localhost/target", "writer", providers.gradleProperty("targetPassword").get())

    transfer {
        sqlFile("sql/select-parcels.sql")
        targetTable("public.parcels")
        deleteAllRows(true)
        geometryColumns("geom:WKT:2056", "label_point:WKB:2056")
    }

    sqlParameterSets(
        mapOf("year" to 2024),
        mapOf("year" to 2025)
    )
    batchSize(5000)
    fetchSize(5000)
}
```

## DuckDbSqlExecutor

`DuckDbSqlExecutor` is Groovy-DSL-first in the MVP. The public names avoid
Groovy-only keywords such as `as`; table and layer renaming uses `alias`, so a
future Kotlin DSL can map the same model without a breaking rename.

The same applies to PostgreSQL writes: `targets` names writable connections,
while `exports.postgres` describes a concrete query-to-table write. The
properties use Kotlin-friendly names such as `target`, `table`, `mode`,
`writePath` and `create`.

## Gzip

```kotlin
import ch.so.agi.gretl.tasks.Gzip

plugins {
    id("ch.so.agi.gretl")
}

tasks.register<Gzip>("compressXml") {
    dataFile("data/input.xml")
    gzipFile(layout.buildDirectory.file("out/input.xml.gz"))
}
```

## XslTransformer

```kotlin
import ch.so.agi.gretl.tasks.XslTransformer

plugins {
    id("ch.so.agi.gretl")
}

tasks.register<XslTransformer>("transformXml") {
    xslFile("xsl/transform.xsl")
    xmlFiles("data/input.xml", "data/other.xml")
    outDirectory(layout.buildDirectory.dir("transformed"))
    fileExtension("xml")
}
```

With an XSL resource bundled in `gretl-core`:

```kotlin
import ch.so.agi.gretl.tasks.XslTransformer

plugins {
    id("ch.so.agi.gretl")
}

tasks.register<XslTransformer>("transformWithResource") {
    xslResource("xslt/eCH0132_to_SO_AGI_SGV_Meldungen_20221109.xsl")
    xmlFiles("data/input.xml")
    outDirectory(layout.buildDirectory.dir("transformed"))
}
```

## ReadShapefile

```kotlin
import ch.so.agi.gretl.geotools.tasks.ReadShapefile

plugins {
    id("ch.so.agi.gretl.geotools")
}

tasks.register<ReadShapefile>("readShapefile") {
    shapefile("data/parcels.shp")
    crsCode("EPSG:2056")
}
```

## Vectorize

```kotlin
import ch.so.agi.gretl.geotools.tasks.Vectorize

plugins {
    id("ch.so.agi.gretl.geotools")
}

tasks.register<Vectorize>("vectorizeRaster") {
    inputRaster("data/input.tif")
    outputGeopackage(layout.buildDirectory.file("vectorized/output.gpkg").get().asFile)
    band(0)
    cellValues(55.0, 65.0)
}
```

## RasterReclassify

```kotlin
import ch.so.agi.gretl.geotools.tasks.RasterReclassify

plugins {
    id("ch.so.agi.gretl.geotools")
}

tasks.register<RasterReclassify>("reclassifyRaster") {
    inputRaster("data/input.tif")
    outputRaster(layout.buildDirectory.file("reclassified/output.tif").get().asFile)
    breaks(0.0, 55.0, 60.0, 65.0, 70.0, 500.0)
    noData(-100.0)
}
```
