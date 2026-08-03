# Spezifikation: P0 – Offline-Image-Test für GRETL

**Ziel-Repository:** `https://github.com/edigonzales/gretl-next`

**DSL-Policy:** Die Offline-P0-Suite prüft ausschließlich Groovy-Gradle-Builds.
Kotlin DSL kann durch Gradle weiterhin zufällig funktionieren, ist aber kein
GRETL-Vertrag und wird weder getestet noch dokumentiert.
**Betroffene Produktmodule:** `gretl-core`, `gretl-geotools`
**Betroffene Distribution:** GRETL-Runtime-Image
**Status:** verbindlicher Umsetzungsauftrag für einen LLM Coding Agent
**Priorität:** P0 / Release-Gate
**Stand:** 30. Juli 2026
**Primärer Gradle-Consumer-Vertrag:** moderne `plugins {}`-DSL
**Verbindlicher Root-Task:** `runtimeImageOfflineTest`
**Zentrales Beweisziel:** Ein aus dem aktuellen Checkout gebautes GRETL-Runtime-Image kann neu erzeugte, gemountete Gradle-Projekte mit `gretl-core` und `gretl-geotools` ausführen, obwohl der Container keinen Netzwerkzugriff besitzt, kein Host-Gradle-Home oder `mavenLocal()` sieht und ein frisches beschreibbares `GRADLE_USER_HOME` verwendet.

---

## 1. Auftrag an den Coding Agent

Implementiere im Repository `edigonzales/gretl-next` eine eigenständige, reproduzierbare und zwingende P0-Teststufe für das Offline-Verhalten des GRETL-Runtime-Images.

Die Teststufe muss das **tatsächlich aus dem aktuellen Checkout gebaute Container-Image** verwenden. Sie darf nicht bloss den Source-Classpath, Gradle TestKit, ein publiziertes Snapshot-Artefakt, ein bereits vorhandenes Registry-Image oder einen zufällig gefüllten lokalen Gradle-Cache testen.

Die Aufgabe ist erst abgeschlossen, wenn der folgende Beweis automatisiert erbracht wird:

1. Das Image wird aus dem aktuellen Repository-Stand gebaut.
2. Die unveränderliche Image-ID wird aufgezeichnet.
3. Genau diese Image-ID wird im Offline-Test verwendet.
4. Der Testcontainer läuft mit Docker-Netzwerkmodus `none`.
5. Der Gradle-Aufruf enthält `--offline`.
6. Das beschreibbare `GRADLE_USER_HOME` ist vor jedem Testlauf frisch und testspezifisch.
7. Es wird kein Host-Gradle-Cache gemountet.
8. Es wird kein Host-Maven-Repository gemountet.
9. `mavenLocal()` ist nicht Teil des notwendigen Auflösungswegs.
10. Es gibt keinen Source-Classpath- oder `withPluginClasspath()`-Fallback.
11. `ch.so.agi.gretl` wird über die moderne Plugin-DSL geladen.
12. `ch.so.agi.gretl.geotools` wird über die moderne Plugin-DSL geladen.
13. Ein realer Core-Task erzeugt fachlich korrektes Output.
14. Ein realer GeoTools-Task erzeugt fachlich korrektes Output.
15. Groovy DSL funktioniert.
17. Die im Image benötigten DuckDB-Erweiterungen sind offline ladbar und ausführbar.
18. Fehlende Artefakte oder Metadaten führen deterministisch zu einem verständlichen Fehler.
19. Ein vorheriger Online-Lauf kann den Offline-Test nicht unbemerkt grün machen.
20. Die CI blockiert Snapshot- und Release-Publikationen, wenn der Offline-Image-Test fehlschlägt.

---

## 2. Dauerhafte Projektabgrenzung

### 2.1 Keine Migration von `sogis/gretljobs`

Die Migration bestehender Jobs aus `sogis/gretljobs` ist dauerhaft **nicht Bestandteil von `gretl-next`** und nicht Bestandteil dieser Spezifikation.

Der Coding Agent darf insbesondere nicht:

- Jobverzeichnisse aus `sogis/gretljobs` kopieren;
- bestehende Jobs umschreiben;
- Migrationsskripte oder Codemods entwickeln;
- `gretljobs` als Offline-Abnahmesuite verwenden;
- Legacy-Task-APIs aus Kompatibilitätsgründen einführen;
- die Fertigstellung dieses P0-Gates von einer späteren Jobmigration abhängig machen;
- ein Folgearbeitspaket «Migration gretljobs» als Ergebnis dieser Aufgabe anlegen.

Alle Offline-Testprojekte und Fixtures werden eigens für `gretl-next` erstellt.

### 2.2 Nur moderne Plugin-DSL

Der verbindliche Consumer-Vertrag lautet:

```groovy
plugins {
    id 'ch.so.agi.gretl'
}
```

Für GeoTools:

```groovy
plugins {
    id 'ch.so.agi.gretl'
    id 'ch.so.agi.gretl.geotools'
}
```

Nicht Bestandteil des Produktvertrags sind:

```groovy
apply plugin: 'ch.so.agi.gretl'
```

und manuell konfigurierte `buildscript.classpath`-Abhängigkeiten.

Daraus folgen ausdrücklich keine Tests für:

- `apply plugin`;
- historische GRETL-Init-Scripts;
- pauschale JAR-Injektion;
- `flatDir` als Consumer-Vertrag;
- unveränderte historische Jobskripte.

### 2.3 Kein vollständiger Runtime-E2E-Ersatz

Diese Spezifikation deckt ausschliesslich das Offline-Gate ab.

Nicht Bestandteil dieses Auftrags sind:

- vollständige PostGIS-E2E-Matrix;
- FTP-, S3- oder HTTP-Service-E2E;
- Daemon-Reuse in langlebigen Servicecontainern;
- vollständige Runtime-Image-Coverage aller GRETL-Tasks;
- Registry-Push und Multi-Arch-Veröffentlichung.

Diese Bereiche können durch die separate Spezifikation «P0: Runtime-Image-E2E» abgedeckt werden.

Der Offline-Test muss jedoch eigenständig lauffähig sein und darf nicht voraussetzen, dass die vollständige Runtime-E2E-Suite bereits implementiert wurde.

---

## 3. Begriffsklärung: Was «offline» in dieser Spezifikation bedeutet

Ein Test gilt nur dann als Offline-Image-Test, wenn **alle** folgenden Ebenen erfüllt sind.

### 3.1 Netzwerkoffline

Der Container besitzt keinen Netzwerkzugriff:

```text
Docker HostConfig.NetworkMode == "none"
```

Der Container sieht nur das Loopback-Interface.

Ein bloss fehlgeschlagener Download oder eine Firewallannahme genügt nicht.

### 3.2 Gradle-offline

Der Gradle-Aufruf enthält:

```text
--offline
```

Damit wird verhindert, dass Gradle bei Dependency-Auflösung bewusst Remote-Repositories kontaktiert.

### 3.3 Cache-kalt auf der beschreibbaren Seite

Das testspezifische beschreibbare `GRADLE_USER_HOME` startet leer.

Es darf keine Artefakte, Metadaten oder Plugin-Auflösungen aus einem vorherigen Test enthalten.

### 3.4 Host-isoliert

Folgendes darf nicht in den Container gemountet werden:

- `$HOME/.gradle`;
- `$GRADLE_USER_HOME`;
- `$HOME/.m2`;
- ein Host-Maven-Repository;
- der Repository-Checkout als Classpath;
- Build-Ausgaben der Produktionsmodule;
- ein TestKit-Plugin-Classpath;
- ein Verzeichnis aus einem vorherigen Online-Bootstrap.

### 3.5 Image-vollständig

Alle Informationen, die zur Auflösung und Ausführung der getesteten GRETL-Plugins notwendig sind, müssen Bestandteil des getesteten Images sein.

Zulässige interne Mechanismen sind insbesondere:

- ein strukturiertes lokales Maven-Repository;
- ein deterministisch erzeugter, read-only Gradle-Dependency-Cache;
- eine Kombination aus beiden;
- ein anderer Gradle-nativer Mechanismus mit gleichwertigem Nachweis.

Unzulässig ist ein nicht nachvollziehbares «funktioniert wegen irgendeines Cache-Layers».

### 3.6 Fachlich offline

Nicht nur Plugin-Anwendung und `tasks` müssen funktionieren.

Mindestens folgende reale Operationen müssen offline erfolgreich sein:

- Core-Dateiverarbeitung;
- Core-SQL mit lokaler Datenbank;
- GeoTools-Shapefile- oder Rasterverarbeitung;
- DuckDB mit den im Produktvertrag enthaltenen Extensions.

---

## 4. Ausgangslage im aktuellen Repository

Der Agent muss den aktuellen Stand vor Änderungen erneut verifizieren.

Zum Zeitpunkt dieser Spezifikation gilt:

- Root-Projektversion: `5.0.0-SNAPSHOT`;
- Java-Toolchain: 17;
- aktuelles Runtime-Image verwendet eine Java-17-JRE;
- das Dockerfile installiert Gradle 7.6.4;
- der Build publiziert `gretl-core` und `gretl-geotools` in `build/runtime-image/maven-repo`;
- zusätzlich werden Implementierungs- und Runtime-JARs nach `libs/` kopiert;
- das Init-Script enthält `mavenLocal()`;
- das Init-Script konfiguriert mehrere Remote-Repositories;
- das Init-Script injiziert alle JARs aus `libs/` pauschal in den Buildscript-Classpath;
- die Default-GRETL-Version im Init-Script lautet aktuell `0.1.0-SNAPSHOT`;
- der Launcher erzwingt aktuell `--no-daemon`;
- der Docker-Build zeichnet noch keine unveränderliche Image-ID über `--iidfile` auf;
- die bestehende CI führt `clean check`, Source-Integrationstests und Published-Artifact-Tests aus;
- ein Offline-Image-Gate fehlt.

Diese Punkte sind Ausgangslage, nicht Zielarchitektur.

### 4.1 Bekannte P0-Risiken

Der Offline-Test muss mindestens folgende Fehlerklassen erkennen:

1. falsche GRETL-Version im Init-Script;
2. fehlender Plugin-Marker;
3. fehlendes Implementierungs-JAR;
4. fehlende POM- oder Gradle-Modulmetadaten;
5. fehlende transitive Runtime-Abhängigkeit;
6. Auflösung über `mavenLocal()`;
7. Erfolg durch Host-Gradle-Cache;
8. Erfolg durch Cache eines vorherigen Tests;
9. Erfolg durch pauschale `flatDir`-/JAR-Injektion statt regulärer Plugin-Auflösung;
10. Download einer fehlenden Dependency bei aktivem Netzwerk;
11. fehlende GeoTools-Worker-Abhängigkeit;
12. DuckDB-Extension wird erst zur Laufzeit aus dem Internet geladen;
13. falsche physische SNAPSHOT-Datei oder unvollständiges `maven-metadata.xml`;
14. Image-Tag zeigt auf ein altes Image;
15. Test führt versehentlich Source-Klassen aus.

