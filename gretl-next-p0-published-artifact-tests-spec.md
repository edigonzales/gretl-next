# Spezifikation: P0 – Tests der publizierten GRETL-Plugin-Artefakte

**Ziel-Repository:** `https://github.com/edigonzales/gretl-next`
**Betroffene Produktmodule:** `gretl-core`, `gretl-geotools`
**Status:** Umsetzungsauftrag für einen LLM Coding Agent
**Priorität:** P0 / Release-Gate
**Stand der Spezifikation:** 30. Juli 2026
**Primäres Ziel:** Nachweisen, dass die von Gradle tatsächlich publizierten Plugin-Marker, Implementierungsartefakte, POMs, Gradle-Module-Metadaten und eingebetteten Laufzeitbestandteile reale GRETL-Tasks korrekt ausführen.

---

## 1. Auftrag an den Coding Agent

Implementiere im Repository `edigonzales/gretl-next` eine neue Testebene für **publizierte Gradle-Plugin-Artefakte**.

Die bereits vorhandenen Funktionstests verwenden überwiegend Gradle TestKit mit:

```java
.withPluginClasspath()
```

Dadurch wird das Plugin direkt aus dem aktuellen Source-Build in das temporäre Testprojekt injiziert. Diese Tests sind wertvoll und müssen unverändert als schnelle Source-/Development-Tests erhalten bleiben. Sie beweisen jedoch nicht, dass Folgendes korrekt ist:

- der publizierte Gradle-Plugin-Marker;
- die Koordinaten des Plugin-Markers;
- die Referenz vom Marker auf das Implementierungsmodul;
- das publizierte POM;
- die publizierte Gradle Module Metadata;
- transitive Runtime-Abhängigkeiten;
- `runtimeOnly`-Abhängigkeiten wie JDBC-Treiber;
- der Inhalt des publizierten Plugin-JARs;
- der im `gretl-geotools`-JAR eingebettete Worker-Classpath;
- die normale Auflösung über `plugins {}` und ein Maven-Repository.

Die neue Testebene muss genau diese Lücke schliessen.

Der Agent soll **keine separate, vereinfachte Dummy-Implementierung** bauen. Die produktiven Publikationen der beiden Module sind zu verwenden. Die fachlichen Tests sollen möglichst dieselben Testklassen und Fixtures erneut ausführen, aber mit einem anderen Ausführungs-Backend.

---

## 2. Verbindliche Zielarchitektur

Nach der Umsetzung müssen mindestens folgende Gradle-Tasks existieren:

```text
preparePublishedTestRepository
verifyPublishedTestRepository
publishedArtifactTest

:gretl-core:publishedFunctionalTest
:gretl-core:publishedIntegrationTest
:gretl-geotools:publishedFunctionalTest
```

Der gewünschte Task-Graph ist:

```text
publishedArtifactTest
├── verifyPublishedTestRepository
│   └── preparePublishedTestRepository
│       ├── :gretl-core:publishAllPublicationsToPublishedTestRepository
│       └── :gretl-geotools:publishAllPublicationsToPublishedTestRepository
├── :gretl-core:publishedFunctionalTest
│   └── preparePublishedTestRepository
├── :gretl-core:publishedIntegrationTest
│   └── preparePublishedTestRepository
└── :gretl-geotools:publishedFunctionalTest
    └── preparePublishedTestRepository
```

Die bestehenden Tasks müssen erhalten bleiben:

```text
:gretl-core:test
:gretl-core:integrationTest
:gretl-geotools:test
check
```

`check` soll weiterhin der schnelle, Docker-freie lokale Check bleiben. `publishedArtifactTest` wird ein separates, explizites Release-/CI-Gate.

---

## 3. Harte Invarianten

Diese Regeln sind nicht optional.

### 3.1 Kein `withPluginClasspath()` im Published-Modus

Im Ausführungsmodus `PUBLISHED_ARTIFACT` darf der verwendete `GradleRunner` unter keinen Umständen `withPluginClasspath()` aufrufen.

Es darf auch keine indirekte Alternative verwendet werden, insbesondere nicht:

- `--include-build`;
- ein Composite Build mit dem Root-Projekt;
- `mavenLocal()`;
- ein `flatDir` auf `build/classes`, `build/libs` oder Source-Ausgaben;
- ein manuell konstruierter Classpath mit `gretl-core/build/classes`;
- `pluginUnderTestMetadata`;
- eine direkte Abhängigkeit des temporären Testprojekts auf `project(':gretl-core')` oder `project(':gretl-geotools')`.

Die einzige erlaubte Quelle der GRETL-Plugins ist das für diesen Testlauf erzeugte Maven-Repository.

### 3.2 Reale Plugin-Marker verwenden

Die temporären Testprojekte müssen die Plugins mit der normalen Plugin-DSL anwenden:

```groovy
plugins {
    id 'ch.so.agi.gretl'
}
```

beziehungsweise:

```groovy
plugins {
    id 'ch.so.agi.gretl.geotools'
}
```

Die Version wird in `pluginManagement.plugins` der generierten `settings.gradle` vorgegeben. Die Buildskripte selbst sollen weiterhin versionslos bleiben. Dadurch wird derselbe Nutzungsstil getestet, der später auch im Runtime-Image vorgesehen ist.

### 3.3 Keine Auflösung über `mavenLocal()`

Die für Published-Tests erzeugte `settings.gradle` darf **kein** `mavenLocal()` enthalten.

Damit wird verhindert, dass ein zufällig lokal publiziertes GRETL-Artefakt einen unvollständigen Test-Repository-Inhalt verdeckt.

### 3.4 Test-Repository vor allen Tests vollständig erzeugen

Das Repository muss beide Plugins vollständig enthalten:

- `ch.so.agi.gretl`;
- `ch.so.agi.gretl.geotools`.

Auch ein reiner Core-Test soll auf ein vollständig vorbereitetes Repository zugreifen. Das vereinfacht die Testinfrastruktur und verhindert unterschiedliche Repository-Zustände.

### 3.5 Fachliche Tests wiederverwenden

Die existierenden `*FunctionalTest`-Klassen sollen nicht in einen zweiten Verzeichnisbaum kopiert werden.

Stattdessen werden dieselben kompilierten JUnit-Testklassen durch einen zweiten Gradle-`Test`-Task erneut ausgeführt. Der Unterschied entsteht über eine Systemproperty und das gewählte Runner-Backend.

### 3.6 Published-Tests testen Artefakte, nicht das Docker-Image

Folgende Themen sind ausdrücklich **nicht** Teil dieses Auftrags:

- `buildRuntimeImage` ausführen;
- Docker-Image starten;
- Offline-Fähigkeit des Runtime-Images;
- Multi-Arch-Images;
- Image-Registry-Publikation;
- `docker/init.gradle` korrigieren;
- Image-SBOM.

Die neue Infrastruktur muss aber so gestaltet sein, dass später ein drittes Backend `RUNTIME_IMAGE` ergänzt werden kann.

---

## 4. Aktueller Repository-Kontext, den der Agent vor Änderungen prüfen muss

Vor der Implementierung muss der Agent diese Dateien erneut lesen:

```text
build.gradle
settings.gradle
.github/workflows/ci.yml

gretl-core/build.gradle
gretl-geotools/build.gradle

gretl-core/src/test/java/ch/so/agi/gretl/CoreFunctionalTestSupport.java
gretl-core/src/integrationTest/java/ch/so/agi/gretl/PostgisIntegrationTestSupport.java
gretl-core/src/integrationTest/java/ch/so/agi/gretl/S3FlociIntegrationTest.java
gretl-core/src/integrationTest/java/ch/so/agi/gretl/FtpDockerIntegrationTest.java

gretl-geotools/src/test/java/ch/so/agi/gretl/geotools/GeoToolsPluginFunctionalTest.java
```

