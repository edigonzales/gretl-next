# GRETL Language Server (LSP)

A Language Server Protocol implementation for GRETL Gradle-Groovy build scripts.
The LSP provides semantic understanding of GRETL task blocks, diagnostics,
completion, hover, and more within VS Code.

## Project Status

See [PHASE_STATUS.md](PHASE_STATUS.md) for implementation progress.

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
