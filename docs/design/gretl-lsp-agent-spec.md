# GRETL LSP und VS-Code-Extension: Spezifikation und Arbeitsanweisung für LLM Coding Agents

**Dateipfad dieser Spezifikation:** `docs/design/gretl-lsp-agent-spec.md`
**Zielsystem:** GRETL Gradle-Plugin plus neue GRETL-Unterstützung für VS Code  
**Primäre Umsetzungssprache Language Server:** Java mit LSP4J  
**Primäre Umsetzungssprache VS-Code-Client:** TypeScript  
**Primäre Script-Sprache der Anwender:** Gradle Groovy, weiterhin `build.gradle` / `*.gradle`  
**Stand:** 2026-06-30  
**Version:** 2.0, aktualisiert nach Analyse von `gretl-core-src.zip` und `gretl-doclet-src.zip`  
**Adressat:** LLM Coding Agent, der selbstständig Phasen umsetzt, testet und dokumentiert

---

## 1. Zweck dieser Spezifikation

Diese Spezifikation beschreibt sehr detailliert, wie für GRETL ein Language Server Protocol Server und eine kleine VS-Code-Extension aufgebaut werden sollen. Die Umsetzung erfolgt in klar abgegrenzten Phasen. Jede Phase muss ein funktionierendes Artefakt liefern, Tests enthalten, dokumentiert werden und ihren Fortschritt im Repository sichtbar tracken.

Der Agent muss diese Spezifikation als verbindliche Arbeitsgrundlage lesen und befolgen. Jede Phase enthält am Ende ein eigenes Kapitel **Agenten-Prompt**. Dieser Prompt verweist explizit auf `docs/design/gretl-lsp-agent-spec.md` und erinnert den Agenten daran, vorhandene Skills, AGENTS.md, CLAUDE.md, Codex-/OpenCode-Konventionen und lokale Repository-Instruktionen zu berücksichtigen.

Diese Spezifikation ist absichtlich ausführlich. Sie ist nicht nur ein Architekturpapier, sondern eine konkrete Implementierungsanweisung auf Projekt-, Paket-, Klassen-, Methoden-, Test- und Dokumentationsebene.

---

## 2a. Verbindliche Aktualisierung aufgrund der gelieferten `gretl-core`- und `gretl-doclet`-Quellen

Diese Sektion ist ein **Override** für alle älteren, generischen Beispiele in dieser Spezifikation. Bei Widersprüchen gilt diese Sektion.

### 2a.1 Tatsächlicher Stand der gelieferten Quellen

Analysierte Sandbox-Dateien:

```text
/mnt/data/gretl-core-src.zip
/mnt/data/gretl-doclet-src.zip
```

Ergebnis der Analyse:

- `gretl-core` enthält die aktuelle Task-DSL unter `ch.so.agi.gretl.tasks`.
- Es gibt 42 mit `@GretlTaskDoc` annotierte Task-Klassen.
- Viele sichtbare DSL-Methoden liegen in Basisklassen (`AbstractDatabaseTask`, `FtpTask`, `S3Task`, `AbstractIli2DbTask`, `AbstractInterlisValidatorTask`) und werden von konkreten Tasks geerbt.
- Das aktuelle `gretl-doclet` enthält `GretlDoclet`, `TaskClassCollector`, `TaskDescriptorExtractor`, `AsciiDocRenderer`, `TypeNameFormatter` und Modell-Records `TaskDescriptor`, `DslMethodDescriptor`, `ParameterDescriptor`.
- Das aktuelle Doclet sammelt annotierte Task-Klassen, rendert AsciiDoc und kennt `@GretlTaskDoc`, `@GretlDslMethod` und `@LocaleText`.
- Das aktuelle Doclet muss für den LSP erweitert werden, nicht ersetzt werden.

### 2a.2 Wichtigste fachliche Korrektur: `DuckDbSqlExecutor`

Für den aktuellen `DuckDbSqlExecutor` ist folgende Top-Level-DSL korrekt:

```groovy
tasks.register('analyse', DuckDbSqlExecutor) {
    database file('build/work.duckdb')
    // oder alternativ:
    // inMemoryDatabase()

    sqlFiles 'src/main/sql/analyse.sql'
}
```

Nicht korrekt als Top-Level-`DuckDbSqlExecutor`-Datenbank ist:

```groovy
database url, usr, pwd
```

Diese dreiargumentige `database`-Form kommt bei `SqlExecutor`, `AbstractDatabaseTask`, `AbstractIli2DbTask` und in verschachtelten PostgreSQL-Kontexten von `DuckDbSqlExecutor` vor, aber nicht als DuckDB-Datei-Konfiguration.

### 2a.3 Task-Inventar aus aktuellen Quellen

| Task | extends | direkt annotierte DSL-Methoden | zwingender LSP-Hinweis |
|---|---|---:|---|
| `Av2ch` | `AbstractCoreGretlTask` | 3 | geerbte DSL-Methoden einbeziehen |
| `Av2geobau` | `AbstractCoreGretlTask` | 3 | geerbte DSL-Methoden einbeziehen |
| `Csv2Excel` | `AbstractCoreGretlTask` | 2 | geerbte DSL-Methoden einbeziehen |
| `CsvExport` | `AbstractDatabaseTask` | 1 | geerbte DSL-Methoden einbeziehen |
| `CsvImport` | `AbstractDatabaseTask` | 1 | geerbte DSL-Methoden einbeziehen |
| `CsvValidator` | `AbstractInterlisValidatorTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `Curl` | `AbstractCoreGretlTask` | 11 | geerbte DSL-Methoden einbeziehen |
| `Db2Db` | `AbstractCoreGretlTask` | 10 | geerbte DSL-Methoden einbeziehen; SQL-Parameter prüfen |
| `DuckDbSqlExecutor` | `AbstractCoreGretlTask` | 9 | geerbte DSL-Methoden einbeziehen; Nested DSL + database/inMemory-Alternative; SQL-Parameter prüfen |
| `FtpDelete` | `FtpTask` | 2 | geerbte DSL-Methoden einbeziehen; Secrets warnen |
| `FtpDownload` | `FtpTask` | 4 | geerbte DSL-Methoden einbeziehen; Secrets warnen |
| `FtpList` | `FtpTask` | 1 | geerbte DSL-Methoden einbeziehen; Secrets warnen |
| `FtpUpload` | `FtpTask` | 3 | geerbte DSL-Methoden einbeziehen; Secrets warnen |
| `Gpkg2Dxf` | `AbstractCoreGretlTask` | 2 | geerbte DSL-Methoden einbeziehen |
| `Gpkg2Shp` | `AbstractCoreGretlTask` | 2 | geerbte DSL-Methoden einbeziehen |
| `GpkgExport` | `AbstractDatabaseTask` | 1 | geerbte DSL-Methoden einbeziehen |
| `GpkgImport` | `AbstractDatabaseTask` | 1 | geerbte DSL-Methoden einbeziehen |
| `GpkgValidator` | `AbstractInterlisValidatorTask` | 1 | geerbte DSL-Methoden einbeziehen |
| `Gzip` | `AbstractCoreGretlTask` | 2 | geerbte DSL-Methoden einbeziehen |
| `Ili2duckdbExport` | `AbstractIli2DbExportTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2duckdbImport` | `AbstractIli2DbTransferTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2duckdbImportSchema` | `AbstractIli2DbSchemaImportTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2gpkgImport` | `AbstractIli2DbTransferTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2pgDelete` | `AbstractIli2DbTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2pgExport` | `AbstractIli2DbExportTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2pgImport` | `AbstractIli2DbTransferTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2pgImportSchema` | `AbstractIli2DbSchemaImportTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2pgReplace` | `Ili2pgImport` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2pgUpdate` | `Ili2pgImport` | 0 | geerbte DSL-Methoden einbeziehen |
| `Ili2pgValidate` | `AbstractIli2DbTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `IliValidator` | `AbstractInterlisValidatorTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `JsonImport` | `AbstractDatabaseTask` | 1 | geerbte DSL-Methoden einbeziehen |
| `JsonValidator` | `AbstractInterlisValidatorTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `S3Bucket2Bucket` | `AbstractCoreGretlTask` | 8 | geerbte DSL-Methoden einbeziehen; Secrets warnen |
| `S3Delete` | `S3Task` | 1 | geerbte DSL-Methoden einbeziehen; Secrets warnen |
| `S3Download` | `S3Task` | 2 | geerbte DSL-Methoden einbeziehen; Secrets warnen |
| `S3Upload` | `S3Task` | 6 | geerbte DSL-Methoden einbeziehen; Secrets warnen |
| `ShpExport` | `AbstractDatabaseTask` | 2 | geerbte DSL-Methoden einbeziehen |
| `ShpImport` | `AbstractDatabaseTask` | 2 | geerbte DSL-Methoden einbeziehen |
| `ShpValidator` | `AbstractInterlisValidatorTask` | 0 | geerbte DSL-Methoden einbeziehen |
| `SqlExecutor` | `AbstractCoreGretlTask` | 5 | geerbte DSL-Methoden einbeziehen; SQL-Parameter prüfen |
| `XslTransformer` | `AbstractCoreGretlTask` | 5 | geerbte DSL-Methoden einbeziehen |

### 2a.4 Basisklassen mit geerbten DSL-Methoden

Der LSP und das Doclet-Metadatenformat müssen **effektive Methoden** pro Task ausweisen. Direkte Methoden reichen nicht.

#### `AbstractDatabaseTask`

| Methode | Pflicht | Default |
|---|---:|---:|
| `database(String jdbcUrl)` | ja | nein |
| `database(String jdbcUrl, String username, String password)` | ja | nein |

#### `AbstractIli2DbExportTask`

| Methode | Pflicht | Default |
|---|---:|---:|
| `dataFiles(Object... paths)` | ja | nein |
| `exportModels(String... names)` | nein | nein |

#### `AbstractIli2DbFileTask`

| Methode | Pflicht | Default |
|---|---:|---:|
| `datasetNames(String... names)` | nein | nein |
| `datasetNamesFromTransferFiles()` | nein | nein |
| `datasetNamesFromFiles(Object... paths)` | nein | nein |
| `datasetNameSlice(int start)` | nein | nein |
| `datasetNameSlice(int start, int endExclusive)` | nein | nein |

#### `AbstractIli2DbSchemaImportTask`

| Methode | Pflicht | Default |
|---|---:|---:|
| `iliFile(Object path)` | nein | nein |
| `iliMetaAttrsFile(Object path)` | nein | nein |

#### `AbstractIli2DbTask`

| Methode | Pflicht | Default |
|---|---:|---:|
| `databaseFile(Object path)` | ja | nein |
| `database(String jdbcUrl)` | ja | nein |
| `database(String jdbcUrl, String username, String password)` | ja | nein |
| `schema(String name)` | nein | nein |
| `dbschema(String name)` | nein | nein |
| `modelNames(String... names)` | nein | nein |
| `models(String value)` | nein | nein |
| `modelDirectories(String... entries)` | nein | nein |
| `modeldir(String value)` | nein | nein |
| `baskets(String value)` | nein | nein |
| `topics(String value)` | nein | nein |
| `dataset(Object dataset)` | nein | nein |
| `datasetSubstring(Iterable<Integer> datasetSubstring)` | nein | nein |
| `datasetSubstring(Integer... datasetSubstring)` | nein | nein |
| `logFile(Object path)` | nein | nein |

#### `AbstractIli2DbTransferTask`

| Methode | Pflicht | Default |
|---|---:|---:|
| `transferFiles(Object... paths)` | ja | nein |
| `repositoryDataIds(String... ids)` | nein | nein |

#### `AbstractInterlisValidatorTask`

| Methode | Pflicht | Default |
|---|---:|---:|
| `dataFiles(Object... paths)` | ja | nein |
| `modelNames(String... names)` | nein | nein |
| `models(String value)` | nein | nein |
| `modelDirectories(String... entries)` | nein | nein |
| `modeldir(String value)` | nein | nein |
| `configFile(Object path)` | nein | nein |
| `configRepositoryId(String id)` | nein | nein |
| `metaConfigFile(Object path)` | nein | nein |
| `metaConfigRepositoryId(String id)` | nein | nein |
| `logFile(Object path)` | nein | nein |
| `xtfLogFile(Object path)` | nein | nein |

#### `FtpTask`

| Methode | Pflicht | Default |
|---|---:|---:|
| `server(String server)` | ja | nein |
| `user(String user)` | ja | nein |
| `password(String password)` | ja | nein |
| `systemType(String systemType)` | nein | nein |
| `fileSeparator(String fileSeparator)` | nein | nein |
| `passiveMode(boolean passiveMode)` | nein | nein |
| `controlKeepAliveTimeout(long controlKeepAliveTimeout)` | nein | nein |

#### `S3Task`

| Methode | Pflicht | Default |
|---|---:|---:|
| `accessKey(String accessKey)` | ja | nein |
| `secretKey(String secretKey)` | ja | nein |
| `bucketName(String bucketName)` | ja | nein |
| `endpoint(String endpoint)` | nein | nein |
| `region(String region)` | nein | nein |

### 2a.5 Nested DSL, die im Metadatenformat explizit auftauchen muss

| Kontext | Methoden/Properties | Bedeutung für LSP |
|---|---|---|
| `DuckDbSqlExecutor` | `database(Object file)`, `inMemoryDatabase()`, `installExtensions(boolean)`, `sqlFiles(Object...)`, `sources`, `targets`, `exports`, `sqlParameters`, `sqlParameterSets` | Top-Level-DuckDB-Datenbank ist **nicht** `database url,user,pwd`; `database` und `inMemoryDatabase` sind Alternativen. |
| `sources {}` / `SourcesConfig` | `postgres(alias)`, `gpkg(alias)`, `csv(alias)` | Nested Completion und Diagnostics. |
| `sources.postgres {}` / `PostgresConfig` | `database(jdbcUrl[, user, password])`, `table(name)`, `mode`, `autoDetectGeometry` | PostgreSQL-Föderationsquelle. |
| `PostgresTableConfig` | `alias`, `mode`, `columns`, `geometry(column)` | Tabellen-/Geometrie-Konfiguration. |
| `GeometryConfig` | `alias`, `srid`, `type`, `encoding`, `force2d`, `include` | In Tests mit Property-Schreibweise `srid = 2056` verwendet. |
| `sources.gpkg {}` / `GpkgConfig` | `file`, `mode`, `layer(name)` | `file` ist Pflicht. |
| `sources.csv {}` / `CsvConfig` | `file`, `table`, `mode`, `header`, `delimiter`, `allVarchar` | `file` ist Pflicht, `table` Default `data`. |
| `targets {}` / `TargetsConfig` | `postgres(alias)` | Alias muss von `exports.postgres.target` referenzierbar sein. |
| `targets.postgres {}` / `PostgresTargetConfig` | `database(jdbcUrl[, user, password])` | Sensitive Passwort-Argumente. |
| `exports {}` / `ExportsConfig` | `gpkg(name)`, `parquet(name)`, `xlsx(name)`, `postgres(name)` | Export-Outline, Export-Dateilinks, Graph-Artefakte. |
| `BaseExportConfig` | `query`, `file`, `overwrite` | Basiskontext für Dateiexporte; Property-Schreibweise nicht blind migrieren. |
| `PostgresExportConfig` | `target`, `query`, `table`, `mode`, `writePath`, `create`, `geometry` | `target` gegen `targets.postgres(alias)` prüfen. |

### 2a.6 Konkrete Pflichtänderungen an den Doclet-Phasen

Phase 1 und Phase 2 müssen zwingend Folgendes liefern:

1. `gretl-doclet` erzeugt zusätzlich `gretl-lsp-metadata.json`.
2. `TaskDescriptorExtractor` oder eine neue Hilfsklasse sammelt nicht nur direkte, sondern auch geerbte `@GretlDslMethod`-Methoden.
3. Das JSON enthält `methods` und `effectiveMethods`.
4. Jede Methode enthält mindestens `name`, `signature`, `parameters`, `required`, `defaultValue`, `description`, `originClass`, `declaredOnTask`.
5. Das JSON kann `requiredGroups` und `conflicts` ausdrücken.
6. Für `DuckDbSqlExecutor` werden `database`/`inMemoryDatabase` als Alternativgruppe modelliert.
7. Für `SqlExecutor` und `DuckDbSqlExecutor` werden `sqlParameters`/`sqlParameterSets` als gegenseitiger Konflikt modelliert.
8. Nested-Kontexte von `DuckDbSqlExecutor` werden modelliert, mindestens `sources`, `targets`, `exports` und deren wichtigsten Unterkontexte.
9. Sensitive Werte werden modelliert: `password`, `secretKey`, Datenbankpasswort-Parameter.
10. Tests müssen mindestens `CsvImport` + geerbtes `database`, `FtpDownload` + geerbte FTP-Methoden, `IliValidator` + geerbte Validator-Methoden und `DuckDbSqlExecutor` + Nested-Kontexte prüfen.

---

## 2. Fachlicher Kontext

GRETL steht für Gradle ETL. Das bestehende GRETL-Projekt ist ein Gradle-Plugin, das benutzerdefinierte Tasks für SQL- und Geodaten-ETL bereitstellt. GRETL-Jobs werden heute und weiterhin als Gradle-Groovy-Skripte geschrieben.

Typische GRETL-Jobs bestehen aus mehreren Gradle-Tasks, die per `dependsOn`, `mustRunAfter`, `finalizedBy` oder `defaultTasks` zu einer ETL-Pipeline verbunden werden. Fachlich entspricht das einem gerichteten azyklischen Graphen von Arbeitsschritten, z. B. Download, Entpacken, INTERLIS-Import, SQL-Umbau, Export, Validierung und Upload.

Die neue GRETL-DSL bleibt in Gradle Groovy eingebettet, verwendet aber bevorzugt eine methodenartige Schreibweise ohne `=`:

```groovy
import ch.so.agi.gretl.tasks.*