Anschliessend muss der Agent alle direkten TestKit-Erzeugungen suchen:

```bash
git grep -n "GradleRunner.create" -- \
  gretl-core/src/test \
  gretl-core/src/integrationTest \
  gretl-geotools/src/test
```

Jede Fundstelle ist zu klassifizieren:

1. soll künftig über das gemeinsame Executor-Backend laufen;
2. ist absichtlich ein spezieller Isolationstest;
3. ist keine GRETL-Plugin-Ausführung und darf lokal bleiben.

Im Abschlussbericht muss der Agent alle verbleibenden direkten `GradleRunner.create()`-Fundstellen begründen.

---

## 5. Neues internes Test-Support-Modul

### 5.1 Modul anlegen

Füge ein neues, nicht publiziertes Subprojekt hinzu:

```text
gretl-test-support
```

Ergänze in `settings.gradle`:

```groovy
include 'gretl-test-support'
```

Das Modul ist reine Testinfrastruktur. Es gehört nicht zum GRETL-Produktoberfläche und darf nicht nach Maven publiziert oder ins Runtime-Image kopiert werden.

### 5.2 `gretl-test-support/build.gradle`

Erstelle:

```groovy
plugins {
    id 'java-library'
}

dependencies {
    api gradleTestKit()

    testImplementation "org.junit.jupiter:junit-jupiter-api:${junitVersion}"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:${junitVersion}"
}
```

Es darf insbesondere **kein** `maven-publish` angewendet werden.

### 5.3 Abhängigkeiten der Produktmodule

Ergänze in `gretl-core/build.gradle`:

```groovy
testImplementation project(':gretl-test-support')
```

Da `integrationTestImplementation` bereits von `testImplementation` erbt, soll die Support-Bibliothek auch im Integrationstest-Classpath verfügbar sein. Dies ist nach der Änderung explizit zu verifizieren.

Ergänze in `gretl-geotools/build.gradle`:

```groovy
testImplementation project(':gretl-test-support')
```

Die Support-Bibliothek darf niemals als `implementation`-Abhängigkeit eines Produktmoduls erscheinen.

---

## 6. Java-API des Test-Support-Moduls

Verwende das Package:

```text
ch.so.agi.gretl.testkit
```

### 6.1 `GretlTestExecutionMode`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlTestExecutionMode.java
```

Implementiere:

```java
public enum GretlTestExecutionMode {
    PLUGIN_CLASSPATH,
    PUBLISHED_ARTIFACT;

    public static GretlTestExecutionMode current();
}
```

Systemproperty:

```text
gretl.test.executionMode
```

Verhalten von `current()`:

- fehlt die Property oder ist sie leer: `PLUGIN_CLASSPATH`;
- Wertvergleich case-insensitive;
- Bindestriche dürfen zu Unterstrichen normalisiert werden;
- unbekannter Wert: `IllegalArgumentException` mit erlaubten Werten im Text;
- kein stilles Zurückfallen auf `PLUGIN_CLASSPATH`, wenn ein ungültiger Wert gesetzt ist.

Beispiele gültiger Werte:

```text
PLUGIN_CLASSPATH
plugin-classpath
PUBLISHED_ARTIFACT
published-artifact
```

### 6.2 `GretlTestSystemProperties`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlTestSystemProperties.java
```

Implementiere eine nicht instanziierbare Konstantenklasse:

```java
public final class GretlTestSystemProperties {
    public static final String EXECUTION_MODE = "gretl.test.executionMode";
    public static final String PUBLISHED_REPOSITORY = "gretl.test.publishedRepository";
    public static final String PLUGIN_VERSION = "gretl.test.pluginVersion";
    public static final String TEST_KIT_DIRECTORY = "gretl.test.testKitDirectory";

    private GretlTestSystemProperties() {
    }
}
```

Keine String-Literale dieser Property-Namen sollen in mehreren Klassen dupliziert werden.

### 6.3 `PublishedArtifactTestConfiguration`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/PublishedArtifactTestConfiguration.java
```

Implementiere als immutable `record`:

```java
public record PublishedArtifactTestConfiguration(
        Path repository,
        String pluginVersion,
        Path testKitDirectory) {

    public static PublishedArtifactTestConfiguration fromSystemProperties();
    public URI repositoryUri();
}
```

Validierungen in `fromSystemProperties()`:

- alle drei Properties müssen im Published-Modus vorhanden sein;
- Repository-Pfad muss absolut sein oder mit `toAbsolutePath().normalize()` normalisiert werden;
- Repository muss existieren;
- Repository muss ein Verzeichnis sein;
- Version darf nicht leer sein;
- TestKit-Verzeichnis muss angelegt werden können;
- Fehlermeldungen müssen den fehlenden Property-Namen nennen;
- Secrets oder Credentials sind hier nicht erlaubt.

`repositoryUri()` muss eine `file:`-URI liefern, die sicher in ein Gradle-Groovy-Skript geschrieben werden kann.

### 6.4 `GretlBuildExecutor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlBuildExecutor.java
```

API:

```java
public interface GretlBuildExecutor {
    BuildResult run(Path projectDirectory, String... arguments);
    BuildResult runAndFail(Path projectDirectory, String... arguments);
}
```

### 6.5 `AbstractGradleBuildExecutor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/AbstractGradleBuildExecutor.java
```

Aufgaben:

- gemeinsamen `GradleRunner` erzeugen;
- Projektverzeichnis setzen;
- Argumente normalisieren;
- genau einmal `--stacktrace` ergänzen;
- `forwardOutput()` aktivieren;
- optional TestKit-Verzeichnis setzen;
- Erfolg mit `build()`;
- erwarteten Fehler mit `buildAndFail()`.

Vorgeschlagene Methoden:

```java
abstract class AbstractGradleBuildExecutor implements GretlBuildExecutor {
    @Override
    public final BuildResult run(Path projectDirectory, String... arguments);

    @Override
    public final BuildResult runAndFail(Path projectDirectory, String... arguments);

    protected abstract GradleRunner customize(GradleRunner runner);

    protected GradleRunner baseRunner(Path projectDirectory, String... arguments);

    static List<String> normalizeArguments(String... arguments);
}
```

Anforderungen:

- `projectDirectory` muss auf Existenz und Verzeichnis geprüft werden;
- `null`-Argumente sind mit einer klaren Fehlermeldung zurückzuweisen;
- vorhandenes `--stacktrace` oder `-s` darf nicht dupliziert werden;
- die Reihenfolge der vom Test übergebenen Argumente bleibt erhalten.

### 6.6 `PluginClasspathBuildExecutor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/PluginClasspathBuildExecutor.java
```

Implementiere:

```java
public final class PluginClasspathBuildExecutor extends AbstractGradleBuildExecutor {
    @Override
    protected GradleRunner customize(GradleRunner runner) {
        return runner.withPluginClasspath();
    }
}
```

**Nur diese Klasse** darf in der allgemeinen Testinfrastruktur `withPluginClasspath()` aufrufen.

### 6.7 `PublishedArtifactBuildExecutor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/PublishedArtifactBuildExecutor.java
```

Implementiere:

```java
public final class PublishedArtifactBuildExecutor extends AbstractGradleBuildExecutor {
    private final PublishedArtifactTestConfiguration configuration;

    public PublishedArtifactBuildExecutor(PublishedArtifactTestConfiguration configuration);

    @Override
    protected GradleRunner customize(GradleRunner runner);
}
```

`customize()` darf ausschliesslich das isolierte TestKit-Verzeichnis konfigurieren:

```java
return runner.withTestKitDir(configuration.testKitDirectory().toFile());
```

Verboten in dieser Klasse:

- `withPluginClasspath()`;
- `withPluginClasspath(Collection<File>)`;
- Erzeugen eines Init-Skripts, das Source-JARs oder Klassenverzeichnisse injiziert;
- `--include-build`;
- globale Maven-Local-Konfiguration.

### 6.8 `GretlBuildExecutors`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlBuildExecutors.java
```

