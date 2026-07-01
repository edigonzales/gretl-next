# GRETL LSP Phase Status

| Phase | Titel | Status | Start | Ende | Commit | Artefakt | Tests | Doku | Bemerkungen |
|------:|-------|--------|-------|------|--------|----------|-------|------|-------------|
| 0 | Arbeitsgrundlage und Projektgerüst | done | 2026-07-01 | 2026-07-01 | - | gretl-lsp Minimalsubprojekt | :gretl-lsp:test passed | docs/lsp/ erstellt | |
| 1 | Doclet-Metadatenformat v1 | implemented | 2026-07-01 | 2026-07-01 | - | gretl-lsp-metadata.json + JSON Schema | :gretl-doclet:test (18 passed) | docs/lsp/METADATA_FORMAT.md | 45 tasks, SqlExecutor verified |
| 2 | LSP-Projekt, Metadaten-Loader und Server-Skelett | done | 2026-07-01 | 2026-07-01 | - | gretl-lsp-all.jar (fat JAR) | :gretl-lsp:test (31 passed) | docs/lsp/DEVELOPMENT.md, docs/lsp/TESTING.md | LSP4J server with initialize, 45 tasks loaded from embedded metadata, stderr logging |
| 3 | Groovy-AST-Parser und GRETL-Zwischenmodell | done | 2026-07-01 | 2026-07-01 | - | GretlScript, GretlTaskBlock, GroovyAstGretlParser, LenientGretlScanner, HybridGretlScriptParser | :gretl-lsp:test (62 passed, 0 failed) | docs/lsp/DECISIONS.md, docs/lsp/TESTING.md | |
| 4 | Diagnostics v1 | done | 2026-07-01 | 2026-07-01 | - | GretlAnalyzer, 8 diagnostic rules, DocumentStore, TextDocument, diagnostics publishing at didOpen/didChange/didClose | :gretl-lsp:test (105 passed, 0 failed) | docs/lsp/README.md | 8 rules: MissingRequiredProperty, UnknownProperty, WrongArgumentCount, UnknownTaskType, UnknownDependency, DefaultTask, DuplicateTaskName, LegacyDsl |
| 5 | Completion, Hover, Signature Help | done | 2026-07-01 | 2026-07-01 | - | CompletionProvider, HoverProvider, SignatureHelpProvider, CompletionContextDetector | :gretl-lsp:test (144 passed, 0 failed) | docs/lsp/README.md, docs/lsp/TESTING.md | Context-dependent completion (task-type, body, dependency), hover (task, property), signature help with active parameter. Data from gretl-lsp-metadata.json, no hardcoded task lists. |
| 6 | Document Symbols, Links und SQL-Parameteranalyse | done | 2026-07-01 | 2026-07-01 | - | DocumentSymbolProvider, DocumentLinkProvider, SqlParameterExtractor, SqlParameterRule | :gretl-lsp:test (170 passed, 0 failed) | docs/lsp/PHASE_STATUS.md | 9 diagnostic rules, workspace root support, file reference extraction, no database connections |
| 7 | VS-Code-Extension v1 mit F5-Workflow | done | 2026-07-01 | 2026-07-01 | - | vscode/gretl-vscode/ Extension mit TypeScript-Client | npm test (9 passed) | docs/lsp/VSCODE_EXTENSION.md, vscode/gretl-vscode/README.md | 4 Settings, 3 Commands, F5-Workflow, copyDevGretlServerJar |
| 8 | GRETL Overview / Job-Graph-Webview | not-started | | | | | | | |
| 9 | Quick Fixes und DSL-Migration | not-started | | | | | | | |
| 10 | GitHub Actions CI und VSIX Packaging | not-started | | | | | | | |
| 11 | Stabilisierung, Dokumentation und Release-Kandidaten | not-started | | | | | | | |
