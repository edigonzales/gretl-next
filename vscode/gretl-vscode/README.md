# GRETL VS Code Extension

A VS Code extension that provides GRETL language support for Gradle Groovy
build scripts. It starts the [GRETL Language Server](../gretl-lsp/) via
`vscode-languageclient` and delivers completion, diagnostics, hover, and more.

## Prerequisites

- **Java 17** — the language server runs on Java 17
- **Node.js 20+** and **npm** — for building the TypeScript extension
- **VS Code 1.85+** — for the extension host APIs used
- **A GRETL monorepo checkout** — to build the language server JAR

## Getting Started

### 1. Build the Language Server JAR

From the repository root, build the fat JAR and stage it into the extension:

```bash
./gradlew copyDevGretlServerJar
```

This runs `:gretl-lsp:shadowJar` and copies the resulting JAR to
`vscode/gretl-vscode/server/gretl-lsp-all.jar`.

### 2. Install Dependencies

```bash
cd vscode/gretl-vscode
npm install
```

### 3. Build the Extension

```bash
npm run build
```

TypeScript compiles to `dist/`.

### 4. Open in VS Code and Press F5

```bash
code .
```

Then press **F5** (or select `Run > Start Debugging`). This launches an
**Extension Development Host** window with the GRETL extension loaded.

The pre-configured launch task (`npm: build`) runs automatically before each
F5 session, so you can edit TypeScript and press F5 again without a separate
build step.

### 5. Open a GRETL Project

In the Extension Development Host:

- Open a folder containing `build.gradle` files (e.g. a GRETL job).
- Open any `build.gradle` or `*.gradle` file.
- The GRETL language server starts automatically. You should see:
  - `GRETL` in the output channel dropdown
  - Diagnostics, completions, and hover triggered as you edit

## Features

### Completions

Context-dependent completions for task types, DSL properties, and
dependency task names. Type as you normally would and accept suggestions
with `Enter` or `Tab`.

### Diagnostics

Real-time validation of GRETL build scripts:
- Missing required properties
- Unknown/tpyo-d properties (with Levenshtein suggestions)
- Wrong argument counts
- Unknown task types and dependencies
- Legacy DSL style detection
- SQL parameter consistency

### Quick Fixes

Press `Ctrl+.` (or `Cmd+.` on macOS) on any diagnostic to see available
quick fix actions:

| Diagnostic | Quick Fix |
|-----------|-----------|
| Missing required property | Inserts the property at the end of the task body |
| Unknown property / dependency | Replaces typo with closest suggestion |
| Legacy DSL | Converts assignment style to modern method-call DSL |

### Hover & Signature Help

Hover over task types and properties for documentation. Signature help
guides argument typing for multi-argument DSL calls.

### GRETL Overview

See [GRETL Overview](#gretl-overview) below.

## Commands

Open the command palette (`Cmd+Shift+P`) and type `GRETL`:

| Command | Action |
|---------|--------|
| `GRETL: Restart Language Server` | Stops and restarts the Java LSP process |
| `GRETL: Show Language Server Logs` | Opens the GRETL output channel |
| `GRETL: Open GRETL Overview` | Opens a read-only webview with job-graph, tasks, diagnostics, SQL files, and SQL parameters |
| `GRETL: Refresh GRETL Overview` | Re-fetches overview data from the language server |

## GRETL Overview

The `GRETL: Open GRETL Overview` command opens a read-only side panel that
shows a complete overview of the current `build.gradle` job:

- **Pipeline** with task dependencies
- **Tasks** with type and required property status
- **Diagnostics** grouped by task
- **SQL Files** and **SQL Parameters**

Click on any task name to navigate to its definition in the editor.
The overview is refreshed automatically on re-open and can be refreshed
manually with `GRETL: Refresh GRETL Overview`.

## Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `gretl.java.path` | `string` | `""` | Path to Java executable. Empty uses `java` from PATH. |
| `gretl.server.jarPath` | `string` | `""` | Path to `gretl-lsp-all.jar`. Empty uses bundled `server/gretl-lsp-all.jar`. |
| `gretl.server.jvmArgs` | `string[]` | `[]` | Extra JVM args (e.g. `-Xmx512m`). |
| `gretl.trace.server` | `enum` | `"off"` | LSP trace verbosity: `off`, `messages`, `verbose`. |

## Layout

```
vscode/gretl-vscode/
  package.json        Extension manifest
  tsconfig.json       TypeScript build config
  src/
    extension.ts      activate / deactivate
    languageServer.ts LanguageClient controller
    commands.ts       Command registrations
    config.ts         Configuration reader
    logging.ts        Output channel factory
  server/
    gretl-lsp-all.jar Language server fat JAR (staged by copyDevGretlServerJar)
  test/
    config.test.js    Pure resolver tests
  .vscode/
    launch.json       F5 debug configurations
    tasks.json         Build and test tasks
```

## Troubleshooting

**Server JAR not found:**
If you see `WARNING: Server JAR not found` in the output channel, run
`./gradlew copyDevGretlServerJar` from the repo root.

**Language server fails to start:**
Check the GRETL output channel for error messages. Common causes:
- Java not on PATH or wrong version (needs Java 17).
- Missing `gretl-lsp-all.jar`.
- Another Gradle process locking the build.

**No completions appear:**
Ensure the language server started (`GRETL language server started` in the
output channel) and that the file is a `*.gradle` file.
