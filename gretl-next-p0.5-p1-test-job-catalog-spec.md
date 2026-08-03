# Spezifikation: P0.5 Runtime-Testarchitektur konsolidieren und P1 persistenten GRETL-Test-Job-Katalog einführen

**Ziel-Repository:** `https://github.com/edigonzales/gretl-next`
**Priorität:** P0.5 und P1
**Status:** verbindlicher Umsetzungsauftrag für einen LLM Coding Agent
**Stand:** 1. August 2026
**Ausgangsbasis:** aktueller `main` nach Umsetzung des dependency-geschlossenen Runtime-Images und des kombinierten Core-/GeoTools-P0-Gates
**Produktprinzip:** GRETL bleibt Gradle-first; der Testkatalog beschreibt reale Gradle-Consumer-Projekte und führt keinen eigenen Workflow- oder DAG-Mechanismus ein.
**Zentrales P0.5-Ziel:** letzte Doppelzuständigkeiten und Lifecycle-Unschärfen der Runtime-Testinfrastruktur beseitigen.
**Zentrales P1-Ziel:** positive produktnahe Consumer-Jobs genau einmal als persistierte echte Gradle-Projekte speichern und über mehrere Ausführungsbackends wiederverwenden.

**DSL-Policy:** Der persistierte Katalog prüft ausschließlich Groovy-Gradle-
Builds. Kotlin DSL kann durch Gradle weiterhin zufällig funktionieren, ist aber
kein GRETL-Vertrag und wird weder getestet noch dokumentiert.

---

## 1. Auftrag

Implementiere zwei eng zusammenhängende Arbeitspakete.

### P0.5 – Konsolidierung

Räume die nach dem Dependency-Closure-Refactoring verbliebenen Architekturprobleme auf:

- `--offline` besitzt genau eine verbindliche Quelle;
- Runtime-Lifecycle und Gradle-Dependency-Policy bleiben getrennt;
- One-shot- und Service-Ausführung besitzen eindeutige Verantwortlichkeiten;
- temporäre Gradle-User-Homes können nicht durch vorbereitende APIs lecken;
- Terminologie beschreibt präzise „keine Remote-Downloads“, nicht fälschlich „ausschließlich im Image gebündelte Artefakte“;
- bestehende Tests bleiben grün;
- keine neue Testarchitektur wird parallel zur bestehenden Infrastruktur aufgebaut.

### P1 – Persistenter Test-Job-Katalog

Führe einen repositoryweiten Katalog realer Gradle-Consumer-Jobs ein.

Ein positiver fachlicher Testjob wird künftig einmalig persistiert mit:

- echter `build.gradle`-Datei;
- eigenen Eingabedateien;
- eigenen erwarteten Ergebnissen beziehungsweise semantischen Assertions;
- einem kleinen maschinenlesbaren `job.yaml`;
- eindeutigen Taskpfaden und Taskklassen;
- einer expliziten Liste unterstützter Ausführungsziele.

Derselbe Job muss ohne dupliziertes Buildscript mindestens über folgende Backends ausführbar sein:

```text
PLUGIN_CLASSPATH
PUBLISHED_ARTIFACT
RUNTIME_IMAGE_ONE_SHOT
```

Eine repräsentative Teilmenge muss zusätzlich laufen über:

```text
RUNTIME_IMAGE_SERVICE
```

Die P1-Pilotmenge besteht verbindlich aus:

```text
combined-core-geotools-pipeline
core-gzip
core-sqlite
geotools-read-shapefile
```

---

## 2. Nichtziele und dauerhafte Abgrenzung

### 2.1 Keine Migration von `sogis/gretljobs`

Die Migration bestehender Jobs aus `https://github.com/sogis/gretljobs` ist dauerhaft außerhalb des Projektumfangs.

Der Coding Agent darf insbesondere nicht:

- bestehende `gretljobs` kopieren;
- bestehende Jobs auf die neue DSL migrieren;
- Legacy-Kompatibilitätsadapter entwickeln;
- Codemods oder Migrationsskripte erstellen;
- historische Task-APIs nur wegen alter Jobs erhalten;
- `gretljobs` als Abnahmetestsuite verwenden;
- die P1-Fertigstellung von einer späteren Migration abhängig machen.

Alle P1-Testjobs werden eigens für `gretl-next` erstellt oder aus bereits vorhandenen `gretl-next`-Testfixtures konsolidiert.

### 2.2 Keine neue Orchestrierungsengine

Der Test-Job-Katalog ist kein eigener Workflow- oder DAG-Executor.

Nicht zulässig:

- eigene Taskabhängigkeitsgraphen im YAML;
- eigene Scheduler;
- eigene Retry-Semantik;
- eigene Parallelitätssteuerung für GRETL-Tasks;
- Nachbau von Gradle-Inputs, Outputs oder Taskabhängigkeiten im Katalog.

Die fachliche Pipeline bleibt vollständig im Gradle-Build:

```text
Sources
→ GRETL Tasks / Transforms
→ Gradle DAG
→ Targets
→ Observability
```

Das Manifest beschreibt lediglich:

- welchen Gradle-Build es gibt;
- welche Top-Level-Tasks aufgerufen werden;
- welche GRETL-Taskklassen dabei tatsächlich erwartet werden;
- auf welchen Testbackends der Job laufen soll;
- welche externen Testfähigkeiten der Job benötigt.

### 2.3 Keine vollständige Testmigration in einem Schritt

P1 migriert verbindlich vier Pilotjobs.

Weitere Jobs wie PostGIS, HTTP, S3, FTP, DuckDB Spatial und INTERLIS werden architektonisch vorbereitet, aber erst in einem folgenden P2-Arbeitspaket vollständig persistiert, sofern sie nicht für einen P1-Nachweis zwingend benötigt werden.

### 2.4 Keine Abschwächung bestehender P0-Gates

Bestehende Tests für folgende Eigenschaften bleiben erhalten:

- Pluginreihenfolge;
- Pluginunabhängigkeit;
- Shared Services;
- Classloader- und Worker-Isolation;
- Configuration Cache;
- Fehlerpropagation;
- Published-Artifact-Verträge;
- Runtime-Image-Dependency-Closure;
- Servicecontainer und Daemon-Wiederverwendung;
- Image-Contract und Distribution-Checks.

Positive Buildscript-Duplikate dürfen entfernt werden. Die zugrunde liegenden Beweisziele dürfen nicht entfernt werden.

---

## 3. Verbindlicher Consumer-Vertrag

Unterstützt wird ausschließlich die moderne Plugin-DSL.

Groovy DSL:

```groovy
plugins {
    id 'ch.so.agi.gretl'
}
```

Kombiniert:

```groovy
plugins {
    id 'ch.so.agi.gretl'
    id 'ch.so.agi.gretl.geotools'
}
```

Nicht Bestandteil des P1-Jobkatalogs:

```groovy
apply plugin: 'ch.so.agi.gretl'
```

oder manuell aufgebaute historische Buildscript-Classpaths.

Die persistierten Jobs dürfen insbesondere nicht enthalten:

```text
apply plugin:
buildscript {
includeBuild
mavenLocal()
flatDir
withPluginClasspath
gretl-core/build
gretl-geotools/build
build/classes
build/resources
```

---

# Teil A – P0.5 Runtime-Testarchitektur konsolidieren

## 4. P0.5-Ausgangslage

Der Agent muss den aktuellen Repository-Stand vor Änderungen vollständig prüfen.

Zum Zeitpunkt dieser Spezifikation gilt sinngemäß:

- der Runtime-Launcher fügt `--offline` hinzu;
- `RuntimeImageGradleArguments` fügt ebenfalls `--offline` hinzu;
- `RuntimeImageBuildExecutor` unterstützt formal `ONE_SHOT` und `SERVICE`, führt aber stets einen kurzlebigen `docker run --rm` aus;
- der echte Servicebetrieb läuft über `RuntimeImageServiceContainer`;
- `RuntimeImageBuildExecutor.toDockerRunRequest(GretlBuildRequest)` kann beim Vorbereiten ein temporäres Gradle-Home erzeugen, ohne dessen Handle an den Aufrufer zurückzugeben;
- die Runtime-Dokumentation verwendet teilweise `bundled-only`, obwohl ein zusätzlich gemountetes lokales Maven-Repository zulässig sein kann;
- Source-/Published-TestKit und Runtime-Image besitzen zwei verschiedene Interfaces mit demselben einfachen Namen `GretlBuildExecutor`.

P0.5 behebt die ersten vier Punkte vollständig und bereitet die Interface-Vereinheitlichung für P1 vor.

---

## 5. Eine einzige Quelle für `--offline`

### 5.1 Verbindliche Entscheidung

Der Runtime-Launcher ist die einzige Produktquelle für `--offline`.

Bevorzugte Datei:

```text
docker/gretl
```

Bevorzugter Inhalt:

```sh
#!/bin/sh
set -eu

exec gradle \
  --offline \
  --init-script /opt/gretl/init/gretl.init.gradle \
  "$@"
```

### 5.2 Begründung

Der Launcher ist:

- Bestandteil des ausgelieferten Images;
- gemeinsamer Einstiegspunkt für One-shot und Service;
- unabhängig von der Java-Testinfrastruktur;
- die einzige Stelle, die den Produktvertrag zuverlässig für reale Benutzer erzwingen kann.

### 5.3 `RuntimeImageGradleArguments` umbenennen

Benenne bevorzugt `RuntimeImageGradleArguments` um zu:

```text
RuntimeImageLifecycleArguments
```

Die Klasse darf danach nur noch ergänzen:

```text
--console=plain
--no-daemon
--daemon
```

Sie darf nicht mehr ergänzen:

```text
--offline
```

### 5.4 Ziel-API

```java
public final class RuntimeImageLifecycleArguments {

    public List<String> arguments(
            RuntimeExecutionMode executionMode,
            List<String> requestedArguments);

    private void rejectForbiddenArguments(
            List<String> requestedArguments);

    private void addIfAbsent(
            List<String> arguments,
            String argument);
}
```

### 5.5 Verbindliche Regeln

Für `ONE_SHOT`:

```text
--console=plain
--no-daemon
```

Für `SERVICE`:

```text
--console=plain
--daemon
```

Weiterhin verboten:

```text
--refresh-dependencies
```

`--offline` darf als Benutzerargument toleriert und unverändert weitergereicht werden, soll aber weder benötigt noch durch die Lifecycle-Klasse ergänzt werden.

### 5.6 Konfliktregeln

```java
case ONE_SHOT -> {
    if (arguments.contains("--daemon")) {
        throw new IllegalArgumentException(
            "ONE_SHOT execution must not use --daemon.");
    }
    addIfAbsent(arguments, "--no-daemon");
}
case SERVICE -> {
    if (arguments.contains("--no-daemon")) {
        throw new IllegalArgumentException(
            "SERVICE execution must not use --no-daemon.");
    }
    addIfAbsent(arguments, "--daemon");
}
```

### 5.7 Tests

Datei:

```text
RuntimeImageLifecycleArgumentsTest.java
```

Methoden:

```java
@Test void oneShotAddsNoDaemon();
@Test void serviceAddsDaemon();
@Test void bothModesAddPlainConsole();
@Test void lifecycleArgumentsDoNotAddOffline();
@Test void preservesExplicitOfflineWithoutDuplicatingIt();
@Test void oneShotRejectsDaemon();
@Test void serviceRejectsNoDaemon();
@Test void rejectsRefreshDependencies();
@Test void preservesProjectProperties();
@Test void preservesSystemProperties();
@Test void preservesTaskNamesAndOrdering();
```

### 5.8 Launcher-Contract-Test

Datei:

```text
RuntimeImageLauncherContractTest.java
```

Methoden:

```java
@Test void launcherAddsOfflineExactlyOnce();
@Test void launcherUsesBundledInitScript();
@Test void launcherForwardsAllUserArguments();
@Test void launcherDoesNotForceNoDaemon();
@Test void launcherDoesNotForceDaemon();
```

