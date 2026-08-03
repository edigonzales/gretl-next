# Spezifikation: P0 – `gretl-core` und `gretl-geotools` gemeinsam testen

**Ziel-Repository:** `https://github.com/edigonzales/gretl-next`
**Priorität:** P0 / zwingendes Release-Gate
**Status:** verbindlicher Umsetzungsauftrag für einen LLM Coding Agent
**Stand:** 31. Juli 2026
**Produktmodule:** `gretl-core`, `gretl-geotools`
**Internes Testmodul:** vorzugsweise `gretl-combined-tests`
**Verbindlicher Root-Task:** `coreGeoToolsCombinedTest`
**Consumer-Vertrag:** moderne Gradle-Plugin-DSL `plugins {}`

**DSL-Policy:** Die P0-Suite prüft ausschließlich Groovy-Gradle-Builds. Kotlin
DSL kann durch Gradle weiterhin zufällig funktionieren, ist aber kein GRETL-
Vertrag und wird weder getestet noch dokumentiert.

---

## 1. Auftrag

Implementiere eine eigenständige, reproduzierbare und zwingende P0-Teststufe, die `gretl-core` und `gretl-geotools` **im selben realistischen Gradle-Consumer-Build** prüft.

Die bestehenden Einzelmodultests reichen nicht aus. Ein Core-Test beweist nicht, dass beide Plugins denselben Gradle-Build konfliktfrei konfigurieren. Ein GeoTools-Test beweist nicht, dass Core-Tasktypen, GeoTools-Tasktypen, Shared Services, Worker-Classpath, Provider-Verdrahtung, publizierte Marker und Gradle-Lifecycle gemeinsam funktionieren.

Die Aufgabe ist erst abgeschlossen, wenn dieselben fachlichen Testklassen mindestens in folgenden Modi erfolgreich laufen:

1. expliziter Source-/Plugin-Classpath über Gradle TestKit;
2. publizierte Plugin-Artefakte aus dem isolierten `published-test`-Repository.

Die Testsuite muss außerdem executor-neutral sein, damit die getrennten P0-Suiten für Runtime-Image und Offline-Image sie ohne fachliche Duplikation wiederverwenden können.

---

## 2. Dauerhafte Projektabgrenzung

### 2.1 Keine Migration von `sogis/gretljobs`

Die Migration bestehender Jobs aus `sogis/gretljobs` ist dauerhaft **nicht Bestandteil von `gretl-next`** und nicht Bestandteil dieser Spezifikation.

Der Coding Agent darf insbesondere nicht:

- reale Jobverzeichnisse aus `sogis/gretljobs` kopieren;
- bestehende Jobs umschreiben;
- Migrationsskripte oder Codemods erstellen;
- Legacy-Task-APIs aus Kompatibilitätsgründen ergänzen;
- `gretljobs` als Abnahmesuite verwenden;
- die Fertigstellung von einer späteren Migration abhängig machen;
- ein Folgearbeitspaket „Migration gretljobs“ anlegen.

Alle Consumer-Projekte und Fixtures werden eigens für `gretl-next` erstellt.

### 2.2 Nur moderne Plugin-DSL

Verbindlich getestet wird:

```groovy
plugins {
    id 'ch.so.agi.gretl'
    id 'ch.so.agi.gretl.geotools'
}
```

und:

Nicht Bestandteil des Produktvertrags sind Kotlin-DSL-Builds sowie:

```groovy
apply plugin: 'ch.so.agi.gretl'
apply plugin: 'ch.so.agi.gretl.geotools'
```

sowie manuell befüllte `buildscript.classpath`-Konfigurationen.

### 2.3 Keine Verschmelzung der Produktionsmodule

Diese P0-Aufgabe verlangt keine Zusammenlegung von Core und GeoTools. Sie muss vielmehr beweisen, dass beide Module gemeinsam verwendbar sind, **während ihre Grenzen erhalten bleiben**.

Verbindliche Invarianten:

- `gretl-core` bleibt frei von aufgelösten `org.geotools`-Modulen;
- `gretl-geotools` wendet `gretl-core` nicht implizit an;
- ein Consumer wendet beide Plugins explizit an;
- GeoTools-Libraries bleiben im isolierten Worker-Classpath;
- Core-Tasks laufen nicht im GeoTools-Worker;
- GeoTools-Tasks verwenden nicht den Core-Build-Service;
- gemeinsame Verwendung bedeutet Komposition, nicht Monolithisierung.

### 2.4 Keine doppelte Image-Infrastruktur

Diese Spezifikation implementiert nicht erneut den vollständigen Runtime-Image- oder Offline-Image-Unterbau. Existierende Executors müssen wiederverwendet werden. Fehlen sie noch, bleibt die kombinierte Testsuite so backend-neutral, dass sie später ohne Änderungen an den Fachtests eingebunden werden kann.

---

## 3. Aktueller Ausgangspunkt

Der Coding Agent muss den aktuellen Repository-Stand vor Änderungen erneut lesen.

Zum Zeitpunkt dieser Spezifikation gilt:

### 3.1 Core

- Plugin-ID: `ch.so.agi.gretl`
- Implementierung: `ch.so.agi.gretl.gradle.GretlPlugin`
- Shared Services:
  - `gretlCoreService`
  - `gretlInterlisService`
- Core-Tasks erben von `AbstractCoreGretlTask`
- INTERLIS-Tasks erben von `AbstractInterlisTask`
- alle Core-Konfigurationen schließen `org.geotools` und `org.geotools.ogc` aus
- `assertNoGeoToolsDependencies` ist Teil von `check`
- Source-, Integration- und Published-Artifact-Tests existieren

### 3.2 GeoTools

- Plugin-ID: `ch.so.agi.gretl.geotools`
- Implementierung: `ch.so.agi.gretl.geotools.GretlGeotoolsPlugin`
- Extension: `gretlGeotools`
- Shared Service: `gretlGeoToolsService`
- `GeoToolsBuildService` besitzt `maxParallelUsages = 1`
- Worker-Code liegt in einem eigenen Source Set
- Worker-Runtime und Libraries werden unter `gretl-geotools-worker-classpath/` in das Plugin-JAR eingebettet
- `ReadShapefile`, `RasterReclassify` und `Vectorize` besitzen Einzelmodultests

### 3.3 Test-Support

Vorhanden sind mindestens:

- `GretlBuildExecutor`
- `AbstractGradleBuildExecutor`
- `PluginClasspathBuildExecutor`
- `PublishedArtifactBuildExecutor`
- `GretlBuildExecutors`
- `GretlTestExecutionMode`
- `GretlTestProjectSettings`

Der aktuelle `PLUGIN_CLASSPATH`-Executor verwendet den parameterlosen TestKit-Aufruf `withPluginClasspath()`. Für ein eigenständiges kombiniertes Testmodul muss der Plugin-Classpath beider Produktionsplugins explizit kontrolliert werden.

---

## 4. Beweisziele

Die Teststufe muss sechs voneinander unabhängige Eigenschaften beweisen.

### 4.1 Koexistenz

Beide Plugins können im selben Projekt in beiden Reihenfolgen angewendet werden.

### 4.2 Unabhängigkeit