API:

```java
public final class GretlBuildExecutors {
    public static GretlBuildExecutor current();

    private GretlBuildExecutors() {
    }
}
```

Verhalten:

```text
PLUGIN_CLASSPATH  -> PluginClasspathBuildExecutor
PUBLISHED_ARTIFACT -> PublishedArtifactBuildExecutor(fromSystemProperties())
```

Es ist erlaubt, pro Aufruf eine neue Executor-Instanz zu erzeugen. Vermeide globalen mutable State.

### 6.9 `GretlTestProjectSettings`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlTestProjectSettings.java
```

API:

```java
public final class GretlTestProjectSettings {
    public static void write(Path projectDirectory, String rootProjectName) throws IOException;
    public static String render(String rootProjectName);

    private GretlTestProjectSettings() {
    }
}
```

Verhalten:

- im Modus `PLUGIN_CLASSPATH`: bisheriges minimales Skript erzeugen;
- im Modus `PUBLISHED_ARTIFACT`: vollständiges `pluginManagement` und `dependencyResolutionManagement` erzeugen.

Minimales Skript:

```groovy
rootProject.name = 'core-test'
```

Published-Skript, sinngemäss:

```groovy
pluginManagement {
    repositories {
        maven { url = uri('file:///.../build/published-test/maven-repo') }
        maven { url = uri('https://jars.sogeo.services/mirror') }
        maven { url = uri('https://repo.osgeo.org/repository/release/') }
        maven { url = uri('https://maven.geo-solutions.it') }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id 'ch.so.agi.gretl' version '5.0.0-SNAPSHOT'
        id 'ch.so.agi.gretl.geotools' version '5.0.0-SNAPSHOT'
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri('file:///.../build/published-test/maven-repo') }
        maven { url = uri('https://jars.sogeo.services/mirror') }
        maven { url = uri('https://repo.osgeo.org/repository/release/') }
        maven { url = uri('https://maven.geo-solutions.it') }
        mavenCentral()
    }
}

rootProject.name = 'core-test'
```

Wichtige Details:

- Published-Repository immer an erster Stelle;
- kein `mavenLocal()`;
- keine Snapshot-Repository-URL von `jars.interlis.guru`;
- keine Credentials;
- Core- und GeoTools-Pluginversion aus derselben Systemproperty;
- Repository-URI und Projektname korrekt für Groovy escapen;
- Zeilenende `\n` am Dateiende;
- UTF-8.

Die vollständigen externen Repositories müssen auch unter `pluginManagement.repositories` stehen, weil die Implementierungsabhängigkeiten des Plugins im Rahmen der Plugin-Auflösung benötigt werden können.

---

## 7. Unit-Tests des Support-Moduls

Erstelle mindestens:

```text
gretl-test-support/src/test/java/ch/so/agi/gretl/testkit/GretlTestExecutionModeTest.java
gretl-test-support/src/test/java/ch/so/agi/gretl/testkit/GretlTestProjectSettingsTest.java
gretl-test-support/src/test/java/ch/so/agi/gretl/testkit/PublishedArtifactTestConfigurationTest.java
```

### 7.1 `GretlTestExecutionModeTest`

Testfälle:

- fehlende Property ergibt `PLUGIN_CLASSPATH`;
- `published-artifact` ergibt `PUBLISHED_ARTIFACT`;
- Gross-/Kleinschreibung wird akzeptiert;
- ungültiger Wert wirft `IllegalArgumentException`;
- Fehlermeldung nennt Property und gültige Werte.

Systemproperties nach jedem Test zwingend wiederherstellen. Nutze `try/finally` oder eine kleine Property-Restore-Hilfe.

### 7.2 `GretlTestProjectSettingsTest`

Teste den gerenderten Published-Text:

- enthält den lokalen Repository-URI;
- lokales Repository steht vor externen Repositories;
- enthält beide Plugin-IDs;
- enthält exakt die konfigurierte Version;
- enthält `FAIL_ON_PROJECT_REPOS`;
- enthält **nicht** `mavenLocal()`;
- enthält **nicht** `withPluginClasspath`;
- enthält den korrekt escapten Projektnamen;
- endet mit Newline.

### 7.3 `PublishedArtifactTestConfigurationTest`

Testfälle:

- fehlendes Repository;
- fehlende Version;
- fehlendes TestKit-Verzeichnis;
- Repository ist Datei statt Verzeichnis;
- gültige Konfiguration wird normalisiert;
- TestKit-Verzeichnis wird erstellt.

---

## 8. Neues Maven-Repository für Published-Tests

### 8.1 Root-Build erweitern

In `build.gradle` ergänzen:

```groovy
def publishedTestRepoDir = layout.buildDirectory.dir('published-test/maven-repo')
```

Unter dem bestehenden `plugins.withId('maven-publish')`-Block eine weitere Repository-Definition ergänzen:

```groovy
maven {
    name = 'publishedTest'
    url = publishedTestRepoDir.get().asFile.toURI()
}
```

Der Name muss exakt `publishedTest` sein, damit Gradle Tasks mit dem Suffix erzeugt:

```text
ToPublishedTestRepository
```

### 8.2 Repository-Clean-Task

Erstelle im Root-Projekt:

```groovy
tasks.register('cleanPublishedTestRepository', Delete) {
    delete publishedTestRepoDir
}
```

Alle Publish-Tasks zum Repository `publishedTest` müssen davon abhängen:

```groovy
subprojects {
    plugins.withId('maven-publish') {
        tasks.matching { it.name.endsWith('ToPublishedTestRepository') }.configureEach {
            dependsOn rootProject.tasks.named('cleanPublishedTestRepository')
        }
    }
}
```

Wichtig:

- kein `finalizedBy`;
- kein Clean nach dem Publish;
- die gemeinsame Clean-Task wird in einem Build nur einmal ausgeführt;
- Verhalten mit `--parallel` prüfen;
- bestehendes `runtimeImage`-Repository nicht wiederverwenden.

### 8.3 Vorbereitungstask

Erstelle:

```groovy
tasks.register('preparePublishedTestRepository') {
    group = 'verification'
    description = 'Publishes GRETL Core and GeoTools plugin artifacts to an isolated local Maven repository for black-box tests.'

    dependsOn ':gretl-core:publishAllPublicationsToPublishedTestRepository'
    dependsOn ':gretl-geotools:publishAllPublicationsToPublishedTestRepository'
}
```

Diese Task darf keine Tests ausführen.

---

## 9. Repository-Vertrag verifizieren

### 9.1 Root-Task `verifyPublishedTestRepository`

Implementiere im Root-Build eine Task:

```groovy
tasks.register('verifyPublishedTestRepository') {
    group = 'verification'
    description = 'Verifies GRETL plugin markers, implementation POMs and packaged worker runtime in the published-test repository.'
    dependsOn tasks.named('preparePublishedTestRepository')
    inputs.dir publishedTestRepoDir
    inputs.property 'gretlVersion', provider { project.version.toString() }
}
```

Die Task muss in `doLast` mit klaren, dateibezogenen Fehlermeldungen prüfen.

### 9.2 Erwartete Implementierungsartefakte

Für `${version}` müssen mindestens existieren:

```text
ch/so/agi/gretl-core/${version}/gretl-core-${version}.jar
ch/so/agi/gretl-core/${version}/gretl-core-${version}.pom
ch/so/agi/gretl-core/${version}/gretl-core-${version}.module