Prüfe den tatsächlichen Launcherinhalt oder führe einen kleinen Container-Canary aus. Ein Test darf nicht nur dieselbe Java-Argumentklasse testen.

---

## 6. Präzise Dependency-Terminologie

### 6.1 Produktvertrag

Der tatsächliche Vertrag lautet:

> Gradle führt während eines Runtime-Jobs keine Remote-Downloads für Plugins oder Dependencies aus.

Lokal verfügbar dürfen sein:

- das image-interne Maven-Repository;
- ein bewusst zusätzlich gemountetes lokales Maven-Repository;
- ein image- oder serviceeigenes Gradle User Home;
- lokale Projektdateien.

### 6.2 Verbindliche Begriffe

Verwende für die allgemeine Runtime-Policy bevorzugt:

```text
LOCAL_ONLY_RESOLUTION
NO_REMOTE_DOWNLOADS
```

Bevorzugter Dokumentationsbegriff:

```text
local-only Gradle dependency resolution
```

`bundled-only` darf nur den Standardfall beschreiben, in dem ausschließlich die Image-Distribution verwendet wird.

### 6.3 Keine unnötige Produkt-Enum

Da jeder reguläre Runtime-Image-Aufruf dieselbe Policy besitzt, soll nicht zwingend eine neue Runtime-Policy-Enum eingeführt werden.

Nicht erwünscht:

```java
ONLINE
OFFLINE
BUNDLED_ONLY
```

### 6.4 Umbenennungen

Prüfe und verbessere Namen wie:

```text
rejectsNonBundledThirdPartyPlugin
```

zu:

```text
failsWhenThirdPartyPluginIsNotAvailableLocally
```

und gegebenenfalls:

```text
RuntimeImageBundledPluginResolutionTest
```

zu:

```text
RuntimeImageLocalPluginResolutionTest
```

### 6.5 Dokumentation

Verbindlicher Text:

> The GRETL runtime launcher starts Gradle with `--offline`. Gradle may resolve plugins and dependencies from local repositories and local caches, but it does not contact remote repositories. This does not restrict application-level network access by GRETL tasks.

---

## 7. One-shot- und Service-Verantwortung trennen

### 7.1 Verbindliche Architektur

`RuntimeImageBuildExecutor` ist ausschließlich ein One-shot-Executor.

`RuntimeImageServiceContainer` beziehungsweise ein P1-Service-Backend ist ausschließlich für langlebige Serviceausführung verantwortlich.

### 7.2 `RuntimeImageBuildExecutor`

Der Executor darf nur akzeptieren:

```java
RuntimeExecutionMode.ONE_SHOT
```

Bevorzugte Prüfung:

```java
private void requireOneShot(GretlBuildRequest request) {
    if (request.runtimeExecutionMode() != RuntimeExecutionMode.ONE_SHOT) {
        throw new IllegalArgumentException(
            "RuntimeImageBuildExecutor supports only ONE_SHOT execution. "
            + "Use RuntimeImageServiceContainer for SERVICE execution.");
    }
}
```

Diese Prüfung erfolgt vor:

- Erzeugung eines Gradle User Home;
- Erstellung eines Docker-Requests;
- Start eines Prozesses.

### 7.3 Kein Service-Vortäuschen durch `docker run --rm`

Nicht zulässig:

- `SERVICE` als Argument an `RuntimeImageBuildExecutor`;
- `--daemon` in einem kurzlebigen `docker run --rm` als Servicebeweis;
- gemeinsamer Executor, der intern je nach Enum völlig unterschiedliche Containerlebenszyklen versteckt.

### 7.4 Servicecontainer

`RuntimeImageServiceContainer` bleibt zuständig für:

- Containerstart;
- langlebiges Gradle User Home;
- `docker exec ... gretl`;
- Gradle-Daemon-Wiederverwendung;
- Container-Cleanup;
- Service-Netzwerk;
- optionale lokale read-only Repository-Mounts.

### 7.5 Tests

```java
class RuntimeImageBuildExecutorLifecycleTest {
    @Test void acceptsOneShotRequest();
    @Test void rejectsServiceRequestBeforeCreatingGradleHome();
    @Test void rejectsServiceRequestBeforeCallingDocker();
}
```

```java
class RuntimeImageServiceContainerLifecycleTest {
    @Test void executesJobsThroughGretlLauncher();
    @Test void reusesGradleDaemon();
    @Test void usesPersistentContainerGradleHome();
    @Test void supportsDefaultNetwork();
    @Test void supportsNamedNetwork();
}
```

---

## 8. Temporäre Gradle-Home-Ressourcen korrekt besitzen

### 8.1 Problem

Eine Methode darf kein temporäres Gradle User Home erzeugen und danach nur einen `DockerRunRequest` zurückgeben, ohne dem Aufrufer das zugehörige Lifecycle-Handle zu übertragen.

### 8.2 Verbindliche Lösung

Entferne oder beschränke:

```java
public DockerRunRequest toDockerRunRequest(GretlBuildRequest request);
```

Bevorzugt bleibt nur:

```java
DockerRunRequest toDockerRunRequest(
        GretlBuildRequest request,
        Path gradleUserHome);
```

mit package-private Sichtbarkeit.

Alternativ darf eingeführt werden:

```java
public record PreparedRuntimeExecution(
        DockerRunRequest dockerRequest,
        GradleUserHomeHandle gradleUserHome)
        implements AutoCloseable {

    @Override
    public void close() {
        gradleUserHome.close();
    }
}
```

### 8.3 Bevorzugung

Bevorzugt wird die kleinere Lösung:

- Request-Erzeugung mit explizitem bereits vorbereitetem Pfad;
- Ressourcenbesitz ausschließlich in `executeInternal()`;
- keine öffentliche Prepare-API, solange sie nicht von Tests oder P1-Backends benötigt wird.

### 8.4 Tests

```java
class RuntimeImageBuildExecutorResourceTest {
    @Test void successfulExecutionClosesTemporaryGradleHome();
    @Test void failedExecutionClosesTemporaryGradleHome();
    @Test void dockerFailureClosesTemporaryGradleHome();
    @Test void rejectedServiceRequestCreatesNoTemporaryHome();
    @Test void commandPreparationCannotLeakUnownedTemporaryDirectory();
}
```

Verwende eine kontrollierte Teststrategie, die erstellte und geschlossene Handles zählt.

---

## 9. P0.5-Kompatibilität

### 9.1 Bestehende Konstruktoren

Bestehende Konstruktoren dürfen delegierend erhalten bleiben, wenn dies die Migration erleichtert. Die delegierende Klasse darf aber keine zweite `--offline`-Quelle behalten.

### 9.2 Kein unnötiger Public-API-Bruch

`gretl-test-support` ist interne Testinfrastruktur. Trotzdem sollen bestehende Tests schrittweise migriert werden.

Nicht erforderlich:

- sofortige Entfernung jedes alten Klassennamens;
- binäre Kompatibilität zu externen Konsumenten;
- Übergangsadapter über mehrere Releases.

### 9.3 P0.5-Abschluss

P0.5 ist abgeschlossen, bevor P1-Backends auf der Runtime-Infrastruktur aufgebaut werden.

---

# Teil B – P1 Persistenter Test-Job-Katalog

## 10. Zielbild

Positive fachliche Consumer-Szenarien werden als echte Gradle-Projekte unter einem zentralen Repository-Verzeichnis gespeichert.

Verbindlicher Root-Pfad:

```text
test-jobs/
```

Das Verzeichnis ist:

- kein Gradle-Subprojekt;
- kein Produktionsartefakt;
- kein Maven-Publikationsinhalt;
- kein Runtime-Image-Inhalt;
- kein Ersatz für `gretljobs`;
- eine Sammlung testbarer Consumer-Fixtures.

### 10.1 Zielstruktur

```text
test-jobs/
├── core/
│   ├── gzip/
│   └── sqlite/
├── geotools/
│   └── read-shapefile/
└── combined/
    └── core-geotools-pipeline/
```

### 10.2 Eigentümermodul

Erzeuge bevorzugt ein neues internes Testsubprojekt:

```text
gretl-job-tests
```

Das Modul:

- enthält die parameterisierten Jobtests;
- hängt von `gretl-test-support` ab;
- enthält Host-Assertion-Abhängigkeiten;
- veröffentlicht keine Artefakte;
- wird nicht in das Runtime-Image aufgenommen;
- besitzt keinen öffentlichen Pluginmarker;
- ist alleiniger Eigentümer der backendübergreifenden positiven Jobausführung.

### 10.3 Alternative

Eine andere Eigentümerstruktur ist nur zulässig, wenn:

- keine positive Joblogik zwischen `gretl-core`, `gretl-geotools`, `gretl-combined-tests` und Runtime-Tests dupliziert wird;
- alle vier Pilotjobs über dieselbe Katalog- und Backendabstraktion laufen;
- das Testmodul nicht publiziert wird;
- Host-Assertions nicht in Consumer-Classpaths lecken.

---

## 11. Trennung der Verantwortlichkeiten

Die Zielarchitektur besteht aus fünf Schichten.

```text
TestJobCatalog
    ↓
TestJobMaterializer
    ↓
TestJobExecutionBackend
    ↓
GretlBuildResult + TaskExecutionTrace
    ↓
TestJobAssertions
```

### 11.1 `TestJobCatalog`

Findet, lädt und validiert persistierte Jobdefinitionen.

### 11.2 `TestJobMaterializer`

Kopiert einen unveränderlichen Job in ein temporäres Ausführungsverzeichnis und erzeugt ausschließlich modeabhängige Bootstrap-Dateien.

### 11.3 `TestJobExecutionBackend`

Führt den materialisierten Gradle-Build in einem bestimmten technischen Ziel aus.

### 11.4 `TaskExecutionTrace`

Beweist, welche Gradle-Taskpfade und konkreten Taskklassen tatsächlich ausgeführt wurden.

### 11.5 `TestJobAssertions`

Prüft fachliche Outputs und backendunabhängige Erwartungen.

---

## 12. Neues Subprojekt `gretl-job-tests`

### 12.1 `settings.gradle`

Ergänze:

```groovy
include 'gretl-job-tests'
```

### 12.2 Build-Datei

```text
gretl-job-tests/build.gradle
```

Grundstruktur:

```groovy
plugins {
    id 'java'
}

base {
    archivesName = 'gretl-job-tests'
}
```

### 12.3 Abhängigkeiten

Mindestens:

```groovy
dependencies {
    testImplementation gradleTestKit()
    testImplementation project(':gretl-test-support')
    testImplementation "org.junit.jupiter:junit-jupiter-api:${junitVersion}"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:${junitVersion}"

    testImplementation "org.geotools:gt-main:${geotoolsVersion}"
    testImplementation "org.geotools:gt-geotiff:${geotoolsVersion}"
    testImplementation "org.geotools:gt-shapefile:${geotoolsVersion}"
    testImplementation "org.geotools:gt-epsg-hsql:${geotoolsVersion}"
}
```

Füge nur tatsächlich benötigte Host-Abhängigkeiten hinzu.

### 12.4 Gemeinsamer Source-Plugin-Classpath

Definiere wie im kombinierten Testmodul eine isolierte Konfiguration:

```groovy
configurations {
    canonicalJobPluginClasspath {
        canBeConsumed = false
        canBeResolved = true
        visible = false
        transitive = true
    }
}

dependencies {
    canonicalJobPluginClasspath project(':gretl-core')
    canonicalJobPluginClasspath project(':gretl-geotools')
}
```

Erzeuge eine deterministische Classpath-Datei.

Der Classpath darf nicht enthalten:

```text
gretl-test-support
gretl-job-tests
gretl-combined-tests
JUnit
Testcontainers
Host-GeoTools-Assertions
Sources-JARs
Javadoc-JARs
Testoutputs
```

### 12.5 Publikationsausschluss

Erzeuge:

```text
assertJobTestsNotPublished
```

Der Task prüft mindestens:

- `published-test/maven-repo`;
- Runtime-Image-Maven-Repository;
- Snapshot-Publikationsoutput.


## 13. Verzeichnis- und Dateivertrag eines Jobs

### 13.1 Mindeststruktur

```text
<job-directory>/
├── job.yaml
├── build.gradle
├── input/
└── expected/
```

`input/` und `expected/` sind optional, wenn ein Job sie fachlich nicht benötigt.

### 13.2 Build-Datei

Ein Job enthält genau eine geprüfte Build-Datei:

```text
build.gradle
```

### 13.3 Keine persistierten modeabhängigen Settings

Standardmäßig enthält ein Job keine feste `settings.gradle`.

Der Materializer erzeugt Settings je nach Backend.

Ausnahmen sind nur zulässig für einen gezielten Settings-Vertragstest, nicht für normale positive Jobs.

### 13.4 Unveränderlichkeit

Der Kataloginhalt wird nie direkt ausgeführt.

Vor jedem Test wird er in ein temporäres Verzeichnis kopiert.

Das Ausführungsbackend darf den ursprünglichen Katalog nicht verändern.

### 13.5 Bytegleiche Builddatei

Nach Materialisierung muss gelten:

```java
assertArrayEquals(
    Files.readAllBytes(sourceBuildFile),
    Files.readAllBytes(materializedBuildFile));
```

Der Materializer darf das Buildscript nicht templatisieren oder umschreiben.

Modeabhängige Werte werden ausschließlich geliefert über:

- `settings.gradle`;
- Gradle-Properties `-P...`;
- System Properties `-D...`;
- Environment-Variablen;
- Mounts;
- Docker-Netzwerk.

---

## 14. `job.yaml` Schema Version 1

### 14.1 Beispiel

```yaml
schemaVersion: 1
id: combined-core-geotools-pipeline
description: Core to GeoTools to Core raster pipeline
category: combined

builds:
  - id: groovy
    file: build.gradle

entryTasks:
  - packageRaster

expectedTasks:
  - path: :generateRaster
    className: ch.so.agi.gretl.tasks.XslTransformer
  - path: :reclassifyRaster
    className: ch.so.agi.gretl.geotools.tasks.RasterReclassify
  - path: :packageRaster
    className: ch.so.agi.gretl.tasks.Gzip

executionTargets:
  pluginClasspath: required
  publishedArtifact: required
  runtimeImageOneShot: required
  runtimeImageService: required

capabilities:
  - filesystem
  - geotools-worker

assertions: combined-core-geotools-pipeline

timeoutSeconds: 300
```

### 14.2 Zulässige Top-Level-Felder

```text
schemaVersion
id
description
category
builds
entryTasks
expectedTasks
executionTargets
capabilities
assertions
timeoutSeconds
```

Unbekannte Felder führen standardmäßig zu einem Validierungsfehler.

### 14.3 `id`

Regex:

```text
[a-z][a-z0-9]*(?:-[a-z0-9]+)*
```

Der ID-Wert muss repositoryweit eindeutig sein.

### 14.4 `category`

Zulässige Startwerte:

```text
core
geotools
combined
```

Später erweiterbar:

```text
interlis
database
network
validator
```

### 14.5 `builds`

Mindestens ein Eintrag.

Record:

```java
public record TestJobBuildVariant(
        String id,
        String file) {
}
```

Regeln:

- `file` verweist auf `.gradle`;
- Pfad ist relativ;
- kein `..`;
- Datei existiert;
- keine zwei Varianten besitzen dieselbe ID;
- keine zwei Varianten verweisen auf dieselbe Datei.

### 14.6 `entryTasks`

Mindestens ein Taskname.

Eintrag darf sein:

```text
packageRaster
```

oder vollständig:

```text
:subproject:packageRaster
```

Keine CLI-Optionen in `entryTasks`.

### 14.7 `expectedTasks`

Jeder Eintrag:

```java
public record ExpectedTaskExecution(
        String path,
        String className) {
}
```

Regeln:

- Pfad beginnt mit `:`;
- Klassenname ist vollqualifiziert;
- keine Duplikate;
- mindestens eine GRETL-Taskklasse pro Job;
- ein Aggregator-Task ohne GRETL-Klasse darf zusätzlich erwartet werden.

### 14.8 `executionTargets`

Enum:

```java
public enum TestJobExecutionRequirement {
    REQUIRED,
    OPTIONAL,
    NOT_APPLICABLE
}
```

Ziele:

```java
public enum TestJobExecutionTarget {
    PLUGIN_CLASSPATH,
    PUBLISHED_ARTIFACT,
    RUNTIME_IMAGE_ONE_SHOT,
    RUNTIME_IMAGE_SERVICE
}
```

In YAML:

```text
required
optional
not-applicable
```

### 14.9 `capabilities`

Startwerte:

```text
filesystem
sqlite
geotools-worker
configuration-cache
```

P1 definiert bereits reservierte spätere Werte:

```text
postgis
http
ftp
s3
duckdb-spatial
```

Die Capability-Liste darf keine Endpointwerte oder Secrets enthalten.

### 14.10 `assertions`

Schlüssel für ein Assertion-Registry-Objekt.

Keine vollqualifizierten Java-Klassennamen im YAML.

### 14.11 `timeoutSeconds`

- positive Ganzzahl;
- mindestens 10;
- höchstens 1800;
- Default 300.

---

## 15. Java-Modellklassen

Package:

```text
ch.so.agi.gretl.test.job
```

### 15.1 `TestJobDescriptor`

```java
public record TestJobDescriptor(
        int schemaVersion,
        String id,
        String description,
        String category,
        List<TestJobBuildVariant> builds,
        List<String> entryTasks,
        List<ExpectedTaskExecution> expectedTasks,
        Map<TestJobExecutionTarget,
            TestJobExecutionRequirement> executionTargets,
        Set<String> capabilities,
        String assertions,
        Duration timeout,
        Path sourceDirectory) {

    public TestJobDescriptor {
        // defensive copies and null checks
    }

    public TestJobBuildVariant requireBuild(String variantId);

    public TestJobExecutionRequirement requirementFor(
            TestJobExecutionTarget target);

    public boolean supports(TestJobExecutionTarget target);
}
```

### 15.2 `TestJobId`

Optionaler Value Type:

```java
public record TestJobId(String value) {
    public TestJobId {
        // validate regex
    }

    @Override
    public String toString() {
        return value;
    }
}
```

Der Agent darf bei sinnvoller Einfachheit bei `String` bleiben, muss dann aber zentral validieren.

### 15.3 YAML Parsing

Verwende eine kleine robuste YAML-Library im internen Testmodul beziehungsweise `gretl-test-support`.

Zulässig:

- Jackson YAML;
- SnakeYAML Engine.

Nicht zulässig:

- selbstgeschriebener YAML-Parser;
- reguläre Ausdrücke zur strukturellen YAML-Auswertung;
- Ausführen von YAML-Tags oder benutzerdefinierten Konstruktoren.

Parser muss unbekannte Felder ablehnen.

---

## 16. `TestJobCatalog`

### 16.1 Interface

```java
public interface TestJobCatalog {
    List<TestJobDescriptor> all();
    Optional<TestJobDescriptor> find(String id);
    TestJobDescriptor require(String id);
    Stream<TestJobDescriptor> supporting(TestJobExecutionTarget target);
    Path rootDirectory();
}
```

### 16.2 Implementierung

```java
public final class FileSystemTestJobCatalog
        implements TestJobCatalog {

    public static FileSystemTestJobCatalog load(Path rootDirectory);

    FileSystemTestJobCatalog(
            Path rootDirectory,
            TestJobYamlReader reader,
            TestJobDescriptorValidator validator);
}
```

### 16.3 Discovery

Suche rekursiv nach `job.yaml`.

Regeln:

- symbolische Links standardmäßig nicht verfolgen;
- deterministische Sortierung nach Job-ID;
- jede `job.yaml` genau einmal;
- keine versteckten Buildverzeichnisse einlesen;
- `build/`, `.gradle/`, `.git/` ignorieren;
- doppelte IDs ablehnen.

### 16.4 Fehlerdiagnose

Fehler nennt mindestens:

- Jobdatei;
- Feld;
- ungültigen Wert;
- erwartete Form.

Beispiel:

```text
Invalid test job descriptor:
  file: test-jobs/core/gzip/job.yaml
  field: expectedTasks[0].path
  value: compressFile
  expected: absolute Gradle task path beginning with ':'
```

### 16.5 Tests

```java
class FileSystemTestJobCatalogTest {
    @Test void loadsAllJobsDeterministically();
    @Test void findsJobById();
    @Test void rejectsDuplicateIds();
    @Test void rejectsUnknownYamlField();
    @Test void rejectsMissingBuildFile();
    @Test void rejectsPathTraversal();
    @Test void ignoresBuildDirectories();
    @Test void doesNotFollowSymlinkOutsideCatalog();
}
```

---

## 17. `TestJobDescriptorValidator`

### 17.1 API

```java
public final class TestJobDescriptorValidator {
    public void validate(TestJobDescriptor descriptor);
    public List<TestJobValidationError> errors(TestJobDescriptor descriptor);
}
```

### 17.2 Fehlerrecord

```java
public record TestJobValidationError(
        String field,
        String message) {
}
```

### 17.3 Verbindliche Prüfungen

- `schemaVersion == 1`;
- ID-Regex;
- Kategorie zulässig;
- Beschreibung nicht leer;
- mindestens ein Build;
- Builddateien vorhanden;
- keine Pfadflucht;
- mindestens ein Entry-Task;
- mindestens ein Expected Task;
- alle Taskpfade absolut;
- alle Klassennamen plausibel;
- alle vier Zielschlüssel vorhanden;
- mindestens Source, Published und Runtime One-shot `REQUIRED` für Pilotjobs;
- Assertion-Key vorhanden;
- Timeout im erlaubten Bereich;
- keine verbotenen Consumer-Konstrukte in Builddateien;
- kein `settings.gradle` im normalen Pilotjob;
- keine generierten Outputs im Katalog.

### 17.4 Verbotene Dateinamen im Katalog

```text
.gradle/
build/
.gradle-cache/
*.class
*.jar
*.log
```

Ausnahme: fachliche Binärfixtures wie Shapefiles sind erlaubt.

---

## 18. `TestJobMaterializer`

### 18.1 Ziel

Erzeugt ein isoliertes temporäres Consumer-Projekt.

### 18.2 API

```java
public interface TestJobMaterializer {
    MaterializedTestJob materialize(
            TestJobDescriptor descriptor,
            TestJobBuildVariant build,
            TestJobExecutionTarget target,
            Path destinationRoot);
}
```

### 18.3 Implementierung

```java
public final class DefaultTestJobMaterializer
        implements TestJobMaterializer {

    private final TestJobSettingsRenderer settingsRenderer;
    private final TaskTraceBootstrap traceBootstrap;

    @Override
    public MaterializedTestJob materialize(...);

    private void copyCatalogFiles(...);
    private void writeSettings(...);
    private void writeTraceBootstrap(...);
    private void verifyBuildFileUnchanged(...);
}
```

### 18.4 `MaterializedTestJob`

```java
public record MaterializedTestJob(
        TestJobDescriptor descriptor,
        TestJobBuildVariant buildVariant,
        TestJobExecutionTarget target,
        Path projectDirectory,
        Path buildFile,
        Path settingsFile,
        Path traceFile) {

    public Path resolve(String relativePath);
}
```

### 18.5 Kopierregeln

Kopiere:

- ausgewählte Builddatei unter ihrem Standardnamen;
- `input/`;
- `expected/`;
- weitere fachliche Dateien;
- keine `job.yaml` in das Consumer-Projekt, außer für Diagnosezwecke unter `.gretl-test/`;
- keine alternative Buildvariante;
- keine bestehenden Outputs.

Für Groovy:

```text
build.gradle
```

### 18.6 Zielverzeichnis

Beispiel:

