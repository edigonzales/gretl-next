---
name: gretl-geotools-worker-change
description: Use when changing GRETL GeoTools tasks, worker runtime code, classloader or process isolation, public task properties, worker logging, GeoTools dependencies, or geospatial module boundaries.
---

# GRETL GeoTools Worker Change

## When To Use

Use this skill for changes in `gretl-geotools`, GeoTools-backed task behavior, worker isolation, raster/vector processing, worker logging, or dependency boundaries between `gretl-core` and `gretl-geotools`.

## Sources Of Truth

- `AGENTS.md` for module rules and required checks.
- `docs/architecture.md` for GeoTools worker isolation, logging protocol, and runtime image expectations.
- `gretl-geotools/src/main/java` for lightweight Gradle plugin shells.
- `gretl-geotools/src/worker/java` for GeoTools-heavy processing.

## Workflow

1. Classify the change as public task API, worker runtime, dependency, logging, or documentation.
2. Read the task shell, worker action, operation request, worker engine, and nearest tests before editing.
3. Keep public task properties limited to Gradle and JDK types.
4. Keep GeoTools-specific processing inside the worker source set.
5. Preserve the worker log bridge and `GRETL_WORKER|<LEVEL>|<message>` fallback protocol.

## Guardrails

- Do not import GeoTools APIs into lightweight plugin shell classes unless the isolation design is intentionally changed.
- Do not add GeoTools, JTS, ImageIO, CRS/EPSG, raster, or similar heavy geospatial dependencies to `gretl-core`.
- Keep the task API stable unless the user explicitly asks for a breaking migration.
- Stop before changing `classLoaderIsolation` to another isolation model unless the design and compatibility impact are documented.

## Verification

- Narrow: run the affected GeoTools task or worker tests first.
- Broad: run `./gradlew :gretl-geotools:check`.
- Run `./gradlew check` when shared module boundaries, `gretl-core`, runtime packaging, or dependency resolution changed.

## Final Output

Report public API impact, worker/runtime impact, dependency boundary impact, log protocol impact, tests run, skipped checks, and remaining risk.