---

## 5. Beziehung zu anderen P0-Spezifikationen

Die Teststufen haben unterschiedliche Beweisziele:

| Teststufe | Beweisziel |
|---|---|
| Source-/TestKit-Test | Code funktioniert direkt aus dem Build-Tree. |
| Published-Artifact-Test | Publizierte Marker, POMs, Module Metadata und JARs sind als externe Gradle-Plugins verwendbar. |
| Offline-Image-Test | Die ausgelieferte Container-Distribution ist ohne Netzwerk und Host-Caches vollständig. |
| Runtime-Image-E2E | Das Image funktioniert zusätzlich mit Diensten, Daemon-Modell und breiter Taskmatrix. |

Der Offline-Image-Test darf keine der anderen Stufen ersetzen.

Falls gemeinsame Testinfrastruktur bereits existiert, muss sie erweitert und wiederverwendet werden.

Es darf keine zweite konkurrierende Prozess-, Docker- oder Consumer-Projekt-Abstraktion entstehen.

---

## 6. Normative Architekturentscheidungen

### 6.1 Lokales Maven-Repository als auditierbare Quelle

Das Image soll ein strukturiertes lokales Maven-Repository enthalten, vorzugsweise:

```text
/opt/gretl/maven-repository
```

Es enthält mindestens:

- Plugin-Marker für `ch.so.agi.gretl`;
- Plugin-Marker für `ch.so.agi.gretl.geotools`;
- Implementierungsartefakte;
- POM-Dateien;
- Gradle Module Metadata, falls publiziert;
- SNAPSHOT-Metadaten, falls die Version ein Snapshot ist;
- alle benötigten transitiven Artefakte und Metadaten oder einen eindeutig dokumentierten ergänzenden Mechanismus.

Die Verzeichnisbezeichnung darf angepasst werden, muss aber zentral konfiguriert und getestet werden.

### 6.2 Kein `flatDir` als Hauptmechanismus

Folgende Konstruktion ist nicht als Zielarchitektur zulässig:

```groovy
flatDir {
    dirs '/home/gradle/libs'
}

classpath fileTree(
    dir: '/home/gradle/libs',
    include: '*.jar'
)
```

Ein `flatDir`-Repository besitzt keine vollständigen Maven-/Gradle-Metadaten und darf fehlende transitive Dependency-Informationen nicht verdecken.

Ein separates natives Library-Verzeichnis ist nur für nicht über Gradle aufzulösende Runtime-Komponenten zulässig und muss einzeln begründet werden.

### 6.3 Gradle-Cache nur deterministisch und image-intern

Da Gradles `--offline`-Modus fehlende Module nicht aus Remote-Repositories laden darf, kann zusätzlich zum lokalen Maven-Repository ein vorbefüllter Dependency-Cache notwendig sein.

Wenn ein solcher Cache verwendet wird, gelten zwingend folgende Regeln:

- Er wird während des Image-Builds oder eines expliziten Staging-Tasks erzeugt.
- Er wird mit exakt der Gradle-Version erzeugt, die im Image läuft.
- Er stammt nicht aus dem Gradle-Home des Entwicklers oder CI-Runners.
- Er wird durch einen reproduzierbaren Bootstrap-Build erzeugt.
- Er enthält keine Lock-Dateien.
- Er enthält keine `gc.properties`.
- Er enthält keine Credentials.
- Er enthält keine absoluten Hostpfade.
- Er ist im finalen Image read-only.
- Ein Manifest beschreibt seinen Inhalt und seine Erzeugung.
- Eine Mutation eines benötigten Cache-Eintrags lässt den Offline-Test fehlschlagen.
- Der beschreibbare Test-Cache bleibt trotzdem leer und testspezifisch.

Zulässige Varianten:

```text
GRADLE_RO_DEP_CACHE=/opt/gretl/gradle-ro-cache
```

oder ein explizit aus einem image-internen Template erzeugtes testspezifisches Cache-Verzeichnis.

Die Verwendung eines read-only Gradle-Caches ist eine Implementierungsentscheidung, kein Ersatz für Repository- und Artefaktverifikation.

### 6.4 Primärer Consumer-Vertrag ist versionlos

Im Runtime-Image soll der primäre Consumer-Build schreiben können:

```groovy
plugins {
    id 'ch.so.agi.gretl'
}
```

Die im Image gebündelte Version wird zentral über Plugin Management gesetzt.

Zusätzlich muss ein Test mit expliziter aktueller Version existieren:

```groovy
plugins {
    id 'ch.so.agi.gretl' version '<current-project-version>'
}
```

Eine falsche explizite Version muss offline verständlich fehlschlagen.

### 6.5 Keine dynamischen Versionen

Im Offline-Image-Vertrag sind unzulässig:

```text
latest.release
latest.integration
5.+
[5.0,6.0)
```

Alle image-internen Dependencies müssen exakt versioniert sein.

### 6.6 Keine benötigten Remote-Repositories

Das Init-Script darf Remote-Repositories für optionale Online-Nutzung ergänzen, aber der Offline-Gate darf sie nicht benötigen.

Für den Offline-Profilmodus soll bevorzugt eine explizite Repository-Policy gelten:

```text
GRETL_OFFLINE_IMAGE=true
```

In diesem Modus werden nur image-interne Repositories registriert.

Dadurch wird verhindert, dass ein zukünftiger Test ohne `--network none` unbemerkt online erfolgreich wird.

---

## 7. Verbindlicher Gradle-Task-Graph

Nach der Umsetzung müssen mindestens folgende Root-Tasks existieren:

```text
cleanRuntimeImageOfflineState
prepareRuntimeImageOfflineDistribution
verifyRuntimeImageOfflineDistribution
buildRuntimeImageForOfflineTest
verifyRuntimeImageOfflinePrerequisites
runtimeImageOfflineTest
```

### 7.1 `cleanRuntimeImageOfflineState`

Verantwortung:

- löscht nur projektspezifische Offline-Staging-Verzeichnisse;
- löscht keine globalen Docker-Caches;
- löscht keine fremden Images;
- löscht keine Host-Gradle-Caches;
- ist idempotent.

Vorgeschlagene Outputs:

```text
build/runtime-image/offline/
build/runtime-image/offline-test/
```

### 7.2 `prepareRuntimeImageOfflineDistribution`

Verantwortung:

- publiziert Core und GeoTools für das Image;
- sammelt alle benötigten Offline-Artefakte;
- erzeugt gegebenenfalls den deterministischen Dependency-Cache;
- erzeugt Manifeste;
- erzeugt das versionierte Init-Script;
- erzeugt `build.info`;
- erzeugt den Docker-Buildkontext.

### 7.3 `verifyRuntimeImageOfflineDistribution`

Verantwortung:

- prüft Maven-Repository und Cache vor dem Docker-Build;
- prüft Marker, Implementierung und Dependency-Closure;
- prüft DuckDB-Extension-Artefakte;
- prüft GeoTools-Worker-Inhalt;
- prüft verbotene Dateien;
- prüft Versionskonsistenz.

### 7.4 `buildRuntimeImageForOfflineTest`

Verantwortung:

- hängt von `verifyRuntimeImageOfflineDistribution` ab;
- baut ein lokales Testimage;
- verwendet einen testspezifischen Tag;
- schreibt die Image-ID in eine Datei;
- erzeugt ein Descriptor-JSON;
- verwendet kein Registry-Push.

Vorgeschlagene Dateien:

```text
build/runtime-image/offline-test/image-id.txt
build/runtime-image/offline-test/image-descriptor.json
```

### 7.5 `verifyRuntimeImageOfflinePrerequisites`

Prüft mindestens:

```text
docker version
docker info
docker image inspect <image-id>
```

Fehlendes Docker führt zu einem klaren Fehler.

### 7.6 `runtimeImageOfflineTest`

Der Root-Task:

- hat `group = 'verification'`;
- hängt von `buildRuntimeImageForOfflineTest` ab;
- hängt von `verifyRuntimeImageOfflinePrerequisites` ab;
- führt ausschliesslich die Offline-Image-Testsource-Sets aus;
- schreibt eigene Reports;
- ist CI-Gate.

### 7.7 Race-Condition-Regeln

Die Tasks müssen auch unter:

```bash
./gradlew runtimeImageOfflineTest --parallel
```

deterministisch funktionieren.

Insbesondere dürfen nicht mehrere Publikations-Tasks jeweils dasselbe Repository vorher löschen.

Das Bereinigen ist genau einmal einem vorbereitenden Lifecycle-Task zugeordnet.

Publikations-Tasks laufen nach diesem Clean-Task, löschen das Repository aber nicht selbst.

---

## 8. Empfohlene Build-Task-Klassen

Komplexe Docker- und Manifestlogik darf nicht ausschliesslich in `doLast`-Closures liegen.

Bevorzugt werden eigene Taskklassen in `buildSrc` oder einem internen Build-Logic-Modul.

### 8.1 `BuildRuntimeImageTask`

```java
public abstract class BuildRuntimeImageTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getBuildContext();

    @Input
    public abstract Property<String> getImageTag();

    @Input
    public abstract Property<String> getDockerExecutable();

    @OutputFile
    public abstract RegularFileProperty getImageIdFile();

    @OutputFile
    public abstract RegularFileProperty getDescriptorFile();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @TaskAction
    public void buildImage();
}
```

`buildImage()` muss sinngemäss ausführen:

```text
docker build
--iidfile <absolute-output-file>
--tag <test-tag>
<context>
```

Nach dem Build:

1. Image-ID lesen;
2. Format validieren;
3. `docker image inspect` ausführen;
4. Descriptor schreiben;
5. Tag-zu-ID-Zuordnung prüfen.

### 8.2 `VerifyOfflineDistributionTask`

```java
public abstract class VerifyOfflineDistributionTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getMavenRepository();

    @Optional
    @InputDirectory
    public abstract DirectoryProperty getReadOnlyGradleCache();

    @InputFile
    public abstract RegularFileProperty getArtifactManifest();

    @Input
    public abstract Property<String> getGretlVersion();

    @Input
    public abstract Property<String> getGradleVersion();

    @TaskAction
    public void verify();
}
```

### 8.3 `GenerateRuntimeInitScriptTask`

```java
public abstract class GenerateRuntimeInitScriptTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getTemplate();

    @Input
    public abstract Property<String> getGretlVersion();

    @Input
    public abstract Property<String> getRepositoryPath();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void generate();
}
```

Die Version wird zur Buildzeit eingesetzt.

Ein veraltbarer hart codierter Default ist verboten.

### 8.4 Configuration-Cache-Anforderungen

Die Tasks sollen:

- Provider API verwenden;
- keine `Project`-Instanz in `@TaskAction` auslesen;
- keine nicht serialisierbaren Closures in Task-Actions verwenden;
- Inputs und Outputs deklarieren;
- keine implizite globale Umgebung mutieren.

---

## 9. Dateisystemvertrag des finalen Images

Bevorzugtes Layout:

```text
/opt/gretl/
├── repository/
│   └── Maven-Repository-Inhalt
├── gradle-ro-cache/
│   └── modules-2/
├── extensions/
│   └── DuckDB-Extensions
├── manifests/
│   ├── offline-artifacts.json
│   ├── dependency-closure.json
│   └── checksums.sha256
├── init/
│   └── gretl.init.gradle
└── build.info

/usr/local/bin/gretl
/home/gradle/project
```

Die exakten Pfade dürfen angepasst werden.

Es muss jedoch eine zentrale Klasse oder Konfiguration geben, welche die Pfade definiert.

### 9.1 Dateirechte

Alle image-internen Offline-Distributionsdateien müssen:

- vom Runtime-Benutzer lesbar sein;
- nicht vom Runtime-Benutzer veränderbar sein;
- keine World-Writable-Artefakte enthalten;
- keine Credentials enthalten.

### 9.2 Verbotene Inhalte

Im finalen Image nicht zulässig:

- `*-sources.jar`;
- `*-javadoc.jar`;
- Testklassen;
- `gretl-test-support`;
- Host-Gradle-Lockdateien;
- `.m2/settings.xml`;
- `gradle.properties` mit Credentials;
- absolute Checkoutpfade;
- Build-Scan-Keys;
- SSH-Schlüssel;
- Registry-Credentials;
- private Zertifikate, sofern nicht explizit Produktbestandteil;
- fremde lokale Repository-Inhalte.

---

## 10. Init-Script-Vertrag

Das Image verwendet ein generiertes oder versioniertes Init-Script.

### 10.1 Verantwortlichkeiten

Das Init-Script darf:

- die image-interne Plugin-Repository-URL registrieren;
- die gebündelte GRETL-Version für die zwei GRETL-Plugin-IDs festlegen;
- image-interne Dependency-Repositories registrieren;
- im Offline-Profil Remote-Repositories unterdrücken;
- klare Diagnosedaten ausgeben, wenn eine Debug-Property gesetzt ist.

Das Init-Script darf nicht:

- `mavenLocal()` benötigen;
- alle JARs pauschal auf den Buildscript-Classpath legen;
- auf den Source-Checkout zeigen;
- eine falsche statische GRETL-Version enthalten;
- Secrets ausgeben;
- Consumer-Repositories unkontrolliert überschreiben, sofern dies nicht explizite Offline-Policy ist.

### 10.2 Versionierung

Die Version wird aus dem Gradle-Projekt bereitgestellt.

Beispielhaft:

```groovy
def bundledGretlVersion = '5.0.0-SNAPSHOT'
```

Diese Zeile wird generiert und getestet.

### 10.3 Plugin Management

Sinngemäss:

```groovy
settingsEvaluated { settings ->
    settings.pluginManagement {
        repositories {
            maven {
                name = 'gretlImageRepository'
                url = uri('/opt/gretl/repository')
                metadataSources {
                    gradleMetadata()
                    mavenPom()
                    artifact()
                }
            }
        }
        plugins {
            id 'ch.so.agi.gretl' version bundledGretlVersion
            id 'ch.so.agi.gretl.geotools' version bundledGretlVersion
        }
    }
}
```

Falls zusätzliche Repositories für Online-Nutzung unterstützt werden, werden sie im Offline-Profil nicht benötigt.

### 10.4 Dependency Repositories

`pluginManagement.repositories` und Projekt-Dependency-Repositories sind getrennte Auflösungsbereiche.

Das Init-Script muss beide korrekt konfigurieren.

Beispielhaft:

```groovy
gradle.beforeProject { project ->
    project.repositories {
        maven {
            name = 'gretlImageDependencies'
            url = uri('/opt/gretl/repository')
        }
    }
}
```

Die konkrete Lösung darf `dependencyResolutionManagement` verwenden, sofern sie mit Groovy-Consumer-Builds funktioniert.

### 10.5 Offline-Policy

Eine zentrale System Property oder Environment Variable:

```text
gretl.image.offline=true
```

beziehungsweise:

```text
GRETL_IMAGE_OFFLINE=true
```

steuert den strikten Offline-Modus.

Im strikten Offline-Modus muss der Init-Script-Diagnoseoutput die aktive Policy nennen, ohne Secrets auszugeben.

---

## 11. Launcher-Vertrag

Der Launcher bleibt ein dünner Wrapper um das im Image installierte Gradle.

Beispiel:

```sh
#!/bin/sh
set -eu

exec gradle "$@"   --init-script /opt/gretl/init/gretl.init.gradle   --console=plain
```

Der Launcher darf nicht global erzwingen:

```text
--offline
--no-daemon
```

Diese Optionen werden durch den konkreten Offline-Testaufruf gesetzt.

Der Launcher muss Argumente unverändert weiterreichen.

Tests müssen mindestens folgende Argumente abdecken:

- `--offline`;
- `--no-daemon`;
- `--project-dir`;
- `--stacktrace`;
- `--info`;
- `-P...`;
- `-D...`;
- ein oder mehrere Tasknamen.

---

## 12. Neues oder wiederverwendetes internes Testmodul

Falls noch nicht vorhanden, erzeuge:

```text
gretl-test-support
```

Das Modul ist nicht publiziert und nicht Bestandteil des Images.

### 12.1 Build-Konfiguration

```groovy
plugins {
    id 'java-library'
}

dependencies {
    api gradleTestKit()
    api "org.junit.jupiter:junit-jupiter-api:${junitVersion}"

    testImplementation "org.junit.jupiter:junit-jupiter-params:${junitVersion}"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:${junitVersion}"
}
```

Nur klar notwendige zusätzliche Testbibliotheken verwenden.

### 12.2 Source Set

Erzeuge ein separates Source Set:

```text
src/runtimeImageOfflineTest/java
src/runtimeImageOfflineTest/resources
```

Konfiguration sinngemäss:

```groovy
sourceSets {
    runtimeImageOfflineTest {
        java.srcDir 'src/runtimeImageOfflineTest/java'
        resources.srcDir 'src/runtimeImageOfflineTest/resources'
        compileClasspath += sourceSets.main.output
        runtimeClasspath += output + compileClasspath
    }
}
```

### 12.3 Testtask

```groovy
tasks.register('runtimeImageOfflineTest', Test) {
    group = 'verification'
    description = 'Runs black-box offline tests against the GRETL runtime image.'
    testClassesDirs = sourceSets.runtimeImageOfflineTest.output.classesDirs
    classpath = sourceSets.runtimeImageOfflineTest.runtimeClasspath
    useJUnitPlatform()
    systemProperty 'gretl.runtime.image.descriptor',
            rootProject.layout.buildDirectory
                    .file('runtime-image/offline-test/image-descriptor.json')
                    .get().asFile.absolutePath
    reports.html.outputLocation =
            layout.buildDirectory.dir('reports/tests/runtimeImageOfflineTest')
    reports.junitXml.outputLocation =
            layout.buildDirectory.dir('test-results/runtimeImageOfflineTest')
}
```

Die tatsächliche Konfiguration soll Provider-basiert erfolgen.

### 12.4 Publikationsschutz

`gretl-test-support` darf:

- nicht in `publishSnapshots` erscheinen;
- nicht im Runtime-Maven-Repository publiziert werden;
- nicht in Produktions-POMs erscheinen;
- nicht ins Image kopiert werden.

---

## 13. Gemeinsames Ausführungsmodell

Wenn bereits eine Executor-Abstraktion aus dem Published-Artifact- oder Runtime-E2E-Auftrag existiert, muss sie erweitert werden.

### 13.1 `GretlExecutionMode`

```java
public enum GretlExecutionMode {
    TESTKIT_CLASSPATH,
    PUBLISHED_ARTIFACT,
    RUNTIME_IMAGE_OFFLINE
}
```

Weitere Modi dürfen existieren.

### 13.2 `GretlBuildExecutor`

```java
public interface GretlBuildExecutor {

    GretlBuildResult execute(GretlBuildRequest request);

    GretlBuildResult executeAndExpectFailure(GretlBuildRequest request);
}
```

### 13.3 `GretlBuildRequest`

```java
public record GretlBuildRequest(
        Path projectDirectory,
        List<String> arguments,
        Map<String, String> environment,
        Set<String> secrets,
        Duration timeout,
        String displayName) {

    public GretlBuildRequest {
        Objects.requireNonNull(projectDirectory);
        Objects.requireNonNull(arguments);
        Objects.requireNonNull(environment);
        Objects.requireNonNull(secrets);
        Objects.requireNonNull(timeout);
        Objects.requireNonNull(displayName);
    }
}
```

### 13.4 `GretlBuildResult`

```java
public record GretlBuildResult(
        int exitCode,
        String stdout,
        String stderr,
        Duration duration,
        String imageId,
        String containerId,
        Path projectDirectory,
        List<String> maskedCommand) {

    public String output() {
        return stdout + System.lineSeparator() + stderr;
    }

    public void assertSuccess();

    public void assertFailure();
}
```

---

## 14. `RuntimeImageDescriptor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/runtime/RuntimeImageDescriptor.java
```

```java
public record RuntimeImageDescriptor(
        String imageId,
        String imageTag,
        String gretlVersion,
        String gradleVersion,
        String javaVersion,
        String repositoryPath,
        Optional<String> readOnlyCachePath,
        String initScriptPath,
        String buildCommit) {

    public static RuntimeImageDescriptor read(Path jsonFile);

    public void validate();

    public boolean usesReadOnlyCache();

    public String shortImageId();
}
```

### 14.1 Validierung

`validate()` prüft:

- `imageId` beginnt mit `sha256:`;
- Tag ist nicht leer;
- GRETL-Version ist nicht leer;
- Gradle-Version ist nicht leer;
- Repository-Pfad ist absolut;
- Init-Script-Pfad ist absolut;
- optionale Cache-Pfade sind absolut;
- keine Werte enthalten Zeilenumbrüche;
- Descriptor-Datei ist kein Symlink ausserhalb des Build-Verzeichnisses.

---

## 15. `OfflineImageRunOptions`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/runtime/OfflineImageRunOptions.java
```

```java
public record OfflineImageRunOptions(
        boolean networkNone,
        boolean gradleOffline,
        boolean noDaemon,
        boolean readOnlyRootFilesystem,
        boolean freshWritableGradleHome,
        boolean useBundledReadOnlyCache,
        Map<String, String> environment,
        Map<Path, ContainerMount> mounts,
        Duration timeout) {

    public static OfflineImageRunOptions strict();

    public OfflineImageRunOptions withoutBundledCache();

    public OfflineImageRunOptions withEnvironment(String key, String value);

    public OfflineImageRunOptions withTimeout(Duration value);
}
```

### 15.1 `strict()`

Muss setzen:

```text
networkNone = true
gradleOffline = true
noDaemon = true
readOnlyRootFilesystem = true
freshWritableGradleHome = true
useBundledReadOnlyCache = true
```

Falls kein read-only Cache verwendet wird, ist `useBundledReadOnlyCache=false` zulässig; die übrigen Garantien bleiben bestehen.