Keines der Plugins wendet das andere stillschweigend an.

### 4.3 Komposition

Outputs eines Core-Tasks können Gradle-nativ Inputs eines GeoTools-Tasks sein und GeoTools-Outputs können Inputs eines Core-Tasks sein.

### 4.4 Isolation

GeoTools-Libraries bleiben außerhalb des normalen Consumer-/Plugin-Classpaths und werden ausschließlich im Worker geladen.

### 4.5 Lifecycle-Korrektheit

Provider, Lazy Configuration, Up-to-date-Checks, Configuration Cache, Multi-Projekt-Builds und parallele Ausführung funktionieren.

### 4.6 Distributionskorrektheit

Dasselbe Verhalten funktioniert mit publizierten Markern, POMs, Module Metadata und JARs.

---

## 5. Normative Architektur

### 5.1 Eigenes internes Subprojekt

Erzeuge vorzugsweise:

```text
gretl-combined-tests
```

Das Modul:

- enthält keine Produktionsklassen;
- definiert keine öffentliche Plugin-ID;
- wird nicht publiziert;
- wird nicht ins Runtime-Image kopiert;
- ist eindeutiger Eigentümer der kombinierten Tests;
- trennt Child-Build-Plugin-Classpath und Host-Assertion-Classpath.

Eine andere Struktur ist nur zulässig, wenn sie keine Produktionsabhängigkeit zwischen Core und GeoTools erzeugt und dieselben Isolationsgarantien bietet.

### 5.2 Ein Testkörper, mehrere Backends

Die Fachtests verwenden ausschließlich eine zentrale Executor-Factory. Source- und Published-Tests dürfen nicht dupliziert werden.

---

## 6. Build des Testmoduls

### 6.1 `settings.gradle`

Ergänze:

```groovy
include 'gretl-combined-tests'
```

### 6.2 `gretl-combined-tests/build.gradle`

```groovy
plugins {
    id 'java'
}

base {
    archivesName = 'gretl-combined-tests'
}
```

`maven-publish` darf nicht angewendet werden.

### 6.3 Explizite Plugin-Classpath-Konfiguration

```groovy
configurations {
    combinedPluginClasspath {
        canBeConsumed = false
        canBeResolved = true
        visible = false
        transitive = true
    }
}
```

### 6.4 Dependencies

Sinngemäß:

```groovy
dependencies {
    combinedPluginClasspath project(':gretl-core')
    combinedPluginClasspath project(':gretl-geotools')

    testImplementation gradleTestKit()
    testImplementation project(':gretl-test-support')
    testImplementation "org.junit.jupiter:junit-jupiter-api:${junitVersion}"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:${junitVersion}"

    // Nur für unabhängige Host-Assertions.
    testImplementation "org.geotools:gt-main:${geotoolsVersion}"
    testImplementation "org.geotools:gt-geotiff:${geotoolsVersion}"
    testImplementation "org.geotools:gt-geopkg:${geotoolsVersion}"
    testImplementation "org.geotools:gt-epsg-hsql:${geotoolsVersion}"
}
```

Die Host-GeoTools-Abhängigkeiten dürfen niemals in den Child-Build-Plugin-Classpath gelangen.

### 6.5 Classpath-Isolationscheck

Erzeuge `assertCombinedPluginClasspathIsolation`.

Er prüft:

- Core-Plugin-JAR vorhanden;
- GeoTools-Plugin-JAR vorhanden;
- Core-Runtime-Abhängigkeiten vorhanden;
- kein `gretl-test-support`;
- kein `gretl-combined-tests`;
- keine Testklassen;
- keine Sources-/Javadoc-JARs;
- keine direkt aufgelösten GeoTools-Worker-Libraries;
- Host-Assertion-GeoTools-JARs fehlen im Child-Classpath.

---

## 7. Expliziter TestKit-Plugin-Classpath

### 7.1 Task `writeCombinedPluginClasspath`

```groovy
def combinedPluginClasspathFile =
        layout.buildDirectory.file(
            'combined-plugin-classpath/classpath.txt'
        )

tasks.register('writeCombinedPluginClasspath') {
    group = 'verification'
    description = 'Writes the explicit TestKit classpath for Core and GeoTools.'

    dependsOn ':gretl-core:jar'
    dependsOn ':gretl-geotools:jar'

    inputs.files configurations.combinedPluginClasspath
    outputs.file combinedPluginClasspathFile

    doLast {
        // kanonische, existierende, eindeutige und sortierte Dateien
    }
}
```

Dateiformat: eine absolute Datei pro UTF-8-Zeile.

### 7.2 `ExplicitPluginClasspathTestConfiguration`

```java
public record ExplicitPluginClasspathTestConfiguration(
        Path classpathFile,
        Path testKitDirectory) {

    public static ExplicitPluginClasspathTestConfiguration
            fromSystemProperties();

    public List<File> readClasspath();

    public void validate();

    private static Path requiredAbsolutePath(String propertyName);
}
```

Validierung:

- Classpath-Datei existiert;
- TestKit-Verzeichnis ist absolut;
- jede Datei existiert;
- keine kanonischen Duplikate;
- Core- und GeoTools-JAR vorhanden;
- kein Test-Support;
- keine Source-/Javadoc-JARs.

### 7.3 `ExplicitPluginClasspathBuildExecutor`

```java
public final class ExplicitPluginClasspathBuildExecutor
        extends AbstractGradleBuildExecutor {

    private final ExplicitPluginClasspathTestConfiguration configuration;

    public ExplicitPluginClasspathBuildExecutor(
            ExplicitPluginClasspathTestConfiguration configuration);

    @Override
    protected GradleRunner customize(GradleRunner runner);
}
```

Implementierung:

```java
return runner
        .withPluginClasspath(configuration.readClasspath())
        .withTestKitDir(configuration.testKitDirectory().toFile());
```

### 7.4 Factory-Anpassung

`GretlBuildExecutors.current()` erkennt eine gesetzte Property für den expliziten Plugin-Classpath. Bestehende Einzelmodultests verwenden weiterhin den bisherigen Executor.

---

## 8. Neue System Properties

Ergänze zentral:

```java
public static final String EXPLICIT_PLUGIN_CLASSPATH =
        "gretl.test.explicitPluginClasspath";
public static final String TEST_KIT_DIRECTORY =
        "gretl.test.testKitDirectory";
public static final String COMBINED_TEST =
        "gretl.test.combined";
```

Hilfsmethoden:

```java
public static Optional<Path> optionalAbsolutePath(String property);
public static Path requiredAbsolutePath(String property);
public static boolean hasExplicitPluginClasspath();
public static boolean booleanProperty(String property, boolean defaultValue);
```

---

## 9. Verbindliche Gradle-Testtasks

### 9.1 Source-Modus

Der normale `test`-Task des kombinierten Moduls:

- hängt von `writeCombinedPluginClasspath` ab;
- setzt `PLUGIN_CLASSPATH`;
- setzt die explizite Classpath-Datei;
- verwendet ein eigenes TestKit-Verzeichnis;
- schließt Image-only-Tags aus.

### 9.2 Published-Modus

Erzeuge:

```text
:gretl-combined-tests:publishedArtifactTest
```

Der Task:

- hängt von `preparePublishedTestRepository` und `verifyPublishedTestRepository` ab;
- setzt `PUBLISHED_ARTIFACT`;
- setzt Repository, Version und eigenes TestKit-Verzeichnis;
- führt dieselben Fachtests aus.

### 9.3 Configuration Cache

Erzeuge:

```text
:gretl-combined-tests:combinedConfigurationCacheTest
```

### 9.4 Root-Aggregator

```groovy
tasks.register('coreGeoToolsCombinedTest') {
    group = 'verification'
    description = 'Runs the complete combined Core and GeoTools P0 suite.'

    dependsOn ':gretl-combined-tests:test'
    dependsOn ':gretl-combined-tests:publishedArtifactTest'
    dependsOn ':gretl-combined-tests:combinedConfigurationCacheTest'
}
```

Der bestehende Root-Task `publishedArtifactTest` muss zusätzlich vom kombinierten Published-Test abhängen.

---

## 10. Basisklasse

```java
abstract class CombinedPluginTestSupport {

    @TempDir
    Path projectDir;

    protected GretlBuildExecutor executor();
    protected BuildResult run(String... arguments);
    protected BuildResult runAndFail(String... arguments);
    protected void writeSettings();
    protected void writeSettings(String projectName);
    protected void writeGroovyBuild(String content);
    protected Path copyResource(String source, String target);
    protected void copyResourceTree(String source, Path target);
    protected void assertTaskOutcome(
            BuildResult result,
            String taskPath,
            TaskOutcome expected);
    protected void assertTaskNotExecuted(
            BuildResult result,
            String taskPath);
    protected void assertNoCombinedPluginWarnings(BuildResult result);
    protected String currentPluginVersion();
    protected boolean isPublishedMode();
}
```

Fachliche Assertions dürfen nicht backendabhängig verzweigen.

`assertNoCombinedPluginWarnings` prüft mindestens auf:

- `NoClassDefFoundError`
- `ClassNotFoundException`
- `LinkageError`
- `ServiceConfigurationError`
- doppelte Service-Registrierung
- multiple SLF4J Provider
- Worker-Protokoll-Leaks `GRETL_WORKER|`

---

## 11. Testprojekt-Builder

```java
public final class CombinedGradleTestProject {

    public static CombinedGradleTestProject create(Path directory);
    public CombinedGradleTestProject settingsGroovy(String content);
    public CombinedGradleTestProject buildGroovy(String content);
    public CombinedGradleTestProject textFile(
            String relativePath,
            String content);
    public CombinedGradleTestProject binaryFile(
            String relativePath,
            byte[] content);
    public CombinedGradleTestProject copyResource(
            Class<?> owner,
            String source,
            String target);
    public CombinedGradleTestProject copyResourceTree(
            Class<?> owner,
            String source,
            String target);
    public CombinedGradleTestProject subproject(
            String name,
            Consumer<CombinedGradleTestProject> configuration);
    public Path path(String relativePath);
    public void assertNoForbiddenConsumerConstructs();
}
```

Verbotene Fixture-Fragmente:

```text
apply plugin:
buildscript {
mavenLocal()
flatDir
includeBuild
withPluginClasspath
gretl-core/build
gretl-geotools/build
build/classes
build/resources
```

---

## 12. Plugin-Anwendung

Testklasse:

```text
CombinedPluginApplicationFunctionalTest
```

Methoden:

```java
@Test void appliesCoreThenGeoToolsWithGroovyDsl();
@Test void appliesGeoToolsThenCoreWithGroovyDsl();
@Test void bothPluginsExposeTheirTaskTypes();
@Test void repeatedPluginManagerApplyIsIdempotent();
@Test void applyingBothDoesNotRegisterDuplicateTasks();
@Test void applyingBothDoesNotRegisterDuplicateExtensions();
@Test void applyingBothDoesNotRegisterConflictingServices();
```

Beide Pluginreihenfolgen müssen denselben relevanten Taskbestand ergeben.

---

## 13. Unabhängigkeit

Testklasse:

```text
CombinedPluginIndependenceFunctionalTest
```

Methoden:

```java
@Test void geoToolsPluginDoesNotImplicitlyApplyCorePlugin();
@Test void corePluginDoesNotImplicitlyApplyGeoToolsPlugin();
@Test void coreOnlyProjectDoesNotRegisterGeoToolsTasks();
@Test void geoToolsOnlyProjectDoesNotRegisterCoreTasks();
@Test void combinedProjectRequiresBothExplicitPluginDeclarations();
@Test void coreImplementationPomDoesNotDependOnGeoTools();
@Test void geoToolsImplementationPomDoesNotDependOnCore();
```

Primär geprüft werden Plugin-Manager-Status, registrierte Tasks, Extensions und Services. Klassenauflösung allein genügt nicht.

---

## 14. Shared Services

Testklasse:

```text
CombinedBuildServiceFunctionalTest
```

Methoden:

```java
@Test void coreAndGeoToolsUseDistinctSharedServiceNames();
@Test void coreTasksAreBoundToCoreService();
@Test void interlisTasksAreBoundToInterlisService();
@Test void geoToolsTasksAreBoundToGeoToolsService();
@Test void multipleProjectsDoNotDuplicateServices();
@Test void repeatedBuildDoesNotFailServiceRegistration();
```

Erwartete Namen:

```text
gretlCoreService
gretlInterlisService
gretlGeoToolsService
```

Keine öffentliche Produktions-API nur für Tests ergänzen.

---

## 15. Classloader- und Worker-Isolation

Testklasse:

```text
CombinedClassloaderIsolationFunctionalTest
```

Methoden:

```java
@Test void coreAndGeoToolsTaskClassesAreVisibleToBuildscript();
@Test void rawGeoToolsLibrariesAreNotVisibleToBuildscript();
@Test void rawGeoToolsLibrariesAreNotVisibleToCoreTaskClassloader();
@Test void workerClasspathContainsWorkerRuntime();
@Test void workerClasspathContainsExpectedGeoToolsLibraries();
@Test void workerClasspathDoesNotContainCorePluginJar();
@Test void workerClasspathDoesNotContainCoreRuntimeDependencies();
@Test void workerClasspathDoesNotContainConsumerOutputs();
@Test void workerProtocolDoesNotLeakToBuildLog();
@Test void coreTaskWorksAfterGeoToolsWorkerExecution();
```

Der Worker-Classpath muss mindestens enthalten:

```text
gretl-geotools-*-worker-runtime.jar
gt-main-*.jar
gt-shapefile-*.jar
gt-geotiff-*.jar
gt-coverage-*.jar
gt-epsg-hsql-*.jar
```

Verbotene Pfadfragmente:

```text
gretl-core
gretl-test-support
/build/classes/
/build/resources/
/src/
```

---

## 16. Kanonische Core→GeoTools→Core-Pipeline

Die wichtigste Canary-Pipeline lautet:

```text
XslTransformer (Core)
        ↓ Provider
RasterReclassify (GeoTools)
        ↓ Provider
Gzip (Core)
```

### 16.1 Fixture