plugins {
    id 'ch.so.agi.gretl'
}

def dataset = findProperty('dataset') ?: 'Olten'

defaultTasks 'executeSql'

tasks.register('executeSql', DuckDbSqlExecutor) {
    inMemoryDatabase()
    sqlFiles 'src/main/sql/transform.sql'
    sqlParameters dataset: dataset
}

// Für SqlExecutor oder verschachtelte PostgreSQL-Kontexte bleibt diese Form korrekt:
// database jdbcUrl, user, password
```

Der Language Server soll nicht einfach Groovy allgemein besser machen. Er soll GRETL-semantisch verstehen, welche Task-Typen existieren, welche DSL-Methoden und Properties ein Task unterstützt, welche Pflichtfelder fehlen, welche Dateien referenziert sind, welche SQL-Parameter verwendet werden und wie der Job-Graph aussieht.

Das bestehende `gretl-doclet` ist zentral. Es liest heute bereits viele Informationen aus den GRETL-Tasks und erzeugt Dokumentation. Es soll so erweitert werden, dass es zusätzlich ein maschinenlesbares, stabiles und verständliches Metadatenformat erzeugt, das der LSP direkt verwenden kann.

---

## 3. Zentrale Designentscheidungen

### 3.1 LSP ist GRETL-semantisch, nicht allgemeiner Groovy-Ersatz

Der LSP soll Gradle-Groovy-Dateien analysieren, aber nur die GRETL-relevanten Inseln tief verstehen:

- `tasks.register('name', TaskType) { ... }`
- `task name(type: TaskType) { ... }`
- `defaultTasks 'name'`
- `dependsOn`, `finalizedBy`, `mustRunAfter`, `shouldRunAfter`
- GRETL-DSL-Calls innerhalb von Task-Closures
- Dateireferenzen wie `file('...')`, `files('...')`, `fileTree(...)`
- SQL-Dateireferenzen und SQL-Parameter `${paramName}`
- Gradle-Projekt-Properties wie `findProperty('dataset')`, `property('dataset')`, `-Pdataset`, `ORG_GRADLE_PROJECT_dataset`

Nicht Ziel des LSP ist:

- vollständige Groovy-Typinferenz
- vollständige Gradle-Configuration-Phase
- Live-Ausführung von `build.gradle`
- automatische Datenbankverbindungen bei jedem Tastendruck
- vollständige SQL-Semantik gegen reale Datenbanken im MVP

### 3.2 Groovy-Parser plus toleranter GRETL-Scanner

Der LSP soll zwei Analysepfade besitzen:

1. **Groovy-AST-Pfad**  
   Für syntaktisch gültige oder fast gültige Dateien wird Groovy bis zu einer frühen Compile-Phase geparst. Danach wird ein eigenes GRETL-Zwischenmodell erzeugt.

2. **Toleranter GRETL-Scanner**  
   Für unfertige Editorzustände, z. B. während der Anwender tippt, erkennt ein leichter Scanner Task-Blöcke, aktuelle Closure, Task-Typ, vorhandene DSL-Calls und aktuelle Completion-Position heuristisch.

Der LSP darf ein Gradle-Script nicht live evaluieren. Keine `GroovyShell.evaluate(build.gradle)`, kein `ProjectBuilder` bei jeder Änderung, keine Tooling-API bei jedem Save. Tooling-API ist nur für explizite Commands zulässig, etwa `GRETL: Refresh Gradle Model` oder `GRETL: Run Task at Cursor`, und erst in späteren Phasen.

### 3.3 Metadaten aus `gretl-doclet` sind Source of Truth

Alle Completion-, Hover-, Diagnostic-, Snippet- und Quick-Fix-Funktionen müssen, soweit möglich, aus einem maschinenlesbaren Metadatenmanifest gespeist werden.

Dieses Manifest heisst:

```text
gretl-lsp-metadata.json
```

Es wird vom erweiterten `gretl-doclet` erzeugt und in einem stabilen Schema versioniert.

### 3.4 Kleine VS-Code-Extension, grosser Java-LSP

Die VS-Code-Extension bleibt bewusst dünn:

- Java-LSP starten und stoppen
- Output Channel anzeigen
- Commands registrieren
- Tree View / Activity Bar für GRETL anbieten
- Webview für Job-Graph und Task-Overview anzeigen
- `.gradle` / `build.gradle` als relevante Dokumente behandeln
- Entwicklung mit F5 ermöglichen

Die eigentliche Semantik lebt im Java-LSP.

---

## 4. Zielbild für Anwender

Beim Schreiben eines GRETL-Jobs in VS Code soll der Anwender folgende Unterstützung erhalten:

- Completion für GRETL-Task-Typen.
- Completion für Properties und DSL-Methoden innerhalb eines Task-Blocks.
- Hover-Dokumentation aus GRETL-Doku/Doclet.
- Signature Help für methodenartige DSL, z. B. `database jdbcUrl, user, password` bei `SqlExecutor` und `database file(...)` bzw. `inMemoryDatabase()` bei `DuckDbSqlExecutor`.
- Diagnostics für fehlende Pflichtfelder.
- Diagnostics für unbekannte Properties oder Tippfehler.
- Diagnostics für unbekannte `dependsOn`-Tasks.
- Diagnostics für fehlende Dateien.
- Diagnostics für SQL-Parameter, die in `.sql` verwendet, aber nicht in `sqlParameters` gesetzt werden.
- Document Symbols / Outline für GRETL-Tasks.
- Document Links von `sqlFiles files('...')` zur SQL-Datei.
- Quick Fixes, z. B. fehlende Pflichtfelder ergänzen oder alte DSL-Schreibweise migrieren.
- Job-Graph / GRETL Overview in einer VS-Code-Webview.
- Command `GRETL: Run Task at Cursor`.
- Command `GRETL: Restart Language Server`.
- Command `GRETL: Show Language Server Logs`.
- Einfache lokale Entwicklung: Fat-JAR bauen, Extension öffnen, F5 drücken.

---

## 5. Vorgeschlagene Repository-Struktur

Der Agent muss zuerst die existierende Repository-Struktur prüfen. Diese Spezifikation schlägt folgende Struktur vor. Falls das reale Repository bereits abweicht, darf der Agent den Vorschlag anpassen, muss die Abweichung aber in `docs/lsp/DECISIONS.md` dokumentieren.

```text
.
├── AGENTS.md
├── CLAUDE.md                         # falls vorhanden
├── build.gradle
├── settings.gradle
├── gradle/
├── gretl-core/                        # bestehendes aktuelles GRETL Core Gradle-Plugin
├── gretl-doclet/                      # bestehendes Doclet
├── gretl-lsp/                         # neuer Java-Language-Server
│   ├── build.gradle
│   └── src/
│       ├── main/java/ch/so/agi/gretl/lsp/...
│       ├── main/resources/...
│       └── test/java/ch/so/agi/gretl/lsp/...
├── gretl-lsp-testkit/                 # optional, ab Phase 4/5
├── vscode/
│   └── gretl-vscode/                  # neue VS-Code-Extension
│       ├── package.json
│       ├── tsconfig.json
│       ├── src/extension.ts
│       ├── server/gretl-lsp-all.jar   # gestaged für lokale Entwicklung/VSIX
│       ├── syntaxes/                  # optional, nur wenn eigene Grammatik sinnvoll
│       ├── test/
│       └── .vscode/
│           ├── launch.json
│           └── tasks.json
├── docs/
│   └── lsp/
│       ├── README.md
│       ├── PHASE_STATUS.md
│       ├── DECISIONS.md
│       ├── METADATA_FORMAT.md
│       ├── DEVELOPMENT.md
│       ├── TESTING.md
│       ├── VSCODE_EXTENSION.md
│       └── TROUBLESHOOTING.md
└── .github/
    └── workflows/
        └── gretl-lsp-ci.yml
```

Falls `gretl-lsp-testkit` zu schwergewichtig ist, können Test-Hilfsklassen zuerst unter `gretl-lsp/src/testFixtures/java` oder `gretl-lsp/src/test/java/.../testkit` liegen. Der Agent soll die kleinste robuste Lösung wählen.

---

## 6. Phase Tracking

### 6.1 Zentrale Tracking-Datei

Ab Phase 0 muss folgende Datei existieren:

```text
docs/lsp/PHASE_STATUS.md
```

Sie enthält eine Tabelle:

```markdown
# GRETL LSP Phase Status

