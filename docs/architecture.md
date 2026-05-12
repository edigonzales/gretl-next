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

## Verification

The tests use Gradle TestKit:

- Core tasks run in temporary Gradle builds against small local fixtures.
- SQL and Db2Db are verified with SQLite.
- GeoTools tasks run through the classloader-isolated worker.
- Raster fixtures come from the existing `gretl-gt` experiments.

The expected project-level check is:

```bash
./gradlew clean check
```