```text
fixtures/combined-pipeline/
├── raster.xml
├── raster-to-asc.xsl
└── expected/
    ├── expected-classes.json
    └── expected-summary.json
```

Das XSLT erzeugt ein deterministisches ESRI ASCII Grid.

### 16.2 Groovy-Consumer

```groovy
import ch.so.agi.gretl.tasks.XslTransformer
import ch.so.agi.gretl.tasks.Gzip
import ch.so.agi.gretl.geotools.tasks.RasterReclassify

plugins {
    id 'ch.so.agi.gretl'
    id 'ch.so.agi.gretl.geotools'
}

def generateRaster = tasks.register('generateRaster', XslTransformer) {
    xslFile 'input/raster-to-asc.xsl'
    xmlFiles 'input/raster.xml'
    outDirectory layout.buildDirectory.dir('generated')
    fileExtension 'asc'
}

def generatedRaster = generateRaster.flatMap {
    it.outDirectory.file('raster.asc')
}

def reclassifyRaster = tasks.register(
        'reclassifyRaster',
        RasterReclassify
) {
    inputRaster.set(generatedRaster)
    outputRaster layout.buildDirectory.file(
        'geotools/reclassified.tif'
    )
    breaks 0d, 55d, 60d, 65d, 70d, 500d
    noData(-100d)
}

tasks.register('packageRaster', Gzip) {
    dataFile(reclassifyRaster.flatMap { it.outputRaster })
    gzipFile layout.buildDirectory.file(
        'distribution/reclassified.tif.gz'
    )
}
```

### 16.3 Zwingende Eigenschaft

Die Pipeline darf keine manuellen `dependsOn` benötigen. Task-Abhängigkeiten werden aus Provider- und Input-/Output-Verdrahtung abgeleitet.

Falls aktuelle Task-Setter Provider-Metadaten verlieren, ist die Task-API Gradle-nativ zu verbessern.

### 16.4 Testmethoden

```java
@Test void executesPipelineWithGroovyDsl();
@Test void infersDependenciesFromProviders();
@Test void producesCorrectAsciiGrid();
@Test void producesCorrectReclassifiedGeoTiff();
@Test void packagesExactGeoTiffBytesIntoGzip();
@Test void executesTasksInExpectedOrder();
```

Dieselben Methoden laufen im Source- und Published-Modus.

---

## 18. Semantische Assertions

### 18.1 `AsciiGridAssertions`

```java
public final class AsciiGridAssertions {
    public static AsciiGrid read(Path path);
    public static void assertDimensions(AsciiGrid grid, int cols, int rows);
    public static void assertCellSize(AsciiGrid grid, double expected);
    public static void assertNoData(AsciiGrid grid, double expected);
    public static void assertValues(
            AsciiGrid grid,
            double[][] expected);
}
```

### 18.2 `GeoTiffAssertions`

```java
public final class GeoTiffAssertions {
    public static RasterSummary read(Path path);
    public static void assertDimensions(
            RasterSummary summary,
            int width,
            int height);
    public static void assertCrs(
            RasterSummary summary,
            String expectedCode);
    public static void assertNoData(
            RasterSummary summary,
            double expected);
    public static void assertBandValues(
            RasterSummary summary,
            int band,
            double[][] expected);
}
```

### 18.3 `GzipAssertions`

```java
public final class GzipAssertions {
    public static byte[] decompress(Path gzip);
    public static void assertDecompressesToFile(
            Path gzip,
            Path expectedFile);
    public static void assertHeaderIsValid(Path gzip);
}
```

GeoTools-Reader und DataStores sind zuverlässig zu schließen. Host-Assertions dürfen keine Datei-Locks hinterlassen.

---

## 19. Task-Outcomes und Inkrementalität

Erster Lauf:

```text
:generateRaster      SUCCESS
:reclassifyRaster    SUCCESS
:packageRaster       SUCCESS
```

Zweiter unveränderter Lauf:

```text
:generateRaster      UP_TO_DATE
:reclassifyRaster    UP_TO_DATE
:packageRaster       UP_TO_DATE
```

Nach Änderung von `raster.xml`:

```text
:generateRaster      SUCCESS
:reclassifyRaster    SUCCESS
:packageRaster       SUCCESS
```

Nach Löschen nur der GZIP-Datei:

```text
:generateRaster      UP_TO_DATE
:reclassifyRaster    UP_TO_DATE
:packageRaster       SUCCESS
```

Testklasse:

```text
CombinedIncrementalBuildFunctionalTest
```

Methoden:

```java
@Test void secondRunIsUpToDate();
@Test void changingCoreInputInvalidatesEntirePipeline();
@Test void deletingGeoToolsOutputRerunsGeoToolsAndGzip();
@Test void deletingOnlyGzipOutputRerunsOnlyGzip();
@Test void changingBreaksRerunsGeoToolsAndGzip();
@Test void changingGzipDestinationDoesNotRerunUpstream();
@Test void cleanRemovesAllOutputs();
@Test void rerunTasksExecutesEntirePipeline();
```

Keine Zeitstempel als primärer Nachweis.

---

## 20. Configuration Cache

Testklasse:

```text
CombinedConfigurationCacheFunctionalTest
```

Methoden:

```java
@Test void pluginApplicationStoresConfigurationCache();
@Test void pipelineReusesConfigurationCache();
@Test void groovyPipelineReusesConfigurationCache();
@Test void cacheDoesNotCaptureSourceCheckoutPaths();
@Test void cacheReusePreservesWorkerIsolation();
```

Erster Lauf mit `--configuration-cache` muss einen Eintrag speichern. Der zweite Lauf im selben Projekt und TestKit-Verzeichnis muss ihn wiederverwenden.

`--configuration-cache-problems=warn` ist keine zulässige Dauerlösung.

---

## 21. Multi-Projekt-Build

Struktur:

```text
root/
├── settings.gradle
├── build.gradle
├── core-only/build.gradle
├── geotools-only/build.gradle
├── mixed-a/build.gradle
└── mixed-b/build.gradle
```

- `core-only`: nur Core
- `geotools-only`: nur GeoTools
- `mixed-a`: Core vor GeoTools
- `mixed-b`: GeoTools vor Core

Testklasse:

```text
CombinedMultiProjectFunctionalTest
```

Methoden:

```java
@Test void supportsCoreOnlyGeoToolsOnlyAndMixedSubprojects();
@Test void mixedSubprojectsCanUseDifferentPluginOrder();
@Test void sharedServicesAreRegisteredOncePerBuild();
@Test void rootAggregateExecutesAllSubprojectTasks();
@Test void subprojectOutputsRemainIsolated();
@Test void parallelMultiProjectBuildCompletesWithoutDeadlock();
@Test void failureInOneSubprojectDoesNotCorruptNextBuild();
```

---

## 22. Parallelität

Testklasse:

```text
CombinedParallelExecutionFunctionalTest
```

Methoden:

```java
@Test void multiProjectBuildWorksWithParallelExecution();
@Test void multipleCoreAndGeoToolsTasksProduceCorrectOutputs();
@Test void workersDoNotCorruptExtractedClasspath();
@Test void serviceRegistrationHasNoRace();
@Test void workerExtractionHasNoRace();
@Test void outputsDoNotLeakBetweenProjects();
```