```text
<temp>/combined-core-geotools-pipeline/groovy/plugin-classpath/
```

Pfad muss eindeutig sein nach:

- Job-ID;
- Buildvariante;
- Backend;
- Testlauf.

### 18.7 Unveränderlichkeitsprüfung

Vor und nach Kopie SHA-256 vergleichen.

```java
private String sha256(Path file);
```

Ein Unterschied führt zu:

```text
Materialized build file differs from canonical source.
```

### 18.8 Tests

```java
class DefaultTestJobMaterializerTest {
    @Test void copiesGroovyBuildByteIdentically();
    @Test void copiesInputTree();
    @Test void copiesExpectedTree();
    @Test void copiesOnlySelectedBuildVariant();
    @Test void generatesTargetSpecificSettings();
    @Test void createsTraceBootstrap();
    @Test void rejectsDestinationInsideCatalog();
    @Test void doesNotCopyBuildOrGradleDirectories();
}
```

---

## 19. Modeabhängige Settings

### 19.1 `TestJobSettingsRenderer`

```java
public interface TestJobSettingsRenderer {
    String renderGroovy(TestJobSettingsRequest request);
}
```

### 19.2 Request

```java
public record TestJobSettingsRequest(
        String projectName,
        TestJobExecutionTarget target,
        Optional<URI> publishedRepository,
        Optional<String> pluginVersion) {
}
```

### 19.3 Source Plugin-Classpath

Settings enthalten nur:

```groovy
rootProject.name = 'core-gzip'
```

Pluginauflösung erfolgt über TestKit `withPluginClasspath(...)`.

### 19.4 Published Artifact

Verwende oder erweitere `GretlTestProjectSettings`.

Das isolierte Published-Test-Repository steht an erster Stelle. Die aktuelle GRETL-Version wird in `pluginManagement.plugins` gesetzt.

### 19.5 Runtime Image

Settings enthalten nur:

```groovy
rootProject.name = 'core-gzip'
```

Das Runtime-Init-Script übernimmt:

- lokales Maven-Repository;
- gebündelte Version;
- Pluginmarker.

Der Materializer darf im Runtime-Modus keine Remote-Repositories erzeugen.

### 19.6 Tests

```java
class DefaultTestJobSettingsRendererTest {
    @Test void sourceSettingsContainOnlyProjectName();
    @Test void publishedSettingsUseIsolatedRepository();
    @Test void publishedSettingsSetBothGretlPluginVersions();
    @Test void runtimeSettingsContainNoRemoteRepository();
    @Test void escapesProjectNameSafely();
}
```

---

## 20. Einheitliches Execution-Backend

### 20.1 Neuer neutraler Name

Verwende nicht ein drittes Interface mit dem Namen `GretlBuildExecutor`.

Verbindlicher Name:

```text
TestJobExecutionBackend
```

### 20.2 Interface

```java
public interface TestJobExecutionBackend
        extends AutoCloseable {

    TestJobExecutionTarget target();

    GretlBuildResult execute(TestJobExecutionRequest request);

    GretlBuildResult executeAndExpectFailure(
            TestJobExecutionRequest request);

    @Override
    default void close() {
    }
}
```

### 20.3 Request

```java
public record TestJobExecutionRequest(
        MaterializedTestJob job,
        List<String> arguments,
        Map<String, String> environment,
        Map<String, String> gradleProperties,
        Set<String> secretValues,
        Duration timeout,
        Optional<String> dockerNetwork) {

    public List<String> effectiveArguments();
}
```

### 20.4 `effectiveArguments()`

Reihenfolge:

1. Gradle Properties als `-Pname=value`;
2. zusätzliche CLI-Argumente;
3. Entry-Tasks aus dem Descriptor, falls keine expliziten Tasks angegeben sind;
4. Trace-Systemproperty beziehungsweise `--init-script`;
5. `--stacktrace`, sofern Backend dies benötigt.

Secrets müssen separat markiert werden.

### 20.5 Keine Servicekenntnis im neutralen Request

Der Request darf einen optionalen Docker-Netzwerknamen tragen.

Er darf keine fachlichen Typen enthalten wie:

```text
PostgisEndpoint
S3Endpoint
FtpEndpoint
```

Diese kommen später aus einem Fixture-/Environment-Layer.

---

## 21. TestKit-Adapter

### 21.1 Bestehende Infrastruktur wiederverwenden

Verwende:

```text
ch.so.agi.gretl.testkit.GretlBuildExecutor
GretlBuildExecutors
PluginClasspathBuildExecutor
ExplicitPluginClasspathBuildExecutor
PublishedArtifactBuildExecutor
```

Nicht neu implementieren:

- GradleRunner-Basissetup;
- Published-Repository-Settings;
- TestKit-Verzeichnisverwaltung;
- Plugin-Classpath-Auflösung.

### 21.2 `TestKitJobExecutionBackend`

```java
public abstract class TestKitJobExecutionBackend
        implements TestJobExecutionBackend {

    private final ch.so.agi.gretl.testkit.GretlBuildExecutor delegate;
    private final TestJobExecutionTarget target;
    private final TestKitBuildResultAdapter resultAdapter;

    @Override
    public GretlBuildResult execute(TestJobExecutionRequest request);

    @Override
    public GretlBuildResult executeAndExpectFailure(
            TestJobExecutionRequest request);
}
```

### 21.3 Konkrete Backends

```java
public final class PluginClasspathJobExecutionBackend
        extends TestKitJobExecutionBackend {
}
```

```java
public final class PublishedArtifactJobExecutionBackend
        extends TestKitJobExecutionBackend {
}
```

### 21.4 `TestKitBuildResultAdapter`

```java
public final class TestKitBuildResultAdapter {

    public GretlBuildResult adapt(
            BuildResult result,
            int exitCode,
            Duration duration,
            List<String> sanitizedArguments);

    GretlTaskOutcome map(
            org.gradle.testkit.runner.TaskOutcome outcome);
}
```

Mapping:

| TestKit | `GretlTaskOutcome` |
|---|---|
| `SUCCESS` | `SUCCESS` |
| `FAILED` | `FAILED` |
| `SKIPPED` | `SKIPPED` |
| `UP_TO_DATE` | `UP_TO_DATE` |
| `FROM_CACHE` | `FROM_CACHE` |
| `NO_SOURCE` | `NO_SOURCE` |

### 21.5 Dauer messen

Messe um den Delegate-Aufruf mit `System.nanoTime()`.

Keine künstliche `Duration.ZERO`, außer in kleinen Unit-Fixtures.

### 21.6 Output

TestKit liefert kombinierten Output.

Setze bevorzugt:

```java
standardOutput = result.getOutput();
standardError = "";
```

Dokumentiere, dass TestKit stdout/stderr nicht zuverlässig trennt.

### 21.7 Tests

```java
class TestKitBuildResultAdapterTest {
    @Test void mapsAllKnownOutcomes();
    @Test void preservesCombinedOutput();
    @Test void createsTaskOutcomeMap();
    @Test void preservesSanitizedArguments();
}
```

---

## 22. Runtime-One-shot-Backend

### 22.1 Implementierung

```java
public final class RuntimeImageOneShotJobExecutionBackend
        implements TestJobExecutionBackend {

    private final RuntimeImageBuildExecutor executor;

    @Override
    public TestJobExecutionTarget target() {
        return TestJobExecutionTarget.RUNTIME_IMAGE_ONE_SHOT;
    }

    @Override
    public GretlBuildResult execute(TestJobExecutionRequest request);
}
```

### 22.2 Request-Mapping

```java
private GretlBuildRequest map(TestJobExecutionRequest request) {
    GretlBuildRequest.Builder builder = GretlBuildRequest
        .builder(request.job().projectDirectory())
        .arguments(request.effectiveArguments())
        .timeout(request.timeout())
        .runtimeImageOptions(runtimeOptions(request))
        .runtimeExecutionMode(RuntimeExecutionMode.ONE_SHOT);

    request.environment().forEach(builder::environment);
    request.secretValues().forEach(builder::secret);
    return builder.build();
}
```

### 22.3 Netzwerk

- ohne Netzwerkname: `RuntimeImageRunOptions.defaults()`;
- mit Netzwerkname: `RuntimeImageRunOptions.onNetwork(name)`.

### 22.4 Kein eigenes `--offline`

Das Backend ergänzt kein `--offline`.

Der Launcher erzwingt es.

---

## 23. Runtime-Service-Backend

### 23.1 Verantwortung

Das Backend führt materialisierte Jobs in einem bereits gestarteten `RuntimeImageServiceContainer` aus.

### 23.2 Lifecycle

```java
public final class RuntimeImageServiceJobExecutionBackend
        implements TestJobExecutionBackend {

    private final RuntimeImageServiceContainer service;
    private final Path mountedJobsRoot;

    public static RuntimeImageServiceJobExecutionBackend start(
            RuntimeImageDescriptor image,
            Path jobsRoot,
            Path gradleUserHome,
            Optional<String> network,
            Optional<String> user);

    @Override
    public GretlBuildResult execute(...);

    @Override
    public void close();
}
```

### 23.3 Materialisierungsort

Service-Jobs müssen unter dem gemounteten `jobsRoot` materialisiert werden.

Das Backend erhält einen relativen Projektpfad.

### 23.4 Ergebnisadapter

`RuntimeImageServiceContainer.execGretl(...)` liefert `ProcessResult`.

Verwende denselben `GradleTaskOutputParser` wie der One-shot-Executor.

### 23.5 Secrets

Erweitere bei Bedarf:

```java
ProcessResult execGretl(
        Path relativeProjectDir,
        List<String> arguments,
        Set<String> secrets);
```

Secrets dürfen weder im Output noch in der sanitized command erscheinen.

### 23.6 Tests

```java
class RuntimeImageServiceJobExecutionBackendTest {
    @Test void executesMaterializedJob();
    @Test void returnsUnifiedTaskOutcomes();
    @Test void rejectsProjectOutsideMountedRoot();
    @Test void reusesServiceContainerAcrossJobs();
    @Test void masksSecrets();
    @Test void closesContainerExactlyOnce();
}
```

---

## 24. Backend-Factory

### 24.1 API

```java
public interface TestJobExecutionBackendFactory {
    TestJobExecutionBackend create(
            TestJobExecutionTarget target,
            TestJobBackendContext context);
}
```

### 24.2 Context

```java
public record TestJobBackendContext(
        Optional<Path> explicitPluginClasspathFile,
        Optional<Path> testKitDirectory,
        Optional<URI> publishedRepository,
        Optional<String> pluginVersion,
        Optional<RuntimeImageDescriptor> runtimeImage,
        Optional<Path> serviceJobsRoot,
        Optional<Path> serviceGradleHome,
        Optional<String> dockerNetwork) {
}
```

### 24.3 Validierung

Für jedes Ziel sind nur relevante Felder erforderlich.

Fehler nennt fehlendes Feld und Ziel.

### 24.4 Keine globale mutable Factory

Keine statische globale Backendinstanz, die zwischen parallelen Tests verändert wird.

---

## 25. Task Execution Trace

### 25.1 Ziel

Die Coverage-Matrix soll nicht nur behaupten, dass eine Testmethode einen Task abdeckt.

Sie soll anhand eines realen Traces beweisen:

- Taskpfad;
- konkrete Taskklasse;
- Outcome;
- Job-ID;
- Buildvariante;
- Backend.

### 25.2 Trace-Format

Bevorzugt JSON Lines:

```json
{"job":"combined-core-geotools-pipeline","build":"groovy","backend":"PLUGIN_CLASSPATH","path":":generateRaster","className":"ch.so.agi.gretl.tasks.XslTransformer","outcome":"SUCCESS"}
{"job":"combined-core-geotools-pipeline","build":"groovy","backend":"PLUGIN_CLASSPATH","path":":reclassifyRaster","className":"ch.so.agi.gretl.geotools.tasks.RasterReclassify","outcome":"SUCCESS"}
```

