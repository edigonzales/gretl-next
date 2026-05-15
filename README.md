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
./gradlew stageRuntimeImage
```

`stageRuntimeImage` creates a Docker build context under
`build/runtime-image/docker`. If Docker is available, the local runtime image can
be built with:

```bash
./gradlew buildRuntimeImage
```

The default image tag is `sogis/gretl-modular:test`; override it with
`-PgretlDockerImage=registry/name:tag`.

## Core Usage Example

```groovy
plugins {
    id 'ch.so.agi.gretl'
}

import ch.so.agi.gretl.tasks.SqlExecutor
import ch.so.agi.gretl.tasks.Db2Db
import ch.so.agi.gretl.tasks.Gzip

tasks.register('executeSql', SqlExecutor) {
    database 'jdbc:sqlite:/tmp/example.db'
    sqlFiles 'sql/init.sql'
}

tasks.register('copyRows', Db2Db) {
    sourceDatabase 'jdbc:sqlite:/tmp/source.db'
    targetDatabase 'jdbc:sqlite:/tmp/target.db'
    transfer 'sql/select-colors.sql', 'colors', true
}

tasks.register('compressXml', Gzip) {
    dataFile 'data/input.xml'
    gzipFile layout.buildDirectory.file('out/input.xml.gz').get().asFile
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
    inputRaster 'data/input.tif'
    outputGeopackage layout.buildDirectory.file('vectorized/output.gpkg').get().asFile
    band 0
    cellValues 55d, 65d
}
```

The GeoTools plugin embeds a worker runtime classpath as plugin resources and
passes that classpath to `workerExecutor.classLoaderIsolation`. GeoTools and
ImageIO dependencies are therefore not normal plugin implementation
dependencies.

## Logging And Docker

Core tasks use Gradle logging directly through the `GretlLogger` abstraction.
In Docker this still flows through Gradle's plain console output.

GeoTools worker code logs through a small structured protocol. While running
inside `classLoaderIsolation`, the worker receives a log sink from the plugin
and the plugin maps worker levels to Gradle `lifecycle`, `info`, `debug` and
`error`. If the worker runtime is ever used without that sink, it falls back to
console lines in the form `GRETL_WORKER|<LEVEL>|<message>`; errors go to
stderr, all other levels to stdout. The same format is parseable by the plugin
for a later `processIsolation` implementation.

The Docker runner executes:

```bash
gradle "$@" --init-script /home/gradle/init.gradle --no-daemon --console=plain
```

The staged image contains a file-based Maven repository for both plugin marker
artifacts, so jobs can use:

```groovy
plugins {
    id 'ch.so.agi.gretl'
    id 'ch.so.agi.gretl.geotools'
}
```

## More Context

See [docs/architecture.md](docs/architecture.md) for the detailed rationale,
dependency boundaries, worker layout, logging boundary, Docker packaging, and
known limits of `classLoaderIsolation`.
