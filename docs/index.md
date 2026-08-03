# GRETL Modular Documentation

This documentation describes the new modular GRETL DSL. It does not document
the old public Step, Connector or TransferSet APIs as supported extension
points.

GRETL supports and tests Groovy Gradle builds. Kotlin DSL builds may still work
through Gradle, but are not tested or supported as a GRETL contract.

## Start Here

- Writing jobs: use the compact examples in the [README](../README.md) and the
  task overview in the [Task reference](reference/reference.adoc).
- Federated DuckDB SQL jobs: read
  [DuckDbSqlExecutor](duckdb-sql-executor.md).
- Migrating old jobs: read [Migration from original GRETL](migration-from-gretl.md).
- Understanding the plugin split: read [Architecture](architecture.md).
- Testing local changes: run `./gradlew clean check` first; run
  `./gradlew :gretl-core:integrationTest` when PostgreSQL/PostGIS behavior is
  relevant.
- Operating jobs: use the lightweight [GRETL Control Plane](control-plane.md)
  server and pull worker when Jenkins-style scheduling and run history are
  needed without replacing the GRETL Gradle runtime.

## Planned Documentation Areas

The current documentation is intentionally small and structured so more detail
can be added without changing the entry points.

- Task reference: complete properties, validation errors and examples per task
  in [reference/reference.adoc](reference/reference.adoc).
- Migration guide: more original GRETL job patterns and equivalent modular DSL.
- Testing guide: fixture strategy, TestKit conventions and Testcontainers setup.
- CI guide: fast checks, Docker-backed integration tests and runtime image build.
- Troubleshooting: common SQL, JDBC, XSLT, GeoTools and worker-isolation errors.
- Best practices: parameterization, transaction boundaries, file layout and
  reproducible job builds.