Aufruf:

```text
--parallel
--max-workers=4
--rerun-tasks
```

Timing ist kein alleiniger Parallelitätsbeweis.

---

## 23. Fehlerpropagation

Testklasse:

```text
CombinedFailurePropagationFunctionalTest
```

Methoden:

```java
@Test void coreFailurePreventsGeoToolsAndDownstreamCore();
@Test void geoToolsFailurePreventsDownstreamCore();
@Test void downstreamCoreFailurePreservesValidGeoToolsOutput();
@Test void fixingCoreInputAllowsSuccessfulRetry();
@Test void fixingGeoToolsConfigurationAllowsSuccessfulRetry();
@Test void failedWorkerDoesNotPoisonLaterCoreBuild();
@Test void failedCoreDoesNotPoisonLaterGeoToolsBuild();
@Test void failedGzipLeavesNoPartialOutput();
```

Core-Fehler: fehlende XML-Datei oder ungültiges XSLT.

GeoTools-Fehler:

```groovy
breaks 0d, 60d, 55d
```

Nach Korrektur muss derselbe Consumer im selben TestKit-Verzeichnis erfolgreich laufen.

---

## 24. Logging-Isolation

Testklasse:

```text
CombinedLoggingIsolationFunctionalTest
```

Methoden:

```java
@Test void coreLoggingWorksBeforeWorker();
@Test void coreLoggingWorksAfterWorker();
@Test void workerProtocolDoesNotAppearInNormalOutput();
@Test void noMultipleSlf4jProviderWarning();
@Test void noLogbackBindingConflict();
@Test void repeatedBuildDoesNotDuplicateCanaryMessages();
@Test void workerFailureIsReportedOnceWithTaskContext();
```

---

## 25. Published-Artifact-Verträge

Testklasse:

```text
CombinedPublishedArtifactContractTest
```

mit Tag `published-artifact-only`.

Methoden:

```java
@Test void repositoryContainsBothMarkersAtSameVersion();
@Test void coreMarkerPointsToCoreImplementation();
@Test void geoToolsMarkerPointsToGeoToolsImplementation();
@Test void corePomContainsNoGeoToolsModules();
@Test void geoToolsPomContainsNoCoreDependency();
@Test void bothJarsContainPluginDescriptors();
@Test void geoToolsJarContainsWorkerRuntime();
@Test void oneConsumerResolvesBothMarkers();
@Test void oneConsumerAppliesBothExplicitVersions();
@Test void missingCoreMarkerFailsClearly();
@Test void missingGeoToolsMarkerFailsClearly();
```

Markerkoordinaten sind anhand der tatsächlichen Publikation zu verifizieren.

Diese P0-Stufe verlangt keine allgemeine Cross-Version-Matrix. Aktuelle Core- und GeoTools-Version müssen gemeinsam funktionieren.

---

## 26. Negative Plugin-Auflösung

Testklasse:

```text
CombinedPluginResolutionFailureTest
```

Methoden:

```java
@Test void nonexistentCoreVersionFailsClearly();
@Test void nonexistentGeoToolsVersionFailsClearly();
@Test void missingCoreMarkerDoesNotFallBackToSource();
@Test void missingGeoToolsMarkerDoesNotFallBackToSource();
@Test void projectWithoutCoreCannotRegisterCoreTaskType();
@Test void projectWithoutGeoToolsCannotRegisterGeoToolsTaskType();
```

---

## 27. Provider- und Lazy-Configuration-Vertrag

Testklasse:

```text
CombinedProviderWiringFunctionalTest
```

Methoden:

```java
@Test void coreOutputProviderFeedsGeoToolsInput();
@Test void geoToolsOutputProviderFeedsCoreInput();
@Test void providerWiringInfersDependencies();
@Test void registeringPipelineDoesNotRealizeUnrelatedTasks();
@Test void helpDoesNotResolveWorkerClasspathEagerly();
@Test void coreOnlyTaskDoesNotStartWorker();
@Test void geoToolsOnlyTaskDoesNotExecuteCorePipeline();
```

Falls `RasterReclassify.inputRaster(Object)` Provider-Metadaten verliert, ist entweder direkt `getInputRaster().set(provider)` zu verwenden oder die Task-API sauber zu verbessern. GeoTools darf dafür keine Produktionsabhängigkeit auf Core erhalten.

---

## 28. Wiederholte Ausführung

Testklasse:

```text
CombinedRepeatedExecutionFunctionalTest
```

Methoden:

```java
@Test void repeatedBuildInSameTestKitDirectorySucceeds();
@Test void buildScriptChangeUsesNewConfiguration();
@Test void repeatedWorkerExtractionIsStable();
@Test void temporaryWorkerDirectoriesDoNotAccumulate();
@Test void noDaemonBuildSucceeds();
@Test void daemonAndNoDaemonProduceEquivalentOutputs();
```

Die langfristige Daemon-PID eines Runtime-Containers gehört nicht in diese Spezifikation.

---

## 29. Groovy-DSL-Vertrag

Testklasse:

```text
CombinedDslParityFunctionalTest
```

Methoden:

```java
```

---

## 30. Testmatrix

Alle Fälle sind P0-verbindlich:

| ID | Modus | DSL | Reihenfolge | Projekt | Prüfung |
|---|---|---|---|---|---|
| CG-001 | Source | Groovy | Core→Geo | Single | Anwendung |
| CG-002 | Source | Groovy | Geo→Core | Single | Anwendung |
| CG-005 | Published | Groovy | Core→Geo | Single | Anwendung |
| CG-006 | Published | Groovy | Geo→Core | Single | Anwendung |
| CG-009 | Source | Groovy | Core→Geo | Single | Pipeline |
| CG-011 | Published | Groovy | Core→Geo | Single | Pipeline |
| CG-013 | Source | Groovy | gemischt | Multi | Parallel |
| CG-014 | Published | Groovy | gemischt | Multi | Parallel |
| CG-015 | Source | Groovy | Core→Geo | Single | Config Cache |
| CG-016 | Published | Groovy | Core→Geo | Single | Config Cache |
| CG-019 | Source | Groovy | Core→Geo | Single | Core-Fehler |
| CG-020 | Source | Groovy | Core→Geo | Single | Geo-Fehler |
| CG-021 | Published | Groovy | Core→Geo | Single | Core-Fehler |
| CG-022 | Published | Groovy | Core→Geo | Single | Geo-Fehler |
| CG-023 | Source | Groovy | Core→Geo | Single | Worker-Isolation |
| CG-024 | Published | Groovy | Core→Geo | Single | Worker-Isolation |
| CG-025 | Source | Groovy | Core→Geo | Single | Up-to-date |
| CG-026 | Published | Groovy | Core→Geo | Single | Up-to-date |

---

## 31. Testtags

Definiere zentral:

```java
public final class GretlTestTags {
    public static final String COMBINED_PLUGIN = "combined-plugin";
    public static final String PUBLISHED_ARTIFACT_ONLY =
            "published-artifact-only";
    public static final String SOURCE_CLASSPATH_ONLY =
            "source-classpath-only";
    public static final String CONFIGURATION_CACHE =
            "configuration-cache";
    public static final String RUNTIME_IMAGE_ONLY =
            "runtime-image-only";
    public static final String OFFLINE_IMAGE_ONLY =
            "offline-image-only";
}
```