### 25.3 `TaskExecutionTraceEntry`

```java
public record TaskExecutionTraceEntry(
        String jobId,
        String buildVariant,
        TestJobExecutionTarget backend,
        String taskPath,
        String taskClassName,
        GretlTaskOutcome outcome) {
}
```

### 25.4 `TaskExecutionTrace`

```java
public record TaskExecutionTrace(
        List<TaskExecutionTraceEntry> entries) {

    public Optional<TaskExecutionTraceEntry> find(String taskPath);

    public boolean contains(String taskPath, String className);
}
```

### 25.5 Bootstrap

Erzeuge beim Materialisieren:

```text
.gretl-test/task-trace.init.gradle
.gretl-test/task-trace.jsonl
```

Der Backend-Aufruf ergänzt:

```text
--init-script <path-to-task-trace.init.gradle>
```

sowie Properties für:

```text
gretl.test.jobId
gretl.test.buildVariant
gretl.test.executionTarget
gretl.test.traceFile
```

### 25.6 Gradle-Listener

Für P1 ist ein `afterTask`-Listener im Test-Init-Script zulässig, sofern:

- er nur in dedizierten Job-Coverage-Läufen aktiv ist;
- er nicht in Configuration-Cache-Tests injiziert wird;
- er threadsicher schreibt;
- er keine Produktionsklassen verändert;
- er keine Taskkonfiguration erzwingt.

### 25.7 Configuration Cache

Der bestehende Configuration-Cache-P0-Test darf nicht durch den Trace-Listener verschlechtert werden.

Für Configuration-Cache-Tests wird:

```text
traceEnabled = false
```

verwendet.

Coverage-Nachweise laufen separat ohne Configuration Cache.

### 25.8 Parser

```java
public final class TaskExecutionTraceReader {
    public TaskExecutionTrace read(Path traceFile);
}
```

### 25.9 Verifikation

```java
public final class ExpectedTaskTraceVerifier {
    public void verify(
            TestJobDescriptor descriptor,
            TaskExecutionTrace trace);
}
```

Prüfe:

- jeder `expectedTasks`-Eintrag vorhanden;
- Pfad stimmt;
- Klasse stimmt exakt;
- Outcome ist `SUCCESS`, `UP_TO_DATE`, `FROM_CACHE` oder fachlich erlaubter Wert;
- kein erwarteter Task `UNKNOWN`;
- fehlender Trace ist Fehler.

### 25.10 Tests

```java
class TaskExecutionTraceReaderTest {
    @Test void readsJsonLines();
    @Test void rejectsMalformedLine();
    @Test void rejectsUnknownOutcome();
    @Test void preservesTaskClassName();
}
```

```java
class ExpectedTaskTraceVerifierTest {
    @Test void acceptsExpectedTaskPathAndClass();
    @Test void rejectsMissingExpectedTask();
    @Test void rejectsWrongTaskClass();
    @Test void rejectsFailedExpectedTaskInPositiveJob();
}
```

---

## 26. Job-Assertions

### 26.1 Interface

```java
public interface TestJobAssertions {
    String id();

    void verify(
            MaterializedTestJob job,
            GretlBuildResult result,
            TaskExecutionTrace trace)
            throws Exception;
}
```

### 26.2 Registry

```java
public final class TestJobAssertionRegistry {
    public TestJobAssertionRegistry(
            Collection<TestJobAssertions> assertions);

    public TestJobAssertions require(String id);
}
```

Doppelte IDs ablehnen.

### 26.3 Keine Reflection aus YAML

Das YAML enthält nur den Registry-Key.

Nicht zulässig:

```yaml
assertionClass: com.example.SomeClass
```

### 26.4 Gemeinsame Assertions

```java
public final class CommonTestJobAssertions {
    public static void assertSuccessful(GretlBuildResult result);
    public static void assertNoClassloaderFailure(GretlBuildResult result);
    public static void assertNoWorkerProtocolLeak(GretlBuildResult result);
    public static void assertNoRemoteDownloadLog(GretlBuildResult result);
}
```

`assertNoRemoteDownloadLog` ist für Runtime-Ziele ergänzend, nicht primärer Beweis.

---

## 27. `TestJobRunner`

### 27.1 Ziel

Zentrale Orchestrierung eines positiven Jobtests.

### 27.2 API

```java
public final class TestJobRunner {

    private final TestJobMaterializer materializer;
    private final TestJobExecutionBackendFactory backendFactory;
    private final TestJobAssertionRegistry assertionRegistry;
    private final TaskExecutionTraceReader traceReader;
    private final ExpectedTaskTraceVerifier traceVerifier;

    public TestJobRunResult run(TestJobRunRequest request);
}
```

### 27.3 `TestJobRunRequest`

```java
public record TestJobRunRequest(
        TestJobDescriptor descriptor,
        TestJobBuildVariant buildVariant,
        TestJobExecutionTarget target,
        Path destinationRoot,
        Map<String, String> gradleProperties,
        Map<String, String> environment,
        Set<String> secrets,
        Optional<String> dockerNetwork,
        boolean traceEnabled) {
}
```

### 27.4 Ablauf

1. Descriptor validieren.
2. Target Requirement prüfen.
3. Job materialisieren.
4. Backend erstellen.
5. Request erzeugen.
6. Build ausführen.
7. Erfolg prüfen.
8. Trace lesen.
9. Expected Tasks prüfen.
10. Fachliche Assertions ausführen.
11. Resultat zurückgeben.
12. Backend und Ressourcen schließen.

### 27.5 `TestJobRunResult`

```java
public record TestJobRunResult(
        MaterializedTestJob job,
        GretlBuildResult buildResult,
        TaskExecutionTrace trace) {
}
```

### 27.6 Fehlerdiagnose

Bei Fehler mindestens:

- Job-ID;
- Buildvariante;
- Backend;
- Projektverzeichnis;
- Gradle-Argumente maskiert;
- Timeout;
- stdout/stderr;
- Task-Outcomes;
- Traceinhalt;
- erwartete Tasks;
- Runtime-Image-ID, falls relevant;
- TestKit-Verzeichnis, falls relevant.


# Teil C – Vier Pilotjobs

## 28. Pilotjob 1: `core-gzip`

### 28.1 Struktur

```text
test-jobs/core/gzip/
├── job.yaml
├── build.gradle
├── input/
│   └── data.txt
└── expected/
    └── payload.txt
```

### 28.2 Buildscript

```groovy
import ch.so.agi.gretl.tasks.Gzip

plugins {
    id 'ch.so.agi.gretl'
}

tasks.register('compressFile', Gzip) {
    dataFile 'input/data.txt'
    gzipFile layout.buildDirectory.file('output/data.txt.gz')
}
```

### 28.3 Manifest

Expected Task:

```yaml
- path: :compressFile
  className: ch.so.agi.gretl.tasks.Gzip
```

Targets:

```yaml
pluginClasspath: required
publishedArtifact: required
runtimeImageOneShot: required
runtimeImageService: required
```

### 28.4 Assertions

```java
public final class GzipTestJobAssertions
        implements TestJobAssertions {

    @Override
    public String id() {
        return "core-gzip";
    }

    @Override
    public void verify(...);
}
```

Prüfe:

- GZIP existiert;
- Header gültig;
- dekomprimierter Inhalt bytegleich zu `input/data.txt`;
- Tasktrace enthält `Gzip`;
- keine Secrets;
- Runtime-Ausgabe enthält keine Downloadmeldung.

---

## 29. Pilotjob 2: `core-sqlite`

### 29.1 Struktur

```text
test-jobs/core/sqlite/
├── job.yaml
├── build.gradle
├── input/
│   └── schema.sql
└── expected/
    └── rows.json
```

### 29.2 Buildscript

```groovy
import ch.so.agi.gretl.tasks.SqlExecutor

plugins {
    id 'ch.so.agi.gretl'
}

tasks.register('initializeDatabase', SqlExecutor) {
    database "jdbc:sqlite:${layout.buildDirectory.file('db/test.db').get().asFile.absolutePath}"
    sqlFiles 'input/schema.sql'
}
```

Verwende eine Provider-freundliche Variante, sofern die Task-API dies unterstützt.

### 29.3 Expected Task

```yaml
- path: :initializeDatabase
  className: ch.so.agi.gretl.tasks.SqlExecutor
```

### 29.4 Targets

Alle vier `required`.

### 29.5 Assertions

Mit JDBC im Hostprozess:

- Datenbankdatei existiert;
- Tabelle existiert;
- erwartete Zeilen vorhanden;
- Werte und Typen stimmen;
- Datei ist nach Assertion schließbar und löschbar;
- kein offenes File Lock.

```java
public final class SqliteTestJobAssertions
        implements TestJobAssertions {
}
```

---

## 30. Pilotjob 3: `combined-core-geotools-pipeline`

### 30.1 Struktur

```text
test-jobs/combined/core-geotools-pipeline/
├── job.yaml
├── build.gradle
├── input/
│   ├── raster.xml
│   └── raster-to-asc.xsl
└── expected/
    └── raster-summary.json
```

### 30.2 Pipeline

```text
XslTransformer
→ RasterReclassify
→ Gzip
```

### 30.3 Builddateien

Verschiebe das vorhandene Groovy-Buildscript aus `CoreGeoToolsPipelineFunctionalTest` byteinhaltlich in die persistierte Datei.

Die Dateien dürfen nicht aus einem Java-Textblock generiert werden.

### 30.4 Provider-Wiring

Weiterhin keine manuellen:

```groovy
dependsOn generateRaster
dependsOn reclassifyRaster
```

Taskabhängigkeiten werden aus Provider-Inputs und Outputs abgeleitet.

### 30.5 Expected Tasks

```yaml
- path: :generateRaster
  className: ch.so.agi.gretl.tasks.XslTransformer
- path: :reclassifyRaster
  className: ch.so.agi.gretl.geotools.tasks.RasterReclassify
- path: :packageRaster
  className: ch.so.agi.gretl.tasks.Gzip
```

### 30.6 Targets

```yaml
pluginClasspath: required
publishedArtifact: required
runtimeImageOneShot: required
runtimeImageService: required
```

### 30.7 Assertions

Wiederverwende beziehungsweise verschiebe:

```text
AsciiGridAssertions
GeoTiffAssertions
GzipAssertions
```

Bevorzugter neutraler Packagepfad:

```text
ch.so.agi.gretl.test.job.assertions
```

Prüfe:

- ASCII Grid Dimension 4 × 3;
- Cellsize 1;
- NoData -9999;
- Koordinaten;
- Eingabewerte;
- GeoTIFF Dimension 4 × 3;
- ein Band;
- EPSG:2056;
- NoData -100;
- erwartete Klassen;
- Envelope;
- GZIP-Header;
- dekomprimiertes GZIP bytegleich zum GeoTIFF.

### 30.8 Configuration Cache

Der backendübergreifende positive Kataloglauf muss nicht gleichzeitig Configuration Cache prüfen.

Der bestehende kombinierte Configuration-Cache-Test bleibt erhalten und materialisiert künftig denselben persistenten Job mit deaktiviertem Trace.

---

## 31. Pilotjob 4: `geotools-read-shapefile`

### 31.1 Struktur

```text
test-jobs/geotools/read-shapefile/
├── job.yaml
├── build.gradle
├── input/
│   ├── points.shp
│   ├── points.shx
│   ├── points.dbf
│   ├── points.prj
│   └── points.cpg
└── expected/
    └── features.json
```

### 31.2 Buildscript

Verwende den vorhandenen realen `ReadShapefile`-Task.

Das Buildscript soll ein deterministisches maschinenlesbares Ergebnis erzeugen, beispielsweise JSON oder CSV unter:

```text
build/output/features.json
```

Falls `ReadShapefile` Daten nur über Taskproperties bereitstellt, darf ein kleiner nachgelagerter normaler Gradle-Task das Ergebnis serialisieren.

### 31.3 Expected Task

```yaml
- path: :readFeatures
  className: ch.so.agi.gretl.geotools.tasks.ReadShapefile
```

