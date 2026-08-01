# Vergleichsanalyse zur Ablösereife von gretl-next

Stand: 23. Juli 2026

## Entscheid

**Gesamturteil: nicht ablösebereit für eine vollständige produktive Ablösung.**

`gretl-next` ist technisch weit fortgeschritten und für klar abgegrenzte
Pilotmigrationen geeignet. Es erreicht 41 der 44 öffentlichen Task-Typen des
Originals, ergänzt vier neue Tasks und bietet mit Control Plane,
Worker-Isolation und LSP erhebliche neue Fähigkeiten. Für einen allgemeinen
Cutover bestehen jedoch mehrere P0-Risiken:

- Das gebaute Image funktioniert mit der dokumentierten versionslosen
  `plugins {}`-DSL standardmässig nicht.
- Die moderne Plugin-DSL ist im Image nicht offlinefähig.
- Drei Original-Tasks fehlen, darunter der umfangreiche `Publisher`.
- Oracle-, SQL-Server- und Derby-Treiber werden vom Code erkannt, sind aber
  nicht im Runtime-Image enthalten.
- Es gibt kein CI-Gate für das tatsächliche Runtime-Image und kein
  Multi-Arch-/Registry-Release.
- Die generierte Task-Metadokumentation und damit auch das LSP sind bei
  geerbten Properties unvollständig.

Für PostgreSQL-, SQLite- und DuckDB-Jobs ohne die drei fehlenden Tasks ist
`gretl-next` nach DSL-Migration und mit expliziter Versionskonfiguration bereits
bedingt pilotfähig.

## Bezugsstand und Evidenz

Verglichen wurden:

- `gretl-next` `main`:
  `d6f3aa35f719a26eb149e3b054c5544a1a3eb450`, 2. Juli 2026.