---

## 16. `ContainerMount`

```java
public record ContainerMount(
        Path hostPath,
        String containerPath,
        MountAccess access,
        MountType type) {
}
```

```java
public enum MountAccess {
    READ_ONLY,
    READ_WRITE
}
```

```java
public enum MountType {
    BIND,
    TMPFS
}
```

Factory-Methoden:

```java
public static ContainerMount readOnlyBind(Path host, String target);
public static ContainerMount readWriteBind(Path host, String target);
public static ContainerMount tmpfs(String target);
```

---

## 17. Prozessausführung

### 17.1 `ExternalProcessRunner`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/process/ExternalProcessRunner.java
```

```java
public final class ExternalProcessRunner {

    public ProcessResult execute(ProcessRequest request);

    private StreamCollector collect(InputStream stream);

    private void terminateProcessTree(Process process);

    private ProcessResult timedOutResult(
            ProcessRequest request,
            String stdout,
            String stderr,
            Duration duration);
}
```

### 17.2 `ProcessRequest`

```java
public record ProcessRequest(
        List<String> command,
        Path workingDirectory,
        Map<String, String> environment,
        Duration timeout,
        Set<String> secrets,
        String displayName) {
}
```

### 17.3 `ProcessResult`

```java
public record ProcessResult(
        List<String> command,
        List<String> maskedCommand,
        int exitCode,
        String stdout,
        String stderr,
        Duration duration,
        boolean timedOut) {
}
```

### 17.4 Anforderungen

- `ProcessBuilder` verwenden;
- keine Shell-String-Konkatenation;
- stdout und stderr parallel lesen;
- vollständige Ausgabe erfassen;
- UTF-8 verwenden;
- Timeout erzwingen;
- Prozessbaum bei Timeout beenden;
- Interrupt-Status wiederherstellen;
- Secrets maskieren;
- Fehlerdiagnose darf das Originalkommando nicht unmaskiert enthalten.

---

## 18. Docker-Abstraktion

### 18.1 `DockerCli`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/docker/DockerCli.java
```

```java
public final class DockerCli {

    public DockerVersion version();

    public String createContainer(DockerCreateRequest request);

    public DockerContainerInspection inspectContainer(String containerId);

    public ProcessResult startAndAttach(
            String containerId,
            Duration timeout,
            Set<String> secrets);

    public int waitForContainer(String containerId, Duration timeout);

    public String logs(String containerId);

    public void removeForce(String containerId);

    public DockerImageInspection inspectImage(String imageId);
}
```

### 18.2 Warum `docker create` statt ausschliesslich `docker run`

Der Offline-Test soll den Container vor dem Start inspizieren können.

Ablauf:

1. `docker create`;
2. Container-ID lesen;
3. `docker inspect`;
4. NetworkMode `none` prüfen;
5. Image-ID prüfen;
6. Mounts prüfen;
7. Environment prüfen;
8. Container starten;
9. Exitcode und Logs erfassen;
10. Container entfernen.

Damit wird die Isolationskonfiguration nicht nur aus der erzeugten Kommandozeile abgeleitet.

### 18.3 `DockerCreateRequest`

```java
public record DockerCreateRequest(
        String imageId,
        String containerName,
        List<String> command,
        Map<String, String> environment,
        List<ContainerMount> mounts,
        boolean networkNone,
        boolean readOnlyRootFilesystem,
        String workingDirectory) {
}
```

### 18.4 `DockerContainerInspection`

```java
public record DockerContainerInspection(
        String id,
        String imageId,
        String networkMode,
        String user,
        boolean readOnlyRootFilesystem,
        Map<String, DockerMountInspection> mounts,
        Map<String, String> environment) {

    public void assertStrictOffline(RuntimeImageDescriptor descriptor);
}
```

`assertStrictOffline()` prüft mindestens:

- `networkMode.equals("none")`;
- Image-ID entspricht Descriptor;
- Root-Filesystem ist read-only, falls verlangt;
- Projektmount ist vorhanden;
- Projektmount ist read-write;
- kein Host-Gradle-Home ist gemountet;
- kein Host-Maven-Home ist gemountet;
- frisches Gradle-Home zeigt auf TMPFS oder testspezifischen Mount;
- `GRADLE_RO_DEP_CACHE` zeigt nur auf einen image-internen Pfad.

---

## 19. `RuntimeImageOfflineExecutor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/RuntimeImageOfflineExecutor.java
```

```java
public final class RuntimeImageOfflineExecutor
        implements GretlBuildExecutor {

    public RuntimeImageOfflineExecutor(
            RuntimeImageDescriptor descriptor,
            DockerCli docker,
            OfflineContainerNameFactory nameFactory,
            SecretMasker secretMasker);

    @Override
    public GretlBuildResult execute(GretlBuildRequest request);

    @Override
    public GretlBuildResult executeAndExpectFailure(
            GretlBuildRequest request);

    DockerCreateRequest createRequest(
            GretlBuildRequest request,
            OfflineImageRunOptions options);

    List<String> gradleArguments(GretlBuildRequest request);

    Map<String, String> containerEnvironment(
            GretlBuildRequest request,
            OfflineImageRunOptions options);

    List<ContainerMount> containerMounts(
            GretlBuildRequest request,
            OfflineImageRunOptions options);

    void verifyInspection(DockerContainerInspection inspection);

    GretlBuildResult result(
            GretlBuildRequest request,
            String containerId,
            ProcessResult process);
}
```

### 19.1 Zwingende Gradle-Argumente

Der Offline-Executor ergänzt:

```text
--offline
--no-daemon
--console=plain
--stacktrace
```

`--rerun-tasks` nur dort, wo kein Up-to-date-Verhalten geprüft wird.

### 19.2 Zwingende Containeroptionen

- Image-ID statt nur Tag;
- `--network none`;
- `--read-only`;
- Projekt als read-write Bind-Mount;
- frisches Gradle-Home als TMPFS;
- `/tmp` als TMPFS;
- testspezifischer Containername;
- kein Docker-Socket;
- keine privileged-Option;
- keine Host-PID-/IPC-Namespace-Freigabe.

### 19.3 Environment

Mindestens:

```text
GRADLE_USER_HOME=/work/gradle-home
GRETL_IMAGE_OFFLINE=true
```

Optional:

```text
GRADLE_RO_DEP_CACHE=/opt/gretl/gradle-ro-cache
```

### 19.4 Cleanup

`execute()` verwendet `try/finally`.

Bei jedem Ausgang:

```text
docker rm -f <container>
```

Cleanup-Fehler werden als suppressed exception angefügt und verdecken nicht die primäre Ursache.

---

## 20. `OfflineContainerNameFactory`

```java
public final class OfflineContainerNameFactory {

    public String create(String displayName);

    String sanitize(String value);

    String shortHash(String value);
}
```

Format:

```text
gretl-offline-<sanitized-test>-<8-char-hash>
```

Maximallänge und Docker-Zeichenregeln beachten.

Keine Zufallswerte ohne reproduzierbaren Test.

---

## 21. Testprojekt-Builder

### 21.1 `GradleTestProject`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/project/GradleTestProject.java
```

```java
public final class GradleTestProject {

    public static GradleTestProject create(Path directory);

    public GradleTestProject settingsGroovy(String content);

    public GradleTestProject buildGroovy(String content);

    public GradleTestProject textFile(
            String relativePath,
            String content);

    public GradleTestProject binaryFile(
            String relativePath,
            byte[] content);

    public GradleTestProject copyResource(
            Class<?> owner,
            String resource,
            String target);

    public GradleTestProject copyResourceTree(
            Class<?> owner,
            String resourceDirectory,
            String targetDirectory);

    public Path path(String relativePath);

    public void assertContainsOnlyExpectedTopLevelFiles(
            Set<String> expected);
}
```

### 21.2 Settings-Datei

Minimal:

```groovy
rootProject.name = 'offline-core-test'
```

Das Consumer-Projekt darf die GRETL-Repository-URL nicht selbst kennen müssen.

### 21.3 Keine Wrapper-Dateien im Consumer

Die Offline-Testprojekte benötigen keinen eigenen Gradle Wrapper.

Sie verwenden das im Image installierte Gradle.

### 21.4 Keine Source-Referenzen

Die generierten Dateien dürfen nicht enthalten:

```text
includeBuild
mavenLocal
flatDir
withPluginClasspath
build/classes
build/resources
gretl-core/build
gretl-geotools/build
```

Implementiere eine statische Fixture-Prüfung.

---

## 22. Artefaktmanifest

### 22.1 `OfflineArtifactManifest`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/offline/OfflineArtifactManifest.java
```

```java
public record OfflineArtifactManifest(
        String gretlVersion,
        String gradleVersion,
        Instant generatedAt,
        List<OfflineArtifact> artifacts,
        List<OfflinePluginMarker> pluginMarkers,
        List<DuckDbExtensionArtifact> duckDbExtensions,
        String generatorVersion) {

    public static OfflineArtifactManifest read(Path path);

    public void validate();

    public Optional<OfflineArtifact> find(
            String group,
            String module,
            String version);
}
```

### 22.2 `OfflineArtifact`

```java
public record OfflineArtifact(
        String group,
        String module,
        String version,
        String classifier,
        String extension,
        String relativePath,
        String sha256,
        long size,
        ArtifactRole role) {
}
```

### 22.3 `ArtifactRole`

```java
public enum ArtifactRole {
    PLUGIN_MARKER_POM,
    IMPLEMENTATION_JAR,
    IMPLEMENTATION_POM,
    GRADLE_MODULE_METADATA,
    RUNTIME_DEPENDENCY,
    GEOTOOLS_WORKER_DEPENDENCY,
    DUCKDB_EXTENSION,
    MAVEN_METADATA
}
```

### 22.4 Manifestregeln

- Relative Pfade verwenden;
- SHA-256 für jede Datei;
- Dateigrösse grösser null;
- keine Duplikate derselben logischen Koordinate;
- keine Source-/Javadoc-Artefakte;
- jedes Manifestartefakt existiert;
- jede relevante Repository-Datei ist manifestiert oder explizit als Metadatei klassifiziert.

---

## 23. Dependency-Closure-Verifikation

### 23.1 `OfflineDependencyClosureVerifier`

```java
public final class OfflineDependencyClosureVerifier {

    public VerificationReport verify(
            Path repository,
            OfflineArtifactManifest manifest,
            Set<ModuleCoordinate> roots);

    ResolvedOfflineModule readModule(
            Path repository,
            ModuleCoordinate coordinate);

    Set<ModuleCoordinate> runtimeDependencies(
            ResolvedOfflineModule module);

    void verifyArtifactFiles(
            ResolvedOfflineModule module);

    void verifyNoDynamicVersions(
            ResolvedOfflineModule module);

