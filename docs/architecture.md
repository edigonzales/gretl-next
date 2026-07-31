# GRETL Modular Architecture Prototype

## Goal

The prototype tests a pragmatic split of GRETL into focused Gradle plugins:

- `gretl-core` owns general GRETL tasks and stable helper APIs.
- `gretl-geotools` owns GeoTools-specific tasks and isolates heavy GeoTools
  dependencies in a Worker API runtime.

The important boundary is not only project structure. The core plugin should not
pull GeoTools, JTS, ImageIO, CRS/EPSG or raster dependencies into builds that
only need SQL or file-oriented GRETL tasks.

## BuildServices

Each plugin registers one Gradle `BuildService`:

- `CoreGretlBuildService` as `gretlCoreService`
- `GeoToolsBuildService` as `gretlGeoToolsService`

Tasks declare `usesService(...)` so Gradle can see the shared resource. The
GeoTools service starts with `maxParallelUsages = 1`, because GeoTools/ImageIO
uses JVM-wide registries and caches in some paths.

A BuildService is not an isolation boundary. It runs in the Gradle daemon JVM and
uses the plugin classloader. It is useful for lifecycle, coordination, caches and
parallelism limits, but not for dependency conflict protection.

## Core Plugin

`gretl-core` keeps the old public package shape under `ch.so.agi.gretl.*`.
The first ported tasks are:

- `SqlExecutor`
- `Db2Db`
- `Gzip`
- `XslTransformer`

Only the dependencies needed for these tasks are included: JDBC drivers,
`ehisqlgen`, Commons IO and Saxon. GeoTools dependencies are deliberately absent.

## GeoTools Plugin

The public task API uses only Gradle and JDK property types:

- `RegularFileProperty`
- `Property<String>`
- `Property<Integer>`
- `Property<Double>`
- `ListProperty<Double>`

GeoTools types such as `CoordinateReferenceSystem`, `SimpleFeatureSource` or
coverage classes do not appear in task properties or extensions. This keeps the
Gradle plugin API light and avoids leaking the worker runtime into build scripts.

## Worker Runtime

`gretl-geotools` has two Java source sets:

- `main`: Gradle plugin, task classes, BuildService, WorkerAction shell
- `worker`: actual GeoTools processing classes

The `workerRuntimeJar` task builds the worker classes. `processResources` embeds
that jar and the worker runtime dependency jars under
`gretl-geotools-worker-classpath/`. At runtime the plugin resolves these embedded
jars and passes them to:

```java
workerExecutor.classLoaderIsolation(spec -> {
    spec.getClasspath().from(getWorkerClasspath());
});
```

`GeoToolsWorkerAction` is intentionally lightweight. It has no direct GeoTools
imports and invokes the worker runtime reflectively after Gradle has created the
isolated worker classloader.

## ClassLoader Isolation Limits

`classLoaderIsolation` keeps Java dependencies off the main plugin classpath and
is the first choice for testing dependency conflicts such as Guava, Jackson,
JTS, CRS and ImageIO versions.

It still runs in the Gradle daemon JVM. JVM-wide state remains shared. GeoTools
and ImageIO use global service registries, so the worker runtime refreshes
GeoTools/ImageIO providers at the start of each worker execution. This is a
prototype-level mitigation for classloader-isolated workers in one JVM.

If native libraries, GDAL bindings, JNI/JNA, memory leaks or registry conflicts
become the dominant problem, the next step is to keep the same task API and
switch the worker submission to `processIsolation`.

## Logging Boundary

Logging is treated as an explicit boundary between Gradle plugin code and
worker runtime code.

Core tasks run directly in Gradle task code. Their `GretlLogger` implementation
is installed by the core plugin and maps to Gradle's `Logger` methods. The
standalone fallback is only used when core classes are executed outside the
Gradle plugin. This avoids accidental behavior changes just because Gradle
classes happen to be visible on a classpath.