| Phase | Titel | Status | Start | Ende | Commit | Artefakt | Tests | Doku | Bemerkungen |
|------:|-------|--------|-------|------|--------|----------|-------|------|-------------|
| 0 | Arbeitsgrundlage und Projektgerüst | not-started | | | | | | | |
| 1 | Doclet-Metadatenformat v1 | not-started | | | | | | | |
| 2 | LSP-Projekt, Metadaten-Loader und Server-Skelett | not-started | | | | | | | |
| 3 | Groovy-AST-Parser und GRETL-Zwischenmodell | not-started | | | | | | | |
| 4 | Diagnostics v1 | not-started | | | | | | | |
| 5 | Completion, Hover, Signature Help | not-started | | | | | | | |
| 6 | Document Symbols, Links und SQL-Parameteranalyse | not-started | | | | | | | |
| 7 | VS-Code-Extension v1 mit F5-Workflow | not-started | | | | | | | |
| 8 | GRETL Overview / Job-Graph-Webview | not-started | | | | | | | |
| 9 | Quick Fixes und DSL-Migration | not-started | | | | | | | |
| 10 | GitHub Actions CI und VSIX Packaging | not-started | | | | | | | |
| 11 | Stabilisierung, Dokumentation und Release-Kandidaten | not-started | | | | | | | |
```

Erlaubte Werte für `Status`:

- `not-started`
- `in-progress`
- `blocked`
- `implemented`
- `verified`
- `done`

Eine Phase ist erst `done`, wenn:

1. Artefakt vorhanden ist.
2. Relevante Tests erfolgreich gelaufen sind.
3. Dokumentation aktualisiert ist.
4. `docs/lsp/PHASE_STATUS.md` aktualisiert ist.
5. Der Agent im Abschlussbericht die tatsächlich ausgeführten Kommandos nennt.

### 6.2 Keine falschen Testbehauptungen

Der Agent darf nie behaupten, Tests seien erfolgreich, wenn sie nicht tatsächlich ausgeführt wurden. Wenn ein Test nicht ausgeführt werden konnte, muss der Grund genannt werden.

### 6.3 Laufende Dokumentation

Jede Phase muss, sofern betroffen, mindestens eine der folgenden Dateien aktualisieren:

- `docs/lsp/README.md`
- `docs/lsp/DECISIONS.md`
- `docs/lsp/METADATA_FORMAT.md`
- `docs/lsp/DEVELOPMENT.md`
- `docs/lsp/TESTING.md`
- `docs/lsp/VSCODE_EXTENSION.md`
- `docs/lsp/TROUBLESHOOTING.md`
- `vscode/gretl-vscode/README.md`

---

## 7. Metadatenformat `gretl-lsp-metadata.json`

### 7.1 Ziel

Das Metadatenformat beschreibt GRETL-Tasks, Properties, DSL-Methoden, Pflichtfelder, Typen, Beispiele, Defaults, File-Kategorien, SQL-Parameterfähigkeit, Status und Deprecations.

Der LSP darf nicht aus der HTML-Dokumentation scrapen. Er muss direkt JSON lesen.

### 7.2 Dateiort

Das vom Doclet erzeugte Manifest soll in mindestens einem stabilen Ort landen:

```text
gretl-doclet/build/generated/gretl-lsp-metadata/gretl-lsp-metadata.json
```

Zusätzlich soll es für den LSP-Test und die Extension-Verpackung kopierbar sein nach:

```text
gretl-lsp/src/main/resources/ch/so/agi/gretl/lsp/metadata/gretl-lsp-metadata.json
vscode/gretl-vscode/server/metadata/gretl-lsp-metadata.json   # optional, nur wenn benötigt
```

Besser ist: Der LSP-Fat-JAR enthält das Manifest als Resource. Die Extension muss dann nur den JAR starten.

### 7.3 JSON-Schema

Zusätzlich zum Manifest muss ein JSON Schema erzeugt oder gepflegt werden:

```text
gretl-doclet/src/main/resources/gretl-lsp-metadata.schema.json
```

Dieses Schema wird in Tests verwendet, um das Manifest zu validieren.

### 7.4 Beispielstruktur

```json
{
  "schemaVersion": "1.0.0",
  "generatedAt": "2026-06-30T20:00:00Z",
  "gretlVersion": "3.2.863",
  "source": {
    "repository": "https://github.com/sogis/gretl",
    "doclet": "gretl-doclet",
    "commit": "unknown-or-git-hash"
  },
  "tasks": [
    {
      "name": "SqlExecutor",
      "qualifiedClassName": "ch.so.agi.gretl.tasks.SqlExecutor",
      "simpleClassName": "SqlExecutor",
      "category": "database",
      "status": "stable",
      "description": "Führt SQL-Dateien in angegebener Reihenfolge aus.",
      "longDescription": "Der SqlExecutor-Task dient dazu, Datenumbauten auszuführen...",
      "examples": [
        {
          "title": "Minimal mit Parametern",
          "language": "groovy",
          "body": "tasks.register('executeSomeSql', SqlExecutor) {\n    database db_uri, db_user, db_pass\n    sqlParameters dataset: 'Olten'\n    sqlFiles files('demo.sql')\n}"
        }
      ],
      "properties": [
        {
          "name": "database",
          "displayName": "database",
          "kind": "dsl-method-and-property",
          "valueType": "Connector",
          "javaType": "ListProperty<String>",
          "required": true,
          "deprecated": false,
          "description": "Datenbankverbindung.",
          "acceptedForms": [
            {
              "style": "method-call",
              "signature": "database url, user, password",
              "insertText": "database ${1:url}, ${2:user}, ${3:password}",
              "argumentCount": 3
            },
            {
              "style": "assignment",
              "signature": "database = [url, user, password]",
              "legacy": true,
              "argumentCount": 1
            }
          ],
          "completion": {
            "label": "database",
            "detail": "Pflicht · Connector",
            "sortText": "0100_database"
          }
        },
        {
          "name": "sqlFiles",
          "kind": "dsl-method-and-property",
          "valueType": "FileCollection",
          "javaType": "Property<FileCollection>",
          "required": true,
          "description": "SQL-Dateien, deren Statements gelesen und ausgeführt werden. Reihenfolge ist relevant.",
          "file": {
            "role": "input",
            "extensions": [".sql"],
            "multiple": true,
            "mustExist": true
          },
          "acceptedForms": [
            {
              "style": "method-call",
              "signature": "sqlFiles files('script.sql')",
              "insertText": "sqlFiles files('${1:script.sql}')",
              "argumentCount": 1
            },
            {
              "style": "assignment",
              "signature": "sqlFiles = files('script.sql')",
              "legacy": true,
              "argumentCount": 1
            }
          ]
        },
        {
          "name": "sqlParameters",
          "kind": "dsl-method-and-property",
          "valueType": "Object",
          "javaType": "Property<Object>",
          "required": false,
          "description": "Map oder Liste von Maps mit SQL-Parametern.",
          "sqlParameterProvider": true,
          "acceptedForms": [
            {
              "style": "method-call",
              "signature": "sqlParameters dataset: 'Olten'",
              "insertText": "sqlParameters ${1:dataset}: ${2:'Olten'}"
            }
          ]
        }
      ]
    }
  ]
}
```

### 7.5 Pflichtfelder im Manifest

Jeder Task-Eintrag muss folgende Felder besitzen:

- `name`
- `qualifiedClassName`
- `simpleClassName`
- `status`
- `description`
- `properties`

Jeder Property-Eintrag muss folgende Felder besitzen:

- `name`
- `kind`
- `valueType`
- `required`
- `description`
- `acceptedForms`

### 7.6 Statuswerte

Erlaubte Task- und Property-Statuswerte:

- `stable`
- `incubating`
- `deprecated`
- `internal`
- `experimental`

### 7.7 Property-Kinds

Erlaubte `kind`-Werte:

- `property`
- `dsl-method`
- `dsl-method-and-property`
- `gradle-inherited`
- `internal`

### 7.8 File-Rollen

Erlaubte `file.role`-Werte:

- `input`
- `output`
- `input-output`
- `directory-input`
- `directory-output`
- `unknown`

### 7.9 Migration-Informationen

Für alte DSL-Schreibweisen muss optional ein `migration`-Block vorhanden sein:

```json
{
  "name": "database",
  "migration": {
    "from": ["database = [url, user, password]"],
    "to": "database url, user, password",
    "codeActionTitle": "In neue GRETL-DSL-Schreibweise migrieren"
  }
}
```

---

## 8. Java-Paketstruktur für `gretl-lsp`

Vorgeschlagener Paketroot:

```text
ch.so.agi.gretl.lsp
```

Unterpakete:

```text
ch.so.agi.gretl.lsp
ch.so.agi.gretl.lsp.server
ch.so.agi.gretl.lsp.config
ch.so.agi.gretl.lsp.document
ch.so.agi.gretl.lsp.metadata
ch.so.agi.gretl.lsp.model
ch.so.agi.gretl.lsp.parser
ch.so.agi.gretl.lsp.scanner
ch.so.agi.gretl.lsp.analysis
ch.so.agi.gretl.lsp.diagnostics
ch.so.agi.gretl.lsp.completion
ch.so.agi.gretl.lsp.hover
ch.so.agi.gretl.lsp.signature
ch.so.agi.gretl.lsp.symbol
ch.so.agi.gretl.lsp.links
ch.so.agi.gretl.lsp.sql
ch.so.agi.gretl.lsp.codegen
ch.so.agi.gretl.lsp.util
ch.so.agi.gretl.lsp.testkit
```

---

## 9. Kernklassen und Methoden: Server

### 9.1 `GretlLanguageServer`

Paket:

```java
package ch.so.agi.gretl.lsp.server;
```

Signatur:

```java
public final class GretlLanguageServer implements LanguageServer, LanguageClientAware {
}
```

Felder:

```java
private final GretlTextDocumentService textDocumentService;
private final GretlWorkspaceService workspaceService;
private final ServerLifecycle lifecycle;
private LanguageClient client;
```

Methoden:

```java
@Override
public CompletableFuture<InitializeResult> initialize(InitializeParams params)
```

Aufgaben:

- Workspace Root ermitteln.
- Capabilities setzen:
  - Text document sync incremental oder full. MVP: full reicht, Phase 11 optional incremental.
  - Completion Provider.
  - Hover Provider.
  - Signature Help Provider.
  - Document Symbol Provider.
  - Document Link Provider.
  - Code Action Provider ab Phase 9.
  - Execute Command Provider ab Phase 7/8.
- ServerInfo setzen: Name `gretl-lsp`, Version aus Build-Properties.

```java
@Override
public CompletableFuture<Object> shutdown()
```

Aufgaben:

- Lifecycle auf Shutdown setzen.
- Keine neuen Analysen starten.

```java
@Override
public void exit()
```

Aufgaben:

- Prozess sauber beenden.

```java
@Override
public TextDocumentService getTextDocumentService()
```

```java
@Override
public WorkspaceService getWorkspaceService()
```

```java
@Override
public void connect(LanguageClient client)
```

Aufgaben:

- Client speichern.
- Client an Services weitergeben.

### 9.2 `GretlServerLauncher`

Paket:

```java
package ch.so.agi.gretl.lsp.server;
```

Signatur:

```java
public final class GretlServerLauncher {
    public static void main(String[] args) throws Exception
}
```

Methoden:

```java
private static void launchStdio(GretlLanguageServer server) throws Exception
```

Aufgaben:

- LSP4J Launcher über `System.in` / `System.out` starten.
- Logging nie auf stdout schreiben, weil stdout für JSON-RPC reserviert ist.
- Logs auf stderr oder Datei.

```java
private static GretlServerConfig parseArgs(String[] args)
```

Unterstützte Argumente:

- `--stdio` default
- `--log-level=DEBUG|INFO|WARN|ERROR`
- `--metadata=/path/to/gretl-lsp-metadata.json`
- `--trace`

### 9.3 `GretlTextDocumentService`

Signatur:

```java
public final class GretlTextDocumentService implements TextDocumentService {
}
```

Felder:

```java
private final DocumentStore documentStore;
private final GretlAnalyzer analyzer;
private final DiagnosticPublisher diagnosticPublisher;
private final CompletionProvider completionProvider;
private final HoverProvider hoverProvider;
private final SignatureHelpProvider signatureHelpProvider;
private final DocumentSymbolProvider documentSymbolProvider;
private final DocumentLinkProvider documentLinkProvider;
private final CodeActionProvider codeActionProvider;
```

Methoden:

```java
@Override
public void didOpen(DidOpenTextDocumentParams params)
```

Aufgaben:

- Dokument speichern.
- Analyse triggern.
- Diagnostics publizieren.

```java
@Override
public void didChange(DidChangeTextDocumentParams params)
```

Aufgaben:

- Dokument aktualisieren.
- Analyse debounce-fähig ausführen.
- Diagnostics publizieren.

MVP darf synchron arbeiten. Ab Phase 11 optional Debounce/Executor.

```java
@Override
public void didClose(DidCloseTextDocumentParams params)
```

Aufgaben:

- Dokument aus Store entfernen.
- Diagnostics für URI leeren.

```java
@Override
public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params)
```

Delegiert an `CompletionProvider`.

```java
@Override
public CompletableFuture<Hover> hover(HoverParams params)
```

Delegiert an `HoverProvider`.

```java
@Override
public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params)
```

Delegiert an `SignatureHelpProvider`.

```java
@Override
public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params)
```

Delegiert an `DocumentSymbolProvider`.

```java
@Override
public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params)
```

Delegiert an `DocumentLinkProvider`.

```java
@Override
public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params)
```

Ab Phase 9.

### 9.4 `GretlWorkspaceService`

Signatur:

```java
public final class GretlWorkspaceService implements WorkspaceService {
}
```

Methoden:

```java
@Override
public void didChangeConfiguration(DidChangeConfigurationParams params)
```

Aufgaben:

- LSP-Konfiguration aktualisieren.
- Metadaten neu laden, falls Pfad geändert wurde.

```java
@Override
public void didChangeWatchedFiles(DidChangeWatchedFilesParams params)
```

Aufgaben:

- Bei Änderung von `.sql`, `gradle.properties`, `gretl-lsp-metadata.json`: relevante Dokumente neu analysieren.

```java
@Override
public CompletableFuture<Object> executeCommand(ExecuteCommandParams params)
```

Commands ab Phase 7/8:

- `gretl.restartLanguageServer` clientseitig, nicht hier.
- `gretl.analyzeWorkspace`
- `gretl.getTaskGraph`
- `gretl.getTaskOverview`
- `gretl.getSqlParameterReport`

---

## 10. Kernklassen und Methoden: Dokumentmodell

### 10.1 `DocumentStore`

```java
public final class DocumentStore {
    public void open(String uri, String text, int version);
    public void changeFull(String uri, String text, int version);
    public Optional<TextDocument> get(String uri);
    public void close(String uri);
    public Collection<TextDocument> allOpenDocuments();
}
```

### 10.2 `TextDocument`

```java
public record TextDocument(
    String uri,
    String languageId,
    int version,
    String text,
    LineIndex lineIndex
) {}
```

### 10.3 `LineIndex`

```java
public final class LineIndex {
    public static LineIndex from(String text);
    public int offsetAt(Position position);
    public Position positionAt(int offset);
    public Range range(int startOffset, int endOffset);
    public String lineText(int zeroBasedLine);
    public int lineCount();
}
```

Anforderungen:

- Muss LF und CRLF korrekt behandeln.
- Tests für leere Datei, einzeilige Datei, CRLF, Unicode.

---

## 11. Kernklassen und Methoden: Metadaten

### 11.1 `GretlMetadata`

```java
public record GretlMetadata(
    String schemaVersion,
    String generatedAt,
    String gretlVersion,
    List<TaskMetadata> tasks
) {
    public Optional<TaskMetadata> findTask(String simpleName);
    public List<TaskMetadata> tasksSortedByName();
}
```

### 11.2 `TaskMetadata`

```java
public record TaskMetadata(
    String name,
    String qualifiedClassName,
    String simpleClassName,
    TaskStatus status,
    String description,
    String longDescription,
    List<GretlExample> examples,
    List<PropertyMetadata> properties
) {
    public Optional<PropertyMetadata> findProperty(String name);
    public List<PropertyMetadata> requiredProperties();
    public List<PropertyMetadata> completionProperties();
}
```

### 11.3 `PropertyMetadata`

```java
public record PropertyMetadata(
    String name,
    PropertyKind kind,
    String valueType,
    String javaType,
    boolean required,
    boolean deprecated,
    String description,
    Optional<FileMetadata> file,
    List<AcceptedForm> acceptedForms,
    Optional<MigrationMetadata> migration,
    boolean sqlParameterProvider
) {}
```

### 11.4 `AcceptedForm`

```java
public record AcceptedForm(
    DslStyle style,
    String signature,
    String insertText,
    OptionalInt argumentCount,
    boolean legacy
) {}
```

### 11.5 `MetadataLoader`

```java
public final class MetadataLoader {
    public GretlMetadata loadDefault();
    public GretlMetadata load(Path path);
    public GretlMetadata load(InputStream inputStream);
}
```

Aufgaben:

- JSON via Jackson lesen.
- Schema-Version prüfen.
- Fehlermeldungen klar formulieren.
- Bei ungültigem Manifest nicht crashen, sondern Fehler an LSP-Client melden.

### 11.6 `MetadataValidator`

```java
public final class MetadataValidator {
    public List<MetadataProblem> validate(GretlMetadata metadata);
}
```

Prüfungen:

- Task-Namen eindeutig.
- Property-Namen pro Task eindeutig.
- Pflichtfelder haben mindestens eine akzeptierte Form.
- File-Extensions beginnen mit `.`.
- Deprecated Property hat Migration oder Begründung.

---

## 12. Kernklassen und Methoden: GRETL-Zwischenmodell

### 12.1 `GretlScript`

```java
public record GretlScript(
    String uri,
    List<GretlTaskBlock> tasks,
    List<DefaultTaskDeclaration> defaultTasks,
    List<GretlVariableDeclaration> variables,
    List<GretlParseProblem> parseProblems,
    boolean astBased,
    boolean scannerFallbackUsed
) {
    public Optional<GretlTaskBlock> taskAt(Position position);
    public Optional<GretlTaskBlock> taskByName(String name);
    public Set<String> taskNames();
}
```

### 12.2 `GretlTaskBlock`

```java
public record GretlTaskBlock(
    String name,
    Optional<String> typeName,
    Range nameRange,
    Range typeRange,
    Range fullRange,
    Range bodyRange,
    List<GretlDslCall> calls,
    List<GretlDependency> dependencies,
    List<GretlExpression> rawExpressions
) {
    public List<GretlDslCall> callsByName(String name);
    public boolean hasCall(String name);
}
```

### 12.3 `GretlDslCall`

```java
public record GretlDslCall(
    String name,
    DslCallStyle style,
    Range nameRange,
    Range fullRange,
    List<GretlArgument> arguments,
    String sourceText
) {}
```

`DslCallStyle`:

```java
public enum DslCallStyle {
    METHOD_CALL,
    ASSIGNMENT,
    SET_METHOD,
    UNKNOWN
}
```

### 12.4 `GretlArgument`

```java
public record GretlArgument(
    GretlExpression expression,
    Range range,
    Optional<String> name
) {}
```

Named Map Arguments, z. B. `dataset: 'Olten'`, müssen als Name `dataset` und Ausdruck `'Olten'` modelliert werden.

### 12.5 `GretlExpression`

```java
public sealed interface GretlExpression permits StringLiteralExpression, BooleanLiteralExpression,
    NumberLiteralExpression, VariableExpression, MethodCallExpression, ListExpression,
    MapExpression, RangeExpression, UnknownExpression {
    Range range();
    String sourceText();
}
```

Relevante Implementierungen:

```java
public record StringLiteralExpression(String value, Range range, String sourceText) implements GretlExpression {}
public record BooleanLiteralExpression(boolean value, Range range, String sourceText) implements GretlExpression {}
public record NumberLiteralExpression(String value, Range range, String sourceText) implements GretlExpression {}
public record VariableExpression(String name, Range range, String sourceText) implements GretlExpression {}
public record MethodCallExpression(String name, List<GretlArgument> arguments, Range range, String sourceText) implements GretlExpression {}
public record ListExpression(List<GretlExpression> values, Range range, String sourceText) implements GretlExpression {}
public record MapExpression(List<MapEntryExpression> entries, Range range, String sourceText) implements GretlExpression {}
public record UnknownExpression(Range range, String sourceText) implements GretlExpression {}
```

### 12.6 `GretlDependency`

```java
public record GretlDependency(
    DependencyKind kind,
    String targetTaskName,
    Range range
) {}
```

`DependencyKind`:

```java
public enum DependencyKind {
    DEPENDS_ON,
    FINALIZED_BY,
    MUST_RUN_AFTER,
    SHOULD_RUN_AFTER
}
```

---

## 13. Kernklassen und Methoden: Parser

### 13.1 `GretlScriptParser`

```java
public interface GretlScriptParser {
    GretlScript parse(String uri, String text);
}
```

### 13.2 `HybridGretlScriptParser`

```java
public final class HybridGretlScriptParser implements GretlScriptParser {
    private final GroovyAstGretlParser astParser;
    private final LenientGretlScanner scanner;

