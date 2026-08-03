# GRETL Modular Documentation

GRETL supports and tests Groovy Gradle builds with `plugins { ... }`. The old
public `Step`, `Connector` and `TransferSet` implementation APIs are not
supported extension points.

## Start here

- Write jobs with the examples in the [README](../README.md) and the generated
  [task reference](reference/reference.adoc).
- Migrate existing jobs with [Migration from original GRETL](migration-from-gretl.md).
- Understand module and worker boundaries in [Architecture](architecture.md).
- Select local and CI gates from the [central testing guide](testing/testing.adoc).
- Diagnose the Docker distribution with
  [Runtime-image testing](testing/runtime-image-tests.adoc).
- Build federated DuckDB jobs with
  [DuckDbSqlExecutor](duckdb-sql-executor.md).
- Operate scheduled jobs with the [GRETL Control Plane](control-plane.md).
- Develop editor support with the documents under [LSP](lsp/).
- Review the active editor architecture in the
  [LSP and VS Code design specification](design/gretl-lsp-agent-spec.md).

The generated task reference is the source of truth for public task properties.
The persistent projects below `test-jobs/` are the source of truth for the
canonical consumer-job catalog; manual copies of either inventory are avoided.