---

## 32. Runtime- und Offline-Image-Integration

Sobald entsprechende Executors existieren, müssen die Image-Suiten mindestens folgende kombinierte Tests wiederverwenden:

- Anwendung beider Plugins;
- Core→GeoTools→Core-Pipeline;
- Worker-Isolation;
- Groovy;
- semantische Outputs.

Erwartete Tasks:

```text
combinedRuntimeImageTest
combinedOfflineImageTest
```

Sind die Executors beim Umsetzungszeitpunkt vorhanden, müssen die Tasks aktiviert werden. Fehlen sie, bleiben alle Fachtests executor-neutral und entsprechend getaggt. Source- und Published-Gates bleiben in jedem Fall zwingend.

---

## 33. Keine direkte Produktionsausführung im Host-JUnit

Nicht zulässig als Ersatz für Consumer-Builds:

```java
new GretlPlugin().apply(...);
new RasterReclassify(...);
new GzipEngine().execute(...);
```

Produktcode wird über echte Gradle-Child-Builds ausgeführt. Direkte Unit-Tests sind nur für kleine Testutilities zulässig.

---

## 34. TestKit-Verzeichnisse

```text
gretl-combined-tests/build/test-kit/source
gretl-combined-tests/build/test-kit/published
gretl-combined-tests/build/test-kit/configuration-cache
gretl-combined-tests/build/test-kit/runtime-image
gretl-combined-tests/build/test-kit/offline-image
```

Konkurrierende Testtasks teilen kein beschreibbares TestKit-Verzeichnis.

---

## 35. Fehlerdiagnose

Bei fehlgeschlagenem Child-Build müssen verfügbar sein:

- Testklasse und Methode;
- Execution Mode;
- Consumer-Projektpfad;
- Gradle-Argumente;
- Plugin-Version;
- Published-Repository, falls relevant;
- stdout und stderr;
- Task-Outcomes;
- kombinierter Plugin-Classpath;
- TestKit-Verzeichnis.

Secrets sind zu maskieren.

### 35.1 `CombinedBuildResultAssertions`

```java
public final class CombinedBuildResultAssertions {
    public static void assertSuccess(BuildResult result);
    public static void assertFailureContains(
            BuildResult result,
            String... fragments);
    public static void assertOutcome(
            BuildResult result,
            String taskPath,
            TaskOutcome expected);
    public static void assertNotExecuted(
            BuildResult result,
            String taskPath);
    public static void assertNoClassloaderFailure(BuildResult result);
    public static void assertNoWorkerProtocolLeak(BuildResult result);
    public static void assertConfigurationCacheStored(BuildResult result);
    public static void assertConfigurationCacheReused(BuildResult result);
}
```

---

## 36. Unit-Tests der Testinfrastruktur

### 36.1 Classpath-Konfiguration

```java
@Test void readsValidClasspath();
@Test void rejectsMissingClasspathFile();
@Test void rejectsMissingEntry();
@Test void rejectsDuplicateCanonicalEntry();
@Test void rejectsTestSupportArtifact();
@Test void requiresCoreAndGeoToolsJars();
@Test void rejectsSourcesAndJavadocJars();
```

### 36.2 Testprojekt-Builder

```java
@Test void writesGroovyProject();
@Test void createsSubproject();
@Test void rejectsPathTraversal();
@Test void detectsForbiddenConsumerConstructs();
```

### 36.3 Assertions

`AsciiGridAssertions`, `GeoTiffAssertions` und `GzipAssertions` benötigen positive und negative Unit-Tests.

---

## 37. Publikations- und Image-Ausschlüsse

`gretl-combined-tests` darf nicht:

- in `publishSnapshots` erscheinen;
- in das Runtime-Maven-Repository publiziert werden;
- in das Docker-Image kopiert werden;
- transitive Dependency eines Produktionsmoduls sein;
- Test-Support in Produktions-POMs ziehen.

Erzeuge `assertCombinedTestsNotPublished`.

---

## 38. CI-Integration

Erweitere `.github/workflows/ci.yml`.

Mindestreihenfolge:

```text
checkout
JDK 17
Gradle setup
clean check
gretl-core integrationTest
publishedArtifactTest
coreGeoToolsCombinedTest
weitere Runtime-/Offline-Gates
Reports
Publish
```

Ein Fehler im kombinierten Gate blockiert Publikation.

Reports:

```text
gretl-combined-tests/build/reports/tests/
gretl-combined-tests/build/test-results/
gretl-combined-tests/build/test-kit/
```

Große TestKit-Verzeichnisse dürfen nur bei Fehlern hochgeladen werden.

---

## 39. Lokale Befehle

```bash
./gradlew :gretl-combined-tests:test
./gradlew :gretl-combined-tests:publishedArtifactTest
./gradlew :gretl-combined-tests:combinedConfigurationCacheTest
./gradlew coreGeoToolsCombinedTest
```

Zusätzlich:

```bash
./gradlew coreGeoToolsCombinedTest --rerun-tasks
./gradlew coreGeoToolsCombinedTest --parallel
```

Gezielt:

```bash
./gradlew :gretl-combined-tests:test \
  --tests '*CoreGeoToolsPipelineFunctionalTest'

./gradlew :gretl-combined-tests:publishedArtifactTest \
  --tests '*CoreGeoToolsPipelineFunctionalTest'
```

---

## 40. Dokumentation

Erzeuge:

```text
docs/development/core-geotools-combined-tests.adoc
```

Inhalt:

- warum Einzelmodultests nicht genügen;
- bewusste Modultrennung;
- Testmodul;
- expliziter Plugin-Classpath;
- Source-/Published-Modus;
- Core→GeoTools→Core-Pipeline;
- Provider-Wiring;
- Worker-Isolation;
- Configuration Cache;
- Multi-Projekt und Parallelität;
- lokale Befehle;
- CI-Gate;
- keine `gretljobs`-Migration.

---

## 41. Schrittweise Umsetzung

### Phase 1 – Analyse

1. Root-Build lesen.
2. Core-Build und Plugin lesen.
3. GeoTools-Build, Plugin und Worker lesen.
4. Task-Basisklassen lesen.
5. Test-Support lesen.
6. Published-Testpfad lesen.
7. bestehende Fixtures prüfen.

### Phase 2 – Testmodul

1. Subprojekt anlegen.
2. Classpath-Konfiguration definieren.
3. Host-/Child-Classpath trennen.
4. Classpath-Datei erzeugen.
5. Isolationschecks implementieren.
6. Publikationsausschluss implementieren.

### Phase 3 – Executor

1. System Properties ergänzen.
2. Configuration Record implementieren.
3. expliziten Executor implementieren.
4. Factory erweitern.
5. Unit-Tests schreiben.

### Phase 4 – Testunterbau

1. Basisklasse.
2. Projekt-Builder.
3. Fixtures.
4. Assertions.
5. Fehlerdiagnose.