- Original-GRETL `main`:
  [`a9222dc0ffd08f8f76570e998101db041822df71`](https://github.com/sogis/gretl/commit/a9222dc0ffd08f8f76570e998101db041822df71),
  6. Juli 2026.
- Stichtag: 23. Juli 2026.

Kennzeichnung:

- **Ausgeführt**: lokal tatsächlich gebaut oder getestet.
- **Code/CI**: aus Quellcode, Testinventar oder Workflow belegt.
- **Ableitung**: daraus resultierende Bewertung.

## Vergleichsmatrix

| Ebene | gretl-next | Original-GRETL | Bewertung |
|---|---|---|---|
| Funktionalität | 41 gemeinsame, 4 neue, 3 fehlende Tasks; neue modulare DSL, Control Plane und LSP | 44 Tasks inklusive Publisher und Spezialexporten | Rot/Gelb |
| Unit/Functional | 391 erfolgreiche reguläre Tests, starke TestKit- und LSP-Abdeckung | Quellinventar mit ungefähr 194 regulären Tests | Gelb |
| Integration | 40 erfolgreiche Testcontainers-Szenarien in 7 Suites | Breiteres Quellinventar, JAR- und Image-Testmatrix vorgesehen | Gelb |
| E2E/Smoke | Manuell erfolgreich mit Workarounds; kein Image- oder Control-Plane-E2E in CI | JAR- und Image-E2E im Workflow, aktueller `main` allerdings rot | Rot |
| Dokumentation | Gute Architektur-, Migrations- und LSP-Dokumente, aber unvollständige Task-Referenz und Metadaten | Vollständiger generierter Task-Katalog und Quarto-Publikation | Gelb/Rot |
| Runtime | Image baut; Defaultstart und moderne Offline-DSL fehlerhaft; Treiber- und Release-Lücken | Reiferes Image-Release, Multi-Arch, Registry und SBOM | Rot |

## Funktionale Parität

Die ursprünglich angenommenen 42 gemeinsamen Tasks lassen sich bei Ausschluss
abstrakter Hilfs- und Basisklassen nicht bestätigen. Der konkrete öffentliche
Bestand ist:

- `gretl-next`: 45 Task-Typen
- Original: 44 Task-Typen
- Gemeinsam: 41
- Nur `gretl-next`: 4
- Nur Original: 3

### Vorhanden, aber verändert: 41

| Familie | Öffentliche Task-Typen |
|---|---|
| SQL/DB | `SqlExecutor`, `Db2Db` |
| INTERLIS/Datenbanken | `Ili2duckdbExport`, `Ili2duckdbImport`, `Ili2duckdbImportSchema`, `Ili2gpkgImport`, `Ili2pgDelete`, `Ili2pgExport`, `Ili2pgImport`, `Ili2pgImportSchema`, `Ili2pgReplace`, `Ili2pgUpdate`, `Ili2pgValidate` |
| Formate/Validierung | `Csv2Excel`, `CsvExport`, `CsvImport`, `CsvValidator`, `GpkgExport`, `GpkgImport`, `GpkgValidator`, `IliValidator`, `JsonImport`, `JsonValidator`, `ShpExport`, `ShpImport`, `ShpValidator` |
| Konvertierung | `Av2ch`, `Av2geobau`, `Gpkg2Dxf`, `Gpkg2Shp` |
| Transport/Utilities | `Curl`, `FtpDelete`, `FtpDownload`, `FtpList`, `FtpUpload`, `S3Bucket2Bucket`, `S3Delete`, `S3Download`, `S3Upload`, `Gzip`, `XslTransformer` |

„Vorhanden“ bedeutet hier Typ- und Fähigkeitsparität, nicht Source- oder
vollständige Verhaltenskompatibilität. Repräsentative Abläufe sind getestet,
aber nicht jede gemeinsame Task wurde gegen das Original mit denselben Ein- und
Ausgaben ausgeführt.

### Neu in gretl-next: 4

- `DuckDbSqlExecutor`
- `RasterReclassify`
- `ReadShapefile`
- `Vectorize`

`DuckDbSqlExecutor` ist funktional besonders relevant: CSV-, GeoPackage- und
PostgreSQL-Quellen sowie Exporte nach GeoPackage, Parquet, XLSX und PostgreSQL
können in einem föderierten Ablauf kombiniert werden.

### Noch nicht migriert: 3

- `Publisher`
- `DatabaseDocumentExport`
- `PostgisRasterExport`

Diese Lücke wird auch im eigenen
[Migrationsdokument](migration-from-gretl.md#tasks-still-not-migrated) explizit
genannt.

Der neue Control Plane ersetzt den `Publisher` nicht:

- Control Plane: Scheduling, Run-Historie, Secrets, Worker-Claiming, Logs,
  Abbruch und Benachrichtigungen.
- Publisher: Publikationspakete, History/Grooming, SFTP- und SIMI-nahe
  Publikationsabläufe.
- LSP/VS Code: Entwicklungshilfe, keine Runtime- oder Publikationsfunktion.

### Wichtigste Breaking Changes

**Code/CI:** Das Plugin-Modell bleibt Gradle-basiert, ist aber nicht
Drop-in-kompatibel:

- Öffentliche `Step`-Klassen, `Connector` und `TransferSet` wurden entfernt.
- `database = [...]`, `sourceDb`, `targetDb` und `transferSets` werden durch
  typisierte Gradle-Properties und Hilfsmethoden ersetzt.
- GeoTools-Funktionen liegen in einem separaten Plugin
  `ch.so.agi.gretl.geotools`.
- Passwörter werden als interne Properties behandelt und nicht mehr als normale
  Gradle-Inputs exponiert.
- Zahlreiche Properties wurden typisiert, umbenannt oder durch strukturierte
  DSL-Blöcke ersetzt.

Die Änderungen sind im
[Migrationsleitfaden](migration-from-gretl.md) nachvollziehbar, decken aber noch
nicht alle produktiven Jobmuster ab.

Positiv sind:

- explizit getestete Whole-Task-Transaktionen und Rollbacks bei `SqlExecutor`
  und `Db2Db`;
- stabile Reihenfolgen bei Dateiinputs;
- JSON-Array- und Einzelobjektverarbeitung;
- Schemaableitung ohne INTERLIS-Modell für bestimmte
  Shapefile-/GeoPackage-Abläufe;
- schwere GeoTools-Abhängigkeiten bleiben durch Worker-Classloader-Isolation
  aus `gretl-core` heraus.

## Testabdeckung

### Tatsächlich ausgeführt

| Modul/Suite | Tests | Ergebnis |
|---|---:|---|
| `gretl-core:test` | 129 | erfolgreich |
| `gretl-core:integrationTest` | 40 | erfolgreich |
| `gretl-geotools:test` | 17 | erfolgreich |
| `gretl-doclet:test` | 18 | erfolgreich |
| `gretl-control-common:test` | 3 | erfolgreich |
| `gretl-control-server:test` | 11 | erfolgreich |
| `gretl-control-worker:test` | 2 | erfolgreich |
| `gretl-lsp:test` | 211 | erfolgreich |
| **Gesamt** | **431** | **0 Fehler, 0 übersprungen** |

Die hohe Gesamtzahl wird wesentlich durch die 211 LSP-Tests geprägt. Da weder
JaCoCo noch ein vergleichbares Coverage-Werkzeug konfiguriert ist, lässt sich
daraus keine seriöse Prozentabdeckung ableiten.

Ausgeführte PostgreSQL- und Testcontainers-Suites:

- `Db2DbPostgisIntegrationTest`: 9
- `DuckDbSqlExecutorPostgisIntegrationTest`: 9
- `FtpDockerIntegrationTest`: 1
- `Ili2pgPostgisIntegrationTest`: 7
- `IoxWkfPostgisIntegrationTest`: 5
- `S3FlociIntegrationTest`: 2
- `SqlExecutorPostgisIntegrationTest`: 7

Relevante Functional- und TestKit-Gruppen umfassen unter anderem SQL, Db2Db,
DuckDB, INTERLIS, FTP, Curl, Gzip, S3, Shapefile, GeoTools und XSLT.

### Ausgeführte Build-Gates

- `./gradlew clean check`: erfolgreich, 57 Tasks ausgeführt.
- `./gradlew :gretl-core:integrationTest`: erfolgreich.
- `./gradlew :gretl-control-server:bootJar
  :gretl-control-worker:bootJar`: erfolgreich.
- `./gradlew generateTaskDocs stageRuntimeImage
  buildRuntimeImage`: erfolgreich.

Dabei traten Warnungen zu Gradle-8-Inkompatibilitäten,
Java-Restricted-Methods auf dem Java-25-Host sowie ungeprüften Operationen in
`RasterReclassifyOperation` auf.

### Wesentliche Testlücken

- Kein echter Server-Worker-externer-`gretl`-Prozess-E2E-Test.
- Keine Systemtests für Cancellation, vollständiges Logstreaming und
  Secret-Übergabe.
- Keine Runtime-Image-Tests in der aktuellen
  [gretl-next-CI](../.github/workflows/ci.yml).
- Keine JAR-vs.-Image-Testmatrix.
- Keine Testabdeckung für die behauptete
  Oracle-, SQL-Server- und Derby-Fähigkeit.
- Keine CI-Verifikation des dokumentierten versionslosen Pluginstarts.
- Keine Code-Coverage-Metrik.

Das Original hat im
[Workflow](https://github.com/sogis/gretl/blob/a9222dc0ffd08f8f76570e998101db041822df71/.github/workflows/gretl.yml)
deutlich stärkere Gates vorgesehen: dieselben Integrationsjobs werden gegen JAR
und Docker-Image ausgeführt, gefolgt von Dokumentation und Publikation.

Allerdings ist auch das Original am Stichtag nicht grün: Der aktuelle
[Action-Lauf #873](https://github.com/sogis/gretl/actions/runs/28767164401)
scheitert bereits in `compileTestJava` mit 27 Fehlern rund um veraltete
`DbDataSelectorTest`-APIs. Der umfassendere Workflow ist damit vorhanden,
liefert für den Vergleichscommit aber kein erfolgreiches Release-Signal.

## Dokumentation

### Stärken von gretl-next

- klare Architektur- und Modulbeschreibung;
- expliziter Migrationsleitfaden;
- Kotlin-DSL-Hilfe;
- umfangreiche DuckDB-Beispiele;
- Control-Plane-Betriebsbeschreibung;
- sehr ausführliche LSP-Dokumentation und Tutorials;
- Doclet- und LSP-Metadatengenerierung.

### Nachgewiesene Lücken

Die frühere kompakte [Task-Referenz](task-reference.md) wurde durch eine
redaktionelle AsciiDoc-Referenz mit eingecheckten Doclet-Tabellen ersetzt:
[GRETL-Referenzdokumentation](reference/reference.adoc). Sie umfasst alle 45
aktuellen Task-Typen. Der `verifyTaskDocs`-Check stellt sicher, dass die
Tabellen aktuell bleiben und jeder Task in der Master-Datei eingebunden ist.

Weitere Befunde:

- Die Dokumentation bezeichnet vollständige Task-Referenz, Testing Guide,
  CI Guide, Troubleshooting und Best Practices weiterhin als
  [geplant](index.md#planned-documentation-areas).
- Der [LSP-Phasenstatus](lsp/PHASE_STATUS.md) nennt CI/VSIX-Paketierung noch
  `not-started`, obwohl inzwischen ein VS-Code-Publish-Workflow existiert.
- Die CI prüft die eingecheckten Task-Tabellen gegen die öffentliche API; die
  Thoth-Website bleibt ein separater Preview-/Release-Schritt.

Das Original besitzt für alle 44 Tasks generierte Referenzseiten, separate
Publisher-Dokumentation und eine Quarto-Publikationsstrecke. Der Workflow ist am
untersuchten Commit allerdings wegen des Test-Kompilierfehlers nicht bis zur
Dokumentationspublikation gelangt.

## Runtime und Docker-Image

### Manuelle Image-Smokes

Das Image `sogis/gretl-modular:test` wurde erfolgreich gebaut:

- Architektur: `linux/arm64`
- Grösse: ungefähr 489 MiB
- Benutzer: nicht-root, `gradle`, UID 1001
- Java 17, Gradle 7.6.4
- 93 JARs
- DuckDB-Extensions: `spatial`, `postgres`/`postgres_scanner`, `excel`

Der [Dockerfile](../docker/Dockerfile) setzt sinnvoll auf einen nicht-root
Benutzer und root-gruppenschreibbare Projektverzeichnisse.

Die Smoke-Ergebnisse:

| Szenario | Ergebnis |
|---|---|
| Versionslose moderne `plugins {}`-DSL | **Fehler** |
| Moderne DSL mit `-Dgretl.version=5.0.0-SNAPSHOT` und Netzwerk | Erfolgreich |
| Moderne DSL mit expliziter Version und dependency-closed Runtime-Image | **Fehler** |
| Legacy `apply plugin:` mit dependency-closed Runtime-Image | Erfolgreich |

Im erfolgreichen Lauf wurden tatsächlich ausgeführt:

- `Gzip` als Core-Task;
- `DuckDbSqlExecutor` mit `spatial`, `postgres` und `excel`;
- `ReadShapefile` als GeoTools-Worker-Task mit zwei Features und Ziel-CRS
  EPSG:2056.

### Reproduzierbarkeitsfehler

Das Image enthält Artefakte in Version `5.0.0-SNAPSHOT`, aber
[`docker/init.gradle`](../docker/init.gradle) verwendet ohne Systemproperty
`0.1.0-SNAPSHOT`. Deshalb kann die in Beispielen verwendete versionslose
Plugin-DSL die lokal eingebetteten Marker nicht auflösen.

Offline löst sich der Plugin-Marker mit expliziter Version zwar auf, dessen
transitive Dependencies werden jedoch nicht vollständig aus dem staged
Maven-Repository gefunden; Gradle versucht beispielsweise `commons-io:2.6` aus
dem Netzwerk zu laden. Nur der Legacy-`flatDir`-Pfad über `apply plugin:`
funktioniert vollständig offline.

Damit ist die dokumentierte moderne Nutzung des Images nicht reproduzierbar.

### Treiberparität

`DbConnector` erkennt weiterhin folgende JDBC-Familien:

- PostgreSQL
- SQLite
- Derby
- Oracle
- DuckDB
- SQL Server

Im Image enthalten sind aber nur:

- PostgreSQL `42.6.0`
- SQLite `3.43.0.0`
- DuckDB `1.5.2.0`

Die [Runtime-Dependencies](../gretl-core/build.gradle) bestätigen diese Auswahl.
Original-GRETL paketiert zusätzlich Derby, Oracle und SQL Server. Solange die
drei DB-Familien vom API-Vertrag weiter suggeriert werden, ist dies ein
Runtime-Defekt und kein reines Dokumentationsproblem.

### Release- und Supply-Chain-Vergleich

| Aspekt | gretl-next | Original |
|---|---|---|
| Java | 17 | 11 |
| Gradle | 7.6.4 | 7.6.4 |
| Image-Build | lokal, Hostarchitektur | Buildx Multi-Arch |
| Registry | keine Image-Publikation | Docker Hub und GHCR |
| Tags | lokaler Test-/SNAPSHOT-Tag | Versionen und `latest` |
| Gradle-Download | ohne SHA-Prüfung | mit SHA-256-Prüfung |
| SBOM | keine | CycloneDX |
| Image-E2E in CI | nein | vorgesehen |
| Control Plane | zwei erfolgreiche Boot-JARs | nicht vorhanden |
| Control-Plane-Container | nicht vorhanden | nicht anwendbar |

Die reifere Multi-Arch- und Publikationskonfiguration des Originals ist in
dessen
[Runtime-Build](https://github.com/sogis/gretl/blob/a9222dc0ffd08f8f76570e998101db041822df71/runtimeImage/build.gradle)
und
[Dockerfile](https://github.com/sogis/gretl/blob/a9222dc0ffd08f8f76570e998101db041822df71/runtimeImage/gretl/Dockerfile.ubi)
belegt.

Zusätzlich fiel im erfolgreichen `gretl-next`-Smoke auf, dass Log4j2 keine
Implementierung fand und auf `SimpleLogger` zurückfiel. Der
Gradle-/Worker-Logbridge funktioniert, die Runtime-Logging-Zusammenstellung
sollte aber bereinigt werden.

## Priorisierte Roadmap

### P0 – Ablöseblocker

1. **Runtime-Version vereinheitlichen**

   - Version in Publikation, Maven-Repository, Image-Metadaten und `init.gradle`
     aus derselben Quelle generieren.
   - CI-Smoke exakt mit der dokumentierten versionslosen `plugins {}`-DSL.

2. **Moderne Offline-Auflösung herstellen**

   - vollständiges Maven-Repository inklusive transitiver Dependencies stagen
     oder einen dokumentierten, getesteten alternativen Mechanismus
     bereitstellen;
   - netzwerkgesperrter Image-Test als obligatorisches CI-Gate.

3. **Fehlende Original-Funktionen entscheiden**

   - produktive Nutzung von `Publisher`, `DatabaseDocumentExport` und
     `PostgisRasterExport` inventarisieren;
   - portieren oder pro Job einen belastbaren Ersatz- oder Migrationsweg
     festlegen;
   - Control Plane nicht als Publisher-Ersatz deklarieren.

4. **JDBC-Vertrag bereinigen**

   - Derby, Oracle und SQL Server wieder paketieren und testen oder aus
     unterstütztem Vertrag und Migrationstabelle entfernen.

5. **Runtime-Release-Gate einführen**

   - Image bauen, Core- und GeoTools-Tasks ausführen und Offline-Smoke
     durchführen;
   - Multi-Arch-Image mit unveränderlichen Versionstags publizieren;
   - Release nur nach erfolgreichem Image-Test.

### P1 – Produktionsreife

- Control-Plane-E2E mit echtem Server, Worker, externem `gretl`, Logs, Secrets
  und Cancellation.
- Doclet-Vererbung reparieren und LSP-Metadaten gegen die öffentliche Task-API
  testen.
- Vollständige Task-Referenz und migrationsfähige Beispiele für alle 48
  Union-Tasks.
- Image-SBOM, OCI-Labels, Dependency- und Image-Scanning sowie
  Gradle-Download-Checksumme.
- Control-Plane- und Worker-Container oder dokumentierte Service-Pakete.
- Gradle-8-Kompatibilität und Runtime-Logging bereinigen.
- DuckDB-Extension-Strategie klären: `excel` ist neu vorhanden, `httpfs` und
  `aws` fehlen gegenüber dem Original.

### P2 – Weiterentwicklung

- JaCoCo oder vergleichbare Coverage-Trends, ohne Coverage-Prozent als
  alleiniges Qualitätsgate.
- Performance-, Parallelitäts- und Langzeittests für GeoTools-Worker und
  Control Plane.
- Release-Provenienz, Signierung und Checksummen für Images und Artefakte.
- Deployment-Beispiele und Betriebsmodelle für Control Plane, Worker und
  Hochverfügbarkeit.

## Empfehlung

- Kein allgemeiner produktiver Cutover auf den untersuchten Stand.
- Ein Pilot ist vertretbar für Jobs, die ausschliesslich PostgreSQL, SQLite oder
  DuckDB nutzen, keinen der drei fehlenden Tasks benötigen und auf die neue DSL
  migriert wurden.
- Vor dem Pilot sollte mindestens der Image-Versionsfehler behoben oder durch
  explizite Versionierung kontrolliert werden.
- Die vollständige Ablösung sollte erst nach erfolgreichem P0-Runtime-Gate und
  einem inventarbasierten Entscheid zu Publisher und Spezialexporten
  freigegeben werden.

Auch das Original ist auf seinem Vergleichscommit wegen des roten CI-Laufs
nicht unmittelbar releasefähig. Seine Runtime-, Image-Test- und
Publikationsarchitektur ist dennoch derzeit deutlich produktionsreifer als jene
von `gretl-next`.