ch/so/agi/gretl-geotools/${version}/gretl-geotools-${version}.jar
ch/so/agi/gretl-geotools/${version}/gretl-geotools-${version}.pom
ch/so/agi/gretl-geotools/${version}/gretl-geotools-${version}.module
```

Wenn Gradle bei SNAPSHOT-Publikationen zusätzliche Metadata-Dateien erzeugt, dürfen diese vorhanden sein; die oben genannten Hauptdateien bleiben Pflicht.

### 9.3 Erwartete Plugin-Marker

Core:

```text
ch/so/agi/gretl/ch.so.agi.gretl.gradle.plugin/${version}/
  ch.so.agi.gretl.gradle.plugin-${version}.pom
```

GeoTools:

```text
ch/so/agi/gretl/geotools/ch.so.agi.gretl.geotools.gradle.plugin/${version}/
  ch.so.agi.gretl.geotools.gradle.plugin-${version}.pom
```

### 9.4 Marker-POM semantisch prüfen

Parse die Marker-POMs mit `XmlSlurper` oder einer vergleichbar robusten XML-API. Keine String-Contains-Prüfung für Koordinaten.

Core-Marker muss genau eine relevante Abhängigkeit enthalten:

```text
groupId:    ch.so.agi
artifactId: gretl-core
version:    ${version}
```

GeoTools-Marker:

```text
groupId:    ch.so.agi
artifactId: gretl-geotools
version:    ${version}
```

Fehlermeldung muss Ist- und Soll-Koordinaten enthalten.

### 9.5 Core-POM prüfen

Das Core-POM muss mindestens diese Abhängigkeiten deklarieren:

```text
commons-io:commons-io
org.xerial:sqlite-jdbc
org.postgresql:postgresql
org.duckdb:duckdb_jdbc
```

Prüfe Gruppe und Artifact-ID semantisch. Die Prüfung soll nicht an einer einzigen Reihenfolge hängen.

Für die JDBC-Treiber ist zusätzlich zu prüfen, dass sie im Maven-POM mit einem Runtime-kompatiblen Scope publiziert sind. Akzeptiert:

- `runtime`;
- fehlender Scope nur dann, wenn Gradles Publikationsmodell nachweislich `compile` erzeugt und der Treiber dadurch zur Laufzeit verfügbar ist.

Bevorzuge `runtime`. Wenn der Ist-Zustand anders ist, dokumentiere ihn im Abschlussbericht, ändere aber keine fachlichen Dependency-Semantiken ohne Notwendigkeit.

### 9.6 Plugin-Descriptor im JAR

Core-JAR muss enthalten:

```text
META-INF/gradle-plugins/ch.so.agi.gretl.properties
```

Der Descriptor muss als Property enthalten:

```text
implementation-class=ch.so.agi.gretl.gradle.GretlPlugin
```

GeoTools-JAR muss enthalten:

```text
META-INF/gradle-plugins/ch.so.agi.gretl.geotools.properties
```

mit:

```text
implementation-class=ch.so.agi.gretl.geotools.GretlGeotoolsPlugin
```

### 9.7 Eingebetteten GeoTools-Worker prüfen

Das publizierte `gretl-geotools`-JAR muss mindestens enthalten:

```text
gretl-geotools-worker-classpath/
gretl-geotools-worker-classpath/gretl-geotools-*-worker-runtime.jar
gretl-geotools-worker-classpath/lib/gt-main-*.jar
gretl-geotools-worker-classpath/lib/gt-geotiff-*.jar
gretl-geotools-worker-classpath/lib/gt-coverage-*.jar
gretl-geotools-worker-classpath/lib/gt-shapefile-*.jar
gretl-geotools-worker-classpath/lib/gt-epsg-hsql-*.jar
```

Die Prüfung soll Präfix/Suffix-Matching verwenden, damit sie nicht an einer hart codierten GeoTools-Version hängt.

Zusätzlich soll geprüft werden:

- kein Eintrag endet auf `-sources.jar`;
- kein Eintrag endet auf `-javadoc.jar`;
- der Worker-Runtime-JAR ist nicht leer;
- jeder verlangte Präfix kommt genau mindestens einmal vor;
- die Fehlermeldung listet bei Fehlern die vorhandenen Einträge unter `gretl-geotools-worker-classpath/` gekürzt auf.

### 9.8 Keine Checksummen als Pflicht

`.sha1`, `.md5`, `.sha256` oder `.sha512` sind für diesen Auftrag keine Pflicht. Falls Gradle sie erzeugt, dürfen sie ignoriert werden.

---

## 10. Bestehende Testinfrastruktur auf Executor umstellen

### 10.1 `CoreFunctionalTestSupport`

Datei:

```text
gretl-core/src/test/java/ch/so/agi/gretl/CoreFunctionalTestSupport.java
```

Ändere:

```java
BuildResult run(String... arguments)
BuildResult runAndFail(String... arguments)
void writeSettings()
```

auf Delegation:

```java
BuildResult run(String... arguments) {
    return GretlBuildExecutors.current().run(projectDir, arguments);
}

BuildResult runAndFail(String... arguments) {
    return GretlBuildExecutors.current().runAndFail(projectDir, arguments);
}