Verwende den tatsächlichen Taskklassennamen aus dem Repository.

### 31.4 Targets

```yaml
pluginClasspath: required
publishedArtifact: required
runtimeImageOneShot: required
runtimeImageService: optional
```

Service darf `required` werden, wenn der Lauf stabil und nicht unverhältnismäßig langsam ist.

### 31.5 Assertions

Prüfe:

- Featureanzahl;
- Attributnamen;
- Attributwerte;
- Geometrietyp;
- CRS;
- mindestens eine konkrete Koordinate;
- GeoTools-Worker-Ausführung im Trace;
- keine rohe GeoTools-Classpath-Leak-Warnung.

---

# Teil D – Bestehende Tests migrieren

## 32. Kombinierte Tests

### 32.1 Behalten

In `gretl-combined-tests` bleiben:

- Pluginreihenfolge;
- Idempotenz;
- Shared Services;
- Unabhängigkeit;
- Classloader-Isolation;
- Worker-Classpath-Isolation;
- Configuration Cache;
- Fehlerpropagation;
- Multi-Projekt-Verhalten;
- negative Auflösungstests.

### 32.2 Positive Pipeline-Methoden

Methoden, die ausschließlich fachliche Pipelineoutputs prüfen, werden in `gretl-job-tests` verschoben.

Beispiele:

```text
executesPipelineWithGroovyDsl
producesCorrectAsciiGrid
producesCorrectReclassifiedGeoTiff
packagesExactGeoTiffBytesIntoGzip
```

### 32.3 Spezielle Pipeline-Tests

Methoden wie:

```text
infersDependenciesFromProviders
configurationCacheCanBeStoredAndReused
```

bleiben im kombinierten Modul, materialisieren aber denselben persistenten Job.

Sie dürfen keine Buildscript-Textblöcke mehr enthalten.

### 32.4 `CombinedPluginTestSupport`

Erweitere:

```java
protected MaterializedTestJob materializeCanonicalJob(
        String jobId,
        String buildVariant,
        boolean traceEnabled);
```

oder verwende direkt den gemeinsamen Materializer.

---

## 33. Runtime-Tests

### 33.1 `RuntimeImageCoreFunctionalTest`

Entferne eingebettete Buildscripts für Gzip und SQLite, sobald die Pilotjobs dieselben Beweisziele vollständig übernehmen.

Behalte separate Runtime-Tests nur für:

- DuckDB Spatial;
- spezielle Imagekomponenten;
- Runtime-spezifische Fehler;
- andere nicht migrierte Tasks.

### 33.2 Dependency-Closure-Tests

Der lokale Plugin-Auflösungstest bleibt zuständig für:

- versionlose Pluginauflösung;
- explizit gebündelte Version;
- abweichende Version;
- nicht lokal verfügbares Plugin;
- fehlendes Repository;
- Mutationen.

Er soll nicht zusätzlich vollständige Gzip-, SQLite- oder GeoTools-Fachjobs duplizieren, wenn der Katalog diese ausführt.

### 33.3 Service-Test

`RuntimeImageDaemonReuseTest` behält:

- PID-Wiederverwendung;
- `gradle --stop`;
- Containerlebensdauer;
- warmen Cache;
- zusätzliche lokale Repository-Mounts.

Für reale positive Jobausführung verwendet er bevorzugt `core-gzip` und die kombinierte Pipeline über das Service-Backend.

---

## 34. Parameterisierte kanonische Jobtests

### 34.1 Testklasse

```java
class CanonicalTestJobFunctionalTest {

    @ParameterizedTest(name = "{0} [{1}] on {2}")
    @MethodSource("jobExecutions")
    void executesCanonicalJob(
            TestJobDescriptor job,
            TestJobBuildVariant build,
            TestJobExecutionTarget target)
            throws Exception;
}
```

### 34.2 Quellen

```java
static Stream<Arguments> jobExecutions();
```

Die Testtask-Systemproperty bestimmt das aktuelle Backend.

Bevorzugt führt jede Gradle-`Test`-Task nur genau ein Backend aus.

### 34.3 Kein Backend-Branching in Assertions

Nicht zulässig:

```java
if (target == RUNTIME_IMAGE_ONE_SHOT) {
    // weaker assertion
}
```

Backendunterschiede betreffen nur:

- Settings;
- Ausführungsprozess;
- Netzwerk/Mounts;
- technische Diagnose.

Fachliche Outputs sind identisch.

### 34.4 Target Requirement

- `REQUIRED`: Test wird erzeugt und muss grün sein;
- `OPTIONAL`: Test darf über Property aktiviert werden, ist nicht Teil des Standardgates;
- `NOT_APPLICABLE`: kein Test erzeugen;
- ein unbekannter oder fehlender Wert ist Fehler.

---

## 35. Gradle-Testtasks

### 35.1 Source

```text
canonicalJobSourceTest
```

Konfiguration:

```text
TestJobExecutionTarget.PLUGIN_CLASSPATH
```

Abhängigkeiten:

- expliziter Plugin-Classpath;
- Classpath-Isolationscheck;
- Jobkatalog-Validierung.

### 35.2 Published

```text
canonicalJobPublishedTest
```

Abhängigkeiten:

- `verifyPublishedTestRepository`;
- Jobkatalog-Validierung.

### 35.3 Runtime One-shot

```text
canonicalJobRuntimeImageTest
```

Abhängigkeiten:

- `buildRuntimeImageForTest`;
- erforderliche Runtime-Prerequisites;
- Jobkatalog-Validierung.

### 35.4 Runtime Service

```text
canonicalJobServiceTest
```

Abhängigkeiten:

- Runtime-Image-Build;
- Service-Prerequisites;
- Jobkatalog-Validierung.

### 35.5 Lokaler Aggregator

```text
canonicalJobTest
```

Abhängigkeiten:

```text
canonicalJobSourceTest
canonicalJobPublishedTest
canonicalJobRuntimeImageTest
canonicalJobServiceTest
```

Dieser Aggregator ist für lokale Vollprüfung gedacht.

Er wird in CI nicht zusätzlich aufgerufen, wenn seine Untertasks bereits über bestehende Hauptgates laufen.

### 35.6 Bestehende Hauptgates

`check` hängt ab von:

```text
canonicalJobSourceTest
```

Root-`publishedArtifactTest` hängt ab von:

```text
:gretl-job-tests:canonicalJobPublishedTest
```

Root-`runtimeImageTest` hängt ab von:

```text
:gretl-job-tests:canonicalJobRuntimeImageTest
:gretl-job-tests:canonicalJobServiceTest
```

### 35.7 Keine Zyklen

Insbesondere darf:

- `canonicalJobRuntimeImageTest` nicht von `runtimeImageTest` abhängen;
- `runtimeImageTest` nicht indirekt erneut sein eigenes Image bauen;
- `publishedArtifactTest` nicht über Jobtests zyklisch auf sich selbst zeigen.

---

## 36. Jobkatalog-Validierungstask

### 36.1 Taskname

```text
validateTestJobCatalog
```

### 36.2 Verantwortung

- alle YAML-Dateien parsen;
- alle Descriptoren validieren;
- IDs eindeutig;
- Builddateien vorhanden;
- verbotene Konstrukte abwesend;
- erwartete Assertions registriert;
- alle `REQUIRED`-Targets durch eine Testtask abgedeckt;
- keine unreferenzierten Assertion-IDs;
- keine Katalogoutputs eingecheckt.

### 36.3 Taskoutput

Erzeuge optional:

```text
build/reports/test-jobs/catalog.json
```

mit:

- Job-IDs;
- Varianten;
- Targets;
- Expected Tasks;
- Capabilities.

### 36.4 `check`

`check` hängt immer von `validateTestJobCatalog` ab.

---

# Teil E – Coverage-Matrix

## 37. Ehrliche Coverage-Klassifikationen

Ersetze unpräzise Klassifikationen durch:

```text
DIRECT_JOB_EXECUTION
STRUCTURAL_CONTRACT_ONLY
DEPENDENCY_PRESENT_ONLY
NOT_YET_COVERED
NOT_APPLICABLE
```

### 37.1 `DIRECT_JOB_EXECUTION`

Nur zulässig, wenn ein Trace die konkrete Taskklasse und den Taskpfad belegt.

### 37.2 `STRUCTURAL_CONTRACT_ONLY`

Beispiel:

- Taskklasse ist registriert;
- Pluginmarker ist vorhanden;
- Task wird aber nicht fachlich ausgeführt.

### 37.3 `DEPENDENCY_PRESENT_ONLY`

Nur Bibliothek oder Runtimekomponente ist nachgewiesen, nicht die Taskausführung.

### 37.4 `NOT_YET_COVERED`

Ehrlicher Zustand für öffentliche Tasks ohne reale Ausführung.

### 37.5 `NOT_APPLICABLE`

Nur mit fachlicher Begründung.

---

## 38. Neue Matrixstruktur

Bevorzugte Datei:

```text
docs/testing/task-coverage.yaml
```

Bestehende Datei darf migriert werden.

Beispiel:

```yaml
schemaVersion: 3

tasks:
  Gzip:
    className: ch.so.agi.gretl.tasks.Gzip
    module: gretl-core
    classification: DIRECT_JOB_EXECUTION
    scenarios:
      - job: core-gzip
        taskPath: :compressFile
        targets:
          pluginClasspath: required
          publishedArtifact: required
          runtimeImageOneShot: required
          runtimeImageService: required

  RasterReclassify:
    className: ch.so.agi.gretl.geotools.tasks.RasterReclassify
    module: gretl-geotools
    classification: DIRECT_JOB_EXECUTION
    scenarios:
      - job: combined-core-geotools-pipeline
        taskPath: :reclassifyRaster
        targets:
          pluginClasspath: required
          publishedArtifact: required
          runtimeImageOneShot: required
          runtimeImageService: required
```

### 38.1 Keine Testmethodennamen als primärer Beweis

Entferne langfristig Felder wie:

```text
testClass
testMethods
```

oder behalte sie nur als ergänzende Diagnose.

Der primäre Nachweis ist:

```text
job + taskPath + className + execution trace
```

---

## 39. `TaskCoverageVerifier`

### 39.1 API

```java
public final class TaskCoverageVerifier {

    public CoverageVerificationReport verify(
            TaskCoverageManifest manifest,
            TestJobCatalog catalog,
            Collection<TaskExecutionTrace> traces,
            Set<String> publicTaskClasses);
}
```

### 39.2 Prüfungen

- jede öffentliche Taskklasse besitzt Matrixeintrag;
- `DIRECT_JOB_EXECUTION` besitzt mindestens ein Szenario;
- referenzierter Job existiert;
- Taskpfad ist im Jobdescriptor erwartet;
- Klassenname stimmt zwischen Matrix und Job;
- für jedes `required` Target existiert ein passender Trace;
- Trace enthält Pfad und Klasse;
- Outcome ist positiv;
- `NOT_YET_COVERED` besitzt keine falsche Direct-E2E-Behauptung;
- keine Matrixeinträge für nicht existierende Taskklassen.

### 39.3 P1-Umfang

Für die vier Pilotjobs müssen vollständige Trace-Nachweise existieren.

Alle übrigen bestehenden Einträge müssen ehrlich neu klassifiziert werden.

Der Agent darf nicht alte falsche `DIRECT_E2E`-Einträge unverändert lassen.

### 39.4 Reports

```text
build/reports/test-jobs/coverage.json
build/reports/test-jobs/coverage.adoc
```

Der Report listet:

- direkt ausgeführte Tasks;
- nur strukturell geprüfte Tasks;
- noch nicht abgedeckte Tasks;
- fehlende Backendausführungen.

---

# Teil F – Servicevorbereitung

## 40. `TestJobEnvironment`

P1 implementiert die neutrale Struktur, aber noch nicht alle Servicefixtures.

