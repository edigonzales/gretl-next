# GRETL Modular Prototype

This repository is a prototype for splitting GRETL into smaller Gradle plugins.
It keeps Gradle as the local pipeline/DAG engine, but separates general GRETL
tasks from GeoTools-heavy processing.

## Modules

- `gretl-core`
  - Gradle plugin ID: `ch.so.agi.gretl`
  - Ported tasks: `SqlExecutor`, `Db2Db`, `Gzip`, `XslTransformer`
  - Shared service: `gretlCoreService`
  - Public task types stay under `ch.so.agi.gretl.tasks.*`.

- `gretl-geotools`
  - Gradle plugin ID: `ch.so.agi.gretl.geotools`
  - Ported tasks: `ReadShapefile`, `Vectorize`, `RasterReclassify`
  - Shared service: `gretlGeoToolsService`
  - GeoTools code runs through Gradle Worker API with `classLoaderIsolation`.
- `gretl-control-common`, `gretl-control-server`, `gretl-control-worker`
  - Lightweight GRETL control plane prototype with a Spring Boot server,
    Git-backed job manifest, Quartz scheduling, run history, log storage,
    encrypted server-side secrets and pull workers that start `gretl` processes.

There is intentionally no raster plugin yet. Raster-like GeoTools tasks stay in
`gretl-geotools` for this prototype.

## Build And Test

The wrapper is pinned to Gradle 7.6.4. Java 17 is configured through Gradle
toolchains.

```bash
./gradlew --version
./gradlew clean check
./gradlew :gretl-core:integrationTest
./gradlew stageRuntimeImage
./gradlew :gretl-control-server:bootRun
./gradlew :gretl-control-worker:bootRun
```

`./gradlew clean check` is the fast local check and does not require Docker.
The `:gretl-core:integrationTest` task starts PostgreSQL/PostGIS containers with
Testcontainers and is run separately.

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

## Documentation

- [Documentation index](docs/index.md)
- [Migration from original GRETL](docs/migration-from-gretl.md)
- [Kotlin DSL examples](docs/kotlin-dsl.md)
- [Task reference](docs/task-reference.md)
- [Architecture](docs/architecture.md)
- [Control Plane](docs/control-plane.md)
