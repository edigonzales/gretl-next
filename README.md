# GRETL Modular Prototype

This repository is a prototype for splitting GRETL into smaller Gradle plugins.
It keeps Gradle as the local pipeline/DAG engine, but separates general GRETL
tasks from GeoTools-heavy processing.

## Modules

- `gretl-core`
  - Gradle plugin ID: `ch.so.agi.gretl`
  - First ported tasks: `SqlExecutor`, `Db2Db`, `Gzip`, `XslTransformer`
  - Shared service: `gretlCoreService`
  - Package compatibility is kept under `ch.so.agi.gretl.*`.

- `gretl-geotools`
  - Gradle plugin ID: `ch.so.agi.gretl.geotools`
  - First GeoTools tasks: `ReadShapefile`, `Vectorize`, `RasterReclassify`
  - Shared service: `gretlGeoToolsService`
  - GeoTools code runs through Gradle Worker API with `classLoaderIsolation`.

There is intentionally no raster plugin yet. Raster-like GeoTools tasks stay in
`gretl-geotools` for this prototype.

## Build

The wrapper is pinned to Gradle 7.6.4. Java 17 is configured through Gradle
toolchains.

```bash
./gradlew --version
./gradlew clean check
```

## Core Usage Example

```groovy
plugins {
    id 'ch.so.agi.gretl'
}

import ch.so.agi.gretl.tasks.SqlExecutor
import ch.so.agi.gretl.tasks.Gzip

tasks.register('executeSql', SqlExecutor) {
    database.set(['jdbc:sqlite:/tmp/example.db'])
    sqlFiles.set(files('sql/init.sql'))
}

tasks.register('compressXml', Gzip) {
    dataFile.set(file('data/input.xml'))
    gzipFile.set(layout.buildDirectory.file('out/input.xml.gz').get().asFile)
}
```

## GeoTools Usage Example

```groovy
plugins {
    id 'ch.so.agi.gretl.geotools'
}

import ch.so.agi.gretl.geotools.tasks.Vectorize

gretlGeotools {
    defaultCrsCode.set('EPSG:2056')
}

tasks.register('vectorizeRaster', Vectorize) {
    inputRaster.set(layout.projectDirectory.file('data/input.tif'))
    outputGeopackage.set(layout.buildDirectory.file('vectorized/output.gpkg'))
    band.set(0)
    cellValues.set([55d, 65d])
}
```

The GeoTools plugin embeds a worker runtime classpath as plugin resources and
passes that classpath to `workerExecutor.classLoaderIsolation`. GeoTools and
ImageIO dependencies are therefore not normal plugin implementation
dependencies.

## More Context

See [docs/architecture.md](docs/architecture.md) for the detailed rationale,
dependency boundaries, worker layout, and known limits of `classLoaderIsolation`.