void writeSettings() throws IOException {
    GretlTestProjectSettings.write(projectDir, "core-test");
}
```

Entferne die direkte `GradleRunner`-Konstruktion aus dieser Klasse.

Alle Fixture-, SQLite- und Datei-Hilfsmethoden bleiben erhalten.

### 10.2 `PostgisIntegrationTestSupport`

Datei:

```text
gretl-core/src/integrationTest/java/ch/so/agi/gretl/PostgisIntegrationTestSupport.java
```

Auch hier müssen `run`, `runAndFail` und `writeSettings` delegieren.

Die Methode, die Standardargumente mit `-PpgUrl`, `-PpgUser` und `-PpgPass` ergänzt, bleibt fachlich erhalten. Empfohlene Reihenfolge:

1. Testargumente plus PostgreSQL-Properties erzeugen;
2. an `GretlBuildExecutors.current()` übergeben;
3. Executor ergänzt `--stacktrace`.

Passwörter dürfen nicht zusätzlich geloggt werden. Bestehendes Verhalten nicht verschlechtern.

### 10.3 `S3FlociIntegrationTest`

Entferne die lokalen Kopien von:

```java
private BuildResult run(...)
private BuildResult runAndFail(...)
private void writeSettings()
private String[] appendStacktrace(...)
```

Nutze stattdessen den gemeinsamen Executor und Settings-Writer.

`@TempDir Path projectDir` bleibt in dieser Klasse.

### 10.4 `FtpDockerIntegrationTest`

Analog zu S3:

- direkte Runner-Erzeugung entfernen;
- gemeinsames Backend verwenden;
- Settings über `GretlTestProjectSettings` schreiben;
- Docker-/FTP-Logik nicht fachlich verändern.

### 10.5 Alle weiteren Core-Fundstellen

Jede weitere Fundstelle von `GradleRunner.create()` in Core-Tests ist auf dieselbe Infrastruktur umzustellen, sofern sie einen GRETL-Task startet.

### 10.6 `GeoToolsPluginFunctionalTest`

Datei:

```text
gretl-geotools/src/test/java/ch/so/agi/gretl/geotools/GeoToolsPluginFunctionalTest.java
```

Ersetze die lokalen Methoden:

```java
run
runAndFail
writeSettings
appendStacktrace
```

mit dem gemeinsamen Test-Support.

Projektname:

```text
geotools-test
```

Fixture-Copy-Hilfen können in der Klasse bleiben.

---

## 11. Core: neue Published-Test-Tasks

### 11.1 `publishedFunctionalTest`

Ergänze in `gretl-core/build.gradle`:

```groovy
tasks.register('publishedFunctionalTest', Test) {
    group = 'verification'
    description = 'Runs GRETL Core functional tests against plugins resolved from the published-test Maven repository.'

    dependsOn rootProject.tasks.named('preparePublishedTestRepository')

    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath

    shouldRunAfter tasks.named('test')

    filter {
        includeTestsMatching 'ch.so.agi.gretl.*FunctionalTest'
    }

    systemProperty 'gretl.test.executionMode', 'PUBLISHED_ARTIFACT'
    systemProperty 'gretl.test.publishedRepository',
            rootProject.layout.buildDirectory.dir('published-test/maven-repo').get().asFile.absolutePath
    systemProperty 'gretl.test.pluginVersion', rootProject.version.toString()
    systemProperty 'gretl.test.testKitDirectory',
            layout.buildDirectory.dir('test-kit/published-functional').get().asFile.absolutePath

    inputs.dir rootProject.layout.buildDirectory.dir('published-test/maven-repo')
    inputs.property 'gretlPluginVersion', rootProject.provider { rootProject.version.toString() }

    useJUnitPlatform()
}
```

Passe die konkrete Provider-Syntax an Gradle 7.6.4 an. Vermeide unnötige eager `.get()`-Aufrufe, wenn eine Provider-Variante funktioniert.

### 11.2 Welche Tests müssen hier laufen?

Mindestens alle Klassen im Package `ch.so.agi.gretl`, deren Name auf `FunctionalTest` endet.

Dazu gehören nach aktuellem Stand unter anderem:

```text
AvFunctionalTest
CurlFunctionalTest
DuckDbSqlExecutorFunctionalTest
FtpFunctionalTest
Gpkg2DxfFunctionalTest
GzipFunctionalTest
Ili2dbFunctionalTest
Ili2duckdbFunctionalTest
InterlisValidatorFunctionalTest
IoxWkfFunctionalTest
ShapefileFunctionalTest
SqlExecutorFunctionalTest
XslTransformerFunctionalTest
```

Der Agent muss die tatsächliche Liste aus dem aktuellen Repository ermitteln und im Abschlussbericht ausgeben.

### 11.3 Keine Unit-Tests doppelt ausführen

Engine- oder reine Unit-Tests sollen nicht über `publishedFunctionalTest` erneut laufen. Die Filterung muss auf die echten Gradle-Funktionstests beschränkt sein.

---

## 12. Core: `publishedIntegrationTest`

Ergänze in `gretl-core/build.gradle`:

```groovy
tasks.register('publishedIntegrationTest', Test) {
    group = 'verification'
    description = 'Runs Docker-backed GRETL Core integration tests against plugins resolved from the published-test Maven repository.'

    dependsOn rootProject.tasks.named('preparePublishedTestRepository')

    testClassesDirs = sourceSets.integrationTest.output.classesDirs
    classpath = sourceSets.integrationTest.runtimeClasspath

    shouldRunAfter tasks.named('integrationTest')
    shouldRunAfter tasks.named('publishedFunctionalTest')

    systemProperty 'gretl.test.executionMode', 'PUBLISHED_ARTIFACT'
    systemProperty 'gretl.test.publishedRepository',
            rootProject.layout.buildDirectory.dir('published-test/maven-repo').get().asFile.absolutePath
    systemProperty 'gretl.test.pluginVersion', rootProject.version.toString()
    systemProperty 'gretl.test.testKitDirectory',
            layout.buildDirectory.dir('test-kit/published-integration').get().asFile.absolutePath

    inputs.dir rootProject.layout.buildDirectory.dir('published-test/maven-repo')
    inputs.property 'gretlPluginVersion', rootProject.provider { rootProject.version.toString() }

    useJUnitPlatform()
}
```

Diese Task soll alle Klassen des `integrationTest`-Source-Sets laufen lassen, sofern sie nicht explizit als nicht artefaktrelevant dokumentiert werden.

Erwartete Suites umfassen mindestens:

```text
Db2DbPostgisIntegrationTest
DuckDbSqlExecutorPostgisIntegrationTest
FtpDockerIntegrationTest
Ili2pgPostgisIntegrationTest
IoxWkfPostgisIntegrationTest
S3FlociIntegrationTest
SqlExecutorPostgisIntegrationTest
```

### 12.1 SourceSet-Classpath prüfen

Nach Einführung von `gretl-test-support` muss `integrationTest` die Support-Klassen sehen. Falls die bestehende `extendsFrom testImplementation`-Konfiguration dafür nicht genügt, ergänze explizit die notwendige Test-Support-Abhängigkeit.

Füge **nicht** pauschal Source-Outputs des GRETL-Plugins als Child-Build-Plugin-Classpath ein. Der Parent-Test-JVM-Classpath darf Test- und Assertion-Helfer enthalten; der temporäre Gradle-Child-Build darf das Plugin nur aus Maven auflösen.

---

## 13. GeoTools: `publishedFunctionalTest`

Ergänze in `gretl-geotools/build.gradle`:

```groovy
tasks.register('publishedFunctionalTest', Test) {
    group = 'verification'
    description = 'Runs GeoTools plugin functional tests against the published plugin artifact.'

    dependsOn rootProject.tasks.named('preparePublishedTestRepository')

    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath

    shouldRunAfter tasks.named('test')

    filter {
        includeTestsMatching 'ch.so.agi.gretl.geotools.GeoToolsPluginFunctionalTest'
    }

    systemProperty 'gretl.test.executionMode', 'PUBLISHED_ARTIFACT'
    systemProperty 'gretl.test.publishedRepository',
            rootProject.layout.buildDirectory.dir('published-test/maven-repo').get().asFile.absolutePath
    systemProperty 'gretl.test.pluginVersion', rootProject.version.toString()
    systemProperty 'gretl.test.testKitDirectory',
            layout.buildDirectory.dir('test-kit/published-functional').get().asFile.absolutePath

    inputs.dir rootProject.layout.buildDirectory.dir('published-test/maven-repo')
    inputs.property 'gretlPluginVersion', rootProject.provider { rootProject.version.toString() }

    useJUnitPlatform()
}
```

### 13.1 Verbindliche GeoTools-Szenarien

Im Published-Modus müssen mindestens erfolgreich laufen:

- Plugin anwenden und Default-Task registrieren;
- `ReadShapefile` über Worker-Isolation;
- `Vectorize` über Worker-Isolation;
- `RasterReclassify` über Worker-Isolation;
- Fehlerfall leere `cellValues`;
- Fehlerfall nicht streng steigende `breaks`;

Gerade diese Tests beweisen, dass der eingebettete Worker-Classpath aus dem publizierten JAR und nicht aus Source-Ausgaben funktioniert.

---

## 14. Root-Aggregation

Erstelle in `build.gradle`:

```groovy
tasks.register('publishedArtifactTest') {
    group = 'verification'
    description = 'Verifies and executes the published GRETL Core and GeoTools Gradle plugin artifacts.'

    dependsOn tasks.named('verifyPublishedTestRepository')
    dependsOn ':gretl-core:publishedFunctionalTest'
    dependsOn ':gretl-core:publishedIntegrationTest'
    dependsOn ':gretl-geotools:publishedFunctionalTest'
}
```

Die Task darf nicht von `check` abhängen und `check` darf nicht automatisch von ihr abhängen. CI ruft beide explizit auf.

---

## 15. Isolationstest gegen versehentliche Source-Injektion

### 15.1 Testklasse

Erstelle in `gretl-core/src/test/java/ch/so/agi/gretl`:

```text
PublishedArtifactIsolationFunctionalTest.java
```

Annotiere die Klasse mit:

```java
@Tag("published-artifact-only")
```

Konfiguriere den normalen `test`-Task so, dass dieser Tag ausgeschlossen wird:

```groovy
tasks.named('test') {
    useJUnitPlatform {
        excludeTags 'published-artifact-only'
    }
}
```

`publishedFunctionalTest` darf diesen Tag nicht ausschliessen.

### 15.2 Test `doesNotResolvePluginWithoutPublishedRepository`

Ablauf:

1. verifizieren, dass der aktuelle Modus `PUBLISHED_ARTIFACT` ist;
2. leeres temporäres Maven-Repository anlegen;
3. isoliertes, leeres TestKit-Verzeichnis anlegen;
4. `settings.gradle` erzeugen, deren `pluginManagement.repositories` **nur** dieses leere Repository enthält;
5. Version aus der Published-Test-Konfiguration verwenden;
6. Buildskript mit `plugins { id 'ch.so.agi.gretl' }` schreiben;
7. direkten `GradleRunner` ohne `withPluginClasspath()` verwenden;
8. `buildAndFail()` erwarten;
9. prüfen, dass die Ausgabe auf nicht gefundenes Plugin/Marker-Artefakt hinweist.

Dieser Test darf absichtlich einen direkten `GradleRunner` verwenden, weil er die Isolation der allgemeinen Infrastruktur kontrolliert. Die Fundstelle ist im Abschlussbericht zu begründen.

Wichtig:

- das TestKit-Verzeichnis muss exklusiv und leer sein;
- keine externen Plugin-Repositories eintragen;
- nicht auf eine einzige exakte englische Gradle-Fehlermeldung festnageln;
- aber Plugin-ID und Version müssen in der Ausgabe erscheinen.

### 15.3 Test `resolvesBothPluginMarkersFromPublishedRepository`

Erzeuge ein minimales Projekt, das beide Plugins anwendet:

```groovy
plugins {
    id 'ch.so.agi.gretl'
    id 'ch.so.agi.gretl.geotools'
}
```

Führe aus:

```text
tasks --all
```

Prüfe mindestens:

- Build erfolgreich;
- Core-Task ist sichtbar;
- GeoTools-Default-Task `readShapefile` ist sichtbar;
- Ausgabe enthält keine Meldung `Plugin ... was not found`.

---

## 16. Semantische Pflicht-Canaries

Obwohl die vollständigen bestehenden Funktionstests erneut laufen, sind folgende Szenarien als unverzichtbare Canaries zu betrachten. Falls aktuelle Testklassennamen oder Methoden geändert wurden, muss der Agent die äquivalenten Fälle identifizieren.

### 16.1 Core / Gzip

Beweist:

- Markerauflösung;
- Plugin-JAR;
- Taskklasse;
- einfache Datei-I/O;
- Provider-basierte Output-Property.

Assertion:

- GZIP-Datei existiert;
- dekomprimierter Inhalt ist byte-/textgleich mit Input.

### 16.2 Core / SqlExecutor + SQLite

Beweist:

- transitive Implementierungsabhängigkeiten;
- publizierter SQLite-JDBC-Treiber;
- Taskausführung;
- SQL-Ressourcen;
- Datenbankresultat.

Assertion:

- Tabelle und Daten wurden tatsächlich erzeugt;
- nicht nur Build-Outcome prüfen.

### 16.3 Core / DuckDB

Beweist:

- publizierter DuckDB-JDBC-Treiber;
- Zusammenspiel der Dependencies;
- SQL-Dateien/Fixtures.

### 16.4 Core / IliValidator oder Ili2duckdb

Beweist:

- komplexer INTERLIS-Dependency-Graph;
- Custom Functions;
- Modelle und Ressourcen;
- reale Taskausführung.

### 16.5 Core / PostGIS-Integration

Beweist:

- PostgreSQL-JDBC zur Laufzeit;
- Testcontainers-Netzwerkzugriff aus dem Child-Build;
- POM-Abhängigkeiten;
- reale Datenbankveränderungen.

### 16.6 GeoTools / ReadShapefile

Beweist:

- publiziertes GeoTools-Plugin;
- Worker-Runtime-JAR;
- eingebettete GeoTools-Libraries;
- EPSG-Unterstützung;
- Worker-Classloader-Isolation.

### 16.7 GeoTools / RasterReclassify und Vectorize

Beweist:

- Raster-/Coverage-Abhängigkeiten;
- GeoTIFF-/ArcGrid-Unterstützung;
- GeoPackage-Unterstützung;
- Worker-Dispatch;
- reale Outputdateien.

---

## 17. Netz- und Repository-Verhalten

### 17.1 Externe Abhängigkeiten dürfen online aufgelöst werden

Diese P0-Testebene ist **kein vollständiger Offline-Test**. Externe Drittanbieter-Abhängigkeiten dürfen aus den explizit konfigurierten Repositories geladen werden.

Die GRETL-eigenen Plugin-Marker und Implementierungsartefakte müssen jedoch aus dem lokalen Published-Test-Repository kommen.

### 17.2 Keine versteckten Repository-Quellen

Untersagt:

- `mavenLocal()`;
- `~/.m2/repository` als `file:`-Repository;
- `jars.interlis.guru/snapshots` für GRETL selbst;
- Root-Composite-Build;
- lokale `build/libs`-Verzeichnisse.

### 17.3 Repository-Reihenfolge

Published-Test-Repository steht immer an erster Stelle. Dadurch wird die lokale Version verwendet, selbst wenn später dieselbe Versionsnummer versehentlich anderswo verfügbar ist.

### 17.4 Snapshot-Cache

Jeder Published-Test-Task erhält ein eigenes TestKit-Verzeichnis unter seinem Modul-Buildverzeichnis. Dadurch wird der globale Benutzer-Gradle-Cache nicht als versteckte Quelle verwendet.

Innerhalb derselben Task dürfen Testmethoden denselben TestKit-Cache teilen, damit Drittanbieter-Abhängigkeiten nicht für jeden Test erneut geladen werden.

---

## 18. Up-to-date- und Cache-Verhalten

Die Published-Test-Tasks müssen mindestens folgende Inputs deklarieren:

- das vollständige Published-Test-Repository-Verzeichnis;
- die GRETL-Version;
- ihre Testklassen und Runtime-Classpaths, was `Test` ohnehin übernimmt;
- relevante Systemproperties.

Wenn ein Plugin-JAR bei unveränderter SNAPSHOT-Version geändert wird, müssen die Published-Tests erneut laufen. Das Repository-Verzeichnis als Input ist dafür erforderlich.

Die Tasks müssen nicht für den Gradle Build Cache optimiert werden, sofern dies mit Testcontainers oder temporären Child-Builds unzuverlässig wäre. Keine falsche `@CacheableTask`-Deklaration hinzufügen.

---

## 19. CI-Integration

Ändere `.github/workflows/ci.yml`.

Die Build-Job-Reihenfolge soll mindestens sein:

```yaml
- name: Build and unit test
  run: ./gradlew clean check

