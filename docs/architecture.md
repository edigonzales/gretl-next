# GRETL Modular Architecture

## Module boundary

GRETL uses a pragmatic two-plugin runtime:

- `gretl-core` owns the general task DSL, database and file processing,
  INTERLIS integrations and shared lightweight runtime utilities.
- `gretl-geotools` owns processing that requires GeoTools, JAI/ImageIO, CRS
  registries or raster/vector coverage APIs.

The boundary is dependency-driven rather than a task-count target. Core does
not load the GeoTools runtime. It does retain legacy `com.vividsolutions.jts`
types brought by INTERLIS and used by lightweight Shapefile/GeoPackage
compatibility code. Moving those tasks alone would not remove JTS from Core, so
that compatibility dependency remains explicit instead of creating another
plugin solely for architectural purity.

Both plugins can be applied independently, in either order, and in mixed
multi-project builds. Public task properties use Gradle and JDK types; heavy
GeoTools types never appear in the job-facing DSL.

## Coordination and isolation

Core tasks execute directly in Gradle. INTERLIS tasks use
`InterlisBuildService` with `maxParallelUsages = 1` because their libraries use
JVM-global state. General Core tasks have no shared service because they need no
shared lifecycle or concurrency limit.

GeoTools tasks use `GeoToolsBuildService`, also limited to one parallel usage,
and submit work through Gradle's Worker API. A BuildService coordinates access;
it is not a classloader boundary.

## GeoTools worker runtime

`gretl-geotools` has a lightweight `main` source set and a GeoTools-heavy
`worker` source set. The build packages worker classes and their dependency
closure under `gretl-geotools-worker-classpath/` in the plugin JAR.

The task shell submits `GeoToolsWorkerAction` with `classLoaderIsolation`.
The action contains no GeoTools imports and invokes the worker runtime only
after Gradle has created the isolated classloader. JVM-wide GeoTools and
ImageIO registries remain shared; the worker refreshes providers at the start
of execution, while the BuildService prevents concurrent registry mutation.

Process isolation is deliberately not used today. It remains an implementation
option if native libraries, registry conflicts or daemon memory become a
measured problem; changing isolation is not required by the public task API.

## Logging boundary

Core logging is bridged directly to Gradle. The GeoTools worker uses the levels
`LIFECYCLE`, `INFO`, `DEBUG` and `ERROR` through a thread-local sink. Without a
sink it emits the stable fallback protocol:

```text
GRETL_WORKER|<LEVEL>|<message>
```

This keeps worker code free of Gradle logging APIs and preserves a path to
future process isolation.

## Documentation generation

Public task classes are annotated once. `gretl-doclet` collects those classes,
extracts inherited DSL methods into a shared descriptor model, and renders both
the AsciiDoc task reference and LSP metadata. Golden tests cover formatting,
ordering, escaping and the metadata schema. `verifyTaskDocs` prevents committed
reference output from drifting from task sources.
German `de_CH` descriptions are based on original GRETL Javadocs where
available and otherwise translated from the authoritative English annotation.

## Test architecture

Verification is intentionally layered:

- Module tests exercise engines, validation and source-classpath TestKit builds.
- `gretl-combined-tests` proves both plugins in one realistic consumer build,
  including plugin order, classloader separation, inferred task dependencies,
  incremental behavior, configuration cache and multi-project execution.
- Published-artifact tests resolve real plugin markers, POMs and transitive
  dependencies from an isolated Maven repository.
- Persistent consumer projects under `test-jobs/` run through source,
  published-artifact, runtime-image one-shot and runtime-image service backends.
- Runtime-image contract and dependency-closure tests prove local-only Gradle
  resolution with a fresh user home and the immutable image ID.

The internal test projects and job catalog are guarded against publication and
runtime-image staging. Detailed commands and trace semantics live in the
[central testing guide](testing/testing.adoc).

## Runtime image

The root build stages a structured Maven repository containing plugin markers,
implementation artifacts and their exact runtime dependency closure. The image
contains Java 17, Gradle 7.6.4, the `gretl` launcher, GeoTools worker runtime and
offline DuckDB extensions.

The launcher supplies `--offline` in both one-shot and service mode. That policy
prevents remote Gradle dependency downloads but does not disable application
network access. Long-lived service containers reuse a compatible Gradle daemon
and image-owned `GRADLE_USER_HOME`; one-shot execution uses an isolated home.

Runtime-image tests build from the current checkout, record the immutable image
ID and use `--pull=never` for all executions. Dependency manifests and checksums
make staged contents auditable independently from Docker execution.