Public job syntax and migration guidance are documented separately:

- [Migration from original GRETL](migration-from-gretl.md)
- [Kotlin DSL examples](kotlin-dsl.md)
- [Task reference](reference/reference.adoc)

GeoTools worker code cannot depend on Gradle logging APIs because the worker
runtime is meant to remain movable between `classLoaderIsolation`,
`processIsolation`, and possible standalone diagnostics. The worker runtime
therefore emits four levels:

- `LIFECYCLE`
- `INFO`
- `DEBUG`
- `ERROR`

During the current `classLoaderIsolation` execution, `GeoToolsWorkerAction`
passes a `BiConsumer<String, String>` into `GeoToolsWorkerRuntime`. Worker
`LogEnvironment` installs that sink for the current thread, emits worker log
messages through it, and clears it in `finally`. On the plugin side,
`WorkerLogBridge` maps the levels as follows:

- `LIFECYCLE` to `logger.lifecycle`
- `INFO` to `logger.info`
- `DEBUG` to `logger.debug`
- `ERROR` to `logger.error`

If no sink is installed, the worker runtime falls back to a line protocol:

```text
GRETL_WORKER|<LEVEL>|<message>
```

`ERROR` is written to stderr; every other level is written to stdout. The plugin
already contains a parser for this protocol. That parser is not needed for the
current in-process worker bridge, but it gives the later `processIsolation`
implementation a stable contract for remapping worker stdout/stderr back into
Gradle logging without changing the public task APIs.

In Docker, Gradle is run with `--console=plain`, so Gradle lifecycle/error output
is suitable for container logs and CI log collectors. Additional diagnostic
messages are visible with Gradle's usual `--info` or `--debug` flags.

## Docker Runtime Image

Docker packaging is deliberately kept at the root build level instead of adding
a third Gradle subproject. `stageRuntimeImage` builds the two plugin
publications into a file-based Maven repository and stages a Docker context
under `build/runtime-image/docker`.

The staged image contains:

- Java 17 runtime
- Gradle 7.6.4
- `/home/gradle/init.gradle`
- structured local Maven repository under `/opt/gretl/maven-repository`,
  including plugin markers, implementation metadata and the resolved runtime
  dependency closure
- `/usr/local/bin/gretl`, a small Gradle runner

The runner uses the bundled init script but is daemon-neutral:

```bash
gradle --init-script /home/gradle/init.gradle "$@"
```

Callers can choose `--no-daemon --offline` for one-shot or diagnostic runs. A
long-lived service container can keep the same `GRADLE_USER_HOME` and reuse a
compatible Gradle daemon across `docker exec` builds.

The init script configures plugin resolution so modern jobs can use the
`plugins {}` DSL without repeating plugin versions:

```groovy
plugins {
    id 'ch.so.agi.gretl'
    id 'ch.so.agi.gretl.geotools'
}
```

It does not inject arbitrary JARs into buildscript classpaths and does not use
`mavenLocal()` by default. Consumer repositories remain additive so additional
modern Gradle plugins can be resolved by the consumer project.

## Verification

The tests use Gradle TestKit:

- Core tasks run in temporary Gradle builds against small local fixtures.
- `SqlExecutor` and `Db2Db` are verified with SQLite in the fast test suite.
- `SqlExecutor` and `Db2Db` are also verified against PostgreSQL/PostGIS with
  Testcontainers in the separate `:gretl-core:integrationTest` suite.
- GeoTools tasks run through the classloader-isolated worker.
- Raster fixtures come from the existing `gretl-gt` experiments.
- Worker logging is verified both through the Gradle bridge and through the
  standalone stdout/stderr fallback.

The fast project-level check is:

```bash
./gradlew clean check
```

The Docker-backed database integration tests run separately:

```bash
./gradlew :gretl-core:integrationTest
```

The runtime image can be staged independently:

```bash
./gradlew stageRuntimeImage
```
