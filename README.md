# GRETL Modular

This repository contains the modular GRETL Gradle runtime. Gradle remains the
local pipeline and task engine; general data-processing tasks and heavy
GeoTools processing are separated so consumers load only the runtime they use.

GRETL supports and tests Groovy Gradle builds with the modern `plugins {}` DSL.
Kotlin DSL may work through Gradle, but it is not a tested GRETL contract.

## Modules

- `gretl-core` provides the `ch.so.agi.gretl` plugin and the general GRETL task
  DSL. The generated [task reference](docs/reference/reference.adoc) is the
  authoritative task inventory.
- `gretl-geotools` provides `ch.so.agi.gretl.geotools`. Public task shells stay
  lightweight; GeoTools processing runs from the embedded worker runtime with
  Gradle classloader isolation.
- `gretl-doclet` generates the task reference and LSP metadata from the same
  task annotations.
- `gretl-combined-tests`, `gretl-job-tests` and `gretl-test-support` are internal
  verification modules and are never published or copied into the runtime image.
- `gretl-control-common`, `gretl-control-server` and `gretl-control-worker`
  implement the optional Control Plane.
- `gretl-lsp` implements editor support from generated task metadata.

`gretl-core` deliberately excludes the GeoTools, JAI/ImageIO and CRS registry
runtime. It still contains the legacy JTS types required by INTERLIS and the
lightweight Shapefile/GeoPackage compatibility tasks. See the
[architecture guide](docs/architecture.md) for the complete boundary.

## Build and test

Use Java 17 and the checked-in Gradle 7.6.4 wrapper:

```bash
./gradlew clean check
./gradlew sourceIntegrationTest
./gradlew publishedArtifactTest
./gradlew runtimeImageTest
./gradlew :gretl-job-tests:coverageTest
./gradlew ciCheck
```

`clean check` is the Docker-free local gate. `sourceIntegrationTest` adds
PostgreSQL/PostGIS and fixture-backed source tests. `publishedArtifactTest`
resolves the actual plugin markers, POMs and transitive dependencies from an
isolated Maven repository. `runtimeImageTest` verifies the built Docker image,
offline Gradle resolution, one-shot and service execution, application
networks, and required canonical jobs.

The central [testing guide](docs/testing/testing.adoc) describes the complete
gate hierarchy. The [canonical job coverage matrix](docs/testing/task-coverage.yaml)
tracks multi-backend job traces; it is not a line-coverage report.

## Runtime image

Stage or build the image with:

```bash
./gradlew stageRuntimeImage
./gradlew buildRuntimeImage
```

The default development tag is `sogis/gretl-modular:test`; override it with
`-PgretlDockerImage=registry/name:tag`. Runtime-image tests require a reachable
Docker daemon and fail explicitly when Docker is unavailable.

The image contains a structured Maven repository at
`/opt/gretl/maven-repository`, Gradle 7.6.4, Java 17, the GeoTools worker
runtime and the required DuckDB extensions. The `gretl` launcher always runs
Gradle with `--offline`. This disables remote build-dependency downloads, not
application connections to PostGIS, S3, FTP or HTTP services.

Detailed contracts and diagnostics are documented in
[runtime-image testing](docs/testing/runtime-image-tests.adoc).

## Core usage example

```groovy
plugins {
    id 'ch.so.agi.gretl'
}

import ch.so.agi.gretl.tasks.SqlExecutor
import ch.so.agi.gretl.tasks.Gzip

tasks.register('executeSql', SqlExecutor) {
    database 'jdbc:sqlite:/tmp/example.db'
    sqlFiles 'sql/init.sql'
}

tasks.register('compressXml', Gzip) {
    dataFile 'data/input.xml'
    gzipFile layout.buildDirectory.file('out/input.xml.gz')
}
```

## GeoTools usage example

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
    outputGeopackage layout.buildDirectory.file('vectorized/output.gpkg')
    band 0
    cellValues 55d, 65d
}
```

## Publishing snapshots

Snapshot publications target `https://jars.interlis.guru/snapshots`. Supply
credentials as Gradle properties or environment variables:

```properties
gretlPublishUsername=...
gretlPublishPassword=...
```

```bash
export GRETL_PUBLISH_USERNAME=...
export GRETL_PUBLISH_PASSWORD=...
./gradlew publishSnapshots
```

Publication is protected by the CI gates; test modules and generated runtime
outputs are not publication artifacts.

## Documentation

- [Documentation index](docs/index.md)
- [Task reference](docs/reference/reference.adoc)
- [Migration from original GRETL](docs/migration-from-gretl.md)
- [Architecture](docs/architecture.md)
- [Testing](docs/testing/testing.adoc)
- [Control Plane](docs/control-plane.md)
