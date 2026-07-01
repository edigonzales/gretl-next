# GRETL LSP Testing Guide

## Running Tests

All LSP tests live in the `gretl-lsp` subproject.

```bash
./gradlew :gretl-lsp:test
```

## Test Structure

### Metadata Tests

`ch.so.agi.gretl.lsp.metadata.MetadataLoaderTest`

Tests the Jackson-based metadata loader with a hand-crafted test fixture
(2 tasks: `SqlExecutor` and `DuckDbSqlExecutor`).

| Test | What it checks |
|------|----------------|
| Loads valid metadata | Schema version, gretlVersion, task count |
| Finds SqlExecutor | Name, qualified class name, category, status |
| Required properties | `database` and `sqlFiles` are required |
| sqlParameterProvider | `sqlParameters` has `sqlParameterProvider=true` |
| File metadata | `sqlFiles` has `.sql` extension, input role |
| Invalid schema version | Returns empty metadata on version mismatch |
| Broken JSON | Throws `IOException` |
| Sorted tasks | `tasksSortedByName()` returns alphabetically ordered |
| Accepted forms | `database` has method-call and legacy assignment form |

Test fixtures live in:
```
gretl-lsp/src/test/resources/ch/so/agi/gretl/lsp/metadata/
  gretl-lsp-metadata.json       -- valid test metadata (2 tasks)
  invalid-version.json           -- schema version "9.9.9" (rejected)
  broken.json                    -- truncated JSON (parse error)
```

### Completion Tests

`ch.so.agi.gretl.lsp.completion.CompletionContextDetectorTest`

| Test | What it checks |
|------|----------------|
| TASK_TYPE from typeRange | Cursor inside parsed type range returns TASK_TYPE |
| TASK_TYPE from line text | Regex `tasks.register('name', \|)` detects type position |
| INSIDE_GRETL_TASK_BODY | Cursor inside body range returns task body context |
| DEPENDENCY_TASK_NAME | Cursor inside dependency range returns dependency context |
| UNKNOWN for empty script | No tasks in script returns UNKNOWN |
| UNKNOWN outside blocks | Cursor outside all task ranges returns UNKNOWN |
| positionInside true | Correctly identifies position inside a range |
| positionInside before | Position before range start is not inside |
| positionInside after | Position after range end is not inside |
| positionInside null range | Null range returns false |

`ch.so.agi.gretl.lsp.completion.CompletionProviderTest`

| Test | What it checks |
|------|----------------|
| Task type completion returns all tasks | Lists all task types from metadata alphabetically |
| Property completion sorts required first | Required properties get sortText prefix "0100" |
| Property completion excludes already-set | Already called properties are not offered |
| Insert text from non-legacy form | Snippet insert text from method-call accepted form |
| Dependency completion shows task names | All script task names appear as completion items |
| Empty for unknown context | UNKNOWN context returns empty list |
| Empty for unknown type | Task block without typeName returns empty list |

### Hover Tests

`ch.so.agi.gretl.lsp.hover.HoverProviderTest`

| Test | What it checks |
|------|----------------|
| Hover over task type | Shows task description, qualified class name, category |
| Hover over DSL property | Shows type, required status ("ja/nein"), description |
| Hover shows signature | Non-legacy accepted form rendered as code block |
| Returns empty for no target | Position outside any block returns empty |
| Hover shows deprecated status | Deprecated property shows "Status: deprecated" |
| Hover shows sqlParameterProvider | `sqlParameterProvider=true` noted in hover |

### Signature Help Tests

`ch.so.agi.gretl.lsp.signature.SignatureHelpProviderTest`

| Test | What it checks |
|------|----------------|
| Signature help for multi-argument call | Returns signature with correct label |
| Active parameter from comma count | Comma count in source text determines active parameter |
| Returns empty outside task block | No signature help outside a task block |
| Returns empty for unknown type | No signature help when task type not known |
| activeParameterIndex counts commas | Line-based comma counting works correctly |
| activeParameterIndex empty text | Null/empty source text returns 0 |

### LSP Protocol Tests

`ch.so.agi.gretl.lsp.server.GretlTextDocumentServiceLspTest`

End-to-end LSP protocol tests using `CompletableFuture` and the full server:

| Test | What it checks |
|------|----------------|
| Completion in empty task body | `didOpen` then `completion` returns required/optional properties |
| Completion suggests task types | `tasks.register('x', \|)` returns known task type names |
| Completion in dependency context | `dependsOn '\|'` returns script task names |
| Hover over property | Hover contains property type and required status |
| Hover over task type | Hover contains task class name and description |
| Signature help for database call | `database dbUri, \|` returns `database url, user, password` |
| Signature help for sqlFiles call | `sqlFiles files(\|` returns a signature |
| Returns null hover for empty script | Unknown documents return null hover |
| Returns null signature help for empty script | Unknown documents return null signature help |

### Server Tests

`ch.so.agi.gretl.lsp.server.GretlLanguageServerInitializeTest`