    void verifyNoExternalFileReferences(
            ResolvedOfflineModule module);
}
```

### 23.2 Wurzelkoordinaten

Mindestens:

```text
Plugin-Marker ch.so.agi.gretl
Plugin-Marker ch.so.agi.gretl.geotools
Implementierung ch.so.agi:gretl-core:<version>
Implementierung ch.so.agi:gretl-geotools:<version>
```

### 23.3 Laufzeitgraph

Der Verifier traversiert den Runtime-Dependency-Graphen.

Für jede Koordinate:

- Metadaten vorhanden;
- Binärartefakt vorhanden, falls erwartet;
- exakte Version;
- keine dynamische Version;
- keine Dateiabhängigkeit auf Hostpfad;
- keine Projektabhängigkeit;
- keine fehlende transitive Kante.

### 23.4 Mindestens explizit zu prüfende Dependencies

Core:

```text
commons-io:commons-io
org.xerial:sqlite-jdbc
org.postgresql:postgresql
org.duckdb:duckdb_jdbc
```

Zusätzlich alle tatsächlichen Runtime-Abhängigkeiten des aktuellen Builds.

GeoTools:

- Implementierungs-JAR;
- Worker-Runtime-JAR;
- `gt-main`;
- `gt-shapefile`;
- `gt-geotiff`;
- `gt-coverage`;
- `gt-epsg-hsql`;
- alle aktuell benötigten Worker-Libraries.

### 23.5 SNAPSHOT-Metadaten

Bei Snapshot-Versionen prüfen:

- `maven-metadata.xml` existiert;
- `timestamp` und `buildNumber` konsistent;
- Marker-POM verweist logisch auf die aktuelle Snapshot-Version;
- physische Dateien existieren;
- keine ältere physische Snapshot-Datei wird ausgewählt;
- Manifest enthält die tatsächlich verwendeten Dateien.

---

## 24. Image-Contract-Testklasse

Datei:

```text
gretl-test-support/src/runtimeImageOfflineTest/java/ch/so/agi/gretl/test/offline/RuntimeImageOfflineContractTest.java
```

Methoden:

```java
@Test
void imageIdMatchesCurrentBuildDescriptor();

@Test
void bundledGretlVersionMatchesProjectVersion();

@Test
void imageContainsStructuredPluginRepository();

@Test
void imageContainsCoreAndGeoToolsPluginMarkers();

@Test
void imageContainsImplementationArtifacts();

@Test
void imageContainsCompleteDependencyManifest();

@Test
void imageContainsNoSourceOrJavadocArtifacts();

@Test
void imageContainsNoTestSupportModule();

@Test
void imageContainsNoCredentials();

@Test
void imageOfflineRepositoryIsReadOnlyForRuntimeUser();

@Test
void initScriptDoesNotRequireMavenLocal();

@Test
void initScriptDoesNotInjectFlatDirectoryClasspath();

@Test
void launcherForwardsOfflineArguments();

@Test
void readOnlyGradleCacheMatchesBundledGradleVersion();

@Test
void buildInfoMatchesImageDescriptor();
```

### 24.1 Image-Dateien lesen

Dateien werden über einen kurzlebigen Container gelesen, beispielsweise:

```text
docker create <image-id>
docker cp <container>:/opt/gretl/... <temp-dir>
docker rm <container>
```

Alternativ darf `docker run --entrypoint ...` verwendet werden.

Der Test darf keine Image-Datei aus dem Buildkontext statt aus dem Image prüfen.

---

## 25. Strikte Isolations-Testklasse

Datei:

```text
RuntimeImageOfflineIsolationTest.java
```

Methoden:

```java
@Test
void containerUsesNetworkModeNone();

@Test
void containerUsesExactBuiltImageId();

@Test
void writableGradleHomeStartsEmpty();

@Test
void noHostGradleHomeIsMounted();

@Test
void noHostMavenRepositoryIsMounted();

@Test
void noSourceCheckoutIsAvailableInsideContainer();

@Test
void rootFilesystemIsReadOnly();

@Test
void onlyProjectAndTmpfsLocationsAreWritable();

@Test
void independentRunsUseDifferentWritableGradleHomes();

@Test
void secondRunDoesNotReuseFirstRunWritableCache();

@Test
void networkCanaryCannotReachExternalAddress();

@Test
void dnsCanaryCannotResolveExternalHost();
```

### 25.1 Leeres Gradle-Home beweisen

Vor Gradle-Start erzeugt der Executor ein kleines Bootstrap-Script oder einen Entry-Point-Wrapper, der prüft:

```text
GRADLE_USER_HOME existiert
GRADLE_USER_HOME ist beschreibbar
GRADLE_USER_HOME enthält keine Dateien ausser erlaubten Testmarkern
```

Die Prüfung findet im Container statt.

### 25.2 Kein Source-Checkout

Im Container dürfen nicht existieren:

```text
/workspace/gretl-next
/src/gretl-next
/checkout
/home/gradle/project/gretl-core
```

Die konkrete Liste ergänzt der Agent anhand des Mount-Modells.

---

## 26. Core-Plugin-Auflösungstests

Datei:

```text
RuntimeImageOfflineCorePluginTest.java
```

Methoden:

```java
@Test
void appliesVersionlessCorePluginWithGroovyDsl();

@Test
void appliesExplicitCorePluginVersionWithGroovyDsl();

@Test
void wrongCorePluginVersionFailsWithoutNetworkAttempt();

@Test
void pluginApplicationDoesNotNeedConsumerRepositoryDeclaration();

@Test
void coreTaskTypesAreLoadableFromPluginClassloader();
```

### 26.1 Groovy-Consumer

```groovy
plugins {
    id 'ch.so.agi.gretl'
}

tasks.register('verifyPlugin') {
    doLast {
        println "CORE_PLUGIN_APPLIED"
    }
}
```

Zusätzlich muss mindestens ein echter GRETL-Tasktyp geladen werden.

---

## 27. Core-Fachtest: Gzip

Datei:

```text
RuntimeImageOfflineGzipTest.java
```

Methoden:

```java
@Test
void compressesFileOfflineWithGroovyDsl();

@Test
@Test
void createsNestedOutputDirectoryOffline();

@Test
void missingInputFailsClearlyOffline();
```

### 27.1 Assertions

Nach Containerende vom Host aus:

- Output existiert;
- Output ist GZIP;
- dekomprimierter Inhalt ist bytegenau gleich;
- Output liegt im gemounteten Projekt;
- kein Output ausserhalb des erwarteten Verzeichnisses;
- Fehlerfall nennt den relativen Eingabepfad;
- Log enthält keinen Downloadversuch.

---

## 28. Core-Fachtest: SQLite

Datei:

```text
RuntimeImageOfflineSqliteTest.java
```

Methoden:

```java
@Test
void executesSqlAgainstNewSqliteDatabaseOffline();

@Test
void executesMultipleSqlFilesInOrderOffline();

@Test
void transactionRollbackWorksOffline();

@Test
void sqliteJdbcDriverLoadsFromImageDistribution();
```

### 28.1 Fixture

Das Testprojekt enthält lokale SQL-Dateien.

Keine Netzwerkressource.

### 28.2 Assertions

Vom Host mit JDBC oder SQLite-Dateiparser:

- Datenbankdatei existiert;
- erwartete Tabelle existiert;
- erwartete Zeilenanzahl;
- erwartete Werte;
- Rollback hinterlässt keine Teilresultate;
- keine fremden Tabellen;
- Treiberversion optional im Diagnoseoutput.

---

## 29. GeoTools-Plugin-Auflösungstests

Datei:

```text
RuntimeImageOfflineGeoToolsPluginTest.java
```

Methoden:

```java
@Test
void appliesVersionlessGeoToolsPluginWithGroovyDsl();

@Test
void appliesExplicitGeoToolsPluginVersionWithGroovyDsl();

@Test
@Test
void defaultReadShapefileTaskIsRegisteredOffline();

@Test
void workerRuntimeStartsWithoutNetwork();

@Test
void workerClasspathContainsNoHostFiles();
```

### 29.1 Worker-Isolation

Der Test muss belegen:

- Worker-JAR stammt aus dem Plugin-Artefakt im Image;
- GeoTools-Libraries stammen aus image-internen Artefakten;
- kein Host-Test-Classpath ist gemountet;
- kein Source-Build-Verzeichnis ist sichtbar;
- Worker-Prozess benötigt keinen Download.

---

## 30. GeoTools-Fachtest: ReadShapefile

Datei:

```text
RuntimeImageOfflineReadShapefileTest.java
```

Methoden:

```java
@Test
void readsShapefileOfflineWithGroovyDsl();

@Test
@Test
void resolvesEpsgDatabaseOffline();

@Test
void invalidShapefileFailsClearlyOffline();
```

### 30.1 Fixture

Lokales, kleines Shapefile-Set:

```text
data.shp
data.shx
data.dbf
data.prj
```

### 30.2 Assertions

Nicht nur Logtext prüfen.

Bevorzugt erzeugt der Test eine maschinenlesbare Resultatdatei oder verwendet einen Task, der Output erzeugt.

Mindestens:

- Feature-Anzahl;
- Geometrietyp;
- erwartetes Attribut;
- EPSG/CRS;
- keine leere Geometrie.

Falls `ReadShapefile` aktuell nur diagnostisch loggt, darf zusätzlich Log geprüft werden; für den P0-Canary ist dann ein zweiter outputerzeugender GeoTools-Test erforderlich.

---

## 31. GeoTools-Fachtest: Raster oder Vectorize

Mindestens einer der folgenden Pfade ist zwingend:

```text
RasterReclassify
Vectorize
```

Empfohlen sind beide.

### 31.1 `RuntimeImageOfflineRasterReclassifyTest`

```java
@Test
void reclassifiesRasterOffline();

@Test
void preservesExpectedCrsOffline();

@Test
void producesExpectedRasterValuesOffline();
```

### 31.2 `RuntimeImageOfflineVectorizeTest`

```java
@Test
void vectorizesRasterOffline();

@Test
void outputGeoPackageContainsExpectedFeatures();

@Test
void outputGeometriesUseExpectedSrid();
```

### 31.3 Semantische Assertions

Raster:

- Breite;
- Höhe;
- Datentyp;
- NoData;
- ausgewählte Zellwerte;
- CRS.

GeoPackage:

- Layername;
- Feature-Anzahl;
- Geometrietyp;
- SRID;
- erwartete Klassenspalte;
- mindestens eine erwartete Geometrieausdehnung.

---

## 32. DuckDB-Offline-Test

Datei:

```text
RuntimeImageOfflineDuckDbTest.java
```

Methoden:

```java
@Test
void opensDuckDbDatabaseOffline();

@Test
void loadsSpatialExtensionOffline();

@Test
void executesSpatialFunctionOffline();

@Test
void loadsPostgresExtensionOffline();

@Test
void loadsExcelExtensionOffline();

@Test
void extensionDirectoryIsInsideImage();

@Test
void extensionsAreNotDownloadedAtRuntime();