```java
public record TestJobEnvironment(
        Map<String, String> gradleProperties,
        Map<String, String> environmentVariables,
        Optional<String> dockerNetwork,
        Set<String> secrets) {

    public static TestJobEnvironment empty();

    public TestJobEnvironment merge(TestJobEnvironment other);
}
```

### 40.1 Merge-Regeln

- doppelte gleiche Werte erlaubt;
- doppelte unterschiedliche Werte führen zu Fehler;
- Secrets vereinigen;
- zwei verschiedene Netzwerke führen zu Fehler;
- defensive Kopien.

---

## 41. `TestServiceFixture`

Nur Interface und Test-Dummy in P1.

```java
public interface TestServiceFixture
        extends AutoCloseable {

    String capability();

    void start();

    TestJobEnvironment environmentFor(
            TestJobExecutionTarget target);

    @Override
    void close();
}
```

P2 implementiert:

```text
PostgisTestServiceFixture
HttpTestServiceFixture
FtpTestServiceFixture
S3TestServiceFixture
```

Die P1-Pilotjobs benötigen keine externen Services.

---

# Teil G – CI und Dokumentation

## 42. CI-Einbindung

### 42.1 P1-Ziel

Die neuen Jobtests werden in bestehende Hauptgates integriert.

Bevorzugte CI-Hauptbefehle:

```bash
./gradlew clean check :gretl-core:integrationTest
./gradlew publishedArtifactTest
./gradlew runtimeImageTest
```

### 42.2 Kein zusätzlicher Vollaggregator

Rufe in CI nicht zusätzlich auf:

```text
canonicalJobTest
```

wenn Source, Published und Runtime bereits über die Hauptgates ausgeführt wurden.

### 42.3 Diagnose-Tasks

Einzelne Tasks dürfen für schnelleres Fail-fast vorgezogen werden, sofern Gradle sie später nicht unnötig erneut ausführt.

### 42.4 Reports

Immer hochladen:

```text
gretl-job-tests/build/reports/tests/
gretl-job-tests/build/test-results/
build/reports/test-jobs/
```

Bei Runtimefehlern zusätzlich:

```text
build/runtime-image/**
gretl-job-tests/build/materialized-jobs/**
```

Secrets maskieren.

---

## 43. Dokumentation

Erzeuge:

```text
docs/testing/persistent-test-jobs.adoc
```

Inhalt:

- Zweck des Katalogs;
- Abgrenzung zu Produktionsjobs;
- Verzeichnisstruktur;
- `job.yaml` Schema;
- Builddatei bleibt identisch;
- modeabhängige Settings;
- Backends;
- Tasktrace;
- Assertions;
- Coverage-Matrix;
- lokale Befehle;
- neuen Job hinzufügen;
- keine `gretljobs`-Migration.

Aktualisiere zusätzlich:

```text
docs/testing/runtime-image-tests.adoc
README
```

---

# Teil H – Tests der Infrastruktur

## 44. Verbindliche Unit-Testklassen

Mindestens:

```text
TestJobYamlReaderTest
TestJobDescriptorValidatorTest
FileSystemTestJobCatalogTest
DefaultTestJobMaterializerTest
DefaultTestJobSettingsRendererTest
TestKitBuildResultAdapterTest
TaskExecutionTraceReaderTest
ExpectedTaskTraceVerifierTest
TestJobAssertionRegistryTest
TestJobEnvironmentTest
RuntimeImageLifecycleArgumentsTest
RuntimeImageBuildExecutorLifecycleTest
RuntimeImageBuildExecutorResourceTest
```

### 44.1 Negative Testfälle

Mindestens:

- unbekanntes YAML-Feld;
- doppelte Job-ID;
- fehlende Builddatei;
- PfadTraversal;
- unbekanntes Backend;
- fehlende Assertion-ID;
- falsche Taskklasse im Trace;
- fehlender Task im Trace;
- Service über One-shot-Executor;
- temporäres Gradle-Home wird bei Fehler geschlossen;
- Builddatei wurde beim Materialisieren verändert;
- Source-Checkout-Referenz im Job;
- `mavenLocal()` im Job;
- `flatDir` im Job.

---

# Teil I – Schrittweise Umsetzung

## 45. Phase 1: Repository erneut analysieren

1. aktuellen Commit dokumentieren;
2. Runtime-Launcher lesen;
3. Runtime-Argumentklassen lesen;
4. One-shot-Executor lesen;
5. Servicecontainer lesen;
6. TestKit-Executors lesen;
7. Combined-Testmodul lesen;
8. Root- und Subprojekt-Gradle-Tasks lesen;
9. CI lesen;
10. Coverage-Matrix lesen;
11. alle eingebetteten positiven Buildscripts inventarisieren.

## 46. Phase 2: P0.5

1. `--offline`-Doppelquelle entfernen;
2. Lifecycle-Argumentklasse umbenennen/anpassen;
3. One-shot-Executor auf One-shot beschränken;
4. Temp-Home-Besitz korrigieren;
5. Terminologie anpassen;
6. Tests aktualisieren;
7. Runtime-Gates vollständig ausführen.

## 47. Phase 3: Jobmodell

1. Package anlegen;
2. YAML-Library ergänzen;
3. Records und Enums;
4. Reader;
5. Validator;
6. Katalog;
7. Unit-Tests.

## 48. Phase 4: Materialisierung

1. Settingsrenderer;
2. Materializer;
3. Bytegleichheitsprüfung;
4. Trace-Bootstrap;
5. Unit-Tests.

## 49. Phase 5: Backends

1. neutrales Interface;
2. TestKit-Adapter;
3. Source-Backend;
4. Published-Backend;
5. Runtime-One-shot-Backend;
6. Runtime-Service-Backend;
7. Factory;
8. Resultadapter;
9. Tests.

## 50. Phase 6: Pilotjobs

1. Gzip;
2. SQLite;
3. kombinierte Pipeline Groovy;
5. ReadShapefile;
6. Assertions;
7. Descriptoren;
8. alle drei Hauptbackends;
9. ausgewählte Serviceausführung.

## 51. Phase 7: Bestehende Tests bereinigen

1. eingebettete Pipeline-Buildscripts entfernen;
2. Runtime-Gzip-/SQLite-Duplikate entfernen;
3. spezielle P0-Tests behalten;
4. Assertions neutral verschieben;
5. keine fachliche Abdeckung verlieren.

## 52. Phase 8: Coverage

1. Trace-Reader;
2. Trace-Verifier;
3. Matrixschema aktualisieren;
4. Pilotjobs auf Direct Execution;
5. übrige Einträge ehrlich klassifizieren;
6. Reports.

## 53. Phase 9: Gradle und CI

1. neues Subprojekt;
2. Testtasks;
3. Hauptgate-Wiring;
4. Publikationsausschluss;
5. CI;
6. Reports.

## 54. Phase 10: Dokumentation und Endverifikation

1. Dokumentation;
2. Unit-Tests;
3. Source-Jobs;
4. Published-Jobs;
5. Runtime-One-shot;
6. Runtime-Service;
7. bestehende Combined-Gates;
8. vollständiger Build;
9. Wiederholung mit `--rerun-tasks`;
10. Abschlussbericht.

---

# Teil J – Auszuführende Befehle

## 55. P0.5

```bash
./gradlew clean check
./gradlew runtimeImageContractTest
./gradlew runtimeImageDependencyClosureTest
./gradlew runtimeImageServiceTest
./gradlew runtimeImageTest
```

## 56. P1 Infrastruktur

```bash
./gradlew :gretl-job-tests:validateTestJobCatalog
./gradlew :gretl-job-tests:test
```

## 57. P1 Backends

```bash
./gradlew :gretl-job-tests:canonicalJobSourceTest
./gradlew :gretl-job-tests:canonicalJobPublishedTest
./gradlew :gretl-job-tests:canonicalJobRuntimeImageTest
./gradlew :gretl-job-tests:canonicalJobServiceTest
./gradlew canonicalJobTest
```

## 58. Bestehende Gates

```bash
./gradlew :gretl-combined-tests:test
./gradlew :gretl-combined-tests:publishedArtifactTest
./gradlew :gretl-combined-tests:combinedConfigurationCacheTest
./gradlew coreGeoToolsCombinedTest
./gradlew publishedArtifactTest
./gradlew runtimeImageTest
```

## 59. Rerun und Parallelität

```bash
./gradlew canonicalJobTest --rerun-tasks
./gradlew :gretl-job-tests:canonicalJobSourceTest --parallel
./gradlew :gretl-job-tests:canonicalJobPublishedTest --parallel
```

Runtime-Docker-Tests nur parallelisieren, wenn Container-, Gradle-Home- und Testverzeichnisse nachweislich isoliert sind.

---

# Teil K – Verbotene Abkürzungen

## 60. Nicht zulässig

- `--offline` weiterhin sowohl im Launcher als auch in der Java-Argumentklasse hinzufügen;
- `RuntimeImageBuildExecutor` weiterhin als echten Serviceexecutor darstellen;
- öffentliches Prepare-Verfahren mit unbesessenem Temp-Verzeichnis;
- einen dritten `GretlBuildExecutor`-Typ einführen;
- persistierte Builddateien templatisieren;
- Builddateien pro Backend kopieren und verändern;
- für Source, Published und Runtime separate `build.gradle`-Dateien erstellen;
- YAML als eigene Orchestrierungsengine verwenden;
- Taskabhängigkeiten im YAML modellieren;
- positive Jobs weiterhin primär als Java-Textblocks erzeugen;
- nur Dateiexistenz prüfen;
- Coverage anhand bloßer Testmethodennamen behaupten;
- `DIRECT_JOB_EXECUTION` ohne Trace;
- alte falsche Coverage-Klassifikationen unverändert lassen;
- alle P0-Combined-Tests durch einen einfachen Pipelinejob ersetzen;
- Testjobmodul publizieren;
- Testjobs ins Runtime-Image kopieren;
- Host-Testlibraries in den Consumer-Plugin-Classpath lecken lassen;
- `mavenLocal()` oder `flatDir` in persistierten Jobs;
- Migration von `sogis/gretljobs`;
- Legacy-Plugin-DSL;
- deaktivierte Tests;
- pauschale Retries;
- CI um einen zusätzlichen doppelt laufenden Vollaggregator erweitern.

---

# Teil L – Definition of Done

## 61. P0.5

- [ ] Runtime-Launcher ist einzige automatische Quelle für `--offline`.
- [ ] Lifecycle-Argumentklasse ergänzt kein `--offline` mehr.
- [ ] `--offline` erscheint im normalen Launcher-Aufruf genau einmal.
- [ ] One-shot ergänzt `--no-daemon`.
- [ ] Service nutzt den Daemon.
- [ ] One-shot-Executor lehnt `SERVICE` ab.
- [ ] Serviceausführung läuft über `RuntimeImageServiceContainer` beziehungsweise Service-Backend.
- [ ] kein öffentliches Verfahren leakt ein temporäres Gradle User Home.
- [ ] Erfolg, Buildfehler und Dockerfehler schließen temporäre Homes.
- [ ] Terminologie unterscheidet lokale Auflösung und im Image gebündelte Auflösung.
- [ ] bestehende Runtime-Tests sind grün.

## 62. Katalog

- [ ] Root-Verzeichnis `test-jobs` existiert.
- [ ] internes Subprojekt `gretl-job-tests` existiert.
- [ ] Subprojekt wird nicht publiziert.
- [ ] Subprojekt gelangt nicht ins Runtime-Image.
- [ ] `job.yaml` Schema Version 1 ist implementiert.
- [ ] unbekannte Felder werden abgelehnt.
- [ ] Job-IDs sind eindeutig.
- [ ] Builddateien werden bytegleich materialisiert.
- [ ] Settings werden backendabhängig erzeugt.
- [ ] persistierte Jobs enthalten keine Source-Classpath-Hacks.
- [ ] `validateTestJobCatalog` existiert.

## 63. Backends