    @Override
    public GretlScript parse(String uri, String text);
}
```

Algorithmus:

1. Versuche AST-Parser.
2. Wenn AST-Parser erfolgreich und mindestens eine plausible GRETL-Struktur gefunden: AST-Ergebnis zurückgeben.
3. Wenn AST-Parser fehlschlägt oder keine Struktur findet: Scanner-Ergebnis zurückgeben.
4. Parse-Probleme aus AST und Scanner im Ergebnis behalten.

### 13.3 `GroovyAstGretlParser`

```java
public final class GroovyAstGretlParser implements GretlScriptParser {
    public GretlScript parse(String uri, String text);
}
```

Interne Methoden:

```java
private ModuleNode parseGroovyModule(String uri, String text) throws GretlParseException;
private List<GretlTaskBlock> extractTasks(ModuleNode module, LineIndex lineIndex, String text);
private List<DefaultTaskDeclaration> extractDefaultTasks(ModuleNode module, LineIndex lineIndex, String text);
private List<GretlVariableDeclaration> extractVariables(ModuleNode module, LineIndex lineIndex, String text);
```

Anforderungen:

- Groovy nur bis zu einer frühen Phase parsen, sodass keine Gradle-Klassenauflösung nötig ist.
- Imports und unbekannte Klassen dürfen kein Fehler sein.
- Source Positions müssen in LSP-Ranges übersetzt werden.
- Wenn Positionen fehlen, Fallback über Textsuche in aktuellem Block.

### 13.4 `TaskRegistrationExtractor`

```java
public final class TaskRegistrationExtractor {
    public Optional<GretlTaskBlock> fromMethodCall(MethodCallExpression expression, ExtractionContext ctx);
}
```

Muss erkennen:

```groovy
tasks.register('name', TaskType) { ... }

tasks.register("name", TaskType) { ... }

tasks.register('name', ch.so.agi.gretl.tasks.TaskType) { ... }

task name(type: TaskType) { ... }

task('name', type: TaskType) { ... }
```

Muss im MVP nicht vollständig unterstützen:

```groovy
def taskType = SqlExecutor
tasks.register('name', taskType) { ... }
```

Dafür soll es eine Info-Diagnostic geben können:

```text
GRETL-LSP kann dynamisch berechnete Task-Typen nicht vollständig analysieren.
```

### 13.5 `DslCallExtractor`

```java
public final class DslCallExtractor {
    public List<GretlDslCall> extractCalls(ClosureExpression closure, ExtractionContext ctx);
}
```

Muss erkennen:

```groovy
database url, usr, pwd
sqlFiles files('demo.sql')
sqlParameters dataset: 'Olten'
models 'DM01AVSO24LV95'

database = [url, usr, pwd]
sqlFiles = files('demo.sql')

