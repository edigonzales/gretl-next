# GRETL Modular Documentation

This documentation describes the new modular GRETL DSL. It does not document
the old public Step, Connector or TransferSet APIs as supported extension
points.

## Start Here

- Writing jobs: use the compact examples in the [README](../README.md) and the
  task overview in [Task reference](task-reference.md).
- Federated DuckDB SQL jobs: read
  [DuckDbSqlExecutor](duckdb-sql-executor.md).
- Migrating old jobs: read [Migration from original GRETL](migration-from-gretl.md).
- Writing Kotlin builds: use the complete examples in [Kotlin DSL examples](kotlin-dsl.md).
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

- Task reference: complete properties, validation errors and examples per task.
- Migration guide: more original GRETL job patterns and equivalent modular DSL.
- Testing guide: fixture strategy, TestKit conventions and Testcontainers setup.
- CI guide: fast checks, Docker-backed integration tests and runtime image build.
- Troubleshooting: common SQL, JDBC, XSLT, GeoTools and worker-isolation errors.
- Best practices: parameterization, transaction boundaries, file layout and
  reproducible job builds.