### Phase 5 – Anwendung und Isolation

1. Reihenfolgen.
2. Idempotenz.
3. Unabhängigkeit.
4. Services.
5. Classloader.
6. Worker-Classpath.

### Phase 6 – Pipeline

1. XML und XSLT.
2. Groovy-Build.
4. Provider-Kanten.
5. Rasterassertions.
6. Gzipassertions.

### Phase 7 – Lifecycle

1. Up-to-date.
2. Änderungen.
3. Configuration Cache.
4. Multi-Projekt.
5. Parallelität.
6. Wiederholung.

### Phase 8 – Fehler

1. Core-Upstream-Fehler.
2. GeoTools-Mittelfehler.
3. Core-Downstream-Fehler.
4. Recovery.
5. Logging.

### Phase 9 – Published

1. Marker.
2. POM-Grenzen.
3. gemeinsame Auflösung.
4. negative Markerfälle.

### Phase 10 – CI und Doku

1. Root-Aggregator.
2. CI.
3. Reports.
4. Dokumentation.
5. vollständige Läufe.

---

## 42. Verbotene Abkürzungen

Nicht zulässig:

- nur beide Plugins anwenden und `tasks` aufrufen;
- getrennte Einzelmodultests als „gemeinsam“ deklarieren;
- Source- und Published-Testlogik duplizieren;
- direkte Engine-Ausführung im JUnit-Host;
- Produktionsabhängigkeit GeoTools→Core nur für Tests;
- GeoTools-Abhängigkeiten in Core;
- Worker-Libraries auf normalem Plugin-Classpath;
- ungeprüfter parameterloser `withPluginClasspath()` für die kombinierte Suite;
- Host-TestRuntime als Child-Build-Classpath;
- manuelle `dependsOn` in der kanonischen Pipeline;
- nur Groovy;
- nur Source;
- nur Published;
- keine Configuration-Cache-Prüfung;
- Timing als einziger Parallelitätsbeweis;
- deaktivierte Tests;
- pauschale Retries;
- Kopieren von `gretljobs`;
- Legacy-Plugin-Syntax;
- Veröffentlichung des Testmoduls;
- Rasteroutput nur auf Existenz prüfen.

---

## 43. Definition of Done

- [ ] Eigenes internes Testmodul oder gleichwertige klare Struktur existiert.
- [ ] Testmodul wird nicht publiziert.
- [ ] Testmodul gelangt nicht ins Runtime-Image.
- [ ] Expliziter gemeinsamer TestKit-Classpath existiert.
- [ ] Classpath enthält Core und GeoTools.
- [ ] Classpath enthält kein Test-Support.
- [ ] Host-GeoTools-Assertions lecken nicht in den Child-Build.
- [ ] Beide Pluginreihenfolgen funktionieren.
- [ ] Groovy DSL funktioniert.
- [ ] Beide Plugins bleiben unabhängig.
- [ ] Shared-Service-Namen kollidieren nicht.
- [ ] Tasktypen sind gemeinsam verwendbar.
- [ ] Raw GeoTools-Libraries sind nicht im Consumer-Classloader sichtbar.
- [ ] Worker-Classpath enthält erwartete Libraries.
- [ ] Worker-Classpath enthält kein Core-JAR.
- [ ] Worker-Classpath enthält keine Consumer-Outputs.
- [ ] Core→GeoTools→Core-Pipeline existiert.
- [ ] Pipeline nutzt Provider-Wiring.
- [ ] Keine manuellen `dependsOn` sind nötig.
- [ ] ASCII Grid ist fachlich korrekt.
- [ ] GeoTIFF ist fachlich korrekt.
- [ ] GZIP enthält bytegenau das GeoTIFF.
- [ ] Zweiter Lauf ist up-to-date.
- [ ] Inputänderung invalidiert die Pipeline korrekt.
- [ ] Outputlöschung invalidiert nur notwendige Tasks.
- [ ] Configuration Cache wird gespeichert und wiederverwendet.
- [ ] Multi-Projekt-Build funktioniert.
- [ ] `--parallel --max-workers=4` funktioniert.
- [ ] Keine Service- oder Worker-Races.
- [ ] Core- und GeoTools-Fehler propagieren korrekt.
- [ ] Korrigierter Build funktioniert anschließend.
- [ ] Logging bleibt konfliktfrei.
- [ ] Worker-Protokoll leckt nicht.
- [ ] Source-Modus ist grün.
- [ ] Published-Modus ist grün.
- [ ] Marker beider Plugins sind korrekt.
- [ ] Core-POM enthält keine GeoTools-Abhängigkeit.
- [ ] GeoTools-POM enthält keine Core-Abhängigkeit.
- [ ] Alle 26 Matrixfälle sind abgedeckt.
- [ ] Root-Task `coreGeoToolsCombinedTest` existiert.
- [ ] CI führt das Gate vor Publikation aus.
- [ ] Reports werden hochgeladen.
- [ ] Keine Tests sind deaktiviert.
- [ ] Keine `gretljobs`-Migration wurde vorgenommen.
- [ ] Dokumentation ist aktuell.
- [ ] Abschließende vollständige Läufe sind grün.

---

## 44. Auszuführende Befehle

Mindestens:

```bash
./gradlew clean check
./gradlew :gretl-core:integrationTest
./gradlew :gretl-geotools:test
./gradlew :gretl-combined-tests:test
./gradlew :gretl-combined-tests:publishedArtifactTest
./gradlew :gretl-combined-tests:combinedConfigurationCacheTest
./gradlew publishedArtifactTest
./gradlew coreGeoToolsCombinedTest
```

Zusätzlich:

```bash
./gradlew coreGeoToolsCombinedTest --rerun-tasks
./gradlew coreGeoToolsCombinedTest --parallel
```

Gezielte Läufe:

```bash
./gradlew :gretl-combined-tests:test \
  --tests '*CoreGeoToolsPipelineFunctionalTest'

./gradlew :gretl-combined-tests:publishedArtifactTest \
  --tests '*CoreGeoToolsPipelineFunctionalTest'

./gradlew :gretl-combined-tests:test \
  --tests '*CombinedClassloaderIsolationFunctionalTest'

./gradlew :gretl-combined-tests:test \
  --tests '*CombinedMultiProjectFunctionalTest'
```

---

## 45. Abschlussbericht des Coding Agents

Der Abschlussbericht enthält:

### 45.1 Architektur

- Testmoduleigentümerschaft;
- expliziter Plugin-Classpath;
- Executor-Erweiterung;
- Source-/Published-Modi;
- Host-/Child-Classpath-Trennung.

### 45.2 Geänderte Dateien

Für jede Datei:

- Pfad;
- Zweck;
- wichtigste Änderung.

### 45.3 Neue Klassen und Methoden

Insbesondere:

- `ExplicitPluginClasspathTestConfiguration`;
- `ExplicitPluginClasspathBuildExecutor`;
- Projekt-Builder;
- Assertions;
- kombinierte Testklassen;
- Build-Tasks.

### 45.4 Pipeline

- XSLT-Input;
- generiertes ASCII Grid;
- Reclassify;
- Gzip;
- Provider-Kanten;
- Task-Outcomes;
- semantische Rasterwerte.

