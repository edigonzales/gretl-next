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
./gradlew publishedArtifactTest
./gradlew stageRuntimeImage
./gradlew runtimeImageSmokeTest
./gradlew runtimeImageTest
./gradlew ciCheck
./gradlew :gretl-control-server:bootRun
./gradlew :gretl-control-worker:bootRun
```

`./gradlew clean check` is the fast local check and does not require Docker.
The `:gretl-core:integrationTest` task starts PostgreSQL/PostGIS containers with
Testcontainers and is run separately.

The normal `test` and `integrationTest` tasks execute Gradle TestKit projects
with the source plugin classpath. `:gretl-core:publishedFunctionalTest`,
`:gretl-core:publishedIntegrationTest` and
`:gretl-geotools:publishedFunctionalTest` instead resolve the real plugin
markers from the isolated Maven repository under
`build/published-test/maven-repo`. The aggregate `publishedArtifactTest` task
publishes and verifies both plugins before running those black-box consumer
projects. It is the published-artifact release gate; it does not test the
Docker runtime image or its dependency-closed execution contract. The runtime-image level remains
a separate deployment gate after the source-classpath and
published-artifact checks. The generated consumer settings do not use
`mavenLocal()`.

`stageRuntimeImage` creates a Docker build context under
`build/runtime-image/docker`. If Docker is available, the local runtime image can
be built with:

```bash
./gradlew buildRuntimeImage
```

The default image tag is `sogis/gretl-modular:test`; override it with
`-PgretlDockerImage=registry/name:tag`.

Runtime-image tests require a running Docker daemon and fail explicitly when
Docker is unavailable. The image-test build records the immutable image ID in
`build/runtime-image/test/image-id.txt`; override its tag with
`-PgretlRuntimeImageTestTag=...`:

```bash
./gradlew buildRuntimeImageForTest
./gradlew runtimeImageContractTest
./gradlew runtimeImageDependencyClosureTest
./gradlew runtimeImageServiceTest
./gradlew runtimeImageIntegrationTest
```

The default consumer contract is the modern `plugins {}` DSL. The GRETL runtime
image starts Gradle with `--offline`, which prevents Gradle from downloading
additional plugins and dependencies while a job is running. It does not disable
the job's network: jobs may still connect to PostGIS, S3, FTP, HTTP and other
application services. One-shot and service lifecycle are independent from this
dependency policy. See [Runtime-image testing](docs/testing/runtime-image-tests.adoc)
and the [runtime task coverage matrix](docs/testing/runtime-image-coverage.yaml).

## Publishing Snapshots

Snapshot publications target
`https://jars.interlis.guru/snapshots`.

Provide credentials either as Gradle properties in `~/.gradle/gradle.properties`
or via environment variables:

```properties
gretlPublishUsername=...
gretlPublishPassword=...
```

```bash
export GRETL_PUBLISH_USERNAME=...
export GRETL_PUBLISH_PASSWORD=...
```

Publish all configured GRETL artifacts with:

```bash
./gradlew publishSnapshots
```

This includes the Gradle plugin publications and plugin marker artifacts for
`gretl-core` and `gretl-geotools`, plus the additional library and Spring Boot
artifacts from the other published modules.

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
- [Task reference](docs/reference/reference.adoc)
- [Architecture](docs/architecture.md)
- [Control Plane](docs/control-plane.md)
