# GRETL LSP Phase Status

| Phase | Titel | Status | Start | Ende | Commit | Artefakt | Tests | Doku | Bemerkungen |
|------:|-------|--------|-------|------|--------|----------|-------|------|-------------|
| 0 | Arbeitsgrundlage und Projektgerüst | done | 2026-07-01 | 2026-07-01 | - | gretl-lsp Minimalsubprojekt | :gretl-lsp:test passed | docs/lsp/ erstellt | |
| 1 | Doclet-Metadatenformat v1 | implemented | 2026-07-01 | 2026-07-01 | - | gretl-lsp-metadata.json + JSON Schema | :gretl-doclet:test (18 passed) | docs/lsp/METADATA_FORMAT.md | 45 tasks, SqlExecutor verified |
| 2 | LSP-Projekt, Metadaten-Loader und Server-Skelett | done | 2026-07-01 | 2026-07-01 | - | gretl-lsp-all.jar (fat JAR) | :gretl-lsp:test (31 passed) | docs/lsp/DEVELOPMENT.md, docs/lsp/TESTING.md | LSP4J server with initialize, 45 tasks loaded from embedded metadata, stderr logging |
| 3 | Groovy-AST-Parser und GRETL-Zwischenmodell | done | 2026-07-01 | 2026-07-01 | - | GretlScript, GretlTaskBlock, GroovyAstGretlParser, LenientGretlScanner, HybridGretlScriptParser | :gretl-lsp:test (62 passed, 0 failed) | docs/lsp/DECISIONS.md, docs/lsp/TESTING.md | |
| 4 | Diagnostics v1 | done | 2026-07-01 | 2026-07-01 | - | GretlAnalyzer, 8 diagnostic rules, DocumentStore, TextDocument, diagnostics publishing at didOpen/didChange/didClose | :gretl-lsp:test (105 passed, 0 failed) | docs/lsp/README.md | 8 rules: MissingRequiredProperty, UnknownProperty, WrongArgumentCount, UnknownTaskType, UnknownDependency, DefaultTask, DuplicateTaskName, LegacyDsl |
| 5 | Completion, Hover, Signature Help | not-started | | | | | | | |
| 6 | Document Symbols, Links und SQL-Parameteranalyse | not-started | | | | | | | |
| 7 | VS-Code-Extension v1 mit F5-Workflow | not-started | | | | | | | |
| 8 | GRETL Overview / Job-Graph-Webview | not-started | | | | | | | |
| 9 | Quick Fixes und DSL-Migration | not-started | | | | | | | |
| 10 | GitHub Actions CI und VSIX Packaging | not-started | | | | | | | |
| 11 | Stabilisierung, Dokumentation und Release-Kandidaten | not-started | | | | | | | |