- [ ] neutrales `TestJobExecutionBackend` existiert.
- [ ] kein dritter gleichnamiger `GretlBuildExecutor` wurde eingeführt.
- [ ] Plugin-Classpath-Backend existiert.
- [ ] Published-Artifact-Backend existiert.
- [ ] Runtime-One-shot-Backend existiert.
- [ ] Runtime-Service-Backend existiert.
- [ ] alle Backends liefern `GretlBuildResult`.
- [ ] TestKit-Outcomes werden vollständig gemappt.
- [ ] Secrets werden maskiert.
- [ ] Backends besitzen klare Ressourcenlifecycles.

## 64. Pilotjobs

- [ ] `core-gzip` existiert als persistierter Job.
- [ ] `core-sqlite` existiert als persistierter Job.
- [ ] `combined-core-geotools-pipeline` existiert mit Groovy.
- [ ] `geotools-read-shapefile` existiert.
- [ ] alle vier laufen über Plugin Classpath.
- [ ] alle vier laufen über Published Artifacts.
- [ ] alle vier laufen über Runtime Image One-shot.
- [ ] mindestens Gzip und kombinierte Pipeline laufen im Servicecontainer.
- [ ] fachliche Outputs sind backendübergreifend identisch.
- [ ] keine positiven Buildscripts dieser vier Jobs bleiben als duplizierte Java-Textblocks zurück.

## 65. Trace und Coverage

- [ ] Tasktrace enthält Job, Variante, Backend, Pfad, Klasse und Outcome.
- [ ] Trace wird für Coverage-Läufe erzeugt.
- [ ] Configuration-Cache-Tests laufen ohne inkompatiblen Trace-Listener.
- [ ] Expected Tasks werden gegen den realen Trace geprüft.
- [ ] Coverage-Matrix verwendet Job, Taskpfad und Taskklasse.
- [ ] `DIRECT_JOB_EXECUTION` wird nur durch Trace vergeben.
- [ ] alle öffentlichen Tasks besitzen einen ehrlichen Matrixstatus.
- [ ] nicht wirklich ausgeführte Tasks sind nicht als Direct E2E markiert.
- [ ] Coverage-Report wird erzeugt.

## 66. Bestehende Tests

- [ ] Pluginreihenfolge bleibt geprüft.
- [ ] Classloader-/Worker-Isolation bleibt geprüft.
- [ ] Shared Services bleiben geprüft.
- [ ] Configuration Cache bleibt geprüft.
- [ ] Fehlerpropagation bleibt geprüft.
- [ ] Published-Verträge bleiben geprüft.
- [ ] Dependency-Closure bleibt geprüft.
- [ ] Daemon-Wiederverwendung bleibt geprüft.
- [ ] keine P0-Beweisziele wurden durch reine Jobtests ersetzt.

## 67. Gradle und CI

- [ ] `canonicalJobSourceTest` existiert.
- [ ] `canonicalJobPublishedTest` existiert.
- [ ] `canonicalJobRuntimeImageTest` existiert.
- [ ] `canonicalJobServiceTest` existiert.
- [ ] `canonicalJobTest` existiert als lokaler Aggregator.
- [ ] `check` enthält Source-Jobtests.
- [ ] `publishedArtifactTest` enthält Published-Jobtests.
- [ ] `runtimeImageTest` enthält Runtime-Jobtests.
- [ ] keine Taskabhängigkeitszyklen.
- [ ] CI führt keine vollständige Jobmatrix doppelt aus.
- [ ] Reports werden hochgeladen.
- [ ] Snapshot-Publikation bleibt gegatet.

## 68. Dokumentation

- [ ] persistente Testjobs sind dokumentiert.
- [ ] neuer Job kann anhand der Dokumentation hinzugefügt werden.
- [ ] lokale und gebündelte Dependency-Auflösung sind korrekt erklärt.
- [ ] keine `gretljobs`-Migration wird suggeriert.
- [ ] lokale Befehle sind aktuell.

---

# Teil M – Abschlussbericht des Coding Agents

## 69. Architekturbericht

Beschreibe:

- P0.5-Konsolidierung;
- einzige `--offline`-Quelle;
- One-shot-/Service-Trennung;
- Gradle-Home-Lifecycle;
- Katalogschichten;
- Backendschichten;
- Settingsrendering;
- Tasktrace;
- Assertion-Registry.

## 70. Geänderte Dateien

Für jede Datei:

- Pfad;
- Zweck;
- wichtigste Änderung;
- entfernte Doppelspurigkeit.

## 71. Neue Klassen und Methoden

Liste mindestens:

- Jobmodelle;
- YAML-Reader;
- Validator;
- Katalog;
- Materializer;
- Settingsrenderer;
- Backends;
- Resultadapter;
- Traceklassen;
- Assertions;
- Runner;
- Gradle-Tasks.

## 72. Pilotjobs

Für jeden Job:

- Verzeichnis;
- Buildvarianten;
- Entry-Tasks;
- erwartete Taskklassen;
- fachliche Assertions;
- erfolgreiche Backends;
- Laufzeiten.

## 73. Entfernte Duplikate

Liste:

- entfernte Java-Buildscript-Textblocks;
- entfernte doppelte Gzip-/SQLite-/Pipeline-Canaries;
- beibehaltene spezielle P0-Tests und deren Grund.

## 74. Coverage

Dokumentiere:

- alte falsche beziehungsweise zu großzügige Einträge;
- neue Klassifikation;
- Trace-Nachweis;
- weiterhin nicht abgedeckte öffentliche Tasks.

Keine kosmetisch beschönigte Abdeckung.

## 75. Testresultate

Für jeden ausgeführten Befehl:

- Kommando;
- Ergebnis;
- Testanzahl;
- Laufzeit;
- Fehler;
- Behebung.

## 76. Abweichungen

Nur technisch notwendige Abweichungen:

- Spezifikationspunkt;
- tatsächliche Lösung;
- Begründung;
- gleichwertiger Nachweis;
- verbleibendes Risiko.

## 77. Verbleibende Risiken und P2

Konkrete Folgearbeiten:

- PostGIS-Fixture;
- HTTP-Fixture;
- S3;
- FTP;
- DuckDB Spatial;
- INTERLIS-Pipelines;
- weitere direkte Taskabdeckung.

Keine allgemeine Floskelliste.

---

# Anhang A – Zielklassenübersicht

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/
├── execution/
│   ├── RuntimeExecutionMode.java
│   ├── RuntimeImageLifecycleArguments.java
│   ├── RuntimeImageBuildExecutor.java
│   ├── GretlBuildResult.java
│   └── GretlTaskOutcome.java
├── job/
│   ├── TestJobDescriptor.java
│   ├── TestJobBuildVariant.java
│   ├── TestJobBuildLanguage.java
│   ├── ExpectedTaskExecution.java
│   ├── TestJobExecutionTarget.java
│   ├── TestJobExecutionRequirement.java
│   ├── TestJobYamlReader.java
│   ├── TestJobDescriptorValidator.java
│   ├── TestJobCatalog.java
│   ├── FileSystemTestJobCatalog.java
│   ├── TestJobMaterializer.java
│   ├── DefaultTestJobMaterializer.java
│   ├── MaterializedTestJob.java
│   ├── TestJobSettingsRenderer.java
│   ├── DefaultTestJobSettingsRenderer.java
│   ├── TestJobExecutionBackend.java
│   ├── TestJobExecutionRequest.java
│   ├── TestJobExecutionBackendFactory.java
│   ├── TestKitJobExecutionBackend.java
│   ├── PluginClasspathJobExecutionBackend.java
│   ├── PublishedArtifactJobExecutionBackend.java
│   ├── RuntimeImageOneShotJobExecutionBackend.java
│   ├── RuntimeImageServiceJobExecutionBackend.java
│   ├── TestKitBuildResultAdapter.java
│   ├── TestJobRunner.java
│   ├── TestJobRunRequest.java
│   ├── TestJobRunResult.java
│   ├── TestJobAssertions.java
│   ├── TestJobAssertionRegistry.java
│   ├── TestJobEnvironment.java
│   └── TestServiceFixture.java
└── trace/
    ├── TaskExecutionTrace.java
    ├── TaskExecutionTraceEntry.java
    ├── TaskExecutionTraceReader.java
    └── ExpectedTaskTraceVerifier.java
```

Namen dürfen an bestehende Packages angepasst werden, solange die Verantwortlichkeiten erhalten bleiben.

---

# Anhang B – Zielverzeichnis der Pilotjobs

```text
test-jobs/
├── core/
│   ├── gzip/
│   │   ├── job.yaml
│   │   ├── build.gradle
│   │   ├── input/data.txt
│   │   └── expected/payload.txt
│   └── sqlite/
│       ├── job.yaml
│       ├── build.gradle
│       ├── input/schema.sql
│       └── expected/rows.json
├── geotools/
│   └── read-shapefile/
│       ├── job.yaml
│       ├── build.gradle
│       ├── input/points.shp
│       ├── input/points.shx
│       ├── input/points.dbf
│       ├── input/points.prj
│       ├── input/points.cpg
│       └── expected/features.json
└── combined/
    └── core-geotools-pipeline/
        ├── job.yaml
        ├── build.gradle
        ├── input/raster.xml
        ├── input/raster-to-asc.xsl
        └── expected/raster-summary.json
```

---

# Anhang C – Backendmatrix P1

| Job | Plugin Classpath | Published | Runtime One-shot | Runtime Service |
|---|---:|---:|---:|---:|
| `core-gzip` | Required | Required | Required | Required |
| `core-sqlite` | Required | Required | Required | Required |
| `combined-core-geotools-pipeline` Groovy | Required | Required | Required | Required |
| `combined-core-geotools-pipeline` | Required | Required | Required | Required |
| `geotools-read-shapefile` | Required | Required | Required | Optional |

Ein Optional-Eintrag darf im Abschlussbericht nicht als vollständig abgedeckt dargestellt werden.

---

# Anhang D – Review-Checkliste

## P0.5

- [ ] nur Launcher setzt automatisch `--offline`;
- [ ] Lifecycle-Argumentklasse sauber;
- [ ] One-shot-Executor nur One-shot;
- [ ] Servicecontainer separat;
- [ ] keine Temp-Home-Leaks;
- [ ] präzise Terminologie.

## Katalog

- [ ] echte Dateien;
- [ ] keine Templatisierung;
- [ ] kleines YAML;
- [ ] zentrale Validierung;
- [ ] nicht publiziert;
- [ ] nicht im Image.

## Backends

- [ ] Source;
- [ ] Published;
- [ ] Runtime One-shot;
- [ ] Service;
- [ ] einheitliches Resultat;
- [ ] klare Lifecycles.

## Pilotjobs

- [ ] Gzip;
- [ ] SQLite;
- [ ] kombinierte Pipeline Groovy;
- [ ] ReadShapefile;
- [ ] semantische Assertions;
- [ ] keine Buildscript-Duplikate.

## Coverage

- [ ] realer Tasktrace;
- [ ] Pfad und Klasse;
- [ ] ehrliche Matrix;
- [ ] keine falschen Direct-E2E-Aussagen.

## CI

- [ ] Hauptgates integriert;
- [ ] keine doppelten Vollausführungen;
- [ ] Reports;
- [ ] Publish bleibt blockiert bei Fehler.

---

# Beginn der Arbeiten

Der Coding Agent soll:

1. diese Spezifikation vollständig lesen;
2. den aktuellen `main` erneut analysieren;
3. P0.5 vollständig abschließen;
4. erst danach die P1-Backends darauf aufbauen;
5. die vier Pilotjobs persistieren;
6. bestehende positive Buildscript-Duplikate entfernen;
7. alle P0-Beweisziele erhalten;
8. die Coverage-Matrix ehrlich korrigieren;
9. alle Tests ausführen und Fehler beheben;
10. erst nach erfüllter Definition of Done den strukturierten Abschlussbericht liefern.

Kleinere Architekturentscheidungen sind selbstständig zu treffen.

Die Beweisziele dürfen nicht abgeschwächt werden.