- name: Run integration tests from source classpath
  run: ./gradlew :gretl-core:integrationTest

- name: Test published GRETL plugin artifacts
  run: ./gradlew publishedArtifactTest --stacktrace
```

Der Name `from source classpath` ist empfohlen, damit der Unterschied in der CI-Oberfläche sichtbar ist.

### 19.1 Publish-Gate

Der bestehende `publish`-Job hängt bereits vom erfolgreichen Build-Job ab. Diese Abhängigkeit muss erhalten bleiben. Damit dürfen Snapshots erst publiziert werden, wenn `publishedArtifactTest` erfolgreich war.

### 19.2 Reports

Die bestehenden Upload-Pfade `**/build/reports/tests/` und `**/build/test-results/` sollen die neuen Reports erfassen.

Ergänze bei Bedarf einen Fehler-Artefakt-Upload für:

```text
build/published-test/maven-repo
```

Dieser Upload soll nur bei Fehlern erfolgen, weil das Repository gross sein kann.

Empfohlener Name:

```text
published-test-repository-on-failure
```

### 19.3 Kein `continue-on-error`

Published-Tests sind P0 und dürfen nicht mit `continue-on-error: true` laufen.

---

## 20. Dokumentation

Aktualisiere mindestens den Abschnitt „Build And Test“ im Root-`README.md` oder erstelle eine fokussierte Datei:

```text
docs/testing-published-artifacts.md
```

Dokumentiere:

```bash
./gradlew clean check
./gradlew :gretl-core:integrationTest
./gradlew publishedArtifactTest
```

Erkläre klar:

- `test`/`integrationTest` verwenden Source-/Plugin-Classpath;
- `publishedFunctionalTest` und `publishedIntegrationTest` lösen das Plugin aus dem temporären Maven-Repository auf;
- `publishedArtifactTest` ist das Artefakt-Release-Gate;
- Docker-Runtime-Image wird dadurch noch nicht getestet;
- kein `mavenLocal()` wird verwendet;
- temporäres Repository liegt unter `build/published-test/maven-repo`.

---

## 21. Fehlermeldungen und Diagnosequalität

Published-Tests müssen bei Fehlern genügend Informationen liefern.

### 21.1 Fehlende Systemproperty

Beispiel:

```text
Missing required system property 'gretl.test.publishedRepository' for PUBLISHED_ARTIFACT test execution.
```

### 21.2 Fehlendes Artefakt

Beispiel:

```text
Missing published GRETL artifact: build/published-test/maven-repo/ch/so/agi/gretl-core/5.0.0-SNAPSHOT/gretl-core-5.0.0-SNAPSHOT.jar
```

### 21.3 Falscher Marker

Beispiel:

```text
Plugin marker ch.so.agi.gretl:ch.so.agi.gretl.gradle.plugin:5.0.0-SNAPSHOT points to ch.so.agi:gretl-core:0.1.0-SNAPSHOT; expected ch.so.agi:gretl-core:5.0.0-SNAPSHOT.
```

### 21.4 Fehlender Worker-Inhalt

Beispiel:

```text
Published gretl-geotools JAR does not contain a worker dependency matching gretl-geotools-worker-classpath/lib/gt-epsg-hsql-*.jar.
```

### 21.5 Child-Build-Fehler

`forwardOutput()` muss aktiv bleiben. Testreports sollen den vollständigen Gradle-Output enthalten.

---

## 22. Verbotene Abkürzungen

Der Agent darf die Aufgabe nicht durch eine der folgenden Varianten scheinbar erfüllen:

1. nur prüfen, dass JAR-Dateien existieren;
2. nur `publishToMavenLocal` aufrufen;
3. temporären Testprojekten `withPluginClasspath()` geben;
4. nur einen `tasks`-Smoke ohne fachliche Taskausführung ausführen;
5. die bestehende Funktionstestsuite kopieren und anschliessend auseinanderlaufen lassen;
6. Pluginversion direkt in allen Build-Fixtures hart codieren;
7. `mavenLocal()` an erste Stelle setzen;
8. fehlende Dependencies durch manuelles Kopieren in den Child-Build-Classpath kaschieren;
9. den GeoTools-Worker direkt aus `sourceSets.worker.output` starten;
10. Published-Tests nur lokal dokumentieren, aber nicht in CI aufnehmen;
11. CI-Fehler mit `continue-on-error` ignorieren;
12. die Runtime-Image-Tests als erledigt deklarieren.

---

## 23. Akzeptanzkriterien

Alle Kriterien müssen erfüllt sein.

### 23.1 Gradle-Tasks

Auf sauberem Checkout:

```bash
./gradlew clean publishedArtifactTest
```

muss erfolgreich sein.

### 23.2 Bestehende Tests

Weiterhin erfolgreich:

```bash
./gradlew clean check
./gradlew :gretl-core:integrationTest
```

### 23.3 Zwei unterschiedliche Ausführungsarten nachweisbar

Der Testreport muss zeigen, dass dieselben Core-Funktionstests mindestens über folgende Tasks liefen:

```text
:gretl-core:test
:gretl-core:publishedFunctionalTest
```

Die Integrationstests entsprechend:

```text
:gretl-core:integrationTest
:gretl-core:publishedIntegrationTest
```

### 23.4 Published-Modus ohne Plugin-Classpath

Ein Code-Review muss eindeutig zeigen:

- `PluginClasspathBuildExecutor` ruft `withPluginClasspath()` auf;
- `PublishedArtifactBuildExecutor` ruft es nicht auf;
- Published-Test-Tasks setzen `PUBLISHED_ARTIFACT`;
- Isolationstest schlägt bei leerem Repository erwartungsgemäss fehl.

### 23.5 Plugin-Marker

Beide Marker-POMs werden semantisch geprüft und zeigen auf die richtige Version der Implementierung.

### 23.6 Core-Runtime-Abhängigkeiten

Mindestens SQLite-, PostgreSQL- und DuckDB-basierte Tasks laufen aus dem publizierten Plugin erfolgreich.

### 23.7 GeoTools-Worker

`ReadShapefile`, `Vectorize` und `RasterReclassify` laufen aus dem publizierten Plugin-JAR erfolgreich über Worker-Isolation.

### 23.8 Keine versteckte lokale Auflösung

Die generierten Settings enthalten kein `mavenLocal()` und keine Source-/Build-Verzeichnis-Repositories.

### 23.9 CI-Gate

GitHub Actions führt `publishedArtifactTest` aus, bevor der Publish-Job freigegeben wird.

### 23.10 Dokumentation

Der Unterschied zwischen Source-Classpath-, Published-Artifact- und künftigem Runtime-Image-Test ist dokumentiert.

---

## 24. Vorgeschlagene Negativ-/Mutationstests zur manuellen Verifikation

Diese Änderungen müssen nicht committed werden. Der Agent soll sie lokal oder gedanklich zur Prüfung verwenden und im Abschlussbericht nennen.

### 24.1 Core-Marker entfernen

Temporär Marker-Publikation verhindern oder Marker-POM nach `preparePublishedTestRepository` löschen.

Erwartung:

- `verifyPublishedTestRepository` schlägt fehl;
- Core Published-Funktionstest kann Plugin nicht auflösen.

### 24.2 Marker auf falsche Version zeigen lassen

Erwartung:

- semantischer Marker-POM-Check schlägt mit Soll/Ist-Version fehl.

### 24.3 SQLite-Treiber aus `runtimeOnly` entfernen

Erwartung:

- Source-Classpath-Test kann abhängig vom Parent-Classpath eventuell noch anders reagieren;
- Published-`SqlExecutorFunctionalTest` muss beim realen Child-Build fehlschlagen;
- dadurch wird der Mehrwert der neuen Ebene demonstriert.

### 24.4 GeoTools-Worker-Libraries nicht ins JAR kopieren

Temporär `processResources`-Copy der Worker-Libraries deaktivieren.

Erwartung:

- `verifyPublishedTestRepository` schlägt fehl;
- spätestens `ReadShapefile` im Published-Modus schlägt fehl.

### 24.5 Published-Repository aus Settings entfernen

Erwartung:

- beide Plugins werden nicht gefunden;
- kein Source-Fallback rettet den Build.

---

## 25. Implementierungsreihenfolge

Der Agent soll in dieser Reihenfolge arbeiten.

### Schritt 1 – Bestandsaufnahme

- alle Runner-Fundstellen erfassen;
- aktuelle Testklassennamen erfassen;
- aktuelle Publication-Tasknamen mit `./gradlew tasks --all` oder `outgoingVariants`/Tasklisting bestätigen;
- keine Annahme über generierte Tasknamen ungeprüft lassen.

### Schritt 2 – Test-Support-Modul

- Modul anlegen;
- Execution Mode;
- Configuration;
- Executor-Klassen;
- Settings-Renderer;
- Unit-Tests.

Danach:

```bash
./gradlew :gretl-test-support:test
```

### Schritt 3 – Bestehende Source-Tests refactoren

- Core Support umstellen;
- GeoTools Support umstellen;
- Integration-Sonderklassen umstellen.

Danach muss das bisherige Verhalten unverändert grün sein:

```bash
./gradlew :gretl-core:test :gretl-geotools:test
./gradlew :gretl-core:integrationTest
```

### Schritt 4 – Published-Test-Repository

- Repository hinzufügen;
- Clean-/Prepare-Tasks;
- manuell publizieren;
- Repository-Inhalt inspizieren.

```bash
./gradlew cleanPublishedTestRepository preparePublishedTestRepository
find build/published-test/maven-repo -maxdepth 8 -type f | sort
```

### Schritt 5 – Repository-Vertrag

- Implementierungsartefakte;
- Marker-POMs;
- Plugin-Descriptors;
- Core-POM;
- GeoTools-Worker.

```bash
./gradlew verifyPublishedTestRepository
```

### Schritt 6 – Published-Funktionstests

- Core Task;
- GeoTools Task;
- Isolationstest.

```bash
./gradlew :gretl-core:publishedFunctionalTest
./gradlew :gretl-geotools:publishedFunctionalTest
```

### Schritt 7 – Published-Integrationstests

```bash
./gradlew :gretl-core:publishedIntegrationTest
```

### Schritt 8 – Aggregation und CI

```bash
./gradlew clean publishedArtifactTest
```

Dann Workflow anpassen und YAML validieren.

### Schritt 9 – Dokumentation und Abschlussprüfung

Gesamtlauf:

```bash
./gradlew clean check
./gradlew :gretl-core:integrationTest
./gradlew publishedArtifactTest
```

Optional ein kombinierter Lauf zur Erkennung von Task-Graph-/Clean-Races:

```bash
./gradlew clean check :gretl-core:integrationTest publishedArtifactTest --parallel
```

Wenn `--parallel` wegen Testcontainers bewusst nicht unterstützt wird, muss wenigstens die Repository-Vorbereitung race-frei sein und die Einschränkung dokumentiert werden.

---

## 26. Erwartete Dateiänderungen

Mindestens:

```text
settings.gradle
build.gradle
.github/workflows/ci.yml
README.md oder docs/testing-published-artifacts.md

