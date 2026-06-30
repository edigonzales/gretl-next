# Erfundene deutsche Beschreibungen

## Übersicht

Die deutschen `@LocaleText(locale = "de_CH", ...)`-Beschreibungen stammen aus zwei Quellen:

- **Original-GRETL** (`../gretl/`): Javadoc-Kommentare auf Getter-Methoden der originalen Tasks
  wurden übernommen und sprachlich verbessert (einheitliche Satzform: "Legt ... fest.",
  "Konfiguriert ...", "Fügt ... hinzu.")
- **Erfunden**: Wo keine Original-Vorlage existiert, wurden die deutschen Texte aus den
  englischen `description`-Feldern übersetzt.

Die folgenden Kategorien sind **komplett erfunden** (keine Vorlage im Original-GRETL):

### 1. Abstrakte Basisklassen in gretl-core (9 Dateien)

Diese Klassen existieren in dieser Form nicht im Original-GRETL. Alle ~52 Methoden-Beschreibungen
wurden aus den englischen `description`-Texten übersetzt.

| Klasse | Methoden |
|--------|---------|
| `AbstractDatabaseTask` | `database(jdbcUrl)`, `database(jdbcUrl, username, password)` |
| `AbstractIli2DbTask` | `databaseFile`, `database(jdbcUrl)`, `database(jdbcUrl, username, password)`, `schema`, `dbschema`, `modelNames`, `models`, `modelDirectories`, `modeldir`, `baskets`, `topics`, `dataset`, `datasetSubstring(Iterable)`, `datasetSubstring(Integer...)`, `logFile` |
| `AbstractIli2DbFileTask` | `datasetNames`, `datasetNamesFromTransferFiles`, `datasetNamesFromFiles`, `datasetNameSlice(int)`, `datasetNameSlice(int, int)` |
| `AbstractIli2DbTransferTask` | `transferFiles`, `repositoryDataIds` |
| `AbstractIli2DbExportTask` | `dataFiles`, `exportModels` |
| `AbstractIli2DbSchemaImportTask` | `iliFile`, `iliMetaAttrsFile` |
| `AbstractInterlisValidatorTask` | `dataFiles`, `modelNames`, `models`, `modelDirectories`, `modeldir`, `configFile`, `configRepositoryId`, `metaConfigFile`, `metaConfigRepositoryId`, `logFile`, `xtfLogFile` |
| `FtpTask` | `server`, `user`, `password`, `systemType`, `fileSeparator`, `passiveMode`, `controlKeepAliveTimeout` |
| `S3Task` | `accessKey`, `secretKey`, `bucketName`, `endpoint`, `region` |

### 2. `DuckDbSqlExecutor` in gretl-core

Komplett neuer Task ohne Original-Vorlage. Alle 9 Methoden-Beschreibungen + Task-Beschreibung übersetzt.

| Methode | Deutsch |
|---------|---------|
| Task | "Führt SQL in einer vorbereiteten DuckDB-Föderationssitzung aus." |
| `database` | "Konfiguriert die DuckDB-Datenbankdatei." |
| `inMemoryDatabase` | "Verwendet eine In-Memory-DuckDB-Datenbank anstelle einer Datenbankdatei." |
| `installExtensions` | "Installiert erforderliche DuckDB-Erweiterungen vor dem Laden. Für die lokale Entwicklung vorgesehen." |
| `sqlFiles` | "Fügt SQL-Dateien hinzu. Pfade werden relativ zum Gradle-Projekt aufgelöst." |
| `sources` | "Konfiguriert föderierte Quellen." |
| `targets` | "Konfiguriert beschreibbare Ziele für SQL und Exporte." |
| `exports` | "Konfiguriert Exporte, die nach den SQL-Dateien ausgeführt werden." |
| `sqlParameters` | "Setzt eine SQL-Parameter-Map für eine einzelne Ausführung aller SQL-Dateien." |
| `sqlParameterSets` | "Setzt mehrere SQL-Parameter-Maps. Für jede Map werden alle SQL-Dateien in Reihenfolge ausgeführt." |

### 3. gretl-geotools (3 Dateien)

Keine Original-Vorlage vorhanden. Alle 3 Task-Beschreibungen + 10 Methoden-Beschreibungen übersetzt.

| Klasse | Methode | Deutsch |
|--------|---------|---------|
| `ReadShapefile` | Task | "Liest ein Shapefile über die GeoTools-Worker-Laufzeitumgebung und protokolliert grundlegende Diagnosedaten." |
| | `shapefile` | "Konfiguriert das Eingabe-Shapefile." |
| | `crsCode` | "Konfiguriert den CRS-Code zum Lesen des Shapefiles." |
| `RasterReclassify` | Task | "Reclassifiziert Rasterwerte in ein neues Raster." |
| | `inputRaster` | "Konfiguriert die Eingabe-Rasterdatei." |
| | `outputRaster` | "Konfiguriert die Ausgabe-Rasterdatei." |
| | `breaks` | "Setzt streng monoton steigende Klassenbruchwerte." |
| | `noData` | "Setzt den No-Data-Wert für das Ausgabe-Raster." |
| `Vectorize` | Task | "Vektorisiert ausgewählte Rasterzellenwerte in ein GeoPackage." |
| | `inputRaster` | "Konfiguriert die Eingabe-Rasterdatei." |
| | `outputGeopackage` | "Konfiguriert die Ausgabe-GeoPackage-Datei." |
| | `band` | "Setzt den zu lesenden Raster-Band-Index." |
| | `cellValues` | "Setzt die zu vektorisierenden Rasterzellenwerte." |

---

## Nicht erfundene Beschreibungen

Alle konkreten Task-Klassen in gretl-core (Gzip, S3*, Curl, Av2ch, Av2geobau, Ftp*, Csv*, Gpkg*,
Json*, *Validator, *Ili2pg*, Ili2gpkg, Ili2duckdb, Db2Db, SqlExecutor, XslTransformer) haben
deutsche Beschreibungen, die auf den **Original-GRETL-Javadoc-Kommentaren** basieren und
sprachlich vereinheitlicht wurden. Diese sind daher nicht im obigen Inventar aufgeführt.

**Sprachliche Verbesserungen:**
- Einheitliche Satzstruktur: "Legt ... fest." statt "Zu transformierende ITF-Datei(en)."
- Einheitliches Verb: "Konfiguriert ..." für reine Konfigurationsmethoden
- Konsistente Begriffe: "Datenbank" statt "DB", "Datenbank-Tabelle" statt "DB-Tabelle"
