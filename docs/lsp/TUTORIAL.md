# GRETL VS Code Extension -- Tutorial

Willkommen! Dieses Tutorial führt dich Schritt für Schritt in die GRETL VS Code
Extension ein. Du lernst, wie du GRETL-Jobs direkt im Editor schreibst -- mit
automatischen Vervollständigungen, Fehlererkennung, Quick Fixes und mehr.

## Inhaltsverzeichnis

1. [Einleitung](#einleitung)
2. [Installation](#installation)
3. [Beispiel 1: Erster GRETL-Job](#beispiel-1-erster-gretl-job)
4. [Beispiel 2: Tasks mit Abhängigkeiten](#beispiel-2-tasks-mit-abhängigkeiten)
5. [Beispiel 3: DuckDbSqlExecutor mit Sources und Exports](#beispiel-3-duckdbsqlexecutor-mit-sources-und-exports)
6. [GRETL Overview](#gretl-overview)
7. [Quick Fixes im Detail](#quick-fixes-im-detail)
8. [Tastenkürzel und Einstellungen](#tastenkürzel-und-einstellungen)

## Einleitung

GRETL ist ein Gradle-basiertes ETL-Framework für Geodaten und strukturierte
Daten. Ein GRETL-Job ist ein `build.gradle`-Skript, das Tasks wie
`SqlExecutor`, `DuckDbSqlExecutor`, `Db2Db` oder `Gzip` definiert.

Die **GRETL VS Code Extension** bringt Sprachunterstützung für diese Skripte in
den Editor:

| Feature | Was sie leistet |
|---------|-----------------|
| **Completions** | Schlüsselwörter und Task-Typen automatisch vorschlagen |
| **Diagnostics** | Fehler und Warnungen schon beim Tippen anzeigen |
| **Quick Fixes** | Fehler per Tastendruck automatisch korrigieren |
| **Hover** | Dokumentation zu Task-Typen und Properties einblenden |
| **Signature Help** | Erwartete Parameter beim Schreiben von Methodenaufrufen anzeigen |
| **Document Symbols** | Aufgabenübersicht im Outline-View |
| **Document Links** | SQL- und Property-Dateien per Klick öffnen |
| **GRETL Overview** | Job-Graph, Pipeline, Tasks, Diagnostics als Webview |

## Installation

Die Extension aus dem VS Code Marketplace installieren:

*[Link folgt nach Veröffentlichung]*

**Voraussetzung:** Java 17 muss installiert und unter `java` erreichbar sein.

```bash
java -version  # muss 17.x anzeigen
```

Nach der Installation die Extension in VS Code aktivieren:

1. Ein Verzeichnis mit einer `build.gradle`-Datei öffnen.
2. Die Extension startet automatisch. In der unteren Statusleiste erscheint
   die Meldung `GRETL language server started`.
3. Im Output-Channel (Ansicht > Output) das Dropdown `GRETL` auswählen,
   um die Server-Logs zu sehen.

## Beispiel 1: Erster GRETL-Job

Wir beginnen mit einem einfachen `SqlExecutor`, der eine SQLite-Datenbank
befüllt. **Keine Installation von PostgreSQL oder Docker nötig.**

### Schritt 1: Projektstruktur anlegen

Erstelle ein neues Verzeichnis mit dieser Struktur:

```
mein-gretl-job/
├── build.gradle
├── settings.gradle
└── sql/
    └── hello.sql
```

Die Dateien findest du fertig vorbereitet unter
[`docs/lsp/tutorial/beispiel1-sql-executor/`](tutorial/beispiel1-sql-executor/).

### Schritt 2: settings.gradle

```groovy
rootProject.name = 'gretl-tutorial-beispiel1'
```

### Schritt 3: sql/hello.sql

```sql
CREATE TABLE IF NOT EXISTS gruesse (
    id INTEGER PRIMARY KEY,
    text VARCHAR(100) NOT NULL
);

INSERT OR IGNORE INTO gruesse (id, text) VALUES (1, 'Hallo GRETL!');
INSERT OR IGNORE INTO gruesse (id, text) VALUES (2, 'Willkommen in der GRETL VS Code Extension.');
```

### Schritt 4: build.gradle -- zeilenweise mit der Extension

Öffne die (leere) `build.gradle` in VS Code. Die GRETL Extension ist aktiv --
das siehst du am Eintrag `GRETL` in der Statusleiste.

**Zeile 1: Plugin deklarieren.** Tippe `plu` und drücke `Ctrl+Space`.
Die Completion zeigt `plugins { }` an. Mit `Enter` übernehmen.
Innerhalb des Blocks tippe `id 'ch.so` -- die Completion schlägt
`ch.so.agi.gretl` vor:

```groovy
plugins {
    id 'ch.so.agi.gretl'
}
```

**Zeile 2: Task-Typ importieren.** Tippe `import ch.so.agi.gretl.tasks.Sq` --
die Completion listet alle GRETL-Task-Typen auf. Wähle `SqlExecutor`:

```groovy
import ch.so.agi.gretl.tasks.SqlExecutor
```

**Zeile 3: Task registrieren.** Tippe `tasks.register('hel` und drücke
`Ctrl+Space` nach dem öffnenden `{`.

Die Completion zeigt alle Properties von `SqlExecutor` an, gruppiert in
*required* und *optional*. `database` und `sqlFiles` erscheinen zuerst, da
sie Pflichtfelder sind. Übernimm `database` mit `Enter` -- die Extension
fügt den Methodenaufruf als Snippet ein.

Vervollständige die Werte. Halte die Maus (hover) über `SqlExecutor` -- ein
Tooltip zeigt den Task-Typ, den vollqualifizierten Klassennamen und die
Kategorie:

```groovy
tasks.register('helloWorld', SqlExecutor) {
    database 'jdbc:sqlite:build/gretl.db'
    sqlFiles 'sql/hello.sql'
}
```

**Fertig!** Das ist ein vollständiger, lauffähiger GRETL-Job. Die Extension
hat dich beim Schreiben jeder Zeile unterstützt.

### Schritt 5: Fehlererkennung live erleben

Lösche probeweise die Zeile mit `sqlFiles`:

```groovy
tasks.register('helloWorld', SqlExecutor) {
    database 'jdbc:sqlite:build/gretl.db'
    /* sqlFiles fehlt */
}
```

Sofort erscheint eine **rote Wellenlinie** unter der schliessenden Klammer.
In der Überschrift des Problems (Ansicht > Probleme) steht:

> GRETL1001: Pflichtparameter `sqlFiles` fehlt

Mache die Änderung rückgängig -- die Meldung verschwindet.

## Beispiel 2: Tasks mit Abhängigkeiten

Ein GRETL-Job besteht oft aus mehreren Tasks, die in einer bestimmten
Reihenfolge ablaufen. Dieses Beispiel zeigt drei `SqlExecutor`-Tasks mit
`dependsOn`, `finalizedBy` und `mustRunAfter`.

Die fertigen Dateien liegen unter
[`docs/lsp/tutorial/beispiel2-abhaengigkeiten/`](tutorial/beispiel2-abhaengigkeiten/).

### build.gradle

```groovy
plugins {
    id 'ch.so.agi.gretl'
}

import ch.so.agi.gretl.tasks.SqlExecutor

defaultTasks 'transformation'

tasks.register('vorbereitung', SqlExecutor) {
    database 'jdbc:sqlite:build/gretl.db'
    sqlFiles 'sql/010_setup.sql'
}

tasks.register('transformation', SqlExecutor) {
    dependsOn 'vorbereitung'
    database 'jdbc:sqlite:build/gretl.db'
    sqlFiles 'sql/020_transform.sql'
    finalizedBy 'bereinigung'
}

tasks.register('bereinigung', SqlExecutor) {
    mustRunAfter 'transformation'
    database 'jdbc:sqlite:build/gretl.db'
    sqlFiles 'sql/030_cleanup.sql'
}
```

### Was die Extension hier leistet

**Completion für Task-Namen.** Wenn du in `dependsOn '|'` den Cursor
zwischen die Anführungszeichen setzt und `Ctrl+Space` drückst, schlägt die
Extension alle im Skript definierten Task-Namen vor. Das vermeidet Tippfehler.

**Diagnostic für ungültige Abhängigkeiten.** Ersetze probeweise
`'vorbereitung'` durch `'vorbereitungX'` -- sofort erscheint eine Warnung:

> GRETL1101: Unbekannte Abhängigkeit 'vorbereitungX'.
> Meintest du 'vorbereitung'?

Die Extension erkennt den Tippfehler und schlägt per Levenshtein-Abstand den
nächstliegenden Task-Namen vor.

**Hover auf Abhängigkeiten.** Fahre mit der Maus über `dependsOn` -- ein
Tooltip erklärt die Property. Fahre über `SqlExecutor` innerhalb eines
Tasks -- der Tooltip zeigt den vollqualifizierten Klassennamen und die
Status-Information.

**Document Links.** `Ctrl+Click` (bzw. `Cmd+Click` auf macOS) auf einen
SQL-Dateipfad wie `'sql/010_setup.sql'` öffnet die Datei direkt im Editor.

**Document Symbols.** Öffne den Outline-View (`Ctrl+Shift+O` bzw.
`Cmd+Shift+O` auf macOS). Die Extension listet:
- `tasks { vorbereitung }` (SqlExecutor)
- `tasks { transformation }` (SqlExecutor)
- `tasks { bereinigung }` (SqlExecutor)
- `files { sql/010_setup.sql }`
- `files { sql/020_transform.sql }`
- `files { sql/030_cleanup.sql }`

**Signature Help.** Wenn du innerhalb von `database ...` ein Komma tippst,
zeigt die Extension die erwartete Signatur an: `database url, user, password`.
Der aktive Parameter wird hervorgehoben.

### Quick Fix: Tippfehler automatisch korrigieren

1. Schreibe absichtlich `dependsOn 'vorbereitungX'`
2. Die Warnung erscheint (GRETL1101)
3. Setze den Cursor auf die gewellte Linie und drücke `Ctrl+.` (bzw. `Cmd+.`
   auf macOS)
4. Die Extension bietet an: **Korrigiere `vorbereitungX` zu `vorbereitung`**
5. Mit `Enter` bestätigen -- der Fehler wird behoben

## Beispiel 3: DuckDbSqlExecutor mit Sources und Exports

Das dritte Beispiel verwendet einen `DuckDbSqlExecutor`. DuckDB läuft embedded
-- kein separater Datenbankserver nötig. Der Task liest eine CSV-Datei über
einen `sources`-Block ein, transformiert sie mit SQL und exportiert das
Ergebnis als Parquet und Excel.

Das vollständige, lauffähige Beispiel findest du hier:
[`docs/examples/duckdb-sql-executor/csv-xlsx-parquet/`](../examples/duckdb-sql-executor/csv-xlsx-parquet/)

```groovy
plugins {
    id 'ch.so.agi.gretl'
}

import ch.so.agi.gretl.tasks.DuckDbSqlExecutor

tasks.register('convert', DuckDbSqlExecutor) {
    inMemoryDatabase()
    installExtensions true

    sources {
        csv('input') {
            file file('data/input.csv')
            table = 'records'
            delimiter = ';'
            header = true
        }
    }

    sqlFiles 'sql/010_transform.sql'

    exports {
        parquet('analyse_parquet') {
            query = 'SELECT * FROM result.analyse'
            file file('build/analyse.parquet')
            overwrite = true
        }

        xlsx('analyse_xlsx') {
            query = 'SELECT * FROM result.analyse'
            file file('build/analyse.xlsx')
            sheet = 'Analyse'
            overwrite = true
        }
    }
}
```

### Was die Extension hier leistet

**Completions in geschachtelten Blöcken.** Die Extension erkennt, dass du
dich im `sources { }`- oder `exports { }`-Block befindest, und schlägt
kontextabhängig die richtigen Properties vor. Nach `exports {` listet
`Ctrl+Space` die verfügbaren Export-Typen (`parquet`, `xlsx`, `gpkg`, …).

**Signature Help für komplexe Methoden.** Tippe `csv(` innerhalb von
`sources { }` -- die Extension zeigt die Signatur und hebt den aktiven
Parameter hervor.

**Document Links.** `Ctrl+Click` auf `'sql/010_transform.sql'` öffnet die
SQL-Datei. Gleiches gilt für `file('data/input.csv')`.

**Hover.** Fahre mit der Maus über `inMemoryDatabase()` oder
`installExtensions` -- die Extension zeigt die Property-Dokumentation aus
den GRETL-Metadaten.

## GRETL Overview

Die Extension bietet eine grafische Übersicht über den gesamten Job.

1. Öffne eine `build.gradle` im Editor.
2. Drücke `Cmd+Shift+P` und tippe `GRETL: Open GRETL Overview`.
3. Ein Webview-Panel öffnet sich mit mehreren Abschnitten:

| Abschnitt | Inhalt |
|-----------|--------|
| **Summary** | Anzahl Tasks, Fehler/Warnungen/Infos, Parse-Methode |
| **Pipeline** | Geordnete Task-Liste mit Abhängigkeitskanten und Problemen |
| **Tasks** | Pro Task eine Karte mit Name, Typ und Status der Pflichtfelder |
| **Diagnostics** | Tabelle aller Diagnostiken gruppiert nach Task |
| **SQL Files** | Referenzierte `.sql`-Dateien |
| **SQL Parameters** | Fehlende und ungenutzte SQL-Parameter |

Klicke auf einen Task-Namen -- der Editor navigiert direkt zur Definition
des Tasks im `build.gradle`. Der Overview wird bei jedem Öffnen automatisch
aktualisiert. Mit `GRETL: Refresh GRETL Overview` kannst du ihn auch manuell
neu laden.

> **Hinweis zu SQL-Parametern:** Der Abschnitt "SQL Parameters" erscheint nur
> bei Tasks, die in den Metadaten `sqlParameterProvider = true` gesetzt haben
> (z.B. `SqlExecutor` und `Ili2pgImport`). Er zeigt an, welche Parameter in
> den SQL-Dateien verwendet, aber nicht im Task definiert sind.

## Quick Fixes im Detail

Die Extension bietet zu den häufigsten GRETL-Diagnostiken automatische
Korrekturen an. Aktiviere sie mit `Ctrl+.` (bzw. `Cmd+.` auf macOS).

### GRETL1001: Fehlende Pflicht-Property

```groovy
tasks.register('x', SqlExecutor) {
    database 'jdbc:sqlite:build/x.db'
    /* sqlFiles fehlt */
}
```

→ `Ctrl+.` zeigt: **"Füge `sqlFiles` hinzu"**. Die Extension fügt den
Aufruf mit der modernen DSL-Signatur am Ende des Task-Blocks ein.

### GRETL1002: Unbekannte Property (Tippfehler)

```groovy
tasks.register('x', SqlExecutor) {
    database 'jdbc:sqlite:build/x.db'
    sqlFlies 'sql/x.sql'   /* Tippfehler: sqlFiles */
}
```

→ `Ctrl+.` zeigt: **"Korrigiere `sqlFlies` zu `sqlFiles`"**. Die Extension
ersetzt den falschen Namen per Levenshtein-Ähnlichkeit.

### GRETL1101: Unbekannte Abhängigkeit

```groovy
tasks.register('y', SqlExecutor) {
    dependsOn 'setupX'   /* existiert nicht, meintest du 'setup'? */
}
```

→ `Ctrl+.` zeigt: **"Korrigiere `setupX` zu `setup`"**.

### GRETL1201: Legacy-DSL

```groovy
tasks.register('x', SqlExecutor) {
    database = ['jdbc:sqlite:build/x.db']
    sqlFiles = files('sql/x.sql')
}
```

Die alte Assignmentschreibweise (`=`) wird als Information (GRETL1201)
gemeldet. Die Extension hebt sie blau unterringelt hervor.

→ `Ctrl+.` zeigt: **"Migriere zu moderner DSL-Schreibweise"**. Aus
`database = ['jdbc:sqlite:build/x.db']` wird `database 'jdbc:sqlite:build/x.db'`.

### Nicht per Quick Fix abgedeckt

- GRETL1003 (falsche Argumentanzahl) -- die Korrektur ist mehrdeutig und
  erfordert Entwicklerentscheid
- GRETL1102/1103 (defaultTasks / doppelte Task-Namen)
- GRETL1301/1302 (SQL-Parameter-Inkonsistenzen)

## Tastenkürzel und Einstellungen

### Tastenkürzel

| Tastenkürzel | Aktion |
|-------------|--------|
| `Ctrl+Space` (macOS: `Cmd+Space`) | Completions auslösen |
| `Ctrl+.` (macOS: `Cmd+.`) | Quick Fixes anzeigen |
| `Ctrl+K Ctrl+I` (macOS: `Cmd+K Cmd+I`) | Hover-Dokumentation einblenden |
| `Ctrl+Click` (macOS: `Cmd+Click`) | Document Link folgen (z.B. SQL-Datei) |
| `Ctrl+Shift+O` (macOS: `Cmd+Shift+O`) | Document Symbols (Outline) |
| `Cmd+Shift+P` `GRETL` | Alle GRETL-Commands anzeigen |

### GRETL-Commands

| Command | Aktion |
|---------|--------|
| `GRETL: Restart Language Server` | LSP-Prozess neu starten |
| `GRETL: Show Language Server Logs` | Output-Channel öffnen |
| `GRETL: Open GRETL Overview` | Job-Graph-Webview öffnen |
| `GRETL: Refresh GRETL Overview` | Webview-Daten neu laden |

### Einstellungen

Die Extension kann über VS Code Settings (`gretl.*`) konfiguriert werden:

| Setting | Typ | Standard | Beschreibung |
|---------|-----|----------|-------------|
| `gretl.java.path` | `string` | `""` | Pfad zu Java 17. Leer = `java` aus PATH |
| `gretl.server.jarPath` | `string` | `""` | Pfad zu `gretl-lsp-all.jar`. Leer = gebündeltes JAR |
| `gretl.server.jvmArgs` | `string[]` | `[]` | Zusätzliche JVM-Argumente (z.B. `-Xmx512m`) |
| `gretl.trace.server` | `enum` | `"off"` | LSP-Trace: `off`, `messages`, `verbose` |

### Problembehandlung

| Problem | Lösung |
|---------|--------|
| Keine Completion | Prüfe, ob die Datei auf `.gradle` endet. Output-Channel auf `GRETL language server started` prüfen. |
| Java nicht gefunden | Java 17 installieren oder `gretl.java.path` setzen. |
| Server-Logs einsehen | `GRETL: Show Language Server Logs` oder Output-Channel > Dropdown `GRETL`. |
| Trace aktivieren | `gretl.trace.server` auf `messages` oder `verbose` setzen. |

## Nächste Schritte

- [GRETL-Job-Beispiele](../examples/) -- weitere lauffähige Jobs
- [Task-Referenz](../task-reference.md) -- alle GRETL-Tasks und Properties
- [Migration von GRETL Classic](../migration-from-gretl.md)
- [Kotlin-DSL-Beispiele](../kotlin-dsl.md)