@Test
void missingBundledExtensionFailsDeterministically();
```

### 32.1 Produktvertrag

Die aktuell im Dockerfile installierten Extensions:

```text
postgres
spatial
excel
```

müssen real geladen werden.

### 32.2 SQL-Canary

Mindestens:

```sql
LOAD spatial;
SELECT ST_AsText(ST_Point(2600000, 1200000));
```

Für jede weitere Extension eine minimale reale Operation.

### 32.3 `INSTALL` versus `LOAD`

Der Agent muss das tatsächliche DuckDB-Verhalten verifizieren.

Ziel ist:

- keine Netzwerkoperation zur Testlaufzeit;
- Extension-Dateien sind vorinstalliert;
- `LOAD` funktioniert;
- `INSTALL` wird nur getestet, wenn es gegen den lokalen Extension-Bestand ohne Netzwerk semantisch Teil des Produktvertrags ist.

Der Test darf nicht fälschlich einen Online-Installationspfad verlangen.

### 32.4 Extension-Manifest

Für jede Extension:

- Name;
- DuckDB-Version;
- Plattform;
- Pfad;
- SHA-256;
- Dateigrösse.

---

## 33. Negativ- und Mutationstests

Mutationstests sind Teil des P0-Gates.

Sie beweisen, dass der Test nicht nur wegen eines versteckten Fallbacks grün ist.

### 33.1 `RuntimeImageOfflineMutationTest`

Methoden:

```java
@Test
void removingCoreMarkerMakesCorePluginResolutionFail();

@Test
void removingCoreImplementationJarMakesBuildFail();

@Test
void removingCorePomMakesBuildFail();

@Test
void removingRequiredRuntimeDependencyMakesTaskFail();

@Test
void removingGeoToolsWorkerJarMakesGeoToolsTaskFail();

@Test
void removingEpsgDatabaseMakesCrsResolutionFail();

@Test
void removingDuckDbSpatialExtensionMakesSpatialTestFail();

@Test
void disablingBundledReadOnlyCacheStillUsesRepositoryOrFailsClearly();

@Test
void wrongBundledVersionFailsClearly();

@Test
void emptyImageRepositoryCannotFallBackToHostCache();
```

### 33.2 Mutationstechnik

Das produktive Testimage wird nicht verändert.

Erzeuge abgeleitete Mutationsimages oder schattiere einzelne image-interne Pfade über leere read-only Mounts.

Beispiel:

```text
--mount type=bind,src=<empty-dir>,dst=/opt/gretl/repository/.../marker,readonly
```

Falls Docker das Mounten einzelner Pfade nicht portabel unterstützt, baue kleine abgeleitete Testimages.

### 33.3 Erwartete Fehler

Fehlermeldung muss enthalten:

- betroffene Plugin-ID oder Koordinate;
- fehlenden Pfad oder Artefakttyp;
- Image-ID;
- Offline-Modus;
- keine Downloadempfehlung als alleinige Diagnose.

---

## 34. Cache-Poisoning-Tests

Datei:

```text
RuntimeImageOfflineCacheIsolationTest.java
```

Methoden:

```java
@Test
void hostGradleCacheCannotInfluenceOfflineRun();

@Test
void previousTestWritableCacheCannotInfluenceNextRun();

@Test
void poisonedWritableCacheDoesNotOverrideBundledArtifacts();

@Test
void stalePluginMarkerDoesNotOverrideCurrentImageMarker();

@Test
void staleSnapshotMetadataDoesNotSelectOlderArtifact();

@Test
void cacheManifestContainsNoLocksOrGcProperties();
```

### 34.1 Hostcache-Canary

Erzeuge auf dem Host einen absichtlich falschen Artefaktpfad oder Marker.

Da dieser Pfad nicht gemountet wird, darf der Container ihn nicht sehen.

### 34.2 Testlaufreihenfolge

Der Cache-Isolationstest muss auch bei umgekehrter Reihenfolge und paralleler Ausführung grün sein.

Keine `@TestMethodOrder`-Abhängigkeit.

---

## 35. Netzwerk-Canary

Datei:

```text
RuntimeImageOfflineNetworkTest.java
```

Methoden:

```java
@Test
void dockerInspectionReportsNetworkNone();

@Test
void onlyLoopbackInterfaceExists();

@Test
void externalDnsResolutionFails();

@Test
void externalTcpConnectionFails();

@Test
void pluginBuildStillSucceedsWithoutNetwork();
```

### 35.1 Canary-Ziel

Verwende kein externes System, dessen Verfügbarkeit den Test beeinflusst.

Es genügt zu prüfen, dass:

- kein Default-Gateway vorhanden ist;
- kein Nicht-Loopback-Interface vorhanden ist;
- DNS-Auflösung eines reservierten Namens fehlschlägt;
- Verbindungsversuch mit kurzem Timeout fehlschlägt.

Die eigentliche Garantie bleibt Docker `NetworkMode=none`.

---

## 36. Log- und Downloadversuchsprüfung

### 36.1 Verbotene Muster

In erfolgreichen Offline-Logs dürfen nicht erscheinen:

```text
Downloading
Download https://
services.gradle.org
plugins.gradle.org
repo.maven.apache.org
jars.sogeo.services
repo.osgeo.org
maven.geo-solutions.it
```

Die Liste wird case-insensitiv geprüft.

### 36.2 Keine alleinige Loggarantie

Die Abwesenheit dieser Strings ersetzt nicht:

- `--network none`;
- Docker-Inspection;
- frisches Gradle-Home;
- Mutationstests.

### 36.3 Erlaubte Diagnosen

Ein Log darf konfigurierte Remote-Repository-URLs nur dann nennen, wenn es sich um eine statische Debug-Ausgabe handelt und bewiesen ist, dass der Offline-Profilmodus diese nicht verwendet.

Bevorzugt werden Remote-Repositories im strikten Offline-Modus gar nicht registriert.

---

## 37. Secret-Maskierung

### 37.1 `SecretMasker`

```java
public final class SecretMasker {

    public String mask(String text, Set<String> secrets);

    public List<String> maskArguments(
            List<String> arguments,
            Set<String> secrets);

    public Map<String, String> maskEnvironment(
            Map<String, String> environment,
            Set<String> secrets);
}
```

### 37.2 Offline-Testsecrets

Obwohl die Offline-Canaries keine echten Credentials benötigen, muss ein Test mit synthetischen Properties existieren:

```text
-PdbPassword=OFFLINE_SECRET_42
```

Die Zeichenfolge darf nicht in:

- Report;
- stdout;
- stderr;
- maskiertem Kommando;
- Exception;
- Diagnoseartefakt

erscheinen.

---

## 38. Zeitlimits

Empfohlene Default-Timeouts:

| Testtyp | Timeout |
|---|---:|
| Image-Inspection | 30 Sekunden |
| Plugin-Anwendung | 2 Minuten |
| Gzip | 2 Minuten |
| SQLite | 3 Minuten |
| ReadShapefile | 4 Minuten |
| Raster/Vectorize | 6 Minuten |
| DuckDB-Extension | 4 Minuten |
| Mutationsimage-Build | 5 Minuten |

Timeouts sind konfigurierbar, aber nicht unbeschränkt.

Bei Timeout:

- Prozessbaum beenden;
- Container entfernen;
- Logs sichern;
- Test fehlschlagen;
- keine automatische Wiederholung des gesamten Tests.

---

## 39. Parallelität und Ressourcennamen

Jeder Test erhält:

- eigenes Projektverzeichnis;
- eigenen Container;
- eigenes beschreibbares Gradle-Home;
- eigenen Reportkontext;
- eindeutigen Containername.

Gemeinsam und read-only dürfen sein:

- Image-ID;
- image-interner Maven-Bestand;
- image-interner read-only Cache;
- Fixtures im Test-JAR.

Keine statischen beschreibbaren Singletons.

---

## 40. Testreports und Diagnosen

### 40.1 Reportpfade

```text
gretl-test-support/build/reports/tests/runtimeImageOfflineTest/
gretl-test-support/build/test-results/runtimeImageOfflineTest/
build/runtime-image/offline-test/diagnostics/
```

### 40.2 Diagnoseartefakte bei Fehler

Mindestens:

- Image-Descriptor;
- `docker image inspect`;
- Container-Inspection;
- maskiertes Docker-Kommando;
- stdout;
- stderr;
- Offline-Artefaktmanifest;
- Dependency-Closure-Report;
- `build.info`;
- Liste der image-internen Repository-Wurzeln;
- Testprojekt ohne Secrets;
- Checksummenreport.

Keine unmaskierten Environment-Dumps.

---

## 41. CI-Integration

Erweitere `.github/workflows/ci.yml`.

### 41.1 Reihenfolge

Mindestens:

```text
checkout
JDK 17
Gradle setup
clean check
source integration tests
publishedArtifactTest
buildRuntimeImageForOfflineTest
runtimeImageOfflineTest
upload reports
publish job
```

Der Publish-Job bleibt von einem erfolgreichen Build-Job abhängig.

### 41.2 CI-Schritt

Sinngemäss:

```yaml
- name: Build and test offline GRETL runtime image
  run: ./gradlew runtimeImageOfflineTest --stacktrace
```

### 41.3 Reports

`if: always()`:

```yaml
- uses: actions/upload-artifact@v4
  with:
    name: runtime-image-offline-test-reports
    path: |
      **/build/reports/tests/runtimeImageOfflineTest/
      **/build/test-results/runtimeImageOfflineTest/
      build/runtime-image/offline-test/diagnostics/
```

### 41.4 Keine Credentials

Offline-Image-Test benötigt keine:

- Maven-Publish-Credentials;
- Docker-Registry-Credentials;
- Cloud-Credentials;
- realen DB-Credentials.

### 41.5 Docker-Fehler

Wenn Docker nicht verfügbar ist, muss CI rot werden.

Kein stilles Skip über JUnit-Assumptions.

---

## 42. Lokale Entwicklerbefehle

Dokumentiere mindestens:

```bash
./gradlew prepareRuntimeImageOfflineDistribution
./gradlew verifyRuntimeImageOfflineDistribution
./gradlew buildRuntimeImageForOfflineTest
./gradlew runtimeImageOfflineTest
```

Zusätzlich:

```bash
./gradlew runtimeImageOfflineTest --rerun-tasks
./gradlew runtimeImageOfflineTest --parallel
```

Soweit kompatibel:

```bash
./gradlew runtimeImageOfflineTest --configuration-cache
./gradlew runtimeImageOfflineTest --configuration-cache
```

Der zweite Lauf soll den Configuration Cache wiederverwenden oder konkrete Inkompatibilitäten benennen.

### 42.1 Gezielte Tests

Beispiel:

```bash
./gradlew :gretl-test-support:runtimeImageOfflineTest   --tests '*RuntimeImageOfflineDuckDbTest'
```

### 42.2 Diagnoseproperty

Optional:

```text
-PgretlOfflineTestKeepContainers=true
```

Standard ist `false`.

Bei Aktivierung klare Warnung und explizite Cleanup-Anleitung.

---

## 43. Dokumentation

Ergänze eine Dokumentationsseite, beispielsweise:

```text
docs/development/runtime-image-offline-test.adoc
```

Inhalt:

- Beweisziel;
- Abgrenzung;
- Architektur;
- Maven-Repository;
- optionaler read-only Cache;
- Gradle-Version-Kopplung;
- Docker `network none`;
- frisches Gradle-Home;
- lokale Ausführung;
- Fehlerdiagnose;
- Mutationstests;
- CI-Gate;
- keine `gretljobs`-Migration.

### 43.1 Troubleshooting

Mindestens:

- Docker fehlt;
- Image-ID-Datei fehlt;
- Plugin-Marker fehlt;
- Snapshot-Metadaten inkonsistent;
- Dependency-Closure unvollständig;
- Gradle-Cache-Version inkompatibel;
- DuckDB-Extension fehlt;
- Bind-Mount nicht beschreibbar;
- Testcontainer blieb nach Abbruch bestehen.

---

## 44. Unit-Tests für Testinfrastruktur

Erzeuge Unit-Tests für mindestens:

### 44.1 `ExternalProcessRunnerTest`

```java
@Test
void capturesStdoutAndStderr();