| Test | What it checks |
|------|----------------|
| Expected capabilities | Full text sync, hover, document symbols |
| Server info | Name `gretl-lsp`, version `0.1.0` |
| Shutdown | Completes successfully, returns null |
| Exit | Does not throw |
| Services | `getTextDocumentService()` and `getWorkspaceService()` return non-null |

`ch.so.agi.gretl.lsp.server.GretlServerLauncherArgsTest`

| Test | What it checks |
|------|----------------|
| `--help` / `-h` | Sets help flag |
| `--stdio` | Sets stdio, default log level INFO |
| `--log-level=DEBUG/WARN` | Parses log levels |
| Invalid log level | Falls back to INFO |
| `--trace` | Sets trace flag |
| `--metadata=/path` | Stores absolute path |
| Unknown option | Throws `IllegalArgumentException` |
| No args | Returns usable config, no modes enabled |

## Smoke Test the Fat JAR

```bash
./gradlew :gretl-lsp:shadowJar
java -jar gretl-lsp/build/libs/gretl-lsp-5.0.0-SNAPSHOT-all.jar --help
```

Verify:
- `--help` prints usage text to **stdout**
- `--stdio` starts the LSP server, all logs go to **stderr**
- No output on stdout when running `--stdio` (stdout is reserved for JSON-RPC)

## Adding Tests for New Features

When adding a new metadata model class or server component:

1. Add test fixtures under `src/test/resources/` if needed.
2. Write focused unit tests in the corresponding package.
3. Run with `./gradlew :gretl-lsp:test`.
4. Update this file with the new test descriptions.

## Test Fixture Policy

The test metadata fixture (`gretl-lsp-metadata.json`) is hand-maintained with 2
tasks. It is intentionally smaller than the full 45+ task manifest to keep unit
tests fast and predictable. Integration tests (future phases) may use the full
generated manifest.

### LineIndex Tests

`ch.so.agi.gretl.lsp.document.LineIndexTest`

| Test | What it checks |
|------|----------------|
| Single line offset/position | Basic offset↔position roundtrip |
| Multiline LF | LF-separated text, correct line boundaries |
| Multiline CRLF | Windows-style newlines handled correctly |
| Empty string | Returns 1 line, offset 0 |
| Trailing newline | Correctly handled as extra empty line |
| Offset past end clamps | Oversized positions clamp to text boundaries |
| Position past end clamps | Oversized offsets return last valid position |
| null input | Returns empty index, no NPE |
| Invalid line index | Returns empty string for out-of-bounds lines |

### Parser Tests

`ch.so.agi.gretl.lsp.parser.GroovyAstGretlParserTest`

| Test | What it checks |
|------|----------------|
| Extracts task name and type | `tasks.register('name', Type)` recognized |
| Extracts method-call DSL | `database url, user, pwd` inside closure |
| Extracts assignment DSL | `sqlFiles = files('demo.sql')` recognized |
| Extracts dependsOn | `dependsOn 'otherTask'` inside closure |
| Extracts defaultTasks | `defaultTasks 'name'` top-level |
| Multiple tasks | Three `tasks.register` calls all extracted |
| Fully qualified type | `ch.so.agi.gretl.tasks.SqlExecutor` recognized |
| sqlParameters named arguments | Map-style arguments extracted correctly |
| astBased flag | `astBased=true` for valid Groovy parse |
| Non-GRETL content | Returns empty script, no exceptions |
| taskByName | `findTask` and `taskAt` methods on GretlScript |
| Returns empty for no tasks | No false positives |

### Scanner Tests

`ch.so.agi.gretl.lsp.scanner.LenientGretlScannerTest`

| Test | What it checks |
|------|----------------|
| Finds valid task | Regex-based scanner picks up `tasks.register` |
| Incomplete line | `database dbUri,` without closing brace handled |
| Partial DSL call | `sql` at start of line handled |
| Empty body | `{ }` with no calls produces empty calls list |
| Missing closing brace | Unclosed `{` body handled gracefully |
| Extracts dependencies | `dependsOn 'y'` from scanner |
| scannerFallbackUsed flag | `scannerFallbackUsed=true` for scanner results |
| No tasks | Returns empty script, no exception |
| defaultTasks | Scanner picks up `defaultTasks` declaration |

Test fixtures live in `gretl-lsp/src/test/resources/ch/so/agi/gretl/lsp/parser/`:

| File | Purpose |
|------|---------|
| `simple-sql-executor.gradle` | Valid GRETL job with SqlExecutor |
| `assignment-style.gradle` | Legacy assignment DSL style |
| `with-dependencies.gradle` | Multi-task with dependsOn chains |
| `default-tasks.gradle` | `defaultTasks` declaration |
| `incomplete-dsl.gradle` | Partial `database dbUri,` call |
| `incomplete-call.gradle` | Partial `sql` at start of body |
| `empty-body.gradle` | Task with empty closure |
| `broken-syntax.gradle` | Invalid Groovy for scanner fallback testing |