gretl-test-support/build.gradle
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlTestExecutionMode.java
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlTestSystemProperties.java
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/PublishedArtifactTestConfiguration.java
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlBuildExecutor.java
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/AbstractGradleBuildExecutor.java
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/PluginClasspathBuildExecutor.java
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/PublishedArtifactBuildExecutor.java
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlBuildExecutors.java
gretl-test-support/src/main/java/ch/so/agi/gretl/testkit/GretlTestProjectSettings.java

gretl-test-support/src/test/java/ch/so/agi/gretl/testkit/GretlTestExecutionModeTest.java
gretl-test-support/src/test/java/ch/so/agi/gretl/testkit/GretlTestProjectSettingsTest.java
gretl-test-support/src/test/java/ch/so/agi/gretl/testkit/PublishedArtifactTestConfigurationTest.java

gretl-core/build.gradle
gretl-core/src/test/java/ch/so/agi/gretl/CoreFunctionalTestSupport.java
gretl-core/src/test/java/ch/so/agi/gretl/PublishedArtifactIsolationFunctionalTest.java
gretl-core/src/integrationTest/java/ch/so/agi/gretl/PostgisIntegrationTestSupport.java
gretl-core/src/integrationTest/java/ch/so/agi/gretl/S3FlociIntegrationTest.java
gretl-core/src/integrationTest/java/ch/so/agi/gretl/FtpDockerIntegrationTest.java

