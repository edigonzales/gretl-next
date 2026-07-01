# GRETL LSP Development Guide

## Prerequisites

- Java 17 (Gradle 7.6.4 does not support newer JDKs)
- Checked-in Gradle wrapper (`./gradlew`)

Set `JAVA_HOME` to a JDK 17 installation, e.g.:

```bash
export JAVA_HOME=/path/to/jdk-17
```

## Building the Language Server

### Compile and test

```bash
./gradlew :gretl-lsp:test
```

This compiles all sources, generates LSP metadata from the doclet, and runs the
test suite (unit tests for metadata loading, argument parsing, and server
capabilities).

### Build fat JAR

```bash
./gradlew :gretl-lsp:shadowJar
```

Output: `gretl-lsp/build/libs/gretl-lsp-5.0.0-SNAPSHOT-all.jar`

The fat JAR bundles LSP4J, Jackson, and all dependencies. It also embeds the
pre-generated `gretl-lsp-metadata.json` as a classpath resource.

### Regenerate metadata

If GRETL task classes have changed, regenerate the metadata:

```bash
./gradlew :gretl-doclet:generateLspMetadata
```

This runs the doclet with the `-lspoutput` option to produce
`gretl-doclet/build/generated/gretl-lsp-metadata/gretl-lsp-metadata.json`.
The LSP build copies this into its own resources automatically.

## Running the Language Server

### Help

```bash
java -jar gretl-lsp/build/libs/gretl-lsp-5.0.0-SNAPSHOT-all.jar --help
```

### STDIO mode (for editor integration)

```bash
java -jar gretl-lsp/build/libs/gretl-lsp-5.0.0-SNAPSHOT-all.jar --stdio
```

The server reads JSON-RPC messages from stdin and writes responses to stdout.
**All server logs go to stderr** -- never stdout, which is reserved for the
LSP protocol.

### With custom metadata path

```bash
java -jar gretl-lsp/build/libs/gretl-lsp-5.0.0-SNAPSHOT-all.jar --stdio --metadata=/path/to/gretl-lsp-metadata.json
```

### With debug logging

```bash
java -jar gretl-lsp/build/libs/gretl-lsp-5.0.0-SNAPSHOT-all.jar --stdio --log-level=DEBUG --trace
```

## Capabilities (Phase 2)

The server currently responds to `initialize` with:

| Capability | Value |
|------------|-------|
| Text document sync | Full |
| Hover provider | true |
| Document symbol provider | true |

Completion, signature help, and document links are reserved for later phases.

## Project Structure

```
gretl-lsp/
  src/
    main/java/ch/so/agi/gretl/lsp/
      metadata/         -- Metadata model, loader, and validator
      server/           -- LSP server implementation
    main/resources/     -- Classpath resources (metadata JSON copied here)
    test/java/          -- Unit tests
    test/resources/     -- Test fixtures
```

## Package Overview

### `ch.so.agi.gretl.lsp.metadata`

| Class | Purpose |
|-------|---------|
| `GretlMetadata` | Top-level metadata container |
| `TaskMetadata` | Single GRETL task descriptor |
| `PropertyMetadata` | DSL property/method descriptor |
| `AcceptedForm` | Syntactic form (method-call, assignment) |
| `MetadataLoader` | Jackson-based JSON deserializer |
| `MetadataValidator` | Structural metadata validation |

### `ch.so.agi.gretl.lsp.server`

| Class | Purpose |
|-------|---------|
| `GretlServerLauncher` | Main entry point, argument parsing, LSP launch |
| `GretlLanguageServer` | `LanguageServer` implementation |
| `GretlTextDocumentService` | Stub `TextDocumentService` |
| `GretlWorkspaceService` | Stub `WorkspaceService` |
| `GretlServerConfig` | Parsed CLI configuration |
| `ServerLogger` | Stderr-only logging |
| `ServerLifecycle` | Server lifecycle state tracking |
