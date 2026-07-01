# GRETL Language Server (LSP)

A Language Server Protocol implementation for GRETL Gradle-Groovy build scripts.
The LSP provides semantic understanding of GRETL task blocks, diagnostics,
completion, hover, and more within VS Code.

## Project Status

See [PHASE_STATUS.md](PHASE_STATUS.md) for implementation progress.

## Current Features (Phase 4)

### Diagnostics

Diagnostics are published on `didOpen` and `didChange`, and cleared on `didClose`.

| Code | Severity | Rule | Description |
|------|----------|------|-------------|
| GRETL1001 | Error | MissingRequiredProperty | Pflichtparameter laut Metadaten fehlt im Task-Body |
| GRETL1002 | Warning | UnknownProperty | DSL-Call entspricht keiner bekannten Property (Levenshtein-Suggestion) |
| GRETL1003 | Warning | WrongArgumentCount | METHOD_CALL Argumentanzahl weicht von AcceptedForm ab |
| GRETL1004 | Warning | UnknownTaskType | Task-Typ nicht im GRETL-Metadatenmanifest |
| GRETL1101 | Warning | UnknownDependency | dependsOn/finalizedBy verweist auf unbekannten Task |
| GRETL1102 | Warning | DefaultTask | defaultTasks verweist auf unbekannten Task |
| GRETL1103 | Warning | DuplicateTaskName | Task-Name ist mehrfach definiert |
| GRETL1201 | Information | LegacyDsl | Alte GRETL-DSL-Schreibweise (Assignment) mit moderner Alternative |

## Goals

- Completion for GRETL task types and DSL properties
- Diagnostics for missing required fields, unknown properties, and dependency problems
- Hover documentation sourced from gretl-doclet metadata
- Signature help for method-style DSL
- Document symbols, links, and SQL parameter analysis
- Lightweight VS Code extension that starts the Java LSP server

## Architecture

```
gretl-doclet          -- generates gretl-lsp-metadata.json (source of truth)
    |
gretl-lsp             -- Java LSP server (LSP4J)
    |
vscode/gretl-vscode   -- TypeScript VS Code extension (thin client)
```

## Development

See [DEVELOPMENT.md](DEVELOPMENT.md).