gretl-geotools/build.gradle
gretl-geotools/src/test/java/ch/so/agi/gretl/geotools/GeoToolsPluginFunctionalTest.java
```

Weitere Dateien sind zulässig, wenn sie die Struktur verbessern. Produktionsklassen unter `src/main` sollen für diesen Auftrag grundsätzlich nicht verändert werden.

---

## 27. Abschlussbericht des Coding Agents

Der Agent muss am Ende liefern:

### 27.1 Zusammenfassung

- welche neue Testebene implementiert wurde;
- warum sie keinen Source-Classpath verwendet;
- welche Tasks neu sind.

### 27.2 Ausgeführte Befehle und Resultate

Mindestens:

```text
:gretl-test-support:test
:gretl-core:test
:gretl-geotools:test
:gretl-core:integrationTest
verifyPublishedTestRepository
:gretl-core:publishedFunctionalTest
:gretl-geotools:publishedFunctionalTest
:gretl-core:publishedIntegrationTest
publishedArtifactTest
```

Mit Testanzahl und Ergebnis, soweit Gradle sie ausgibt.

### 27.3 Wiederverwendete Testklassen

Exakte Liste der Testklassen, die im Published-Modus erneut ausgeführt wurden.

### 27.4 Verbleibende direkte `GradleRunner.create()`-Stellen

Jede verbleibende Stelle mit Begründung.

### 27.5 Repository-Inhalt

Kurze Liste der geprüften Marker- und Implementierungskoordinaten.

### 27.6 Bekannte Grenzen

Explizit nennen:

- Runtime-Image noch nicht getestet;
- vollständige Offline-Fähigkeit noch nicht getestet;
- externe Drittanbieter-Repositories werden benötigt;
- Multi-Arch nicht getestet.

---

## 28. Definition of Done

Die Aufgabe ist erst abgeschlossen, wenn alle folgenden Aussagen wahr sind:

- [ ] Core und GeoTools werden in ein isoliertes Maven-Repository publiziert.
- [ ] Beide Plugin-Marker werden semantisch geprüft.
- [ ] Core- und GeoTools-Implementierungsartefakte werden geprüft.
- [ ] Der eingebettete GeoTools-Worker-Classpath wird im publizierten JAR geprüft.
- [ ] Dieselben Core-Funktionstests laufen erneut ohne `withPluginClasspath()`.
- [ ] Dieselben Core-Integrationstests laufen erneut ohne `withPluginClasspath()`.
- [ ] Die GeoTools-Plugin-Funktionstests laufen erneut ohne `withPluginClasspath()`.
- [ ] Ein negativer Isolationstest beweist, dass ein leeres Repository nicht durch Source-Injektion umgangen wird.
- [ ] `mavenLocal()` wird in den Child-Builds nicht verwendet.
- [ ] `publishedArtifactTest` ist ein hartes GitHub-Actions-Gate.
- [ ] Bestehende Source-Classpath-Tests bleiben grün und schnell separat ausführbar.
- [ ] Dokumentation erklärt die drei Ebenen Source, Published Artifact und zukünftiges Runtime Image.
- [ ] `./gradlew clean publishedArtifactTest` ist auf einem sauberen Checkout erfolgreich.

---

## 29. Leitgedanke für Implementierungsentscheidungen

Bei jeder Detailentscheidung ist diese Frage anzuwenden:

> Würde der Test weiterhin grün werden, wenn die Source-Klassen korrekt sind, aber Marker-POM, Implementierungs-POM, publiziertes JAR oder eingebetteter Worker fehlerhaft sind?

Wenn die Antwort **ja** lautet, umgeht der Test wahrscheinlich die publizierten Artefakte und erfüllt diese Spezifikation nicht.

Der gewünschte Nachweis lautet:

> Ein unabhängiges Gradle-Testprojekt kann die beiden GRETL-Plugins über ihre publizierten Marker aus einem Maven-Repository auflösen und reale Core-, Datenbank-, INTERLIS- und GeoTools-Tasks mit den publizierten Laufzeitbestandteilen korrekt ausführen.