### 45.5 Isolationsnachweis

- Core ohne GeoTools-Dependencies;
- GeoTools ohne Core-POM-Abhängigkeit;
- Worker-Classpath;
- Consumer-Classloader;
- keine Host-TestRuntime-Leaks.

### 45.6 Testresultate

Für jeden Befehl:

- Kommando;
- Ergebnis;
- Testanzahl;
- Laufzeit;
- gefundene Fehler;
- Behebungen.

### 45.7 Configuration Cache

- erster Lauf;
- gespeicherter Cache;
- zweiter Lauf;
- Reuse-Nachweis;
- behobene Probleme.

### 45.8 Parallelität

- Argumente;
- Projektstruktur;
- Outputs;
- Race-/Deadlock-Nachweis.

### 45.9 Abweichungen

Nur technisch notwendige Abweichungen mit Spezifikationspunkt, Begründung, gleichwertigem Nachweis und Risiko.

### 45.10 Verbleibende Risiken

Nur konkrete tatsächliche Risiken, keine allgemeinen Floskeln.

---

## 46. Vorrangregeln

Bei Konflikten gelten:

1. Produktionsmodultrennung erhalten;
2. echte gemeinsame Consumer-Ausführung;
3. Worker-Isolation;
4. Provider- und Gradle-Lifecycle-Korrektheit;
5. identische Fachtests in Source und Published;
6. moderne `plugins {}`-DSL;
7. keine `gretljobs`-Migration;
8. konkrete Klassen- und Methodennamen dieser Spezifikation.

Namen dürfen an gleichwertige vorhandene Abstraktionen angepasst werden. Beweisziele dürfen nicht abgeschwächt werden.

---

# Anhang A – Vorgeschlagene Testklassen

```text
gretl-combined-tests/src/test/java/ch/so/agi/gretl/combined/
├── CombinedPluginTestSupport.java
├── CombinedPluginApplicationFunctionalTest.java
├── CombinedPluginIndependenceFunctionalTest.java
├── CombinedBuildServiceFunctionalTest.java
├── CombinedClassloaderIsolationFunctionalTest.java
├── CoreGeoToolsPipelineFunctionalTest.java
├── CombinedIncrementalBuildFunctionalTest.java
├── CombinedConfigurationCacheFunctionalTest.java
├── CombinedMultiProjectFunctionalTest.java
├── CombinedParallelExecutionFunctionalTest.java
├── CombinedFailurePropagationFunctionalTest.java
├── CombinedLoggingIsolationFunctionalTest.java
├── CombinedPublishedArtifactContractTest.java
├── CombinedPluginResolutionFailureTest.java
├── CombinedProviderWiringFunctionalTest.java
├── CombinedRepeatedExecutionFunctionalTest.java
├── CombinedDslParityFunctionalTest.java
└── assertions/
    ├── AsciiGridAssertions.java
    ├── GeoTiffAssertions.java
    ├── GzipAssertions.java
    └── CombinedBuildResultAssertions.java
```

---

# Anhang B – Pipeline-Outcome-Matrix

| Änderung | XSLT Core | Reclassify GeoTools | Gzip Core |
|---|---:|---:|---:|
| erster Lauf | SUCCESS | SUCCESS | SUCCESS |
| unverändert | UP_TO_DATE | UP_TO_DATE | UP_TO_DATE |
| XML geändert | SUCCESS | SUCCESS | SUCCESS |
| XSL geändert | SUCCESS | SUCCESS | SUCCESS |
| Breaks geändert | UP_TO_DATE | SUCCESS | SUCCESS |
| GeoTIFF gelöscht | UP_TO_DATE | SUCCESS | SUCCESS |
| GZIP gelöscht | UP_TO_DATE | UP_TO_DATE | SUCCESS |
| `--rerun-tasks` | SUCCESS | SUCCESS | SUCCESS |
| Core upstream fehlerhaft | FAILED | nicht ausgeführt | nicht ausgeführt |
| GeoTools fehlerhaft | SUCCESS/UP_TO_DATE | FAILED | nicht ausgeführt |

---

# Anhang C – Classpath-Vertrag

## Child-Build-Plugin-Classpath darf enthalten

- Core-Plugin-JAR;
- GeoTools-Plugin-JAR;
- Core-Runtime-Abhängigkeiten;
- notwendige Plugin-Runtime-Abhängigkeiten.

## Darf nicht enthalten

- `gretl-test-support`;
- JUnit;
- Testcontainers;
- Host-Assertion-GeoTools-JARs;
- `gretl-combined-tests`;
- Source-/Javadoc-JARs;
- Testklassen;
- ungeprüfte Buildverzeichnis-Fallbacks.

## GeoTools-Worker-Classpath darf enthalten

- Worker-Runtime;
- GeoTools-Module;
- GeoTools-transitive Worker-Abhängigkeiten;
- EPSG-Datenbank.

## GeoTools-Worker-Classpath darf nicht enthalten

- Core-Plugin-JAR;
- Consumer-Buildoutputs;
- JUnit;
- TestKit;
- Test-Support.

---

# Anhang D – Review-Checkliste

## Modulgrenzen

- [ ] Core hat keine GeoTools-Abhängigkeit.
- [ ] GeoTools hat keine Core-Produktabhängigkeit.
- [ ] Consumer wendet beide explizit an.
- [ ] Services sind getrennt.
- [ ] Worker ist isoliert.

## Testunterbau

- [ ] eigenes Testmodul;
- [ ] expliziter Classpath;
- [ ] Source und Published;
- [ ] keine Publikation;
- [ ] keine Image-Aufnahme;
- [ ] keine Host-Classpath-Leaks.

## Pipeline

- [ ] Core→GeoTools→Core;
- [ ] Provider-Wiring;
- [ ] keine manuellen `dependsOn`;
- [ ] Groovy;
- [ ] semantische Outputs;
- [ ] inkrementell;
- [ ] Configuration Cache.

## Robustheit

- [ ] Multi-Projekt;
- [ ] parallel;
- [ ] wiederholt;
- [ ] Core-Fehler;
- [ ] GeoTools-Fehler;
- [ ] Recovery;
- [ ] Logging;
- [ ] kein Worker-Protokoll-Leak.

## CI

- [ ] Root-Gate;
- [ ] vor Publish;
- [ ] Reports;
- [ ] keine deaktivierten Tests;
- [ ] lokale Befehle dokumentiert.

---

# Anhang E – Nichtziele

Diese Spezifikation verlangt nicht:

- Migration bestehender `gretljobs`;
- Legacy-Plugin-Syntax;
- allgemeine Cross-Version-Kompatibilitätsmatrix;
- Verschmelzung von Core und GeoTools;
- direkte GeoTools-Abhängigkeit in Core;
- vollständige externe Service-E2E-Matrix;
- vollständige Runtime-/Offline-Image-Infrastruktur;
- ein neues öffentliches Kombinationsplugin;
- einen neuen öffentlichen Tasktyp nur für Tests;
- Performance-Benchmarks mit festen Zeitgrenzen.

Sie verlangt eine belastbare, produktnahe und wiederverwendbare gemeinsame Testsuite.
