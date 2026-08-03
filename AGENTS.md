# AGENTS.md

## Repository Overview

This repository is a Gradle multi-project build for the modular GRETL runtime.
Use the checked-in Gradle wrapper and Java 17.

Main subprojects:

- `gretl-core`: core GRETL Gradle plugin and task APIs for SQL, DB transfer,
  gzip, XSLT and shared runtime utilities.
- `gretl-geotools`: GeoTools GRETL Gradle plugin. Heavy GeoTools processing code
  belongs in the `worker` source set and is invoked through Gradle worker
  isolation.
- `gretl-doclet`: custom Javadoc doclet for generating task DSL
  documentation.
- `gretl-control-common`: shared GRETL Control Plane manifest model, API DTOs
  and validation. Keep it free of server and worker implementation concerns.
- `gretl-control-server`: Spring Boot Control Plane server for API, UI,
  scheduling, persistence, secrets, run state, logs and notifications.
- `gretl-control-worker`: Spring Boot pull worker that claims runs from the
  server and starts external `gretl` processes.

The repository also contains `docker/` runtime-image assets, docs under
`docs/`, and example GRETL jobs under `docs/examples/`.

## Build And Test Commands

Use `./gradlew`, not a system Gradle installation.

Preferred checks:

- `./gradlew check`: default full project verification.
- `./gradlew :gretl-core:integrationTest`: PostgreSQL/PostGIS behavior only;
  this uses Testcontainers and is intentionally separate from the fast check.
- `./gradlew stageRuntimeImage`: stages the Docker runtime image context.
- `./gradlew buildRuntimeImage`: builds the local runtime image if Docker is
  available.
- `./gradlew :gretl-control-server:bootJar :gretl-control-worker:bootJar`:
  verify Control Plane executable jars.
- `./gradlew :gretl-control-server:bootRun`: run the Control Plane server
  locally.
- `./gradlew :gretl-control-worker:bootRun`: run a local worker.

For Spring Boot dependency upgrades, run:

```bash
./gradlew check :gretl-control-server:bootJar :gretl-control-worker:bootJar
```

## Module Rules

### `gretl-core`

- Keep the public GRETL task DSL stable unless the change explicitly requires a
  breaking migration.
- Do not add GeoTools, JAI/ImageIO, CRS/EPSG, raster or other heavy geospatial
  dependencies to this module. Legacy JTS remains only for INTERLIS and the
  existing lightweight Shapefile/GeoPackage compatibility code; do not expand
  that dependency boundary without an explicit design change.
- Keep passwords and secrets out of Gradle task inputs.
- Prefer focused unit and TestKit tests for task behavior.

### `gretl-geotools`

- Keep public task properties limited to Gradle and JDK types.
- Keep GeoTools-specific processing classes in `src/worker/java`.
- Do not import GeoTools APIs into lightweight plugin shell classes unless the
  isolation design is intentionally changed.
- Preserve the worker log protocol and Gradle log bridge behavior.

### `gretl-doclet`

- Keep this module narrowly focused on task DSL documentation extraction.
- Avoid coupling generated docs to implementation details that are not part of
  the job-facing API.

### `gretl-control-common`

- Put only shared API contracts, manifest types and validation here.
- Do not add Spring Boot server, persistence or worker process execution logic.
- Keep DTOs stable because both server and worker depend on them.

### `gretl-control-server`

- Keep durable job configuration sourced from `gretl-server.yml`; the UI must
  not edit Git manifests.
- Keep worker endpoints token-protected.
- Store server-managed secrets encrypted and pass them only to claimed runs.
- Keep run metadata in the database and full logs in the configured log
  directory.
- Use Quartz for scheduling and keep overlap behavior explicit.

### `gretl-control-worker`

- Workers pull work from the server; do not require server-to-worker network
  access.
- Start a fresh external `gretl` process per claimed run.
- Apply JVM resources through process environment and Gradle/JVM options.
- Stream stdout/stderr back to the server and poll for cancellation.
- Do not run GRETL jobs in Docker or Kubernetes from the worker unless a future
  design explicitly changes that boundary.

## Editing Rules

- Keep changes scoped to the requested behavior.
- Avoid unrelated refactors, formatting churn and metadata changes.
- Do not commit generated files, `build/`, `bin/`, `.gradle/`, IDE settings or
  local runtime outputs.
- Preserve ASCII in new files unless an existing file clearly uses another
  character set or the content requires non-ASCII text.
- Add comments only where they explain non-obvious behavior or constraints.
- Prefer existing project patterns over new abstractions.
- Keep documentation and examples aligned with actual commands and module
  names.

## Dependency Guidance

- The Gradle wrapper is pinned to Gradle 7.6.4. Do not upgrade it as part of a
  normal dependency change unless explicitly requested.
- Java compilation targets Java 17 through Gradle toolchains.
- Spring Boot is used by the Control Plane modules only.
- Non-Spring GRETL modules do not import the Spring Boot dependency BOM; avoid
  adding unversioned dependencies there.
- For Spring Boot 3.5.x with Gradle 7.6.4, keep
  `org.junit.platform:junit-platform-launcher` on the Control Plane test runtime
  classpaths to avoid JUnit Platform launcher/engine skew.

## Operational Notes

- The Control Plane server reads `gretl-server.yml` from the repository root by
  default.
- Worker authentication uses a bearer token configured through
  `GRETL_CONTROL_WORKER_TOKEN`.
- Server-managed secret encryption uses `GRETL_CONTROL_SECRET_KEY`; do not rely
  on the development fallback key in production.
- A worker needs access to the GRETL job monorepo and a runnable `gretl`
  executable.
- Manifest parameters are passed as Gradle properties and `GRETL_PARAM_*`
  environment variables.
- Secrets are passed to workers as `GRETL_SECRET_*` environment variables for
  the claimed run only.

## Before Finishing Work

- Run the narrowest meaningful test first.
- Run `./gradlew check` before handing off broad changes.
- For Control Plane packaging or Spring Boot dependency changes, also run
  `./gradlew :gretl-control-server:bootJar :gretl-control-worker:bootJar`.
- Mention any skipped checks and why they were skipped.