database.set([url, usr, pwd])
```

Muss Gradle-Standardcalls als rohe Expressions behalten, aber nicht als GRETL-Property-Diagnostic markieren, z. B.:

```groovy
dependsOn 'download'
doLast { println 'done' }
onlyIf { ... }
```

### 13.6 `DependencyExtractor`

```java
public final class DependencyExtractor {
    public List<GretlDependency> extract(ClosureExpression closure, ExtractionContext ctx);
}
```

Muss erkennen:

```groovy
dependsOn 'a'
dependsOn 'a', 'b'
dependsOn tasks.named('a')
finalizedBy 'cleanup'
mustRunAfter 'importData'
shouldRunAfter 'validateData'
```

Für dynamische Abhängigkeiten soll `UnknownExpression` verwendet werden.

### 13.7 `LenientGretlScanner`

```java
public final class LenientGretlScanner implements GretlScriptParser {
    @Override
    public GretlScript parse(String uri, String text);
}
```

Methoden:

```java
private List<ScannedTaskHeader> scanTaskHeaders(String text, LineIndex lineIndex);
private Optional<Range> findClosureRange(int startOffset, String text, LineIndex lineIndex);
private List<GretlDslCall> scanCallsInside(Range bodyRange, String text, LineIndex lineIndex);
private List<GretlDependency> scanDependenciesInside(Range bodyRange, String text, LineIndex lineIndex);
private List<DefaultTaskDeclaration> scanDefaultTasks(String text, LineIndex lineIndex);
```

Der Scanner darf regex-basiert sein, muss aber Tests für unfertige Groovy-Zustände haben:

```groovy
tasks.register('x', SqlExecutor) {
    database dbUri,
```

```groovy
tasks.register('x', SqlExecutor) {
    sql
```

```groovy
tasks.register('x', SqlExecutor) {
```

---

## 14. Kernklassen und Methoden: Analyse

### 14.1 `GretlAnalyzer`

```java
public final class GretlAnalyzer {
    private final GretlScriptParser parser;
    private final GretlMetadataProvider metadataProvider;
    private final List<GretlDiagnosticRule> diagnosticRules;

    public AnalysisResult analyze(TextDocument document, WorkspaceContext workspaceContext);
}
```

### 14.2 `AnalysisResult`

```java
public record AnalysisResult(
    TextDocument document,
    GretlScript script,
    GretlMetadata metadata,
    List<Diagnostic> diagnostics,
    TaskGraph taskGraph,
    SqlParameterReport sqlParameterReport
) {}
```

### 14.3 `AnalysisCache`

```java
public final class AnalysisCache {
    public Optional<AnalysisResult> get(String uri, int version);
    public void put(String uri, int version, AnalysisResult result);
    public void invalidate(String uri);
    public void clear();
}
```

MVP kann ohne Cache starten. Ab Phase 6 oder 8 sinnvoll.

### 14.4 `WorkspaceContext`

```java
public record WorkspaceContext(
    Path workspaceRoot,
    Optional<Path> gradleProperties,
    Map<String, String> knownProjectProperties,
    Map<String, TextDocument> openSqlDocuments
) {}
```

---

## 15. Diagnostics-Regeln

Alle Diagnostic-Regeln implementieren:

```java
public interface GretlDiagnosticRule {
    List<Diagnostic> evaluate(AnalysisInput input);
}
```

```java
public record AnalysisInput(
    TextDocument document,
    GretlScript script,
    GretlMetadata metadata,
    WorkspaceContext workspaceContext
) {}
```

### 15.1 `UnknownTaskTypeRule`

Fehler, wenn ein Task-Typ statisch erkannt wird, aber nicht im Manifest existiert und nicht als bekannter Gradle-Task ignoriert wird.

Severity:

- `Warning` für unbekannte Task-Typen, weil es externe Gradle-Tasks geben kann.
- `Information`, wenn Task-Typ dynamisch ist.

Methode:

```java
private boolean isKnownExternalGradleTask(String typeName)
```

Bekannte externe Tasks im MVP:

- `Copy`
- `Delete`
- `Sync`
- `Zip`
- `Exec`
- `JavaExec`
- `Download`, falls `de.undercouch.gradle.tasks.download.Download` häufig verwendet wird

### 15.2 `MissingRequiredPropertyRule`

Fehler, wenn ein GRETL-Task einen Pflichtparameter laut Metadaten nicht setzt.

Methoden:

```java
private Set<String> presentPropertyNames(GretlTaskBlock task)
private Range diagnosticRange(GretlTaskBlock task)
private Diagnostic createMissingPropertyDiagnostic(GretlTaskBlock task, PropertyMetadata property)
```

Beispiel:

```text
GRETL1001: Pflichtparameter `sqlFiles` fehlt für Task `SqlExecutor`.
```

### 15.3 `UnknownPropertyRule`

Warnung, wenn ein Call innerhalb eines GRETL-Task-Blocks weder:

- GRETL-Property laut Metadaten,
- bekannte Gradle-Task-Methode,
- bekannte Groovy-/Gradle-Konfigurationsmethode

ist.

Tippfehler-Erkennung mit Levenshtein-Distanz:

```java
private Optional<String> suggestClosestProperty(String unknown, TaskMetadata taskMetadata)
```

Beispiel:

```groovy
sqlFile files('demo.sql')
```

Diagnostic:

```text
GRETL1002: Unbekannte Property `sqlFile`. Meintest du `sqlFiles`?
```

### 15.4 `WrongArgumentCountRule`

Prüft argumentCount aus `AcceptedForm`.

Beispiel:

```groovy
database dbUri, dbUser
```

Diagnostic:

```text
GRETL1003: `database` erwartet 3 Argumente: database url, user, password.
```

### 15.5 `UnknownDependencyRule`

Prüft `dependsOn`, `finalizedBy`, `mustRunAfter`, `shouldRunAfter` auf vorhandene Task-Namen im Dokument.

Beispiel:

```groovy
dependsOn 'importDat'
```

Diagnostic:

```text
GRETL1101: Task `importDat` existiert nicht. Meintest du `importData`?
```

### 15.6 `DefaultTaskRule`

Prüft `defaultTasks`.

```text
GRETL1102: defaultTasks verweist auf unbekannten Task `validateData`.
```

### 15.7 `DuplicateTaskNameRule`

Fehler bei doppelten Task-Namen.

```text
GRETL1103: Task-Name `importData` ist mehrfach definiert.
```

### 15.8 `FileReferenceRule`

Prüft Properties mit `file`-Metadaten.

Muss erkennen:

```groovy
sqlFiles files('src/main/sql/transform.sql')
dataFile file('data.xtf')
dataFile files('a.xtf', 'b.xtf')
dataFile fileTree('data') { include '*.xtf' }
```

MVP:

- String-Literale in `file(...)` / `files(...)` prüfen.
- Relative Pfade gegen Workspace Root oder aktuelle `build.gradle`-Directory auflösen.
- Fehlende Datei als Warning.
- Falsche Extension als Warning.

Nicht MVP:

- Komplexe Expressions auswerten.

### 15.9 `SqlParameterRule`

Prüft bei Tasks mit `sqlFiles` und `sqlParameters`:

- SQL-Dateien lesen.
- `${paramName}` extrahieren.
- `sqlParameters` Map-Keys extrahieren.
- Fehlende Parameter warnen.
- Unused Parameter informieren.

Muss unterstützen:

```groovy
sqlParameters dataset: 'Olten'
sqlParameters [dataset: 'Olten']
sqlParameters [[dataset:'Olten'], [dataset:'Grenchen']]
```

Methoden:

```java
private Set<String> extractUsedSqlParameters(Path sqlFile)
private Set<String> extractProvidedSqlParameters(GretlDslCall sqlParametersCall)
private List<Diagnostic> compare(Set<String> used, Set<String> provided)
```

SQL-Extraktion:

```java
public final class SqlParameterExtractor {
    public Set<SqlParameterOccurrence> extract(String sqlText);
}
```

Regex im MVP:

```text
\$\{([A-Za-z_][A-Za-z0-9_]*)\}
```

### 15.10 `LegacyDslRule`

Findet alte Schreibweise:

```groovy
database = [db_uri, db_user, db_pass]
sqlFiles = files('demo.sql')
```

Severity:

- `Hint` oder `Information`, nicht Error.

Diagnostic:

```text
GRETL1201: Alte GRETL-DSL-Schreibweise. Quick Fix kann zu `database db_uri, db_user, db_pass` migrieren.
```

---

## 16. Completion

### 16.1 `CompletionProvider`

```java
public final class CompletionProvider {
    public Either<List<CompletionItem>, CompletionList> complete(CompletionParams params, AnalysisResult analysis);
}
```

### 16.2 Completion-Kontexte

```java
public enum CompletionContextKind {
    TASK_TYPE,
    INSIDE_GRETl_TASK_BODY,
    DEPENDENCY_TASK_NAME,
    FILE_PATH,
    SQL_PARAMETER_NAME,
    TOP_LEVEL,
    UNKNOWN
}
```

`CompletionContextDetector`:

```java
public final class CompletionContextDetector {
    public CompletionContext detect(TextDocument document, GretlScript script, Position position);
}
```

### 16.3 Task-Type Completion

Bei:

```groovy
tasks.register('x', |)
```

Liefert alle TaskMetadata:

- `SqlExecutor`
- `DuckDbSqlExecutor`
- `IliValidator`
- `CsvImport`
- etc.

CompletionItem:

- `label`: Task-Name
- `kind`: Class
- `detail`: Kategorie + Status
- `documentation`: Beschreibung + wichtigste Pflichtfelder
- `insertText`: Task-Name

### 16.4 Property-/DSL-Completion

Bei:

```groovy
tasks.register('x', SqlExecutor) {
    |
}
```

Liefert nur Properties aus `SqlExecutor`.

Sortierung:

1. fehlende Pflichtfelder
2. optionale häufige Felder
3. weitere optionale Felder
4. deprecated ganz unten

CompletionItem für `sqlFiles`:

- `label`: `sqlFiles`
- `insertTextFormat`: Snippet
- `insertText`: `sqlFiles files('${1:script.sql}')`
- `documentation`: Beschreibung

### 16.5 Dependency Completion

Bei:

```groovy
dependsOn '|'
```

Liefert Task-Namen aus dem Dokument.

### 16.6 File Path Completion

Optional MVP+, ab Phase 6:

Bei:

```groovy
sqlFiles files('src/main/sql/|')
```

Liefert `.sql`-Dateien.

### 16.7 SQL Parameter Completion

Bei:

```groovy
sqlParameters |
```

Wenn `sqlFiles` auf vorhandene SQL-Dateien zeigt, schlage verwendete SQL-Parameter vor:

```groovy
sqlParameters dataset: '${1:Olten}'
```

---

## 17. Hover

### 17.1 `HoverProvider`

```java
public final class HoverProvider {
    public Optional<Hover> hover(TextDocument document, AnalysisResult analysis, Position position);
}
```

Hover-Fälle:

- Task-Typ: Beschreibung, Status, Pflichtfelder, Beispiel.
- Property/DSL-Call: Typ, Pflicht, Beschreibung, Signaturen.
- Dependency-Taskname: Task-Typ und kurze Zusammenfassung.
- SQL-Parameter: Wo definiert / wo verwendet.

Beispiel Hover für `sqlFiles`:

```markdown
**sqlFiles**  
Typ: `Property<FileCollection>`  
Pflicht: ja  

SQL-Dateien, deren Statements gelesen und ausgeführt werden. Die Reihenfolge der Dateien ist relevant.

Beispiel:
```groovy
sqlFiles files('demo.sql')
```
```

---

## 18. Signature Help

### 18.1 `SignatureHelpProvider`

```java
public final class SignatureHelpProvider {
    public Optional<SignatureHelp> signatureHelp(TextDocument document, AnalysisResult analysis, Position position);
}
```

Muss bei methodenartiger DSL helfen:

```groovy
database dbUri, |
```

Signatur:

```text
database url, user, password
```

Aktiver Parameter: `user`.

Methode:

```java
private int activeParameterIndex(String lineText, int character)
```

MVP genügt zeilenbasiert.

---

## 19. Document Symbols und Outline

### 19.1 `DocumentSymbolProvider`

```java
public final class DocumentSymbolProvider {
    public List<Either<SymbolInformation, DocumentSymbol>> symbols(AnalysisResult analysis);
}
```

Soll für jeden Task ein Symbol liefern:

```text
executeSql : DuckDbSqlExecutor
importData : Ili2duckdbImport
```

Optional Child-Symbole:

- `database`
- `sqlFiles`
- `dependsOn importData`

---

## 20. Document Links

### 20.1 `DocumentLinkProvider`

```java
public final class DocumentLinkProvider {
    public List<DocumentLink> links(AnalysisResult analysis, WorkspaceContext workspaceContext);
}
```

Links:

- `sqlFiles files('src/main/sql/transform.sql')` → Datei öffnen
- `dataFile file('data.xtf')` → Datei öffnen
- `models 'SomeModel'` später optional → Modellquelle öffnen, wenn lokal bekannt

---

## 21. Task Graph und Overview-Modell

### 21.1 `TaskGraphBuilder`

```java
public final class TaskGraphBuilder {
    public TaskGraph build(GretlScript script);
}
```

### 21.2 `TaskGraph`

```java
public record TaskGraph(
    List<TaskGraphNode> nodes,
    List<TaskGraphEdge> edges,
    List<TaskGraphProblem> problems
) {
    public Optional<TaskGraphNode> findNode(String taskName);
}
```

### 21.3 `TaskGraphNode`

```java
public record TaskGraphNode(
    String taskName,
    Optional<String> taskType,
    Range range,
    NodeStatus status,
    List<String> missingRequiredProperties,
    int diagnosticCount
) {}
```

### 21.4 `TaskGraphEdge`

```java
public record TaskGraphEdge(
    String fromTask,
    String toTask,
    DependencyKind kind
) {}
```

Kantenrichtung:

```text
A --dependsOn--> B bedeutet fachlich: B läuft vor A.
```

Für Anzeige kann die Extension die Richtung als Pipeline-Reihenfolge invertieren:

```text
B → A
```

### 21.5 `TaskOverviewService`

```java
public final class TaskOverviewService {
    public GretlOverview overview(AnalysisResult analysis);
}
```

### 21.6 `GretlOverview`

```java
public record GretlOverview(
    String uri,
    List<GretlOverviewTask> tasks,
    TaskGraph graph,
    List<GretlOverviewDiagnostic> diagnostics,
    SqlParameterReport sqlParameterReport
) {}
```

Dieses Modell wird per `workspace/executeCommand` an die VS-Code-Extension geliefert.

---

## 22. VS-Code-Extension

### 22.1 Struktur

```text
vscode/gretl-vscode/
├── package.json
├── tsconfig.json
├── src/
│   ├── extension.ts
│   ├── languageServer.ts
│   ├── commands.ts
│   ├── gretlExplorer.ts
│   ├── overviewWebview.ts
│   ├── graphRenderer.ts
│   ├── config.ts
│   └── logging.ts
├── server/
│   └── gretl-lsp-all.jar
├── test/
│   ├── extension.test.ts
│   └── fixtures/
├── .vscode/
│   ├── launch.json
│   └── tasks.json
└── README.md
```

### 22.2 `package.json` Contributions

Muss enthalten:

- `main`: `./dist/extension.js`
- `activationEvents`:
  - `workspaceContains:**/build.gradle`
  - `workspaceContains:**/*.gradle`
  - `onCommand:gretl.restartLanguageServer`
  - `onCommand:gretl.showLanguageServerLogs`
  - `onCommand:gretl.openOverview`
  - `onCommand:gretl.runTaskAtCursor`
- `contributes.commands`
- `contributes.configuration`
- `contributes.viewsContainers`
- `contributes.views`

Settings:

```json
{
  "gretl.java.path": {
    "type": "string",
    "default": "",
    "markdownDescription": "Path to Java executable used to start the GRETL language server. Empty means use java from PATH."
  },
  "gretl.server.jarPath": {
    "type": "string",
    "default": "",
    "markdownDescription": "Path to GRETL language server fat JAR. Empty means bundled server/gretl-lsp-all.jar."
  },
  "gretl.server.jvmArgs": {
    "type": "array",
    "items": { "type": "string" },
    "default": [],
    "markdownDescription": "Additional JVM arguments passed to the GRETL language server."
  },
  "gretl.trace.server": {
    "type": "string",
    "enum": ["off", "messages", "verbose"],
    "default": "off"
  }
}
```

### 22.3 `extension.ts`

Methoden:

```ts
export async function activate(context: vscode.ExtensionContext): Promise<void>
export async function deactivate(): Promise<void>
```

`activate` muss:

1. Output Channel erzeugen.
2. Config lesen.
3. Language Client starten.
4. Commands registrieren.
5. Tree Provider registrieren.
6. Statusbar optional registrieren.

### 22.4 `languageServer.ts`

```ts
export class GretlLanguageClientController {
    constructor(private readonly context: vscode.ExtensionContext, private readonly output: vscode.OutputChannel) {}
    start(): Promise<void>;
    stop(): Promise<void>;
    restart(): Promise<void>;
    get client(): LanguageClient | undefined;
}
```

Methoden:

```ts
private resolveJavaCommand(): string
private resolveServerJar(): vscode.Uri
private buildServerOptions(): ServerOptions
private buildClientOptions(): LanguageClientOptions
```

Serverstart:

```text
<java> <jvmArgs> -jar <serverJar> --stdio
```

Document Selector:

- Sprache `groovy`, Scheme `file`, Pattern `**/*.gradle`
- Optional auch `build.gradle` explizit

### 22.5 Commands

```ts
registerCommand('gretl.restartLanguageServer', ...)
registerCommand('gretl.showLanguageServerLogs', ...)
registerCommand('gretl.openOverview', ...)
registerCommand('gretl.runTaskAtCursor', ...)
registerCommand('gretl.refreshOverview', ...)
registerCommand('gretl.copyTaskName', ...)
```

### 22.6 GRETL Explorer

Tree View in Activity Bar:

```text
GRETL
  build.gradle
    defaultTasks: executeSql
    Tasks
      importData        Ili2duckdbImport
      executeSql        DuckDbSqlExecutor
    SQL Files
      transform.sql
    Problems
      Missing sqlParameters.dataset
```

Klassen:

```ts
export class GretlExplorerProvider implements vscode.TreeDataProvider<GretlTreeItem> {
    getTreeItem(element: GretlTreeItem): vscode.TreeItem;
    getChildren(element?: GretlTreeItem): Thenable<GretlTreeItem[]>;
    refresh(): void;
}
```

### 22.7 Overview Webview

Read-only Webview, inspiriert von ilimap Mapping Overview, aber GRETL-spezifisch:

- Pipeline-Graph
- Task-Liste
- Pflichtfelder-Status
- Dependencies
- SQL-Dateien
- SQL-Parameter
- Diagnostics gruppiert nach Task

Klassen:

```ts
export class GretlOverviewPanel {
    static openOrReveal(context: vscode.ExtensionContext, client: LanguageClient, documentUri: vscode.Uri): Promise<void>;
    update(model: GretlOverviewModel): void;
    dispose(): void;
}
```

MVP kann Graph als einfache HTML/SVG-Liste darstellen. Keine externe Graph-Library im MVP, ausser bereits vorhanden und bewusst entschieden.

---

## 23. Build, Fat-JAR und lokale Entwicklung

### 23.1 Gradle Tasks

Im Root-Build sollen folgende Tasks existieren:

```text
./gradlew :gretl-lsp:test
./gradlew :gretl-lsp:shadowJar
./gradlew copyDevGretlServerJar
```

Falls Shadow Plugin nicht gewünscht ist, kann Gradle Application Plugin plus Distribution verwendet werden. Für die VS-Code-Extension ist ein einzelner JAR aber deutlich einfacher.

### 23.2 Fat-JAR

Zielpfad:

```text
gretl-lsp/build/libs/gretl-lsp-all.jar
```

Stage Task:

```text
copyDevGretlServerJar
```

Kopiert nach:

```text
vscode/gretl-vscode/server/gretl-lsp-all.jar
```

### 23.3 Lokale Extension-Entwicklung mit F5

Die Dokumentation muss exakt beschreiben:

```bash
# 1. Server bauen und in Extension kopieren
./gradlew copyDevGretlServerJar

# 2. VS-Code-Extension öffnen
cd vscode/gretl-vscode
npm install
npm run build
code .

# 3. In VS Code F5 drücken
# Launch configuration: "Run GRETL Extension"

# 4. Im Extension Development Host ein GRETL-Projekt öffnen
# build.gradle öffnen
# Completion, Diagnostics, Hover testen
```

`.vscode/launch.json` in der Extension:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Run GRETL Extension",
      "type": "extensionHost",
      "request": "launch",
      "args": ["--extensionDevelopmentPath=${workspaceFolder}"],
      "outFiles": ["${workspaceFolder}/dist/**/*.js"],
      "preLaunchTask": "npm: build"
    }
  ]
}
```

`.vscode/tasks.json`:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "npm: build",
      "type": "npm",
      "script": "build",
      "group": "build",
      "problemMatcher": "$tsc"
    }
  ]
}
```

---

## 24. Teststrategie

### 24.1 Unit Tests Java

Jede zentrale Klasse braucht Unit Tests.

Pflicht-Testklassen:

```text
LineIndexTest
MetadataLoaderTest
MetadataValidatorTest
GroovyAstGretlParserTest
LenientGretlScannerTest
TaskRegistrationExtractorTest
DslCallExtractorTest
DependencyExtractorTest
MissingRequiredPropertyRuleTest
UnknownPropertyRuleTest
WrongArgumentCountRuleTest
UnknownDependencyRuleTest
FileReferenceRuleTest
SqlParameterExtractorTest
SqlParameterRuleTest
CompletionProviderTest
HoverProviderTest
SignatureHelpProviderTest
DocumentSymbolProviderTest
DocumentLinkProviderTest
TaskGraphBuilderTest
```

### 24.2 LSP-Protokolltests

Ein Testkit soll direkte LSP-Requests gegen den Server ausführen, mindestens:

- initialize
- didOpen
- completion
- hover
- signatureHelp
- documentSymbol
- documentLink
- diagnostics

Klasse:

```java
public final class LspTestClient {
    public InitializeResult initialize();
    public void open(String uri, String text);
    public List<Diagnostic> diagnostics(String uri);
    public List<CompletionItem> completion(String uri, Position position);
    public Optional<Hover> hover(String uri, Position position);
}
```

### 24.3 Golden Tests

Für komplexe Beispiele sollen Golden Files verwendet werden:

```text
gretl-lsp/src/test/resources/golden/simple-sql-executor/build.gradle
gretl-lsp/src/test/resources/golden/simple-sql-executor/expected-diagnostics.json
gretl-lsp/src/test/resources/golden/simple-sql-executor/expected-symbols.json
```

### 24.4 Extension Tests

Node-basierte Tests im MVP:

- Config Resolver
- Java Command Resolver
- Server Jar Resolver
- HTML escaping in Webview
- Overview model rendering pure functions

Später optional VS-Code Extension Integration Tests.

### 24.5 Smoke Test Projekt

Ein kleines Beispielprojekt:

```text
docs/lsp/examples/simple-gretl-job/
├── build.gradle
├── gradle.properties
└── src/main/sql/transform.sql
```

Soll für manuelle Tests und CI-Smoke verwendet werden.

---

## 25. Phasenplan

# Phase 0: Arbeitsgrundlage und Projektgerüst

## Ziel

Repository auf LSP-Arbeiten vorbereiten: Dokumentationsstruktur, Phase Tracking, leere oder minimale Subprojekte, lokale Agenten-Regeln respektieren.

## Artefakt

- `docs/lsp/PHASE_STATUS.md`
- `docs/lsp/README.md`
- `docs/lsp/DECISIONS.md`
- leeres/minimales `gretl-lsp`-Subprojekt
- optional leeres/minimales `vscode/gretl-vscode`

## Implementierungsanweisung

1. Lies zuerst vorhandene Agent-Instruktionen:
   - `AGENTS.md`
   - `CLAUDE.md`, falls vorhanden
   - `.skills/**/SKILL.md`, soweit relevant
   - bestehende `docs/agent/*`, falls vorhanden

2. Prüfe `settings.gradle` und ergänze neue Subprojekte nur minimal:

```groovy
include 'gretl-lsp'
```

Später:

```groovy
// VS-Code-Extension ist kein Gradle-Subprojekt, ausser das Repository nutzt bereits Gradle-Tasks dafür.
```

3. Erzeuge `docs/lsp/PHASE_STATUS.md` mit allen Phasen.

4. Erzeuge `docs/lsp/README.md` mit kurzer Zielbeschreibung.

5. Erzeuge `docs/lsp/DECISIONS.md` mit erstem Eintrag:

```markdown
# Decisions

## 0001 - GRETL-LSP als semantischer LSP für Gradle-Groovy

Der LSP implementiert keine vollständige Groovy-/Gradle-IDE, sondern erkennt GRETL-Task-Blöcke und wertet diese mit Doclet-Metadaten aus.
```

6. Lege `gretl-lsp/build.gradle` minimal an:

```groovy
plugins {
    id 'java-library'
    id 'application'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.4'
}

test {
    useJUnitPlatform()
}

application {
    mainClass = 'ch.so.agi.gretl.lsp.server.GretlServerLauncher'
}
```

Versionen müssen an existierende Repository-Konventionen angepasst werden. Wenn zentrale Versionen existieren, diese verwenden.

## Tests

- `./gradlew :gretl-lsp:test`
- falls Root-Build betroffen: `./gradlew test` oder kleinster relevanter Check gemäss AGENTS.md

## Dokumentation

- `docs/lsp/README.md`
- `docs/lsp/PHASE_STATUS.md`
- `docs/lsp/DECISIONS.md`

## Definition of Done

- Projekt baut.
- Phase 0 in `PHASE_STATUS.md` mindestens `implemented`, nach Tests `done`.
- Keine opportunistischen Refactorings.

## Agenten-Prompt

```text
Setze Phase 0 aus der Spezifikation `docs/design/gretl-lsp-agent-spec.md` um. Lies zuerst alle lokalen Agent-Instruktionen wie AGENTS.md, CLAUDE.md und relevante `.skills/**/SKILL.md`. Arbeite eng an der Phase: erstelle die LSP-Dokumentationsstruktur, das Phase-Tracking und ein minimales `gretl-lsp`-Projekt. Führe die kleinsten relevanten Tests aus, aktualisiere `docs/lsp/PHASE_STATUS.md` und dokumentiere alle Abweichungen in `docs/lsp/DECISIONS.md`. Behaupte keine erfolgreichen Tests, wenn du sie nicht ausgeführt hast.
```