@Test
void returnsExitCode();

@Test
void timesOutAndTerminatesProcessTree();

@Test
void masksSecrets();

@Test
void preservesArgumentsWithSpaces();
```

### 44.2 `RuntimeImageDescriptorTest`

```java
@Test
void readsValidDescriptor();

@Test
void rejectsMissingImageId();

@Test
void rejectsInvalidImageId();

@Test
void rejectsRelativeRepositoryPath();
```

### 44.3 `OfflineArtifactManifestTest`

```java
@Test
void validatesChecksums();

@Test
void rejectsDuplicateCoordinates();

@Test
void rejectsSourceJar();

@Test
void rejectsMissingFile();

@Test
void rejectsDynamicVersion();
```

### 44.4 `DockerContainerInspectionTest`

```java
@Test
void acceptsStrictOfflineInspection();

@Test
void rejectsBridgeNetwork();

@Test
void rejectsHostGradleMount();

@Test
void rejectsWrongImageId();

@Test
void rejectsWritableRootFilesystemWhenRequired();
```

### 44.5 `OfflineContainerNameFactoryTest`

```java
@Test
void createsValidDockerName();

@Test
void truncatesLongDisplayName();

@Test
void addsStableHash();

@Test
void removesInvalidCharacters();
```

---

## 45. Exakte P0-Testmatrix

| ID | Test | DSL | Netzwerk | Gradle-Home | Erwartung |
|---|---|---|---|---|---|
| OI-001 | Core Plugin versionlos | Groovy | none | frisch | Erfolg |
| OI-002 | Core Plugin explizit | Groovy | none | frisch | Erfolg |
| OI-005 | GeoTools versionlos | Groovy | none | frisch | Erfolg |
| OI-006 | GeoTools explizit | Groovy | none | frisch | Erfolg |
| OI-009 | Gzip | Groovy | none | frisch | fachlich korrekt |
| OI-011 | SQLite SQL | Groovy | none | frisch | fachlich korrekt |
| OI-012 | ReadShapefile | Groovy | none | frisch | fachlich korrekt |
| OI-014 | Raster/Vectorize | Groovy | none | frisch | fachlich korrekt |
| OI-015 | DuckDB spatial | Groovy | none | frisch | fachlich korrekt |
| OI-016 | DuckDB postgres Extension | Groovy | none | frisch | Extension lädt |
| OI-017 | DuckDB excel Extension | Groovy | none | frisch | Extension lädt |
| OI-018 | falsche Plugin-Version | Groovy | none | frisch | klarer Fehler |
| OI-019 | fehlender Marker | Groovy | none | frisch | klarer Fehler |
| OI-020 | fehlende Runtime-Dependency | Groovy | none | frisch | klarer Fehler |
| OI-021 | fehlender Worker | Groovy | none | frisch | klarer Fehler |
| OI-022 | fehlende DuckDB Extension | Groovy | none | frisch | klarer Fehler |
| OI-023 | kein Hostcache-Mount | n/a | none | frisch | verifiziert |
| OI-024 | unabhängige Läufe | Groovy | none | je frisch | keine Cachekopplung |
| OI-025 | Netzwerk-Canary | n/a | none | frisch | Zugriff unmöglich |

Alle 25 IDs sind verbindlich.

---

## 46. Schrittweise Umsetzung

### Phase 1: Ist-Zustand und Architektur

1. Repository lesen.
2. Runtime-Image-Dateien lesen.
3. aktuelle Plugin-Publikationen untersuchen.
4. Published-Artifact-Testinfrastruktur untersuchen.
5. Gradle-Version und Cacheformat bestätigen.
6. entscheiden, ob lokales Maven-Repository allein genügt oder zusätzlicher read-only Cache nötig ist.
7. Entscheidung als Architecture Decision Record dokumentieren.

### Phase 2: Distribution

1. strukturiertes Offline-Repository erzeugen;
2. Dependency-Closure vervollständigen;
3. Manifest erzeugen;
4. Checksummen erzeugen;
5. Init-Script generieren;
6. DuckDB-Extensions vorbereiten;
7. Docker-Kontext erzeugen;
8. Distribution verifizieren.

### Phase 3: Image

1. Dockerfile anpassen;
2. read-only Pfade einrichten;
3. Runtime-Benutzer prüfen;
4. Image mit `--iidfile` bauen;
5. Descriptor erzeugen;
6. Image inspizieren.

### Phase 4: Testinfrastruktur

1. Prozess-Runner;
2. Docker-CLI;
3. Descriptor;
4. Manifestparser;
5. Offline-Executor;
6. Projekt-Builder;
7. Secret-Masker;
8. Unit-Tests.

### Phase 5: Positive Tests

1. Plugin-Auflösung;
2. Groovy;
4. Gzip;
5. SQLite;
6. GeoTools;
7. DuckDB.

### Phase 6: Negative Tests

1. Marker-Mutation;
2. JAR-Mutation;
3. Dependency-Mutation;
4. Worker-Mutation;
5. Extension-Mutation;
6. Cache-Isolation;
7. falsche Version.

### Phase 7: CI und Dokumentation

1. Root-Task;
2. CI-Gate;
3. Reportupload;
4. Entwicklerdokumentation;
5. finaler vollständiger Lauf.

---

## 47. Verbotene Abkürzungen

Nicht zulässig:

- nur `docker build`;
- nur `gradle tasks`;
- nur Plugin-Anwendung ohne realen Task;
- nur ein Gzip-Test;
- nur Logassertions;
- Netzwerk bleibt aktiv;
- `--offline` fehlt;
- Host-Gradle-Cache wird gemountet;
- `mavenLocal()` wird benötigt;
- Source-Checkout wird gemountet;
- `withPluginClasspath()` im Offline-Image-Modus;
- Test gegen Registry-Image;
- Test gegen Tag ohne Image-ID-Verifikation;
- `flatDir` als ungeprüfter Hauptmechanismus;
- vorheriger Online-Lauf seedet den Testcache;
- Tests laufen in definierter Reihenfolge;
- fehlende Tests werden mit `@Disabled` markiert;
- Docker-Fehlen führt zu Skip;
- komplette Testtask-Retries;
- Mutationstests werden als optional behandelt;
- `gretljobs` wird migriert oder als Fixture kopiert;
- nur Groovy DSL;
- nur Core ohne GeoTools;
- DuckDB-Extensions werden nicht real geladen.

---

## 48. Definition of Done

Die Aufgabe ist nur abgeschlossen, wenn alle folgenden Punkte erfüllt sind:

- [ ] Root-Task `runtimeImageOfflineTest` existiert.
- [ ] Image wird aus aktuellem Checkout gebaut.
- [ ] Image-ID wird über `--iidfile` aufgezeichnet.
- [ ] Tests verwenden die Image-ID.
- [ ] Container-Inspection bestätigt `NetworkMode=none`.
- [ ] Gradle-Aufruf enthält `--offline`.
- [ ] Gradle-Aufruf enthält `--no-daemon`.
- [ ] Beschreibbares `GRADLE_USER_HOME` startet pro Test frisch.
- [ ] Kein Host-Gradle-Cache wird gemountet.
- [ ] Kein Host-Maven-Repository wird gemountet.
- [ ] Kein Source-Checkout ist im Container sichtbar.
- [ ] Kein `withPluginClasspath()` wird verwendet.
- [ ] `mavenLocal()` ist nicht erforderlich.
- [ ] Core Plugin versionlos mit Groovy funktioniert.
- [ ] Core Plugin explizit mit Groovy funktioniert.
- [ ] GeoTools Plugin versionlos mit Groovy funktioniert.
- [ ] GeoTools Plugin explizit mit Groovy funktioniert.
- [ ] Gzip erzeugt fachlich korrektes Output.
- [ ] SQLite-Test erzeugt fachlich korrekte Daten.
- [ ] ReadShapefile funktioniert offline.
- [ ] Raster oder Vectorize funktioniert offline.
- [ ] GeoTools Worker verwendet nur image-interne Artefakte.
- [ ] EPSG-Auflösung funktioniert offline.
- [ ] DuckDB spatial lädt und funktioniert offline.
- [ ] DuckDB postgres lädt offline.
- [ ] DuckDB excel lädt offline.
- [ ] Extension-Dateien sind manifestiert und gehasht.
- [ ] Dependency-Closure ist vollständig verifiziert.
- [ ] Plugin-Marker sind verifiziert.
- [ ] Snapshot-Metadaten sind verifiziert.
- [ ] Keine dynamischen Versionen sind enthalten.
- [ ] Keine Source-/Javadoc-JARs sind enthalten.
- [ ] Keine Credentials sind enthalten.
- [ ] Mutation «Marker fehlt» schlägt korrekt fehl.
- [ ] Mutation «Runtime-Dependency fehlt» schlägt korrekt fehl.
- [ ] Mutation «GeoTools Worker fehlt» schlägt korrekt fehl.
- [ ] Mutation «DuckDB Extension fehlt» schlägt korrekt fehl.
- [ ] Unabhängige Tests teilen keinen beschreibbaren Cache.
- [ ] Logs zeigen keine Downloads.
- [ ] Secrets werden maskiert.
- [ ] Prozesse besitzen Timeouts.
- [ ] Container werden zuverlässig entfernt.
- [ ] Tests funktionieren mit `--rerun-tasks`.
- [ ] Tests funktionieren mit `--parallel`.
- [ ] CI führt den Test vor Publikation aus.
- [ ] Reports werden immer hochgeladen.
- [ ] Keine Tests sind deaktiviert.
- [ ] Keine `gretljobs`-Migration wurde vorgenommen.
- [ ] Dokumentation ist aktualisiert.
- [ ] Abschliessender vollständiger Lauf ist grün.

---

## 49. Erforderliche Ausführungen vor Abschluss

Mindestens:

```bash
./gradlew clean check
./gradlew :gretl-core:integrationTest
./gradlew publishedArtifactTest
./gradlew verifyRuntimeImageOfflineDistribution
./gradlew buildRuntimeImageForOfflineTest
./gradlew runtimeImageOfflineTest
```

Zusätzlich:

```bash
./gradlew runtimeImageOfflineTest --rerun-tasks
./gradlew runtimeImageOfflineTest --parallel
```

Soweit möglich:

```bash
./gradlew runtimeImageOfflineTest --configuration-cache
./gradlew runtimeImageOfflineTest --configuration-cache
```

Ein grüner aggregierender Lauf ohne einzelne Taskausführung reicht nicht.

---

## 50. Abschlussbericht des Coding Agents

Der Coding Agent liefert am Ende einen strukturierten Bericht.

### 50.1 Architektur

- Offline-Repository-Mechanismus;
- optionaler Cache-Mechanismus;
- Versionierung;
- Init-Script;
- Image-Dateisystem;
- Image-ID;
- Docker-Isolation.

### 50.2 Geänderte Dateien

Für jede Datei:

- Pfad;
- Zweck;
- wichtigste Änderung.

### 50.3 Neue Klassen

Insbesondere:

- Taskklassen;
- `RuntimeImageDescriptor`;
- `OfflineArtifactManifest`;
- `OfflineDependencyClosureVerifier`;
- `ExternalProcessRunner`;
- `DockerCli`;
- `RuntimeImageOfflineExecutor`;
- `GradleTestProject`;
- Testklassen.

### 50.4 Testresultate

- ausgeführter Befehl;
- Anzahl Tests;
- Laufzeit;
- Ergebnis;
- gefundener Fehler;
- vorgenommene Behebung.

### 50.5 Isolationsnachweis

Explizit:

- NetworkMode;
- frisches Gradle-Home;
- keine Hostmounts;
- keine Source-Klassen;
- keine Remote-Auflösung;
- genaue Image-ID.

### 50.6 Mutationsergebnisse

Für jede Mutation:

- manipuliertes Artefakt;
- erwarteter Fehler;
- tatsächlicher Fehler;
- Nachweis, dass kein Fallback griff.

### 50.7 Abweichungen

Nur technisch notwendige Abweichungen.

Jede Abweichung enthält:

- betroffenen Spezifikationspunkt;
- Begründung;
- gleichwertigen Nachweis;
- verbleibendes Risiko.

### 50.8 Verbleibende Risiken

Nur konkrete offene Risiken.

Keine allgemeinen Floskeln.

---

## 51. Anweisung zur selbstständigen Umsetzung

Der Coding Agent soll:

1. diese Spezifikation vollständig lesen;
2. keine Rückfragen zu kleineren Architekturdetails stellen;
3. den aktuellen Repository-Stand als Wahrheit prüfen;
4. eine robuste Gradle-native Lösung wählen;
5. implementieren;
6. Tests ausführen;
7. Fehler beheben;
8. Tests erneut ausführen;
9. CI und Dokumentation aktualisieren;
10. erst nach erfüllter Definition of Done abschliessen.

Ein teilweise implementierter Test, ein Konzeptpapier oder ein einzelner grüner Smoke-Test erfüllt den Auftrag nicht.

---

## 52. Prioritäts- und Vorrangregeln

Bei Konflikten gelten in dieser Reihenfolge:

1. Sicherheit und tatsächliche Offline-Isolation;
2. Verwendung des aktuellen, unveränderlich identifizierten Images;
3. moderne `plugins {}`-DSL;
4. keine `gretljobs`-Migration;
5. vollständige Core-/GeoTools-/DuckDB-Canaries;
6. Wiederverwendung bestehender Testinfrastruktur;
7. konkrete Klassen- und Methodennamen dieser Spezifikation;
8. kosmetische Buildstruktur.

Die Klassen- und Methodennamen dürfen an bereits vorhandene gleichwertige Abstraktionen angepasst werden.

Die Beweisziele dürfen nicht abgeschwächt werden.

---

# Anhang A – Methodenkontrakte

## A.1 `RuntimeImageOfflineExecutor.execute`

Vorbedingungen:

- Projektverzeichnis existiert;
- Projektverzeichnis ist Verzeichnis;
- Descriptor ist validiert;
- Docker ist verfügbar;
- Request enthält mindestens einen Gradle-Task oder eine Gradle-Option;
- Timeout ist positiv.

Ablauf:

1. Containername erzeugen.
2. Mountliste erzeugen.
3. Gradle-Argumente ergänzen.
4. Environment erzeugen.
5. Container erstellen.
6. Container inspizieren.
7. strikte Offline-Isolation prüfen.
8. Container starten.
9. Ausgabe und Exitcode erfassen.
10. Ergebnisobjekt erzeugen.
11. Erfolg erwarten.
12. Container im `finally` entfernen.

Nachbedingungen:

- Exitcode null;
- erwartete Outputdateien durch Fachtest geprüft;
- kein Container verbleibt;
- keine Secrets in Resultat;
- Image-ID im Resultat entspricht Descriptor.

## A.2 `executeAndExpectFailure`

Wie `execute`, aber:

- Exitcode muss ungleich null sein;
- Timeout gilt nicht als erwarteter fachlicher Fehler;
- Fehlerdiagnose wird maskiert;
- der konkrete Test prüft Fehlerursache.

## A.3 `OfflineDependencyClosureVerifier.verify`

Vorbedingungen:

- Repository existiert;
- Manifest validiert;
- Root-Koordinaten nicht leer.

Ablauf:

1. Wurzeln in Queue legen.
2. besucht-Menge führen.
3. Metadaten jeder Koordinate lesen.
4. Artefakte prüfen.
5. Runtime-Dependencies bestimmen.
6. dynamische Versionen ablehnen.
7. neue Dependencies in Queue legen.
8. Report mit Knoten und Kanten erzeugen.

Nachbedingungen:

- jede Kante endet in existierender Koordinate;
- jedes erwartete Binärartefakt existiert;
- keine verbotene Version;
- keine externe File-Dependency;
- Report deterministisch sortiert.

## A.4 `DockerContainerInspection.assertStrictOffline`

Fehlertexte müssen den tatsächlichen und erwarteten Wert nennen.

Beispiele:

```text
Expected Docker NetworkMode 'none' but was 'bridge' for container <id>.
```

```text
Expected image <expected-id> but container was created from <actual-id>.
```

```text
Forbidden host Gradle mount detected: <source> -> <destination>.
```

## A.5 `SecretMasker`

Regeln:

- längere Secrets vor kürzeren ersetzen;
- leere Strings ignorieren;
- null nicht zulassen;
- sowohl rohe als auch URL-encodierte Testwerte berücksichtigen, falls diese im Kommando vorkommen;
- Ersatztext: `***`;
- Originalwerte niemals in Exception-Nachricht aufnehmen.

---

# Anhang B – Fehlerdiagnose-Matrix

| Fehler | Erwartete Erkennung | Primärer Test |
|---|---|---|
| falscher Image-Tag | ID-Vergleich | Contract |
| NetworkMode bridge | Docker inspect | Isolation |
| `--offline` fehlt | Command-Assertion | Executor Unit Test |
| Hostcache gemountet | Mount-Inspection | Isolation |
| Marker fehlt | Plugin resolution failure | Mutation |
| Implementierungs-JAR fehlt | Plugin load failure | Mutation |
| POM fehlt | Closure verifier / resolution | Mutation |
| SQLite-JAR fehlt | realer SQLite-Task | Mutation |
| Worker fehlt | GeoTools task failure | Mutation |
| EPSG fehlt | CRS test | Mutation |
| spatial Extension fehlt | DuckDB load failure | Mutation |
| alte Snapshot-Datei | Metadata check | Cache test |
| falsche Init-Version | version assertion | Contract |
| Downloadversuch | Log scanner + none network | Network |
| Secret im Log | Secret scanner | Security |
| Container bleibt liegen | cleanup assertion | Infrastructure |

---

# Anhang C – Mindest-Fixtures

## C.1 Gzip

```text
fixtures/offline/gzip/input.txt
```

Inhalt mit:

- UTF-8;
- Umlaut;
- Zeilenumbrüchen;
- mindestens 1 KB;
- deterministischem Inhalt.

## C.2 SQLite

```text
fixtures/offline/sqlite/001-schema.sql
fixtures/offline/sqlite/002-data.sql
fixtures/offline/sqlite/003-query.sql
```

## C.3 Shapefile

Kleines, selbst erstelltes Dataset mit:

- mindestens drei Features;
- mindestens einem Stringattribut;
- mindestens einem numerischen Attribut;
- EPSG:4326 oder EPSG:2056;
- allen Sidecar-Dateien.

## C.4 Raster

Kleines deterministisches Raster:

- maximal 100 × 100 Zellen;
- bekannte Klassenwerte;
- definierter NoData-Wert;
- CRS-Datei oder eingebettetes CRS.

## C.5 DuckDB

Lokale SQL-Datei mit:

- Datenbankerzeugung;
- Spatial-LOAD;
- Punktgeometrie;
- kleiner Tabelle;
- deterministischer Resultatdatei.

Fixtures dürfen keine externen URLs referenzieren.

---

# Anhang D – Code-Review-Checkliste

## Buildlogik

- [ ] keine konkurrierenden Clean-Abhängigkeiten;
- [ ] Provider API;
- [ ] deklarierte Outputs;
- [ ] Image-ID-Datei atomar geschrieben;
- [ ] Descriptor atomar geschrieben;
- [ ] keine globalen Docker-Prunes;
- [ ] keine Hostcache-Kopie.

## Image

- [ ] nicht Root;
- [ ] Repository read-only;
- [ ] Cache read-only;
- [ ] Init-Script versioniert;
- [ ] Launcher dünn;
- [ ] Extensions vorinstalliert;
- [ ] keine Credentials.

## Tests

- [ ] `--network none`;
- [ ] `--offline`;
- [ ] frisches Home;
- [ ] Core Groovy;
- [ ] GeoTools Groovy;
- [ ] DuckDB;
- [ ] Mutationen;
- [ ] Timeouts;
- [ ] Cleanup.

## CI

- [ ] Gate vor Publish;
- [ ] Reports `always()`;
- [ ] keine Secrets;
- [ ] Docker-Verfügbarkeit klar;
- [ ] lokaler Befehl entspricht CI-Befehl.

---

# Anhang E – Nichtziele

Diese Spezifikation verlangt nicht:

- dass beliebige Drittanbieterplugins offline verfügbar sind;
- dass Remote-HTTP-Dependencies offline gespiegelt werden;
- dass echte externe Datenbanken im Offline-Test laufen;
- dass ein Kubernetes-Pod getestet wird;
- dass Multi-Arch-Images gebaut werden;
- dass das Image minimal gross ist;
- dass bestehende `gretljobs` unverändert funktionieren;
- dass Legacy-Plugin-Syntax funktioniert;
- dass der Gradle-Daemon im Offline-Test wiederverwendet wird.

Sie verlangt jedoch, dass die getesteten GRETL-Produktbestandteile als vollständige Offline-Distribution funktionieren.
