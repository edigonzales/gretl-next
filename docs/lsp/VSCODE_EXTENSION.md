# GRETL VS Code Extension Documentation

This document describes the architecture, lifecycle, and integration of the
GRETL VS Code extension with the Java Language Server.

## Architecture

```
VS Code IDE
  |
  +-- Extension Host
        |
        +-- TypeScript Extension (gretl-vscode)
        |     src/extension.ts        activate/deactivate
        |     src/languageServer.ts   LanguageClient wrapper
        |     src/commands.ts         Custom command handlers
        |     src/config.ts           Settings reader
        |     src/logging.ts          Output channel
        |
        +-- vscode-languageclient (LSP client)
              |
              +-- stdin/stdout / JSON-RPC
                    |
                    +-- Java Process (gretl-lsp-all.jar)
                          LSP4J LanguageServer
                          GretlLanguageServer
                          GretlTextDocumentService
                          CompletionProvider, HoverProvider, ...
```

The extension is intentionally thin. It starts a single Java process that
communicates via LSP over stdio. All GRETL-specific logic lives in the Java
LSP server (`gretl-lsp`).

## Lifecycle

### Activation

The extension activates when:

1. A workspace contains `build.gradle` or `*.gradle` files (`workspaceContains`).
2. A GRETL command is invoked manually (`onCommand`).

On activation:

1. An output channel `GRETL` is created.
2. Config is read from `gretl.*` settings.
3. The Java LSP process is started: `<java> -jar <serverJar> --stdio`.
4. A `LanguageClient` is created with document selector `{ language: 'groovy', pattern: '**/*.gradle' }`.
5. Commands are registered.
6. File watchers are set up for `**/*.{gradle,sql,properties}`.

### Deactivation

On deactivation the language client is stopped. The Java process exits.

## Settings

All settings are prefixed with `gretl.`:

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `java.path` | `string` | `""` | Path to Java executable. Empty uses `java` from PATH. |
| `server.jarPath` | `string` | `""` | Path to `gretl-lsp-all.jar`. Empty uses bundled `server/gretl-lsp-all.jar`. |
| `server.jvmArgs` | `string[]` | `[]` | Extra JVM args. |
| `trace.server` | `enum` | `"off"` | LSP trace: `off`, `messages`, `verbose`. |

## Commands

| Command ID | Title | Description |
|-----------|-------|-------------|
| `gretl.restartLanguageServer` | Restart Language Server | Stops and restarts the LSP process. |
| `gretl.showLanguageServerLogs` | Show Language Server Logs | Opens the output channel. |
| `gretl.openOverview` | Open GRETL Overview | Placeholder for future job-graph webview (Phase 8). |

## File Watchers

The extension watches changes to:

- `**/*.gradle` — triggers re-analysis
- `**/*.sql` — triggers SQL parameter re-analysis
- `**/*.properties` — triggers project property re-evaluation

## Development Workflow

### F5 Debug

The `.vscode/launch.json` configures a debug session that:

1. Runs `npm run build` as a pre-launch task.
2. Starts an Extension Development Host with the extension loaded.
3. Maps breakpoints in `src/` to compiled output in `dist/`.

### Adding a Command

1. Add command contribution to `package.json` (`contributes.commands`).
2. If command goes to the LSP, implement `workspace/executeCommand` in
   `GretlWorkspaceService`.
3. Register the command handler in `src/commands.ts`.

### Adding a Setting

1. Add the property to `package.json` (`contributes.configuration.properties`).
2. Add the field to `GretlConfig` in `src/config.ts`.
3. Read it in `loadConfig()`.

## Testing

### TypeScript Tests

```bash
cd vscode/gretl-vscode
npm test
```

Tests are plain JavaScript (`node:test`) covering pure resolver functions:
`resolveJavaCommand()` and `resolveServerJar()`.

### Manual Integration Tests

1. `./gradlew copyDevGretlServerJar`
2. `cd vscode/gretl-vscode && npm install && npm run build`
3. Press F5 in VS Code.
4. In the Extension Development Host, open a GRETL project with `build.gradle`.
5. Verify completions, diagnostics, and hover work.

## Troubleshooting

### Server JAR not found

Build and copy the JAR:
```bash
./gradlew copyDevGretlServerJar
```

### Java not found

Check `gretl.java.path` setting or ensure `java` is on PATH and is Java 17.
```bash
java -version  # should show 17.x
```

### Extension not activating

Check the VS Code Developer Tools console (`Help > Toggle Developer Tools` in
the Extension Development Host). Activation errors appear there.

### Language server stderr

The Java LSP writes all logs to stderr. In the Extension Development Host,
stderr from the language server process is captured. Set `gretl.trace.server`
to `messages` or `verbose` to see LSP protocol messages in the output channel.