---

# Phase 1: `gretl-doclet` um Metadatenformat v1 erweitern
> **Aktualisierung aus Quellenanalyse:** Phase 1 muss auf dem aktuellen `gretl-doclet` mit `GretlDoclet`, `TaskDescriptorExtractor`, `AsciiDocRenderer` und den bestehenden Model-Records aufbauen. Kein Rewrite. Zusätzlich zu AsciiDoc wird JSON erzeugt. Phase 1 darf noch einfache direkte Methoden ausgeben, muss aber das JSON-Modell bereits so vorbereiten, dass Phase 2 `effectiveMethods`, `requiredGroups`, `conflicts`, `sensitive` und `contexts` ergänzen kann.



## Ziel

Das bestehende `gretl-doclet` erzeugt zusätzlich zur menschlichen Dokumentation ein maschinenlesbares Manifest `gretl-lsp-metadata.json` und ein JSON Schema.

## Artefakt

- `gretl-lsp-metadata.json`
- `gretl-lsp-metadata.schema.json`
- Tests für Doclet-Metadaten
- `docs/lsp/METADATA_FORMAT.md`

## Implementierungsanweisung

### 1. Bestehendes Doclet verstehen

Der Agent muss zuerst das bestehende `gretl-doclet` analysieren:

- Einstiegsklasse des Doclets finden.
- Datenmodell der bestehenden Dokumentation finden.
- Task-Klassen-Erkennung finden.
- Property-Erkennung finden.
- Optional-/Pflicht-Logik finden.
- Bestehende Tests finden.

Kein Rewrite des Doclets. Nur erweitern.

### 2. Neues Doclet-Modell anlegen

Paketvorschlag im Doclet:

```java
package ch.so.agi.gretl.doclet.lsp;
```

Klassen:

```java
public record LspMetadataDocument(
    String schemaVersion,
    String generatedAt,
    String gretlVersion,
    LspMetadataSource source,
    List<LspTaskMetadata> tasks
) {}
```

```java
public record LspTaskMetadata(
    String name,
    String qualifiedClassName,
    String simpleClassName,
    String category,
    String status,
    String description,
    String longDescription,
    List<LspExample> examples,
    List<LspPropertyMetadata> properties
) {}
```

```java
public record LspPropertyMetadata(
    String name,
    String displayName,
    String kind,
    String valueType,
    String javaType,
    boolean required,
    boolean deprecated,
    String description,
    LspFileMetadata file,
    List<LspAcceptedForm> acceptedForms,
    LspMigrationMetadata migration,
    boolean sqlParameterProvider
) {}
```

```java
public record LspAcceptedForm(
    String style,
    String signature,
    String insertText,
    Integer argumentCount,
    boolean legacy
) {}
```

```java
public record LspFileMetadata(
    String role,
    List<String> extensions,
    boolean multiple,
    boolean mustExist
) {}
```

```java
public record LspMigrationMetadata(
    List<String> from,
    String to,
    String codeActionTitle
) {}
```

### 3. Metadaten-Builder

Klasse:

```java
public final class LspMetadataBuilder {
    public LspMetadataDocument build(DocletTaskCatalog taskCatalog, LspMetadataOptions options);
}
```

Falls es keinen `DocletTaskCatalog` gibt, vorhandenes Modell verwenden oder einen Adapter schreiben.

Methoden:

```java
private LspTaskMetadata toTaskMetadata(TaskDoc taskDoc)
private LspPropertyMetadata toPropertyMetadata(PropertyDoc propertyDoc, TaskDoc taskDoc)
private List<LspAcceptedForm> acceptedForms(PropertyDoc propertyDoc, TaskDoc taskDoc)
private LspFileMetadata inferFileMetadata(PropertyDoc propertyDoc)
private boolean isSqlParameterProvider(PropertyDoc propertyDoc)
private String inferCategory(TaskDoc taskDoc)
private String inferStatus(TaskDoc taskDoc)
```

### 4. Akzeptierte DSL-Formen erzeugen

Für jede Property soll mindestens die legacy assignment form erzeugt werden, solange bestehende GRETL-Jobs diese nutzen:

```groovy
propertyName = value
```

Für neue DSL zusätzlich method-call form:

```groovy
propertyName value
```

Für bekannte Sonderfälle:

- `database`: `database url, user, password`
- `sqlFiles`: `sqlFiles files('script.sql')`
- `sqlParameters`: `sqlParameters key: value`
- File Properties: `dataFile file('...')` oder `dataFile files('...')`
- Boolean: `deleteAllRows true`
- String: `models 'ModelName'`

Wenn keine Sonderform bekannt ist, generisch:

```json
{
  "style": "method-call",
  "signature": "propertyName value",
  "insertText": "propertyName ${1:value}",
  "argumentCount": 1
}
```

### 5. Optionale Annotationen vorbereiten

