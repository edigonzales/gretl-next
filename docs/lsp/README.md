# GRETL Language Server (LSP)

A Language Server Protocol implementation for GRETL Gradle-Groovy build scripts.
The LSP provides semantic understanding of GRETL task blocks, diagnostics,
completion, hover, and more within VS Code.

## Project Status

See [PHASE_STATUS.md](PHASE_STATUS.md) for implementation progress.

## Current Features (Phase 9)

### Code Actions (Quick Fixes)

Quick fix code actions are provided for the most common GRETL diagnostics.

| Diagnostic | Code Action | Description |
|-----------|-------------|-------------|
| GRETL1001 Missing required property | "Fuge `<prop>` hinzu" | Inserts the missing property at the end of the task body using its modern DSL form |
| GRETL1002 Unknown property | "Korrigiere `<old>` zu `<new>`" | Replaces the wrong property name with the closest Levenshtein suggestion |
| GRETL1101 Unknown dependency | "Korrigiere `<old>` zu `<new>`" | Replaces the wrong dependency task name with the closest match |
| GRETL1201 Legacy DSL | "Migriere zu moderner DSL-Schreibweise" | Converts `prop = [a, b, c]` to `prop a, b, c` and other assignment forms to method-call |

**Not covered by quick fixes in MVP:**
- GRETL1003 (wrong argument count) -- ambiguous fix, requires developer judgement
- GRETL1102/1103 (unknown default task / duplicate task name)
- GRETL1301/1302 (SQL parameter issues)

### Completion

Context-dependent completion triggered at any position. Three completion
contexts are detected automatically.

| Context | Example | Result |
|---------|---------|--------|
| Task type | `tasks.register('x', \|)` | All GRETL task types from metadata, sorted alphabetically |
| Task body | `tasks.register('x', SqlExecutor) { \| }` | Missing properties first (required), then optional, deprecated last. Includes snippet insert text. |
| Dependency | `dependsOn '\|'` | All task names defined in the current script |

### Hover

Hover over GRETL symbols shows documentation from metadata.

| Hover target | Content |
|-------------|---------|
| Task type name | Description, qualified class name, category, status, required fields |
| DSL property name | Type, required flag, description, non-legacy signature as code block, deprecated/sqlParameterProvider info |

### Signature Help

Signature help guides argument typing for multi-argument DSL calls.

| Call | Signature | Active parameter |
|------|-----------|-----------------|
| `database dbUri, \|` | `database url, user, password` | 1 (user) |
| `sqlFiles files(\|` | `sqlFiles files('...')` | 0 |

Active parameter detection is line-based by counting commas before the cursor.

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