Falls automatische Inferenz nicht reicht, neue Annotationen einführen:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface GretlDsl {
    String name() default "";
    boolean required() default false;
    String valueType() default "";
    String[] signatures() default {};
    String[] examples() default {};
    String[] fileExtensions() default {};
    String fileRole() default "unknown";
    boolean sqlParameterProvider() default false;
}
```

Aber: Phase 1 soll möglichst ohne breite Annotationierung aller Tasks funktionieren. Annotationen nur dort ergänzen, wo Inferenz nicht zuverlässig ist.

### 6. JSON Writer

Klasse:

```java
public final class LspMetadataWriter {
    public void write(LspMetadataDocument document, Path outputFile) throws IOException;
}
```

Anforderungen:

- Pretty JSON.
- Stabile Sortierung: Tasks alphabetisch, Properties alphabetisch oder bestehende Doku-Reihenfolge, aber deterministisch.
- Keine nicht-deterministischen Werte in Tests, ausser `generatedAt` kann im Test fixiert werden.

### 7. JSON Schema

Datei:

```text
gretl-doclet/src/main/resources/gretl-lsp-metadata.schema.json
```

Muss mindestens Pflichtfelder validieren.

### 8. Gradle Integration

Ein Task oder Doclet-Option muss das Manifest erzeugen:

```bash
./gradlew :gretl-doclet:generateLspMetadata
```

Falls das Doclet nur im Rahmen eines bestehenden Doku-Tasks läuft, zusätzlich einen Copy-/Generate-Task erstellen.

## Tests

Pflichttests:

```text
LspMetadataBuilderTest
LspMetadataWriterTest
LspMetadataSchemaTest
```

Testfälle:

- `SqlExecutor` enthält `database`, `sqlFiles`, `sqlParameters`.
- `database` ist required.
- `sqlFiles` ist required.
- `sqlParameters` ist optional.
- `sqlParameters` hat `sqlParameterProvider=true`.
- File Properties enthalten plausible Extensions.
- Manifest ist deterministisch sortiert.
- Manifest validiert gegen JSON Schema.

## Dokumentation

`docs/lsp/METADATA_FORMAT.md` muss enthalten:

- Zweck des Formats.
- Wo es erzeugt wird.
- Beispiel JSON.
- Schema-Versionierung.
- Regeln für neue Task-Properties.
- Wann Annotationen nötig sind.

## Definition of Done

- Manifest kann lokal erzeugt werden.
- Manifest enthält mindestens die wichtigsten GRETL-Tasks.
- Tests laufen.
- LSP muss in späterer Phase keine HTML-Doku scrapen.

## Agenten-Prompt

```text
Setze Phase 1 aus `docs/design/gretl-lsp-agent-spec.md` um. Lies zuerst AGENTS.md, CLAUDE.md und relevante Skills. Erweitere `gretl-doclet`, ohne es neu zu schreiben, so dass ein stabiles `gretl-lsp-metadata.json` plus JSON Schema erzeugt wird. Implementiere Klassen auf Builder-/Writer-/Schema-Test-Ebene gemäss Spezifikation. Prüfe besonders `SqlExecutor` mit `database`, `sqlFiles` und `sqlParameters`. Führe die relevanten Gradle-Tests und den Metadaten-Generate-Task aus. Aktualisiere `docs/lsp/METADATA_FORMAT.md` und `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 2:
> **Aktualisierung aus Quellenanalyse:** Phase 2 ist nicht optional. Sie muss geerbte DSL-Methoden aus Basisklassen und Nested-Kontexte abbilden. Ohne diese Phase wären `CsvImport`, `FtpDownload`, `IliValidator`, `Ili2pg*` und `DuckDbSqlExecutor` im LSP fachlich falsch.

 LSP-Projekt, Metadaten-Loader und Server-Skelett

## Ziel

Ein startbarer Java-LSP-Fat-JAR existiert. Der Server kann initialisiert werden, lädt Metadaten und publiziert noch keine oder nur triviale Diagnostics.

## Artefakt

- `gretl-lsp` mit LSP4J-Server
- `GretlServerLauncher`
- `GretlLanguageServer`
- `MetadataLoader`
- Fat-JAR Task
- Tests für Initialize und Metadata Loading

## Implementierungsanweisung

### 1. Dependencies

Ergänze im `gretl-lsp`:

- LSP4J
- Jackson Databind
- JUnit Jupiter
- AssertJ optional, wenn im Repo üblich
- JSON Schema Validator optional, wenn in Phase 1 gewählt
- Shadow Plugin oder vergleichbare Fat-JAR-Lösung

### 2. Server-Skelett implementieren

Klassen:

- `GretlServerLauncher`
- `GretlLanguageServer`
- `GretlTextDocumentService`
- `GretlWorkspaceService`
- `ServerLifecycle`
- `GretlServerConfig`
- `ServerLogger`

### 3. Metadata Loader implementieren

Klassen aus Kapitel 11.

Default-Verhalten:

1. Wenn `--metadata=/path` gesetzt: Datei laden.
2. Sonst Resource aus JAR laden.
3. Wenn keine Resource vorhanden: minimales Empty-Metadata erzeugen und Warning loggen.

### 4. Capabilities

InitializeResult soll mindestens enthalten:

- TextDocumentSync full
- CompletionProvider mit `.`? Triggercharacters später, MVP leer
- HoverProvider true
- SignatureHelpProvider mit Triggercharacters `(`, `,`, space optional
- DocumentSymbolProvider true
- DocumentLinkProvider true

### 5. Fat-JAR

Task:

```bash
./gradlew :gretl-lsp:shadowJar
```

Output:

```text
gretl-lsp/build/libs/gretl-lsp-all.jar
```

### 6. Smoke Start

Der JAR muss ausführbar sein:

```bash
java -jar gretl-lsp/build/libs/gretl-lsp-all.jar --help
java -jar gretl-lsp/build/libs/gretl-lsp-all.jar --stdio
```

`--help` darf kein LSP starten, sondern Usage auf stdout ausgeben. `--stdio` darf keine Logs auf stdout schreiben.

## Tests

- `MetadataLoaderTest`
- `GretlLanguageServerInitializeTest`
- `GretlServerLauncherArgsTest`

## Dokumentation

- `docs/lsp/DEVELOPMENT.md`: Server bauen und starten.
- `docs/lsp/TESTING.md`: LSP-Tests.

## Definition of Done

- `./gradlew :gretl-lsp:test :gretl-lsp:shadowJar` läuft.
- JAR existiert.
- Initialize-Test prüft Capabilities.

## Agenten-Prompt

```text
Setze Phase 2 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Implementiere ein startbares Java-LSP-Skelett mit LSP4J, Metadaten-Loader und Fat-JAR-Erzeugung. Der Server muss initialize beantworten und Metadaten laden können. Er darf bei `--stdio` keine Logs auf stdout schreiben. Ergänze Tests und Dokumentation in `docs/lsp/DEVELOPMENT.md` und `docs/lsp/TESTING.md`. Aktualisiere `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 3: Groovy-AST-Parser und GRETL-Zwischenmodell

## Ziel

Der LSP kann GRETL-Task-Blöcke in Gradle-Groovy-Dateien erkennen und in ein eigenes Zwischenmodell übersetzen. Zusätzlich existiert ein toleranter Scanner für unfertigen Code.

## Artefakt

- Modellklassen aus Kapitel 12
- Parserklassen aus Kapitel 13
- Tests mit echten GRETL-Snippets

## Implementierungsanweisung

### 1. Groovy Dependency

Nicht `groovy-all.jar` als altes monolithisches JAR verwenden. Stattdessen gezielte Groovy-Module deklarieren:

```groovy
implementation 'org.apache.groovy:groovy:<version>'
```

Optional:

```groovy
implementation 'org.apache.groovy:groovy-astbuilder:<version>'
```

Version an Gradle-/Repo-Kontext anpassen. In `docs/lsp/DECISIONS.md` begründen.

### 2. Modellklassen implementieren

Alle Klassen aus Kapitel 12 als Java Records oder final Classes.

### 3. AST Parser implementieren

Implementiere:

- `GroovyAstGretlParser`
- `TaskRegistrationExtractor`
- `DslCallExtractor`
- `DependencyExtractor`
- `ExpressionConverter`
- `RangeConverter`

### 4. Scanner implementieren

Implementiere:

- `LenientGretlScanner`
- `HybridGretlScriptParser`

### 5. Scope

Erkenne mindestens:

```groovy
tasks.register('executeSql', SqlExecutor) {
    database dbUri, dbUser, dbPwd
    sqlFiles files('demo.sql')
}
```

```groovy
tasks.register('executeSql', SqlExecutor) {
    database = [dbUri, dbUser, dbPwd]
    sqlFiles = files('demo.sql')
}
```

```groovy
defaultTasks 'executeSql'
```

```groovy
tasks.register('executeSql', SqlExecutor) {
    dependsOn 'importData'
}
```

## Tests

Pflichttests:

- AST erkennt Task-Name und Typ.
- AST erkennt method-call DSL.
- AST erkennt assignment DSL.
- AST erkennt `dependsOn`.
- AST erkennt `defaultTasks`.
- Scanner erkennt unfertigen Task-Block.
- Hybrid fällt auf Scanner zurück bei kaputtem Code.

## Dokumentation

- `docs/lsp/DECISIONS.md`: Warum Groovy-AST plus Scanner.
- `docs/lsp/TESTING.md`: Parser-Teststrategie.

## Definition of Done

- Parser liefert GRETL-Zwischenmodell für Beispiel-Jobs.
- Tests decken gültige und unfertige Editorzustände ab.

## Agenten-Prompt

```text
Setze Phase 3 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Implementiere das GRETL-Zwischenmodell, den Groovy-AST-basierten Parser und den toleranten Scanner. Der LSP darf Gradle-Skripte nicht ausführen. Tests müssen gültige und unfertige Gradle-Groovy-Snippets abdecken. Dokumentiere die Parser-Entscheidung in `docs/lsp/DECISIONS.md` und aktualisiere `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 4: Diagnostics v1

## Ziel

Der LSP publiziert nützliche Diagnostics für GRETL-Task-Blöcke.

## Artefakt

- `GretlAnalyzer`
- Diagnostic Rules v1
- Diagnostic Publishing bei didOpen/didChange
- Tests

## Implementierungsanweisung

### 1. Analyzer implementieren

Klassen:

- `GretlAnalyzer`
- `AnalysisInput`
- `AnalysisResult`
- `DiagnosticPublisher`
- `DiagnosticCode`

### 2. Regeln implementieren

Mindestens:

- `MissingRequiredPropertyRule`
- `UnknownPropertyRule`
- `WrongArgumentCountRule`
- `UnknownDependencyRule`
- `DefaultTaskRule`
- `DuplicateTaskNameRule`
- `LegacyDslRule`

### 3. Diagnostic Codes

Definiere Codes:

```text
GRETL1001 Missing required property
GRETL1002 Unknown property
GRETL1003 Wrong argument count
GRETL1101 Unknown dependency task
GRETL1102 Unknown default task
GRETL1103 Duplicate task name
GRETL1201 Legacy DSL style
```

### 4. Publishing

Bei `didOpen` und `didChange`:

```java
client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
```

Bei `didClose` leeren:

```java
client.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of()));
```

## Tests

- Unit Tests pro Rule.
- LSP-Test: didOpen mit fehlendem `sqlFiles` publiziert Diagnostic.
- LSP-Test: Korrektur entfernt Diagnostic.

## Dokumentation

- `docs/lsp/TESTING.md`
- `docs/lsp/README.md`: erste Features.

## Definition of Done

- Diagnostics funktionieren in LSP-Test.
- Keine Diagnostics für unbekannte externe Gradle-Tasks wie `Copy`.
- Legacy DSL nur Hint/Info, nicht Error.

## Agenten-Prompt

```text
Setze Phase 4 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Implementiere Analyzer und Diagnostics v1 mit den spezifizierten Diagnostic Codes. Publiziere Diagnostics bei didOpen/didChange und lösche sie bei didClose. Ergänze Unit- und LSP-Protokolltests. Dokumentiere die verfügbaren Diagnostics und aktualisiere `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 5: Completion, Hover und Signature Help

## Ziel

Der LSP unterstützt kontextabhängige Completion, Hover und Signature Help für GRETL-Tasks und DSL-Calls.

## Artefakt

- `CompletionProvider`
- `HoverProvider`
- `SignatureHelpProvider`
- Tests

## Implementierungsanweisung

### 1. Completion-Kontext erkennen

Implementiere:

- `CompletionContextDetector`
- `CompletionContext`

Kontexte:

- Task-Type nach `tasks.register('name', |)`
- Task-Body
- Dependency String
- File Path optional später, aber vorbereiten

### 2. Completion-Provider

Implementiere:

- Task-Type Completion aus Metadata.
- Property Completion aus TaskMetadata.
- Dependency Completion aus Script Tasks.

### 3. Hover

Implementiere Hover für:

- Task-Typ
- Property/DSL-Call
- Dependency Task Name

### 4. Signature Help

Implementiere Signature Help für:

- `database url, usr, pwd`
- generische `property value`
- `sqlParameters key: value`

## Tests

- Completion in leerem Task-Body zeigt fehlende Pflichtfelder zuerst.
- Completion nach `tasks.register('x', ` zeigt GRETL-Task-Typen.
- Hover über `sqlFiles` enthält Typ und Pflichtstatus.
- Signature Help bei `database dbUri, ` setzt aktiven Parameter auf 1.

## Dokumentation

- `docs/lsp/README.md`: Feature-Liste.
- `docs/lsp/TESTING.md`: Completion/Hover-Tests.

## Definition of Done

- Completion/Hover/Signature Help funktionieren über LSP-Protokolltests.
- Keine hartcodierten Task-Listen ausser in Tests; echte Daten aus Manifest.

## Agenten-Prompt

```text
Setze Phase 5 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Implementiere kontextabhängige Completion, Hover und Signature Help aus `gretl-lsp-metadata.json`. Nutze keine hartcodierten Task-Listen im Produktionscode. Ergänze Unit- und LSP-Protokolltests und aktualisiere Dokumentation sowie `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 6: Document Symbols, Document Links und SQL-Parameteranalyse

## Ziel

Der LSP liefert Outline-Symbole, klickbare Links auf Dateien und SQL-Parameter-Diagnostics.

## Artefakt

- `DocumentSymbolProvider`
- `DocumentLinkProvider`
- `SqlParameterExtractor`
- `SqlParameterRule`
- Tests

## Implementierungsanweisung

### 1. Document Symbols

Für jeden Task:

```text
executeSql : SqlExecutor
```

Optional Child-Symbole für GRETL-Calls.

### 2. Document Links

Für File-Referenzen:

```groovy
sqlFiles files('src/main/sql/transform.sql')
```

Link auf Datei.

### 3. SQL Parameter

Extrahiere Parameter aus SQL:

```sql
DELETE FROM target WHERE dataset = ${dataset};
```

Vergleiche mit:

```groovy
sqlParameters dataset: 'Olten'
```

Diagnostics:

- Fehlender Parameter: Warning.
- Unused Parameter: Information.

## Tests

- Symbol-Test für drei Tasks.
- Document-Link-Test für SQL-Datei.
- SQL-Parameter-Test: fehlend.
- SQL-Parameter-Test: unused.
- SQL-Parameter-Test: Liste von Maps.

## Dokumentation

- `docs/lsp/README.md`
- `docs/lsp/TESTING.md`
- `docs/lsp/TROUBLESHOOTING.md` für fehlende Dateien.

## Definition of Done

- Outline kann GRETL-Jobs abbilden.
- SQL-Parameteranalyse arbeitet ohne Datenbank.

## Agenten-Prompt

```text
Setze Phase 6 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Implementiere Document Symbols, Document Links für File-Referenzen und SQL-Parameteranalyse aus referenzierten `.sql`-Dateien. Keine Datenbankverbindungen. Ergänze Tests für Symbole, Links und SQL-Parameter-Diagnostics. Aktualisiere Dokumentation und `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 7: VS-Code-Extension v1 mit F5-Workflow

## Ziel

Eine lauffähige VS-Code-Extension startet den Java-LSP, zeigt Logs und bietet erste Commands. Lokale Entwicklung mit F5 ist dokumentiert und getestet.

## Artefakt

- `vscode/gretl-vscode`
- `package.json`
- TypeScript-Client
- F5 Launch Configuration
- `copyDevGretlServerJar`
- README mit Entwicklungsanleitung

## Implementierungsanweisung

### 1. Extension-Grundstruktur

Lege `vscode/gretl-vscode` an mit:

- `package.json`
- `tsconfig.json`
- `src/extension.ts`
- `src/languageServer.ts`
- `src/commands.ts`
- `server/.gitkeep`
- `.vscode/launch.json`
- `.vscode/tasks.json`
- `README.md`

### 2. Dependencies

```bash
npm install vscode-languageclient
npm install --save-dev typescript @types/node @types/vscode @vscode/vsce
```

Versionen an Repo-Konventionen anpassen.

### 3. Language Client

Nutze `vscode-languageclient/node`.

Server Options:

```ts
const serverOptions: ServerOptions = {
  command: javaCommand,
  args: [...jvmArgs, '-jar', serverJarPath, '--stdio'],
  options: { cwd: workspaceRoot }
};
```

Client Options:

```ts
const clientOptions: LanguageClientOptions = {
  documentSelector: [
    { scheme: 'file', language: 'groovy', pattern: '**/*.gradle' }
  ],
  outputChannel,
  synchronize: {
    fileEvents: vscode.workspace.createFileSystemWatcher('**/*.{gradle,sql,properties}')
  }
};
```

### 4. Commands

- `gretl.restartLanguageServer`
- `gretl.showLanguageServerLogs`
- `gretl.openOverview` stub
- `gretl.runTaskAtCursor` stub oder Terminalausführung, falls einfach

### 5. Build Task Root

Root-Gradle Task:

```groovy
tasks.register('copyDevGretlServerJar', Copy) {
    dependsOn ':gretl-lsp:shadowJar'
    from project(':gretl-lsp').layout.buildDirectory.file('libs/gretl-lsp-all.jar')
    into layout.projectDirectory.dir('vscode/gretl-vscode/server')
}
```

Pfad an reale Jar-Namen anpassen.

### 6. README

`vscode/gretl-vscode/README.md` muss Schritt für Schritt erklären:

1. `./gradlew copyDevGretlServerJar`
2. `cd vscode/gretl-vscode`
3. `npm install`
4. `npm run build`
5. `code .`
6. F5 mit `Run GRETL Extension`
7. Im Extension Development Host ein GRETL-Projekt öffnen.
8. Logs anzeigen.

## Tests

- `npm run build`
- `npm test` mit mindestens Config-Resolver-Test
- `./gradlew copyDevGretlServerJar`

## Dokumentation

- `docs/lsp/VSCODE_EXTENSION.md`
- `vscode/gretl-vscode/README.md`

## Definition of Done

- Extension startet LSP im Development Host.
- F5 Workflow dokumentiert.
- Server-JAR wird sauber gestaged.

## Agenten-Prompt

```text
Setze Phase 7 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Erstelle eine kleine TypeScript-VS-Code-Extension unter `vscode/gretl-vscode`, die den Java-LSP per `java -jar server/gretl-lsp-all.jar --stdio` startet. Implementiere Settings, Commands, Logs und den F5-Workflow. Ergänze `copyDevGretlServerJar`, TypeScript-Build und Tests. Dokumentiere Inbetriebnahme ausführlich in `vscode/gretl-vscode/README.md` und `docs/lsp/VSCODE_EXTENSION.md`. Aktualisiere `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 8: GRETL Overview / Job-Graph-Webview

## Ziel

Die VS-Code-Extension zeigt eine read-only Übersicht des GRETL-Jobs: Task-Graph, Task-Status, Diagnostics, SQL-Dateien und SQL-Parameter.

## Artefakt

- LSP Command `gretl.getOverview`
- TypeScript Webview
- GRETL Explorer Tree View optional
- Tests für Modell und HTML-Escaping

## Implementierungsanweisung

### 1. LSP Execute Command

Implementiere im LSP:

```java
public final class GretlExecuteCommandService {
    public Object execute(String command, List<Object> arguments);
}
```

Command:

```text
gretl.getOverview
```

Argument:

```json
{ "uri": "file:///.../build.gradle" }
```

Resultat:

```json
{
  "uri": "file:///.../build.gradle",
  "tasks": [...],
  "graph": {...},
  "diagnostics": [...],
  "sqlParameterReport": {...}
}
```

### 2. Webview

`GretlOverviewPanel` öffnet beside editor.

Abschnitte:

- Summary
- Pipeline
- Tasks
- Diagnostics
- SQL Files
- SQL Parameters

MVP HTML ohne externe JS-Abhängigkeiten.

### 3. Security

- Alle Inhalte HTML-escapen.
- Keine ungeprüften Scripts.
- CSP setzen.

### 4. Refresh

- Manuelles Refresh.
- Automatisch nach Save.
- Optional debounce bei Edit.

## Tests

- Java: `TaskGraphBuilderTest`.
- Java: `GetOverviewCommandTest`.
- TypeScript: HTML escaping.
- TypeScript: Overview rendering enthält Tasks.

## Dokumentation

- `docs/lsp/VSCODE_EXTENSION.md`
- `vscode/gretl-vscode/README.md`

## Definition of Done

- Webview ist read-only.
- Klick auf Task springt optional zur Range; falls nicht umgesetzt, dokumentieren.
- Kein unsicheres HTML.

## Agenten-Prompt

```text
Setze Phase 8 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Implementiere ein LSP-Command `gretl.getOverview` und eine read-only VS-Code-Webview für GRETL Job-Graph, Tasks, Diagnostics, SQL-Dateien und SQL-Parameter. Halte die Webview sicher: HTML escaping und CSP. Ergänze Java- und TypeScript-Tests und aktualisiere Dokumentation sowie `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 9: Quick Fixes und DSL-Migration

## Ziel

Der LSP bietet Code Actions für häufige GRETL-Probleme, insbesondere fehlende Pflichtfelder und Migration von alter `=`-Schreibweise zur neuen DSL.

## Artefakt

- `CodeActionProvider`
- `WorkspaceEditFactory`
- Quick Fix Tests

## Implementierungsanweisung

### 1. CodeActionProvider

```java
public final class GretlCodeActionProvider {
    public List<Either<Command, CodeAction>> codeActions(CodeActionParams params, AnalysisResult analysis);
}
```

### 2. Quick Fix: fehlendes Pflichtfeld

Für Diagnostic `GRETL1001`:

```groovy
tasks.register('executeSql', SqlExecutor) {
    database dbUri, dbUser, dbPwd
}
```

Quick Fix ergänzt:

```groovy
    sqlFiles files('${1:script.sql}')
```

Ohne Snippet-Unterstützung im LSP: normalen Text einfügen.

### 3. Quick Fix: Tippfehler

Für `GRETL1002` mit Vorschlag:

```groovy
sqlFile files('demo.sql')
```

zu:

```groovy
sqlFiles files('demo.sql')
```

### 4. Quick Fix: Dependency korrigieren

`dependsOn 'importDat'` → `dependsOn 'importData'`.

### 5. Quick Fix: Legacy DSL migrieren

```groovy
database = [dbUri, dbUser, dbPwd]
```

zu:

```groovy
database dbUri, dbUser, dbPwd
```

```groovy
sqlFiles = files('demo.sql')
```

zu:

```groovy
sqlFiles files('demo.sql')
```

### 6. Keine riskanten Mass-Edits im MVP

Einzelne Code Action pro Diagnostic. Workspace-weite Migration erst später.

## Tests

- Missing required property Quick Fix.
- Typo Quick Fix.
- Unknown dependency Quick Fix.
- Legacy migration Quick Fix.
- Edits erhalten Einrückung.

## Dokumentation

- `docs/lsp/README.md`
- `docs/lsp/VSCODE_EXTENSION.md`

## Definition of Done

- Code Actions erscheinen nur für passende Diagnostics.
- Edits sind minimal.
- Tests prüfen exakten Text nach Anwendung.

## Agenten-Prompt

```text
Setze Phase 9 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Implementiere Code Actions für fehlende Pflichtfelder, Property-Tippfehler, unbekannte Dependencies und Migration von alter `=`-Schreibweise zur neuen GRETL-DSL. Edits müssen minimal und gut getestet sein. Aktualisiere Dokumentation und `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 10: GitHub Actions CI und VSIX Packaging

## Ziel

Eine GitHub Action baut und testet Java-LSP, Doclet-Metadaten und VS-Code-Extension. Optional erzeugt sie ein VSIX-Artefakt.

## Artefakt

- `.github/workflows/gretl-lsp-ci.yml`
- CI-Dokumentation
- optional VSIX Upload Artifact

## Implementierungsanweisung

### 1. Workflow

Datei:

```text
.github/workflows/gretl-lsp-ci.yml
```

MVP:

```yaml
name: GRETL LSP CI

on:
  push:
    branches: [ main ]
  pull_request:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Node
        uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: vscode/gretl-vscode/package-lock.json

      - name: Generate LSP metadata
        run: ./gradlew :gretl-doclet:generateLspMetadata

      - name: Run Java tests
        run: ./gradlew :gretl-lsp:test

      - name: Build language server fat JAR
        run: ./gradlew :gretl-lsp:shadowJar copyDevGretlServerJar

      - name: Install VS Code extension dependencies
        working-directory: vscode/gretl-vscode
        run: npm ci

      - name: Build VS Code extension
        working-directory: vscode/gretl-vscode
        run: npm run build

      - name: Test VS Code extension
        working-directory: vscode/gretl-vscode
        run: npm test

      - name: Package VSIX
        working-directory: vscode/gretl-vscode
        run: npm run package:vsix

      - name: Upload VSIX artifact
        uses: actions/upload-artifact@v4
        with:
          name: gretl-vscode-vsix
          path: vscode/gretl-vscode/*.vsix
```

Versionen an Repo-Konventionen anpassen.

### 2. package.json Scripts

In Extension:

```json
{
  "scripts": {
    "vscode:prepublish": "npm run build",
    "build": "tsc -p ./",
    "watch": "tsc -watch -p ./",
    "test": "node --test test/*.test.js",
    "package:vsix": "vsce package --out gretl-vscode.vsix",
    "check:vsix": "node ./scripts/assert-vsix-contents.js gretl-vscode.vsix"
  }
}
```

### 3. VSIX-Inhalt prüfen

Script:

```text
vscode/gretl-vscode/scripts/assert-vsix-contents.js
```

Prüft:

- `extension/package.json`
- `extension/dist/extension.js`
- `extension/server/gretl-lsp-all.jar`
- `extension/README.md`

## Tests

- Workflow lokal soweit möglich durch Kommandos simulieren.
- `npm run check:vsix`.
- Java und Node Tests.

## Dokumentation

- `docs/lsp/DEVELOPMENT.md`: CI-Kommandos lokal.
- `docs/lsp/VSCODE_EXTENSION.md`: VSIX bauen.

## Definition of Done

- CI-Datei vorhanden.
- Lokale Kommandos laufen.
- VSIX wird erzeugt und geprüft.

## Agenten-Prompt

```text
Setze Phase 10 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Erstelle eine GitHub Action, die Doclet-Metadaten erzeugt, Java-LSP testet, Fat-JAR baut, die VS-Code-Extension baut/testet und ein VSIX-Artefakt erzeugt. Ergänze Scripts zur VSIX-Prüfung. Führe die lokalen Äquivalente der CI-Schritte aus, soweit möglich. Aktualisiere Dokumentation und `docs/lsp/PHASE_STATUS.md`.
```

---

# Phase 11: Stabilisierung, Dokumentation und Release-Kandidat

## Ziel

Das Feature-Set wird stabilisiert, dokumentiert und für interne Nutzung vorbereitet.

## Artefakt

- Vollständige Doku
- Beispielprojekt
- Troubleshooting
- Release-Checkliste
- Performance-/Robustheitsverbesserungen

## Implementierungsanweisung

### 1. Performance

Prüfen:

- Keine vollständige Reanalyse unnötig oft.
- Diagnostics bei grossen `build.gradle` noch schnell.
- SQL-Dateien gecacht nach mtime oder Dokumentversion.
- Keine Logs auf stdout.

### 2. Robustheit

Prüfen:

- Server stürzt bei kaputtem Groovy nicht ab.
- Server stürzt bei ungültigem Manifest nicht ab.
- Extension meldet fehlendes Java verständlich.
- Extension meldet fehlenden Server-JAR verständlich.

### 3. Dokumentation komplettieren

Dateien:

- `docs/lsp/README.md`
- `docs/lsp/DEVELOPMENT.md`
- `docs/lsp/TESTING.md`
- `docs/lsp/VSCODE_EXTENSION.md`
- `docs/lsp/TROUBLESHOOTING.md`
- `vscode/gretl-vscode/README.md`
- `vscode/gretl-vscode/CHANGELOG.md`

### 4. Beispielprojekt

Erzeuge:

```text
docs/lsp/examples/simple-gretl-job/
```

Mit:

- `build.gradle`
- `gradle.properties`
- `src/main/sql/transform.sql`
- `README.md`

### 5. Release Checklist

Datei:

```text
docs/lsp/RELEASE_CHECKLIST.md
```

Inhalt:

- Java Tests
- Extension Tests
- Fat-JAR bauen
- VSIX bauen
- VSIX installieren
- F5 testen
- Beispielprojekt öffnen
- Completion testen
- Diagnostics testen
- Overview testen

## Tests

Voller relevanter Check:

```bash
./gradlew :gretl-doclet:generateLspMetadata :gretl-lsp:test :gretl-lsp:shadowJar copyDevGretlServerJar
cd vscode/gretl-vscode
npm ci
npm run build
npm test
npm run package:vsix
npm run check:vsix
```

## Definition of Done

- Alle Phasen in `PHASE_STATUS.md` sind mindestens `done` oder bewusst dokumentiert `blocked`.
- Doku erklärt Endanwender- und Entwickler-Workflow.
- Ein interner Test mit F5 ist möglich.

## Agenten-Prompt

```text
Setze Phase 11 aus `docs/design/gretl-lsp-agent-spec.md` um. Beachte AGENTS.md, CLAUDE.md und relevante Skills. Stabilisiere LSP und VS-Code-Extension, vervollständige Dokumentation, erstelle ein Beispielprojekt und eine Release-Checkliste. Führe den vollen relevanten Check aus oder dokumentiere exakt, was nicht ausführbar war. Aktualisiere `docs/lsp/PHASE_STATUS.md` und liefere einen Abschlussbericht mit geänderten Dateien, Tests, Ergebnissen und Risiken.
```

---

## 26. Nichtfunktionale Anforderungen

### 26.1 Stabilität

- Der LSP darf bei ungültigem Groovy nicht abstürzen.
- Der LSP darf bei ungültigem Manifest nicht abstürzen.
- Der LSP darf bei fehlenden Dateien nicht abstürzen.
- Extension muss fehlendes Java verständlich melden.

### 26.2 Performance

- Kein Gradle-Build bei jeder Änderung.
- Keine Datenbankverbindung bei jeder Änderung.
- SQL-Dateien nur lesen, wenn referenziert.
- Grosse SQL-Dateien defensiv behandeln; optional Limit mit Hinweis.

### 26.3 Sicherheit

- Keine Passwörter loggen.
- Keine Secrets in Diagnostics ausgeben.
- Keine Webview-Inhalte ohne escaping.
- Keine Ausführung von Build-Skripten für Analyse.

### 26.4 Wartbarkeit

- Produktionscode darf keine Test-Fixtures kennen.
- Keine hartcodierten GRETL-Task-Listen im LSP, ausser erlaubte externe Gradle-Tasks.
- Doclet-Metadaten sind Source of Truth.
- Entscheidungen dokumentieren.

---

## 27. Akzeptanzkriterien Gesamtprojekt

Das Gesamtprojekt ist akzeptiert, wenn:

1. `gretl-doclet` erzeugt `gretl-lsp-metadata.json`.
2. `gretl-lsp` startet als Fat-JAR.
3. VS-Code-Extension startet den LSP per F5.
4. In einem GRETL `build.gradle` funktionieren:
   - Diagnostics für fehlende Pflichtfelder.
   - Completion für Task-Typen.
   - Completion für Properties innerhalb Task.
   - Hover für Task/Property.
   - Signature Help für `database url, usr, pwd`.
   - Outline für Tasks.
   - Document Links für SQL-Dateien.
   - SQL-Parameter-Diagnostics.
   - Overview Webview.
5. CI baut Java und TypeScript und erzeugt ein VSIX.
6. Dokumentation beschreibt Entwicklung und Nutzung.
7. Tests decken Parser, Analyzer, LSP und Extension ab.

---

## 28. Abschlussbericht-Template für Agenten

Jede Phase endet mit folgendem Bericht:

```markdown
## Abschlussbericht Phase X

### Geänderte Dateien
- ...

### Implementiertes Verhalten
- ...

### Tests / Kommandos tatsächlich ausgeführt
- `...` → erfolgreich / fehlgeschlagen

### Nicht ausgeführte Tests
- ... mit Begründung

### Dokumentation aktualisiert
- ...

### Phase Tracking
- `docs/lsp/PHASE_STATUS.md` aktualisiert: ja/nein

### Risiken / offene Punkte
- ...

### Commit
- Nicht committet / Commit: `<hash>` / Commit-Message: `...`
```

---

## 29. Hinweise für Agenten

- Arbeite phasenweise. Nicht mehrere Phasen vermischen, ausser es ist technisch zwingend und dokumentiert.
- Lies vorhandene Skills und lokale Agentenanweisungen vor Änderungen.
- Suche vorhandene Tests, bevor du neue Strukturen einführst.
- Ergänze Regressionstests vor oder zusammen mit der Implementierung.
- Führe den kleinsten relevanten Test aus, danach breitere Tests.
- Aktualisiere Dokumentation laufend.
- Halte Implementierungen klein, nachvollziehbar und robust.
- Behaupte nie, etwas sei getestet, wenn es nicht getestet wurde.
- Vermeide opportunistische Refactorings.
- Keine Build-Script-Evaluation im LSP.
- Keine Secrets loggen.
- Keine dynamische Datenbankprüfung im MVP.

---

## 30. Quellen und technische Bezugspunkte

Diese Spezifikation orientiert sich an folgenden öffentlich zugänglichen technischen Bezugspunkten und an den Vorgaben des Auftraggebers:

- GRETL Repository: `https://github.com/sogis/gretl`
- GRETL Referenzdokumentation: `https://gretl.app/reference.html`
- GRETL Deployment-Dokumentation: `https://gretl.app/deployment.html`
- Gradle Plugin Portal `ch.so.agi.gretl`: `https://plugins.gradle.org/plugin/ch.so.agi.gretl`
- Eclipse LSP4J: `https://github.com/eclipse-lsp4j/lsp4j`
- VS Code Language Server Extension Guide: `https://code.visualstudio.com/api/language-extensions/language-server-extension-guide`
- VS Code Extension Manifest: `https://code.visualstudio.com/api/references/extension-manifest`
- VS Code Publishing Extensions / vsce: `https://code.visualstudio.com/api/working-with-extensions/publishing-extension`
- Referenzprojekt `ilinexus`: `https://github.com/edigonzales/ilinexus`
- `ilinexus` VS-Code-Extension `vscode/ilimap-vscode` als strukturelles Vorbild.



---

# Anhang: konkrete Tests aus aktueller Quellenanalyse, die der Agent priorisieren muss

Der Agent soll aus den gelieferten Tests in `gretl-core` zusätzliche LSP-Fixtures ableiten. Besonders nützlich sind:

- `SqlExecutorFunctionalTest`: `database`, `sqlFiles`, `sqlParameterSets`, `dependsOn`, Rollback-Szenarien; Kotlin-DSL bleibt ausserhalb des unterstützten MVP-Vertrags.
- `DuckDbSqlExecutorFunctionalTest`: `inMemoryDatabase`, `database file(...)`, `sources.gpkg`, `sources.csv`, `exports.parquet`, `exports.xlsx`, `exports.gpkg`, `sqlParameterSets`.
- `DuckDbSqlExecutorPostgisIntegrationTest`: `sources.postgres`, `targets.postgres`, `exports.postgres`, `geometry`, `target`-Aliasprüfung.
- `FtpFunctionalTest`: geerbte `server`, `user`, `password` plus konkrete Download/Upload/Delete-Methoden.
- `S3FlociIntegrationTest`: `accessKey`, `secretKey`, `bucketName`, `sourceFile/sourceDir/sourceFiles`, `acl`, `downloadDir`.
- `Ili2dbFunctionalTest` und `Ili2pgPostgisIntegrationTest`: geerbte INTERLIS-/ili2db-Methoden aus `AbstractIli2DbTask` und verwandten Basisklassen.

Diese Fixtures sollen in Phase 12 in Golden Tests überführt werden. Einige davon sollen bereits in Phase 4–7 als reduzierte Testdaten einfliessen.
