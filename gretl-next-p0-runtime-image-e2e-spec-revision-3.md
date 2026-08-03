# Spezifikation: P0 – Runtime-Image-E2E für GRETL wieder einführen

**Ziel-Repository:** `https://github.com/edigonzales/gretl-next`

**DSL-Policy:** Die Runtime-Image-E2E prüft ausschließlich Groovy-Gradle-
Builds. Kotlin DSL kann durch Gradle weiterhin zufällig funktionieren, ist aber
kein GRETL-Vertrag und wird weder getestet noch dokumentiert.
**Betroffene Produktmodule:** `gretl-core`, `gretl-geotools`
**Zusätzliche Testinfrastruktur:** neues internes Modul `gretl-test-support`
**Status:** verbindlicher Umsetzungsauftrag für einen LLM Coding Agent
**Priorität:** P0 / Release-Gate
**Stand der Spezifikation:** 30. Juli 2026, Revision 3
**Primäres Ziel:** Nachweisen, dass das tatsächlich gebaute GRETL-Runtime-Image als abgeschlossene, daemonfähige Gradle-Distribution gemountete Gradle-Projekte mit der modernen `plugins {}`-DSL korrekt ausführt – ohne Bindung an die historische `flatDir`-Architektur, ohne versteckte Abhängigkeit vom Source-Build, vom Host-Gradle-Cache oder von `mavenLocal()`.

---

## 1. Auftrag an den Coding Agent

Implementiere im Repository `edigonzales/gretl-next` eine vollständige End-to-End-Testebene für das GRETL-Runtime-Image.

Die neue Testebene muss das **tatsächlich gebaute Container-Image** ausführen. Es genügt nicht, den Docker-Build erfolgreich abzuschliessen oder innerhalb des Images nur `gradle tasks` aufzurufen.

Die Tests müssen beweisen, dass:

1. das Image aus den Artefakten des aktuellen Commits gebaut wird;
2. genau dieses unveränderliche Image in allen E2E-Tests verwendet wird;
3. `gretl-core` und `gretl-geotools` innerhalb des Images geladen werden;
4. reale GRETL-Tasks fachlich korrekte Resultate erzeugen;
5. externe Dienste wie PostgreSQL/PostGIS, FTP, S3 und HTTP aus dem Image erreichbar sind;
6. lokale Datei- und Datenbankausgaben über gemountete Arbeitsverzeichnisse korrekt auf den Host zurückgeschrieben werden;
7. das Image mit leerem Gradle-User-Home und ohne Internetzugriff funktioniert;
8. keine Abhängigkeit von `mavenLocal()`, einem Host-Gradle-Cache oder dem Source-Classpath besteht;
9. die eingebettete GeoTools-Worker-Laufzeit tatsächlich im Container funktioniert;
10. die im Image vorinstallierten DuckDB-Erweiterungen offline verwendbar sind;
11. Fehler, Exit-Codes, Logausgaben und Secrets korrekt behandelt werden;
12. die Runtime-Image-Tests vor jeder Snapshot- oder Release-Publikation als zwingendes CI-Gate laufen;
13. beliebige gemountete Jobverzeichnisse mit `settings.gradle(.kts)`, `build.gradle(.kts)`, Properties und Fixtures ohne Source-Checkout ausgeführt werden können;
14. `gretl-core` und `gretl-geotools` ausschliesslich über die moderne Gradle-Plugin-DSL `plugins {}` angewendet werden können;
15. GRETL gemeinsam mit zusätzlichen, ebenfalls über `plugins {}` angewendeten Gradle-Plugins in demselben Consumer-Build verwendet werden kann;
16. ein langlebiger Container mehrere Builds über denselben kompatiblen Gradle-Daemon ausführen und den Daemon nachweislich wiederverwenden kann.

Die Aufgabe ist erst abgeschlossen, wenn die vollständige Runtime-Image-Testmatrix grün ist und die CI das Image-Gate zuverlässig ausführt.

### 1.1 Dauerhafte und verbindliche Projektabgrenzung: keine Migration von `sogis/gretljobs`

Die Migration bestehender Jobs aus `sogis/gretljobs` ist **definitiv und dauerhaft nicht Bestandteil dieses Projekts**.

Diese Abgrenzung hat Vorrang vor allen anderen Formulierungen dieser Spezifikation. Sie gilt nicht nur für P0, sondern für das gesamte Vorhaben `gretl-next`, solange kein eigenständiges, ausdrücklich beauftragtes Folgeprojekt mit separater Spezifikation eröffnet wird.

Der Coding Agent darf im Rahmen dieses Auftrags insbesondere **nicht**:

- bestehende Jobverzeichnisse aus `sogis/gretljobs` migrieren, ändern oder als kompatibel erklären;
- Pull Requests, Patches oder Branches für `sogis/gretljobs` erzeugen;
- Migrationsskripte, Codemods, automatische Rewrite-Regeln oder Kompatibilitätsadapter für bestehende Jobs implementieren;
- bestehende Jobs inventarisieren, massenweise kompilieren oder als Abnahmesuite verwenden;
- Produktionscode in `gretl-next` anpassen, nur damit historische Jobskripte unverändert weiterlaufen;
- Legacy-Syntax wie `apply plugin: 'ch.so.agi.gretl'` als Produktvertrag unterstützen oder testen;
- historische Task-APIs allein aus Kompatibilitätsgründen wieder einführen;
- die Fertigstellung dieses P0-Auftrags von einer späteren Jobmigration abhängig machen.

Das Repository `sogis/gretljobs` dient in dieser Spezifikation ausschliesslich als **externe Evidenz für das gewünschte Betriebsmodell eines langlebigen Containers mit wiederverwendbarem Gradle-Daemon**. Aus dieser Analyse entsteht keinerlei Quellcode-, Syntax-, Task-API- oder Job-Kompatibilitätsanforderung.

Der verbindliche Consumer-Vertrag von `gretl-next` beginnt bei neu geschriebenen oder unabhängig angepassten Gradle-Projekten, welche die moderne Syntax verwenden:

```groovy
plugins {
    id 'ch.so.agi.gretl'
}
```

beziehungsweise:

```groovy
plugins {
    id 'ch.so.agi.gretl'
    id 'ch.so.agi.gretl.geotools'
}
```

Ob ein bestehender Job aus einem anderen Repository mit diesem Vertrag kompatibel ist, wird in diesem Projekt weder geprüft noch garantiert.

---

## 2. Ausgangslage im aktuellen Repository

Der Agent muss die aktuelle Implementierung vor Änderungen nochmals verifizieren. Die Spezifikation basiert auf folgendem Repository-Stand:

### 2.1 Vorhandene Image-Build-Tasks

Im Root-Build existieren bereits:

```text
cleanRuntimeImageRepository
stageRuntimeImage
buildRuntimeImage
```

`stageRuntimeImage`:

- publiziert `gretl-core` und `gretl-geotools` in ein lokales Maven-Repository;
- kopiert die Plugin-Publikationen in den Docker-Kontext;
- kopiert die Implementierungs-JARs;
- kopiert externe Runtime-Classpaths in `libs/`;
- kopiert `Dockerfile`, `gretl` und `init.gradle`;
- erzeugt eine `build.info`.

`buildRuntimeImage` baut derzeit lediglich ein lokales Image. Es gibt keinen nachgelagerten automatischen Image-Vertragstest und keine fachliche E2E-Suite.

### 2.2 Aktuelles Runtime-Image

Das aktuelle Dockerfile verwendet sinngemäss:

```text
Java 17 JRE
Gradle 7.6.4
USER gradle
WORKDIR /home/gradle/project
ENTRYPOINT gretl
```

Das Image enthält:

```text
/home/gradle/init.gradle
/home/gradle/build.info
/home/gradle/maven-repo
/home/gradle/libs
/usr/local/bin/gretl
```

Beim Image-Build werden DuckDB-Erweiterungen vorinstalliert:

```text
postgres
spatial
excel
```

### 2.3 Aktuelle Risiken, die durch P0 geschlossen werden müssen

Der aktuelle Stand enthält mindestens folgende nachweisbare Risiken:

- Die Projektversion und die Default-Version in `docker/init.gradle` können auseinanderlaufen.
- Das Init-Script enthält `mavenLocal()` und kann dadurch lokale Artefakte verwenden.
- Die aktuelle Kombination aus lokalem Maven-Repository, `libs/`, `flatDir` und pauschaler Buildscript-Classpath-Injektion kann unvollständige Metadaten verdecken, transitive Abhängigkeiten umgehen und Classpath-Konflikte erzeugen. Diese Architektur ist ausdrücklich **nicht** als Ziel vorgegeben.
- Das Image kann bei aktivem Netzwerk unbemerkt fehlende Abhängigkeiten herunterladen.
- Ein gefüllter Gradle-Cache kann fehlende Bestandteile des Images verdecken.
- Ein erfolgreicher `docker build` beweist nicht, dass reale Tasks funktionieren.
- Es gibt derzeit keine CI-Stufe, die das fertige Image fachlich testet.
- Die GeoTools-Worker-Ressourcen können im Source-Test funktionieren, aber im Image fehlen oder falsch gepackt sein.
- Die Runtime kann mit einer veralteten oder falschen Plugin-Version starten.
- Fehlerhafte Dateiberechtigungen bei Bind-Mounts werden erst beim realen Betrieb sichtbar.

Diese Risiken sind Bestandteil des P0-Auftrags und dürfen nicht als spätere Optimierungen verschoben werden.

### 2.4 Aus `sogis/gretljobs` abgeleitetes Betriebsmodell

Für diese Revision wurde der aktuelle Betriebsstand von `sogis/gretljobs` ausschliesslich zur Klärung des Container-Lebenszyklus analysiert:

- Seit dem 8. Juni 2026 startet die lokale Entwicklungsumgebung einen ständig laufenden Compose-Service `gretl-service`.
- Der Service bleibt über einen neutralen Langläufer-Prozess aktiv; Gradle-Projekte werden anschliessend mit `docker compose exec` im bestehenden Container ausgeführt.
- Das globale `--no-daemon` wurde bewusst aus dem GRETL-Launcher entfernt.
- Zweck der Änderung ist die Wiederverwendung des Gradle-Daemons ab der zweiten Ausführung.
- `--no-daemon` bleibt eine bewusst vom Aufrufer wählbare Diagnose- und Isolationsoption.

Daraus folgt für `gretl-next` ausschliesslich:

1. Das Produktimage muss **daemonfähig** sein und darf den Daemon nicht global abschalten.
2. Das Image muss einen langlebigen Servicebetrieb unterstützen, darf aber zusätzlich einen isolierten One-shot-Betrieb anbieten.
3. Die E2E-Suite muss beide Betriebsarten testen.
4. Die Abhängigkeits- und Plugin-Bereitstellung darf neu entworfen werden; historische `flatDir`-Mechanismen sind keine Kompatibilitätsanforderung.
5. Der sichtbare Consumer-Vertrag ist das Ausführen eines gemounteten Gradle-Projekts mit moderner `plugins {}`-DSL; die interne Verpackung ist austauschbar.

Aus der Analyse von `sogis/gretljobs` folgt ausdrücklich **keine** Verpflichtung, bestehende Jobs, Legacy-Syntax, historische Task-APIs oder bisher eingebettete Drittanbieter-Plugins zu unterstützen.

### 2.5 Jenkins-Abgrenzung

Die Analyse des zentralen `gretljobs/Jenkinsfile` zeigt, dass `gretl` innerhalb eines Jenkins-Containers ohne erzwungenes `--no-daemon` aufgerufen wird. Damit ist der Daemon grundsätzlich erlaubt. Ob derselbe Daemon über mehrere Jenkins-Ausführungen hinweg lebt, hängt jedoch vom Lebenszyklus des konkreten Jenkins-Agenten beziehungsweise Kubernetes-Pods ab und wird von diesem Projekt nicht vorausgesetzt.

Das Ziel dieser Spezifikation ist deshalb:

- Daemon-Wiederverwendung innerhalb eines langlebigen Runtime-Containers verbindlich unterstützen und testen;
- keine unbelegte Annahme treffen, dass jeder Jenkins-Agent zwingend dauerhaft lebt;
- den Image-Vertrag so gestalten, dass sowohl langlebige als auch ephemere Agenten korrekt funktionieren.

---

## 3. Beziehung zur Spezifikation „P0: Tests der publizierten Artefakte“

Diese Spezifikation ergänzt die separate P0-Spezifikation für publizierte Gradle-Artefakte.

Die Testebenen haben unterschiedliche Beweisziele:

| Testebene | Beweisziel |
|---|---|
| Source-/TestKit-Test | Produktionscode funktioniert direkt aus dem Build-Tree. |
| Published-Artifact-Test | Plugin-Marker, POMs, Metadaten und publizierte JARs funktionieren aus einem isolierten Maven-Repository. |
| Runtime-Image-E2E | Die komplette ausgelieferte Container-Distribution funktioniert inklusive Image-Dateisystem, Init-Script, Runtime-Libs, Benutzer, Gradle, Java und Entry Point. |

Die Runtime-Image-E2E-Suite darf den Published-Artifact-Test nicht ersetzen.

Falls die Published-Artifact-Spezifikation bereits umgesetzt wurde, muss deren Ausführungsabstraktion erweitert und wiederverwendet werden. Es darf keine zweite konkurrierende Testarchitektur entstehen.

Die verbindlichen Ausführungsmodi lauten:

```java
TESTKIT_CLASSPATH
PUBLISHED_ARTIFACT
RUNTIME_IMAGE
```

Die exakten Enum-Namen dürfen nur angepasst werden, wenn im Repository bereits gleichwertige Namen existieren. Die semantische Trennung ist zwingend.

---

## 4. Verbindliche Zielarchitektur

Nach der Umsetzung müssen mindestens folgende Root-Tasks existieren:

```text
verifyRuntimeImagePrerequisites
stageRuntimeImage
buildRuntimeImageForTest
verifyRuntimeImageContract
runtimeImageOfflineTest
runtimeImageServiceTest
runtimeImageSmokeTest
runtimeImageE2eTest
runtimeImageTest
ciCheck
```

Zusätzlich müssen mindestens folgende Modul-Tasks existieren:

```text
:gretl-core:runtimeImageFunctionalTest
:gretl-core:runtimeImageIntegrationTest
:gretl-geotools:runtimeImageFunctionalTest
:gretl-test-support:test
:gretl-test-support:runtimeImageContractTest
:gretl-test-support:runtimeImageOfflineTest
```

Der gewünschte Task-Graph ist:

```text
runtimeImageTest
├── verifyRuntimeImageContract
│   └── buildRuntimeImageForTest
│       ├── verifyRuntimeImagePrerequisites
│       └── stageRuntimeImage
├── runtimeImageOfflineTest
│   └── buildRuntimeImageForTest
├── runtimeImageServiceTest
│   └── buildRuntimeImageForTest
└── runtimeImageE2eTest
    ├── :gretl-core:runtimeImageFunctionalTest
    │   └── buildRuntimeImageForTest
    ├── :gretl-core:runtimeImageIntegrationTest
    │   └── buildRuntimeImageForTest
    └── :gretl-geotools:runtimeImageFunctionalTest
        └── buildRuntimeImageForTest
```

`runtimeImageSmokeTest` ist eine kleinere, schnellere Teilmenge:

```text
runtimeImageSmokeTest
├── verifyRuntimeImageContract
├── runtimeImageOfflineTest
├── :gretl-core:runtimeImageFunctionalSmokeTest
├── :gretl-core:runtimeImageIntegrationSmokeTest
└── :gretl-geotools:runtimeImageFunctionalSmokeTest
```

`ciCheck` muss mindestens abhängen von:

```text
check
:gretl-core:integrationTest
publishedArtifactTest
runtimeImageTest
```

Falls `publishedArtifactTest` noch nicht existiert, darf `ciCheck` vorübergehend ohne diese Abhängigkeit implementiert werden. Die Runtime-Image-Spezifikation selbst darf jedoch nicht abgeschwächt werden.

### 4.1 Ergebnisorientierte Architektur statt Nachbau des Altimages

Der Coding Agent darf und soll die Runtime-Distribution neu strukturieren, wenn der aktuelle oder historische Ansatz nicht sauber funktioniert.

Nicht verbindlich sind insbesondere:

- `/home/gradle/libs` als Produktvertrag;
- `flatDir`;
- pauschales `fileTree(...*.jar)` auf jedem Buildscript-Classpath;
- die genaue Verzeichnisstruktur des ursprünglichen `sogis/gretl`-Images;
- ein bestimmtes Base-Image;
- ein bestimmter Mechanismus zur Vorinstallation der Plugins.

Verbindlich ist das beobachtbare Verhalten:

```text
mount job directory
→ execute gretl/Gradle in that directory
→ resolve bundled GRETL plugins
→ optionally resolve/use additional plugins
→ execute tasks
→ write results back into the mounted directory
```

Der Agent muss die gewählte Distributionsarchitektur in einer kurzen Architecture Decision Record dokumentieren und mindestens folgende Alternativen bewerten:

1. **Bevorzugt:** vollständiges strukturiertes Maven-Repository im Image mit Plugin-Markern, Implementierungsmodulen, POM/GMM-Metadaten und der benötigten transitiven Laufzeitmenge;
2. strukturierte lokale Ivy-/Maven-Distribution mit synthetisch erzeugter, verifizierter Dependency-Closure;
3. gezielt erzeugte Plugin-Distribution beziehungsweise init-plugin-basierte Classpath-Bereitstellung mit klarer Classloader-Isolation;
4. Fat-/Distribution-JARs nur, wenn Service-Loader, Native Libraries, Lizenzdateien und GeoTools-/DuckDB-Ressourcen nachweislich korrekt behandelt werden.

`flatDir` darf nur beibehalten werden, wenn der Agent nachweist, dass eine strukturierte Lösung objektiv nicht praktikabel ist. In diesem Fall sind eine schriftliche Begründung und zusätzliche Classpath-/Transitivitäts-Negativtests zwingend. Ein blosses „funktioniert im Smoke-Test“ reicht nicht.

### 4.2 Keine Jobmigration als verstecktes Arbeitspaket

Die Implementierung dieses Runtime-Image-P0 darf keine versteckte oder vorbereitende Migration fremder Job-Repositories enthalten.

Insbesondere sind folgende Punkte keine Deliverables:

- Anpassungen an realen Jobskripten;
- Kompatibilitätslisten für bestehende Jobs;
- automatische Übersetzung alter Task-Konfigurationen;
- Validierung des gesamten `sogis/gretljobs`-Bestands;
- Migrationsleitfäden, welche konkrete bestehende Jobs umschreiben;
- Parallelbetrieb des alten und neuen Task-API-Vertrags;
- ein Drop-in-Ersatz für das bisherige GRETL-Image.

Fixtures in dieser E2E-Suite müssen klein, kontrolliert, eigens für `gretl-next` geschrieben und vollständig im Testcode beziehungsweise unter Testressourcen versioniert sein. Sie dürfen fachlich repräsentative Muster abbilden, aber nicht aus realen Job-Repositories übernommen werden, sofern dadurch eine implizite Kompatibilitätszusage entsteht.

### 4.3 `check` bleibt schnell und Docker-frei

Der vorhandene `check`-Task darf **nicht** von Runtime-Image-Tests abhängen.

Ziel:

```text
./gradlew check
```

bleibt der schnelle lokale Entwicklungszyklus.

Der vollständige Release-/CI-Zyklus lautet:

```text
./gradlew ciCheck
```

---

## 5. Harte Invarianten

Diese Regeln sind nicht optional.

### 5.1 Das getestete Image muss unveränderlich adressiert werden

Die Tests dürfen nicht nur mit einem veränderlichen Tag wie folgendem arbeiten:

```text
sogis/gretl-modular:test
```

`buildRuntimeImageForTest` muss Docker mit `--iidfile` aufrufen und die erzeugte Image-ID in eine Datei schreiben:

```text
build/runtime-image/test/image-id.txt
```

Beispielinhalt:

```text
sha256:0123456789abcdef...
```

Alle Runtime-Image-Tests müssen die Image-ID aus dieser Datei lesen und Docker mit dieser ID starten.

Der menschenlesbare Test-Tag darf zusätzlich gesetzt werden, ist aber nicht die primäre Identität.

### 5.2 Kein Pull und kein fremdes Image

Bei allen Image-Testläufen gilt:

```text
--pull=never
```

oder eine funktional gleichwertige Garantie.

Die Tests dürfen kein Registry-Image herunterladen.

### 5.3 Kein Source-Classpath

Der Runtime-Image-Modus darf niemals:

- `withPluginClasspath()` verwenden;
- `includeBuild` auf das Root-Projekt verwenden;
- `build/classes` mounten;
- `build/libs` direkt in das Consumer-Projekt eintragen;
- Source- oder Test-Classpaths des Hosts in den Container injizieren;
- die GRETL-Produktklassen aus dem JUnit-Prozess ausführen.

Die einzige GRETL-Laufzeit im E2E-Pfad ist diejenige im gebauten Image.

### 5.4 Kein `mavenLocal()` im Runtime-Image

Das produktive Init-Script darf `mavenLocal()` nicht konfigurieren.

Dies gilt für:

- `pluginManagement.repositories`;
- `buildscript.repositories`;
- normale Projekt-Repositories.

Ein Entwickler-Override darf nur über eine explizite, dokumentierte Opt-in-Property möglich sein. Der Standardpfad und alle Tests müssen ohne `mavenLocal()` laufen.

### 5.5 Leeres Gradle-User-Home

Jeder Runtime-Image-Testlauf erhält ein neues, leeres Host-Verzeichnis, das nach folgendem Ziel gemountet wird:

```text
/home/gradle/.gradle
```

Es darf kein Host-Verzeichnis wie folgendes gemountet werden:

```text
$HOME/.gradle
$HOME/gradlecache
```

Die Tests dürfen nicht vom globalen Gradle-Cache abhängen.

### 5.6 Kein öffentliches Internet in fachlichen E2E-Tests

Alle fachlichen Runtime-Image-Tests müssen Gradle mit folgendem Argument starten:

```text
--offline
```

Netzwerkzugriff ist nur für lokale Testcontainer erlaubt:

- PostGIS;
- FTP;
- S3;
- HTTP-Testserver.

Für rein lokale Tests muss Docker mit folgendem Netzwerkmodus laufen:

```text
--network none
```

### 5.7 Keine echten Cloud-Dienste

Runtime-Image-E2E-Tests dürfen keine echten externen S3-, FTP-, Datenbank- oder HTTP-Dienste verwenden.

Erlaubt sind nur lokal orchestrierte, gepinnte Testcontainer oder lokale Testserver.

### 5.8 Semantische Assertions

Ein Exit-Code `0` allein ist kein ausreichender Test.

Jeder fachliche Test muss das Resultat ausserhalb des Containers prüfen, beispielsweise:

- Dateiinhalt;
- Datenbankinhalt;
- Objektanzahl;
- Geometrie-SRID;
- Rasterwerte;
- GeoPackage-Layer;
- S3-Objektmetadaten;
- FTP-Verzeichnisinhalt;
- erzeugte INTERLIS-Datei;
- Rollback nach Fehler;
- Abwesenheit eines Secrets im Log.

### 5.9 Consumer-Vertrag für gemountete Jobverzeichnisse

Ein Consumer-Projekt darf ausschliesslich aus gemounteten Dateien bestehen. Es darf weder den GRETL-Source-Checkout noch Build-Ausgaben des Hostprojekts sehen.

Mindestens zu unterstützen:

```text
settings.gradle
build.gradle
gradle.properties
job.properties und beliebige Job-Fixtures
Unterverzeichnisse mit SQL, Modellen, Transferdateien und Hilfsdateien
```

Der Aufruf muss sowohl mit `--project-dir=<job>` als auch über ein gesetztes Working Directory funktionieren.

### 5.10 Ausschliesslich moderne Plugin-Anwendung

Der verbindliche Consumer-Vertrag verwendet ausschliesslich die moderne Gradle-Plugin-DSL.

Verbindliche Formen für `gretl-core`:

```groovy
plugins {
    id 'ch.so.agi.gretl'
}
```

```groovy
plugins {
    id 'ch.so.agi.gretl' version '<bundled-version>'
}
```

Verbindliche Formen für `gretl-geotools`:

```groovy
plugins {
    id 'ch.so.agi.gretl'
    id 'ch.so.agi.gretl.geotools'
}
```

```groovy
plugins {
    id 'ch.so.agi.gretl' version '<bundled-version>'
    id 'ch.so.agi.gretl.geotools' version '<bundled-version>'
}
```

Legacy-Anwendung über `buildscript {}`, manuelle Classpath-Abhängigkeiten oder `apply plugin: 'ch.so.agi.gretl'` ist kein Produktvertrag. Der Coding Agent darf dafür weder Kompatibilitätscode noch Tests ergänzen. Dass eine solche Form zufällig funktioniert, begründet keine Unterstützungsgarantie.

### 5.11 Zusätzliche Gradle-Plugins

Das Init-Script beziehungsweise die Image-Distribution darf Consumer-Repositories und weitere Plugins nicht blockieren oder überschreiben.

Ein deterministischer E2E-Test muss ein kleines zusätzliches Fixture-Plugin aus einem separat gemounteten lokalen Maven-Repository gemeinsam mit GRETL anwenden. Damit wird ohne öffentliches Internet bewiesen, dass:

- GRETL-Repositories ergänzt und nicht exklusiv erzwungen werden;
- Plugin-Resolution-Strategien des Consumers erhalten bleiben;
- keine pauschale Classpath-Injektion zu Versions- oder Classloader-Konflikten führt.

Drittanbieter-Plugins gehören standardmässig nicht zur GRETL-Distribution. Falls später ein zusätzliches Plugin ausdrücklich als Produktbestandteil beschlossen wird, benötigt dies eine separate, dokumentierte Entscheidung, eine gepinnte Version und eigene Offline-/E2E-Tests. Diese Spezifikation verlangt lediglich, dass ein Consumer zusätzliche Plugins über die normale moderne Plugin-DSL und seine eigenen Repositories kombinieren kann.

### 5.12 Daemonfähiger Servicebetrieb

Der Launcher darf `--no-daemon` nicht global erzwingen.

Das Image muss mindestens zwei Betriebsweisen erlauben:

```text
ONE_SHOT
LONG_LIVED_SERVICE
```

- `ONE_SHOT`: ein Container führt einen Build aus und endet; Tests dürfen hier gezielt `--no-daemon` verwenden.
- `LONG_LIVED_SERVICE`: der Container bleibt aktiv; mehrere `docker exec`-Aufrufe verwenden denselben kompatiblen Gradle-Daemon.

Daemon-Wiederverwendung darf nur zwischen Builds desselben Vertrauensbereichs und desselben Containerbenutzers stattfinden. Eine daemonübergreifende Mehrmandantenfähigkeit ist kein Ziel.

### 5.13 Keine stillen Testausschlüsse

Jeder öffentliche GRETL-Task muss in einer Coverage-Matrix klassifiziert werden.

Zulässige Klassifikationen:

```text
DIRECT_E2E
COVERED_BY_CHAIN
NOT_APPLICABLE_WITH_REASON
```

`NOT_APPLICABLE_WITH_REASON` darf nur verwendet werden, wenn ein Task im Runtime-Image objektiv nicht ausführbar oder nicht mehr öffentlich ist. Eine blosse Aufwandsbegründung ist unzulässig.

Ein automatischer Inventory-Test muss fehlschlagen, wenn ein neuer öffentlicher Task keine Klassifikation besitzt.

---

## 6. Teststrategie

Die Runtime-Image-Teststrategie besteht aus fünf Schichten.

### 6.1 Schicht A: Image-Vertrag

Prüft die statische und ausführbare Struktur des Images:

- Image existiert lokal;
- Image-ID stimmt;
- Entry Point stimmt;
- Arbeitsverzeichnis stimmt;
- Benutzer ist nicht Root;
- erwartete Dateien sind vorhanden;
- `build.info` stimmt;
- Gradle- und Java-Version stimmen;
- Plugin-Publikationen sind vorhanden;
- GeoTools-Worker-Inhalt ist vorhanden;
- keine Source-/Javadoc-JARs sind enthalten;
- Dateiberechtigungen sind korrekt.

### 6.2 Schicht B: Offline-Bootstrap

Prüft mit leerem Cache und `--network none`:

- Core-Plugin mit Groovy DSL;
- GeoTools-Plugin mit Groovy DSL;
- versionlose Plugin-DSL über das Init-Script;
- explizite aktuelle Plugin-Version;
- Gzip als minimaler Core-Canary;
- ReadShapefile als GeoTools-Canary;
- DuckDB inklusive vorinstallierter Erweiterungen.

### 6.3 Schicht C: Smoke-E2E

Schnelle fachliche Release-Canaries:

- Core-Dateitask;
- SQLite-/SQL-Task;
- PostGIS-Task;
- HTTP-Task;
- S3-Task;
- FTP-Task;
- INTERLIS-/ili2pg-Kette;
- GeoTools-Worker-Task.

### 6.4 Schicht D: Daemon-/Service-E2E

Prüft einen langlebigen Runtime-Container:

- erster Build startet einen kompatiblen Gradle-Daemon;
- zweiter Build verwendet denselben Daemon-PID;
- ein zweites gemountetes Jobverzeichnis kann im selben Container ausgeführt werden;
- Änderungen an `build.gradle` werden trotz Daemon korrekt erkannt;
- `gretl --stop` beendet den Daemon, nicht aber den Servicecontainer;
- ein nachfolgender Build startet kontrolliert einen neuen Daemon;
- ein expliziter `--no-daemon`-Lauf funktioniert weiterhin;
- Projektzustände, Properties und Taskregistrierungen lecken nicht zwischen Jobverzeichnissen.

### 6.5 Schicht E: vollständige Runtime-Image-E2E-Matrix

Führt alle als runtime-image-fähig klassifizierten vorhandenen funktionalen und Integrationstests erneut über den Container-Executor aus.

Die Schicht E ist das eigentliche P0-Release-Gate.

---

## 7. Neues internes Modul `gretl-test-support`

### 7.1 Zweck

Erzeuge ein neues internes, nicht publiziertes Modul:

```text
gretl-test-support
```

Das Modul enthält ausschliesslich gemeinsame Testinfrastruktur.

Kein Produktionsmodul darf zur Laufzeit davon abhängen.

### 7.2 `settings.gradle`

Ergänze:

```groovy
include 'gretl-test-support'
```

### 7.3 `gretl-test-support/build.gradle`

Verwende mindestens:

```groovy
plugins {
    id 'java-library'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    api gradleTestKit()
    api "org.junit.jupiter:junit-jupiter-api:${junitVersion}"

    testImplementation "org.junit.jupiter:junit-jupiter-params:${junitVersion}"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:${junitVersion}"
}
```

Füge nur dann weitere Bibliotheken hinzu, wenn sie klar notwendig sind. Für Prozessausführung, XML, JSON und Dateisystem sind bevorzugt JDK-Klassen zu verwenden.

### 7.4 Publikationsschutz

Das Modul darf nicht:

- `maven-publish` anwenden;
- in `publishSnapshots` aufgenommen werden;
- in das Runtime-Image kopiert werden;
- in einer produktiven POM erscheinen.

Füge einen Test oder eine Gradle-Verifikation hinzu, die dies sicherstellt.

---

## 8. Gemeinsames Ausführungsmodell

### 8.1 Enum `GretlExecutionMode`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlExecutionMode.java
```

Implementiere:

```java
public enum GretlExecutionMode {
    TESTKIT_CLASSPATH,
    PUBLISHED_ARTIFACT,
    RUNTIME_IMAGE;

    public static final String SYSTEM_PROPERTY = "gretl.test.executionMode";

    public static GretlExecutionMode current();
    public static GretlExecutionMode parse(String value);
    public boolean isRuntimeImage();
}
```

#### `current()`

Verhalten:

- liest `gretl.test.executionMode`;
- Default ist `TESTKIT_CLASSPATH`;
- akzeptiert nur dokumentierte Werte;
- normalisiert Bindestriche und Gross-/Kleinschreibung nicht stillschweigend übermässig;
- wirft bei unbekanntem Wert eine klare `IllegalArgumentException`.

Fehlermeldung muss enthalten:

```text
unknown value
property name
allowed values
```

### 8.2 Interface `GretlBuildExecutor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlBuildExecutor.java
```

Signatur:

```java
public interface GretlBuildExecutor {
    GretlBuildResult execute(GretlBuildRequest request);
    GretlBuildResult executeAndExpectFailure(GretlBuildRequest request);
}
```

Die Implementierung darf keine JUnit-Assertions verstecken, ausser der klaren Erfolg-/Fehlererwartung dieser zwei Methoden.

### 8.3 Record `GretlBuildRequest`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlBuildRequest.java
```

Verwende einen unveränderlichen Record oder eine gleichwertige immutable Klasse:

```java
public record GretlBuildRequest(
        Path projectDirectory,
        List<String> arguments,
        Map<String, String> environment,
        Set<String> secretValues,
        Duration timeout,
        RuntimeImageRunOptions runtimeImageOptions) {
}
```

Implementiere zusätzlich einen Builder:

```java
public static Builder builder(Path projectDirectory);
```

Builder-Methoden:

```java
Builder argument(String value);
Builder arguments(String... values);
Builder arguments(Collection<String> values);
Builder environment(String name, String value);
Builder secret(String value);
Builder timeout(Duration value);
Builder runtimeImageOptions(RuntimeImageRunOptions value);
GretlBuildRequest build();
```

Validierungen:

- `projectDirectory` existiert;
- Pfad ist ein Verzeichnis;
- Argumente enthalten keine `null`-Werte;
- Timeout ist positiv;
- Secret-Werte werden nicht in `toString()` ausgegeben;
- Collections werden defensiv kopiert.

### 8.4 Record `GretlBuildResult`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlBuildResult.java
```

Signatur:

```java
public record GretlBuildResult(
        int exitCode,
        String standardOutput,
        String standardError,
        Duration duration,
        List<String> sanitizedCommand,
        Map<String, GretlTaskOutcome> taskOutcomes) {

    public boolean successful();
    public String output();
    public Optional<GretlTaskOutcome> taskOutcome(String taskPath);
}
```

`output()` verbindet `stdout` und `stderr` deterministisch.

Das Resultat darf niemals unredigierte Secrets enthalten.

### 8.5 Enum `GretlTaskOutcome`

```java
public enum GretlTaskOutcome {
    SUCCESS,
    FAILED,
    SKIPPED,
    UP_TO_DATE,
    FROM_CACHE,
    NO_SOURCE,
    UNKNOWN
}
```

Der Runtime-Image-Executor darf Task-Outcomes best-effort aus dem Plain-Console-Output extrahieren. Fachliche Tests dürfen jedoch nicht ausschliesslich von dieser Parserlogik abhängen.

### 8.6 Factory `GretlBuildExecutors`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlBuildExecutors.java
```

Methoden:

```java
public final class GretlBuildExecutors {
    public static GretlBuildExecutor forCurrentMode();
    public static GretlBuildExecutor forMode(GretlExecutionMode mode);
}
```

Verhalten:

```text
TESTKIT_CLASSPATH -> TestKitClasspathBuildExecutor
PUBLISHED_ARTIFACT -> PublishedArtifactBuildExecutor
RUNTIME_IMAGE -> RuntimeImageBuildExecutor
```

Falls `PublishedArtifactBuildExecutor` aus der vorherigen P0-Umsetzung bereits existiert, muss genau diese Implementierung verwendet werden.

---

## 9. Prozessausführung

### 9.1 Klasse `ProcessExecutor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/process/ProcessExecutor.java
```

Methoden:

```java
public ProcessResult execute(ProcessRequest request);
```

### 9.2 Record `ProcessRequest`

```java
public record ProcessRequest(
        List<String> command,
        Path workingDirectory,
        Map<String, String> environment,
        Duration timeout,
        Set<String> secretValues) {
}
```

### 9.3 Record `ProcessResult`

```java
public record ProcessResult(
        int exitCode,
        String standardOutput,
        String standardError,
        Duration duration,
        List<String> sanitizedCommand) {
}
```

### 9.4 Verbindliches Verhalten

`ProcessExecutor` muss:

- `ProcessBuilder` verwenden;
- Argumente als Liste übergeben;
- keine Shell-String-Kommandos zusammensetzen;
- `stdout` und `stderr` parallel konsumieren;
- Deadlocks bei grossen Ausgaben vermeiden;
- UTF-8 verwenden;
- einen konfigurierbaren Timeout erzwingen;
- bei Timeout den Prozess zunächst normal und danach forcierend beenden;
- den Interrupt-Status des Threads wiederherstellen;
- Exit-Code, Laufzeit und Ausgaben zurückgeben;
- Secrets in Ausgaben und Command-Darstellung redigieren;
- bei Startfehlern eine Exception mit ausführbarer Diagnose werfen.

Unzulässig:

```java
Runtime.getRuntime().exec("docker run ...")
```

Unzulässig sind auch selbst gestartete Threads ohne Join oder Exception-Weitergabe.

### 9.5 Klasse `SecretRedactor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/process/SecretRedactor.java
```

Methoden:

```java
public String redact(String value, Set<String> secrets);
public List<String> redact(List<String> values, Set<String> secrets);
```

Regeln:

- leere Secrets ignorieren;
- längere Secrets vor kürzeren ersetzen;
- exakte Secret-Werte durch `***` ersetzen;
- URL-encodierte und einfache Property-Formen berücksichtigen, soweit ohne unsichere Heuristik möglich;
- Original-Collections nicht mutieren.

Unit-Tests müssen mindestens überlappende Secrets, leere Werte und Secrets in JDBC-URLs abdecken.

---

## 10. Docker-Abstraktion

### 10.1 Klasse `DockerCli`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/docker/DockerCli.java
```

Methoden:

```java
public DockerInfo verifyAvailable();
public DockerImageInspection inspectImage(String imageReference);
public ProcessResult runContainer(DockerRunRequest request);
public ProcessResult removeContainer(String containerName, boolean force);
public boolean imageExists(String imageReference);
```

`verifyAvailable()` führt mindestens aus:

```text
docker version --format ...
docker info --format ...
```

und liefert eine klare Diagnose für:

- Docker CLI fehlt;
- Docker Daemon nicht erreichbar;
- Berechtigung verweigert;
- inkompatibler Server.

Die Runtime-Image-Tests dürfen bei fehlendem Docker nicht stillschweigend übersprungen werden. Der explizite Task `runtimeImageTest` muss fehlschlagen.

### 10.2 Record `DockerRunRequest`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/docker/DockerRunRequest.java
```

Felder:

```java
public record DockerRunRequest(
        String imageReference,
        String containerName,
        Path projectDirectory,
        Path gradleUserHome,
        List<String> commandArguments,
        Map<String, String> environment,
        Optional<String> network,
        boolean networkDisabled,
        Optional<String> user,
        Duration timeout,
        Set<String> secretValues,
        Map<Path, String> additionalReadOnlyMounts,
        Map<Path, String> additionalReadWriteMounts) {
}
```

Validierungen:

- Image-Referenz nicht leer;
- genau eines von `network` oder `networkDisabled` gesetzt;
- Mount-Ziele sind absolute Containerpfade;
- Host-Pfade existieren;
- Projekt- und Gradle-Home-Pfade dürfen nicht identisch sein;
- Containername entspricht Docker-Regeln;
- keine doppelten Mount-Ziele.

### 10.3 Klasse `DockerRunCommandBuilder`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/docker/DockerRunCommandBuilder.java
```

Methode:

```java
public List<String> build(DockerRunRequest request);
```

Die resultierende Command-Liste muss mindestens enthalten:

```text
docker
run
--rm
--pull=never
--name <eindeutiger-name>
--mount type=bind,src=<project>,dst=/home/gradle/project
--mount type=bind,src=<gradle-home>,dst=/home/gradle/.gradle
--workdir /home/gradle/project
-e GRADLE_USER_HOME=/home/gradle/.gradle
<network-option>
<image-id>
<gretl-argumente>
```

Für lokale Offline-Tests:

```text
--network none
```

Für servicebasierte Tests:

```text
--network <testcontainers-network-id-or-name>
```

### 10.4 Benutzerstrategie

Implementiere:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/docker/ContainerUserResolver.java
```

Methoden:

```java
public Optional<String> resolve();
public Optional<String> resolveFromOverride();
public Optional<String> resolvePosixUser();
```

Regeln:

1. Property `gretl.test.runtimeImage.user` hat Vorrang.
2. Auf Linux/macOS darf `id -u` und `id -g` verwendet werden.
3. Ergebnisformat ist `<uid>:<gid>`.
4. Auf Windows wird standardmässig kein `--user` gesetzt.
5. Der Test darf nur temporäre Arbeitsverzeichnisse verwenden.
6. Keine globale `chmod 777`-Änderung ausserhalb der Testverzeichnisse.

Die Image-Tests müssen mindestens nachweisen, dass erzeugte Dateien vom Host les- und löschbar sind.

---

## 11. Runtime-Image-Beschreibung

### 11.1 Record `RuntimeImageDescriptor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/runtime/RuntimeImageDescriptor.java
```

Felder:

```java
public record RuntimeImageDescriptor(
        String imageId,
        String imageTag,
        String gretlVersion,
        String expectedGradleVersion,
        String expectedJavaMajorVersion,
        Path imageIdFile) {
}
```

Methoden:

```java
public static RuntimeImageDescriptor fromSystemProperties();
public void verify();
```

System-Properties:

```text
gretl.test.runtimeImage.idFile
gretl.test.runtimeImage.tag
gretl.test.runtimeImage.version
gretl.test.runtimeImage.gradleVersion
gretl.test.runtimeImage.javaVersion
```

`verify()` muss:

- Datei vorhanden;
- genau eine nichtleere Image-ID;
- Format beginnt mit `sha256:`;
- Image lokal vorhanden;
- Version nicht leer;
- Gradle-Version nicht leer;
- Java-Version nicht leer.

### 11.2 Keine impliziten Defaults für Versionen

Für die Image-ID und GRETL-Version sind keine veraltbaren Default-Werte zulässig.

Fehlende Properties müssen einen klaren Fehler erzeugen.

---

## 12. `RuntimeImageBuildExecutor`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/RuntimeImageBuildExecutor.java
```

### 12.1 Konstruktor

```java
public RuntimeImageBuildExecutor(
        RuntimeImageDescriptor image,
        DockerCli docker,
        ContainerUserResolver userResolver,
        RuntimeImageGradleArguments gradleArguments)
```

### 12.2 Methoden

```java
@Override
public GretlBuildResult execute(GretlBuildRequest request);

@Override
public GretlBuildResult executeAndExpectFailure(GretlBuildRequest request);

DockerRunRequest toDockerRunRequest(GretlBuildRequest request);
GretlBuildResult toBuildResult(ProcessResult result);
```

### 12.3 Argumentprofile

Der Executor darf nicht mehr für jeden Lauf pauschal `--no-daemon` und `--offline` setzen. Implementiere explizite Profile:

```java
public enum RuntimeInvocationProfile {
    ONE_SHOT_ONLINE,
    ONE_SHOT_OFFLINE,
    LONG_LIVED_DAEMON
}
```

Gemeinsame Testargumente:

```text
--console=plain
--stacktrace
--rerun-tasks
```

Profil `ONE_SHOT_ONLINE`:

```text
--no-daemon
```

Profil `ONE_SHOT_OFFLINE`:

```text
--no-daemon
--offline
```

Profil `LONG_LIVED_DAEMON`:

```text
--daemon
```

`--daemon` darf entfallen, wenn die Distribution den Daemon nachweislich standardmässig aktiviert und keine Property ihn abschaltet. `--no-daemon` ist in diesem Profil verboten.

`--rerun-tasks` darf entfallen, wenn ein konkreter Test bewusst Up-to-date-, Cache- oder Incremental-Verhalten prüft.

### 12.4 Temporäres Gradle-Home

Pro Ausführung:

```java
Path createIsolatedGradleUserHome(Path projectDirectory);
```

Das Verzeichnis muss:

- ausserhalb des Projekt-Unterverzeichnisses oder klar getrennt liegen;
- leer starten;
- nach Testende von JUnit bereinigt werden;
- nicht zwischen Tests geteilt werden;
- keine Host-Initialisierungsskripte enthalten.

### 12.5 Containername

Methode:

```java
String createContainerName(GretlBuildRequest request);
```

Format sinngemäss:

```text
gretl-e2e-<testclass>-<hash>
```

Maximallänge und erlaubte Zeichen beachten.

### 12.6 Cleanup

Da `docker run --rm` verwendet wird, sollte kein Container verbleiben.

Zusätzlich muss bei Timeout oder Startfehler ein Best-effort-Cleanup erfolgen:

```text
docker rm -f <containerName>
```

Cleanup-Fehler dürfen den ursprünglichen Fehler nicht verdecken, müssen aber als suppressed exception oder Diagnose sichtbar sein.

---

## 13. Runtime-Image-Optionen pro Test

### 13.1 Record `RuntimeImageRunOptions`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/runtime/RuntimeImageRunOptions.java
```

Felder:

```java
public record RuntimeImageRunOptions(
        Optional<String> dockerNetwork,
        boolean networkDisabled,
        Map<String, String> containerEnvironment,
        Map<Path, String> readOnlyMounts,
        Map<Path, String> readWriteMounts) {
}
```

Factory-Methoden:

```java
public static RuntimeImageRunOptions offline();
public static RuntimeImageRunOptions onNetwork(String network);
public RuntimeImageRunOptions withEnvironment(String name, String value);
public RuntimeImageRunOptions withReadOnlyMount(Path host, String container);
public RuntimeImageRunOptions withReadWriteMount(Path host, String container);
```

`offline()` setzt zwingend `networkDisabled=true`.

---

## 14. Gemeinsame Test-Support-Basisklasse

### 14.1 Klasse `AbstractGretlBuildTestSupport`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/support/AbstractGretlBuildTestSupport.java
```

Diese Klasse ersetzt keine JUnit-Testklasse. Sie stellt gemeinsame Infrastruktur bereit.

Felder:

```java
protected Path projectDir;
```

Der konkrete `@TempDir`-Mechanismus kann in den Modulen bleiben.

Methoden:

```java
protected GretlBuildExecutor executor();
protected GretlBuildResult run(String... arguments);
protected GretlBuildResult runAndFail(String... arguments);
protected GretlBuildRequest buildRequest(String... arguments);
protected List<String> defaultArguments();
protected Map<String, String> defaultEnvironment();
protected Set<String> secretValues();
protected RuntimeImageRunOptions runtimeImageRunOptions();
protected void writeSettings(String content);
protected void writeGroovyBuild(String content);
protected Path copyResource(String source, String target);
protected void copyResourceTree(String source, Path target);
```

### 14.2 Defaultverhalten

`runtimeImageRunOptions()` liefert für lokale Tests:

```java
RuntimeImageRunOptions.offline()
```

Servicebasierte Support-Klassen überschreiben diese Methode.

### 14.3 Rückgabetypen bestehender Tests

Bestehende funktionale Tests, die `org.gradle.testkit.runner.BuildResult` verwenden, müssen auf `GretlBuildResult` migriert werden.

Beispiel:

Vorher:

```java
BuildResult result = runAndFail("compressFile");
assertTrue(result.getOutput().contains("missing.xml"));
```

Nachher:

```java
GretlBuildResult result = runAndFail("compressFile");
assertTrue(result.output().contains("missing.xml"));
```

Die fachlichen Assertions dürfen nicht abgeschwächt werden.

### 14.4 TestKit-Modus bleibt erhalten

`TestKitClasspathBuildExecutor` muss weiterhin `withPluginClasspath()` verwenden.

Die Runtime-Image-Änderung darf die schnellen Source-Tests nicht unnötig verlangsamen.

---

## 15. JUnit-Tags

Definiere Konstanten:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/tags/GretlTestTags.java
```

```java
public final class GretlTestTags {
    public static final String EXTERNAL_BUILD = "gretl-external-build";
    public static final String RUNTIME_IMAGE = "runtime-image";
    public static final String RUNTIME_IMAGE_SMOKE = "runtime-image-smoke";
    public static final String RUNTIME_IMAGE_OFFLINE = "runtime-image-offline";
    public static final String REQUIRES_POSTGIS = "requires-postgis";
    public static final String REQUIRES_FTP = "requires-ftp";
    public static final String REQUIRES_S3 = "requires-s3";
    public static final String REQUIRES_HTTP = "requires-http";
}
```

### 15.1 Tagging-Regeln

- Jede Testklasse, die einen externen Gradle-Build startet, erhält `EXTERNAL_BUILD`.
- Jede für das Image geeignete Klasse erhält `RUNTIME_IMAGE`.
- Eine kleine verpflichtende Teilmenge erhält zusätzlich `RUNTIME_IMAGE_SMOKE`.
- Offline-Vertragstests erhalten `RUNTIME_IMAGE_OFFLINE`.
- Service-Tags dienen Dokumentation und gezielter Ausführung.

Tags dürfen nicht dazu verwendet werden, schwierige Tests stillschweigend auszuschliessen.

---

## 16. Gradle-Konfiguration in `gretl-core`

### 16.1 Abhängigkeiten

Ergänze:

```groovy
testImplementation project(':gretl-test-support')
integrationTestImplementation project(':gretl-test-support')
```

Entferne direkte TestKit-Hilfsimplementierungen erst, wenn die neue Abstraktion vollständig verwendet wird.

### 16.2 Task `runtimeImageFunctionalTest`

```groovy
tasks.register('runtimeImageFunctionalTest', Test) {
    group = 'verification'
    description = 'Runs GRETL core functional tests against the built runtime image.'

    dependsOn rootProject.tasks.named('buildRuntimeImageForTest')

    testClassesDirs = sourceSets.test.output.classesDirs
    classpath = sourceSets.test.runtimeClasspath

    useJUnitPlatform {
        includeTags 'runtime-image'
    }

    systemProperty 'gretl.test.executionMode', 'RUNTIME_IMAGE'
    // Weitere Image-Properties über Provider setzen.

    shouldRunAfter tasks.named('test')
}
```

### 16.3 Task `runtimeImageFunctionalSmokeTest`

```groovy
tasks.register('runtimeImageFunctionalSmokeTest', Test) {
    // gleiche Klassen/Classpath
    useJUnitPlatform {
        includeTags 'runtime-image-smoke'
        excludeTags 'requires-postgis', 'requires-ftp', 'requires-s3'
    }
}
```

Der Smoke-Task muss mindestens Core-Datei-, SQLite- und DuckDB-Canaries enthalten.

### 16.4 Task `runtimeImageIntegrationTest`

```groovy
tasks.register('runtimeImageIntegrationTest', Test) {
    dependsOn rootProject.tasks.named('buildRuntimeImageForTest')

    testClassesDirs = sourceSets.integrationTest.output.classesDirs
    classpath = sourceSets.integrationTest.runtimeClasspath

    useJUnitPlatform {
        includeTags 'runtime-image'
    }

    systemProperty 'gretl.test.executionMode', 'RUNTIME_IMAGE'
}
```

### 16.5 Forking

Docker- und Testcontainers-basierte Tests dürfen nicht unkontrolliert parallel auf denselben Ressourcen laufen.

Setze initial:

```groovy
maxParallelForks = 1
```

Parallelisierung darf später nur mit nachgewiesener Isolation erhöht werden.

Gradle `--parallel` muss trotzdem funktionieren, weil Module parallel laufen können. Image-ID, Repository und Testdaten dürfen dadurch nicht kollidieren.

---

## 17. Gradle-Konfiguration in `gretl-geotools`

### 17.1 Abhängigkeit

```groovy
testImplementation project(':gretl-test-support')
```

### 17.2 Task `runtimeImageFunctionalTest`

Wie bei Core, aber mit dem GeoTools-Test-Source-Set.

Der Task muss alle mit `runtime-image` markierten GeoTools-Funktionstests ausführen.

### 17.3 Task `runtimeImageFunctionalSmokeTest`

Mindestens:

```text
ReadShapefile
RasterReclassify oder Vectorize
```

`ReadShapefile` ist zwingend, weil damit die eingebettete Worker-Laufzeit und ihre GeoTools-Abhängigkeiten geprüft werden.

---

## 18. Gradle-Task `verifyRuntimeImagePrerequisites`

### 18.1 Zweck

Der Task prüft vor dem Image-Build:

- Docker CLI verfügbar;
- Docker Daemon erreichbar;
- BuildKit/klassischer Builder kann Images bauen;
- `docker build` unterstützt `--iidfile`;
- benötigte Dateien im Docker-Kontext vorhanden;
- Projektversion gültig;
- `stageRuntimeImage` kann beide Plugin-Publikationen erzeugen.

### 18.2 Implementierung

Bevorzugt als eigene Task-Klasse in `buildSrc`:

```text
buildSrc/src/main/java/ch/so/agi/gretl/build/VerifyRuntimeImagePrerequisitesTask.java
```

Methoden:

```java
@TaskAction
public void verify();

void verifyDockerCli();
void verifyDockerDaemon();
void verifyProjectVersion();
void verifyDockerSources();
```

Die Klasse darf `ExecOperations` injizieren.

Alternativ ist eine robuste Root-Build-Implementierung zulässig. Eine lose Sammlung von Shell-Aufrufen ohne strukturierte Fehlerdiagnose ist nicht ausreichend.

---

## 19. Gradle-Task `buildRuntimeImageForTest`

### 19.1 Eigene Task-Klasse

Datei:

```text
buildSrc/src/main/java/ch/so/agi/gretl/build/BuildRuntimeImageTask.java
```

Annotation:

```java
@DisableCachingByDefault(
        because = "Docker daemon state and local image availability are external to Gradle outputs")
```

Properties:

```java
@InputDirectory
public abstract DirectoryProperty getContextDirectory();

@Input
public abstract Property<String> getImageTag();

@Input
public abstract Property<String> getGretlVersion();

@Input
public abstract Property<String> getGradleVersion();

@OutputFile
public abstract RegularFileProperty getImageIdFile();
```

Methoden:

```java
@TaskAction
public void buildImage();

void deleteStaleImageIdFile();
List<String> buildCommand();
String readAndValidateImageId();
void verifyImageExists(String imageId);
```

### 19.2 Docker-Befehl

Sinngemäss:

```text
docker build
--pull=false
--tag <eindeutiger-test-tag>
--iidfile <absolute-path>/image-id.txt
--build-arg GRETL_VERSION=<project.version>
--build-arg GRADLE_VERSION=<configured-version>
<label/build args falls required>
<runtime-image-context>
```

### 19.3 Test-Tag

Default sinngemäss:

```text
gretl-next-e2e:<sanitized-version>
```

Der Tag muss über folgende Property überschreibbar sein:

```text
-PgretlRuntimeImageTestTag=...
```

### 19.4 Stale-Datei-Schutz

Vor dem Build muss die alte `image-id.txt` gelöscht werden.

Wenn Docker keinen neuen Inhalt schreibt, muss der Task fehlschlagen.

### 19.5 Verifikation

Nach dem Build:

```text
docker image inspect <image-id>
```

Der Task muss prüfen, dass der menschenlesbare Tag auf dieselbe Image-ID zeigt.

---

## 20. Änderungen am Dockerfile

### 20.1 GRETL-Version als Build-Argument

Ergänze:

```dockerfile
ARG GRETL_VERSION
ENV GRETL_VERSION=${GRETL_VERSION}
```

Der Build muss fehlschlagen, wenn `GRETL_VERSION` leer ist.

Dies kann über einen frühen `RUN test -n "$GRETL_VERSION"`-Schritt erfolgen.

### 20.2 OCI-Labels

Setze mindestens:

```dockerfile
LABEL org.opencontainers.image.title="GRETL"
LABEL org.opencontainers.image.version="${GRETL_VERSION}"
LABEL org.opencontainers.image.source="https://github.com/edigonzales/gretl-next"
```

Keine dynamische Build-Zeit verwenden, wenn sie unnötig den Layer-Cache zerstört.

### 20.3 Nicht-Root-Betrieb

`USER gradle` bleibt verbindlich.

Der Contract-Test muss sicherstellen, dass die konfigurierte UID ungleich `0` ist.

### 20.4 DuckDB-Erweiterungen

Die Vorinstallation bleibt erhalten.

Zusätzlich muss der Image-Build oder Contract-Test prüfen, dass die erwarteten Extension-Dateien vorhanden sind.

Die E2E-Suite muss anschliessend eine reale DuckDB-Abfrage mit mindestens `spatial` ausführen.

### 20.5 Keine Testwerkzeuge ins Produktimage

JUnit, Test-Fixtures und `gretl-test-support` dürfen nicht in das Image kopiert werden.

---

## 21. Änderungen an `docker/init.gradle`

### 21.1 Keine harte Default-Version

Unzulässig:

```groovy
def gretlVersion = System.getProperty('gretl.version', '0.1.0-SNAPSHOT')
```

Implementiere stattdessen:

```groovy
def gretlVersion = System.getProperty('gretl.version') ?: System.getenv('GRETL_VERSION')
if (!gretlVersion) {
    throw new GradleException('GRETL version is missing. Set -Dgretl.version or GRETL_VERSION.')
}
```

### 21.2 Entferne `mavenLocal()`

Entferne alle Standardaufrufe von:

```groovy
repositories.mavenLocal()
```

Ein optionaler Entwicklerpfad darf nur so oder ähnlich funktionieren:

```text
-Dgretl.enableMavenLocal=true
```

Er muss standardmässig `false` sein und darf in keinem CI-Test gesetzt werden.

### 21.3 Gebündelte Distribution zuerst

Die vom Agenten gewählte gebündelte GRETL-Distribution muss bei der Auflösung Vorrang haben. Bei einer Maven-basierten Lösung ist die Reihenfolge beispielsweise:

```text
/opt/gretl/repository oder ein gleichwertiger Imagepfad
optionale, vom Consumer oder Betreiber definierte Mirrors
Maven Central / Plugin Portal
```

Der konkrete Pfad ist kein öffentlicher Vertrag, muss aber in `RuntimeImageDescriptor`, `build.info`, Tests und Dokumentation konsistent beschrieben sein.

Die Offline-Tests stellen sicher, dass GRETL und seine gebündelten Laufzeitabhängigkeiten ausschliesslich aus der Image-Distribution aufgelöst werden.

### 21.4 Explizite und versionlose Plugin-DSL

Das Image muss beide Formen unterstützen:

```groovy
plugins {
    id 'ch.so.agi.gretl'
}
```

und:

```groovy
plugins {
    id 'ch.so.agi.gretl' version '<aktuelle-version>'
}
```

Dasselbe gilt für `ch.so.agi.gretl.geotools`.

### 21.5 Strukturierte Plugin- und Dependency-Bereitstellung

Das Init-Script soll primär Konfiguration liefern, nicht alle JARs blind in jeden Buildscript-Classloader legen.

Bevorzugtes Zielbild:

```text
/opt/gretl/repository
├── Plugin-Marker für ch.so.agi.gretl
├── Plugin-Marker für ch.so.agi.gretl.geotools
├── Implementierungs-POMs und .module-Dateien
├── Implementierungs-JARs
└── vollständige verifizierte Runtime-Dependency-Closure
```

Das Init-Script konfiguriert dann:

1. `pluginManagement.repositories` für die moderne Plugin-DSL;
2. Default-Versionen der gebündelten GRETL-Plugins;
3. normale Projekt-Repositories, ohne Consumer-Repositories zu löschen.

Unzulässig als bevorzugter Endzustand:

```groovy
classpath fileTree(dir: '/home/gradle/libs', include: '*.jar')
```

undifferenziert für jedes Projekt.

Der Agent darf einen anderen sauberen Mechanismus wählen, wenn alle Consumer-, Offline-, moderne-Plugin-DSL-, Zusatzplugin- und Classloader-Tests bestehen.

### 21.6 Repository- und Plugin-Komposition

Das Image muss GRETL lokal bereitstellen, aber zusätzliche Plugins zulassen.

Ein Consumer darf beispielsweise Folgendes konfigurieren:

```groovy
pluginManagement {
    repositories {
        maven { url = uri('/fixture/plugin-repo') }
        gradlePluginPortal()
    }
}
```

Die vom Image injizierte GRETL-Konfiguration muss sich additiv verhalten. Sie darf den oben definierten Repositoryblock nicht ersetzen.

### 21.7 Diagnostik

Bei fehlendem lokalen Repository oder falscher Version muss die Fehlermeldung die erwartete Version und den Repositorypfad enthalten.

---

## 22. Änderungen an `/usr/local/bin/gretl`

Der Launcher bleibt klein, POSIX-kompatibel und daemonneutral.

### 22.1 Verbindlicher Launcher-Vertrag

Bevorzugtes Zielbild:

```sh
#!/bin/sh
set -eu

if [ -f /home/gradle/build.info ]; then
    cat /home/gradle/build.info
fi

exec gradle --init-script /home/gradle/init.gradle "$@"
```

Gleichwertige Pfade wie `/opt/gretl/init.gradle` sind erlaubt, wenn Image, Dokumentation und Tests konsistent sind.

Der Launcher darf standardmässig **nicht** hinzufügen:

```text
--no-daemon
--offline
--rerun-tasks
```

Diese Optionen gehören zum Aufrufer beziehungsweise zum Testprofil.

Prüfe zusätzlich:

- Argumente werden als getrennte POSIX-Argumente weitergereicht;
- Exit-Code von Gradle wird unverändert zurückgegeben;
- keine Secrets werden ausgegeben;
- keine Shell-Neuinterpretation oder `eval`-Verwendung;
- ein Benutzer kann `--no-daemon`, `--offline`, `--project-dir`, `-P...`, `-D...` und Tasknamen frei kombinieren;
- der Launcher funktioniert in One-shot- und Servicecontainern.

### 22.2 One-shot-Betrieb

Das Produktimage darf weiterhin einen direkten Aufruf unterstützen:

```bash
docker run --rm \
  --mount type=bind,src="$PWD/job",dst=/home/gradle/project \
  <image> \
  --no-daemon --project-dir=/home/gradle/project <task>
```

Dieser Modus ist für CI-Isolation, Offline-Tests und einmalige Jobs vorgesehen.

### 22.3 Langlebiger Servicebetrieb

Das Image muss mit einer Compose- oder Kubernetes-Konfiguration als langlebiger Container betreibbar sein. Das Produktimage muss dafür nicht zwingend `sleep` als Default-Entry-Point verwenden.

Beispiel:

```yaml
services:
  gretl-service:
    image: <image>
    init: true
    entrypoint: ["sleep", "infinity"]
    volumes:
      - ./jobs:/home/gradle/project
      - gretl-gradle-home:/home/gradle/.gradle
```

Jobaufruf:

```bash
docker compose exec gretl-service \
  gretl --project-dir=/home/gradle/project/my-job <task>
```

Das Image darf alternativ ein eigenes neutrales `gretl-service`-Kommando oder `tini` bereitstellen. Entscheidend ist, dass der Containerprozess stabil lebt und `docker exec`-Aufrufe denselben `GRADLE_USER_HOME` und Benutzer verwenden.

### 22.4 Daemon-Lebenszyklus

Verbindlich:

- Daemon ist standardmässig erlaubt;
- `gradle --status` funktioniert im Servicecontainer;
- `gretl --stop` beziehungsweise `gradle --stop` funktioniert;
- identische Gradle-Version, Java-Home und JVM-Argumente führen zur Wiederverwendung;
- unterschiedliche inkompatible JVM-Argumente dürfen einen separaten Daemon erzeugen und müssen diagnostizierbar sein;
- der Servicecontainer darf nicht beendet werden, wenn der Gradle-Client nach einem Build endet.

Der Contract-Test muss einen absichtlich fehlschlagenden Gradle-Task starten und prüfen, dass der jeweilige `docker exec`- beziehungsweise One-shot-Aufruf einen Nichtnull-Exit-Code liefert.

---

## 23. Image-Vertragstests

### 23.1 Source-Set und Task

Im Modul `gretl-test-support`:

```groovy
sourceSets {
    runtimeImageContractTest {
        compileClasspath += sourceSets.main.output
        runtimeClasspath += output + compileClasspath
    }
}
```

Task:

```groovy
tasks.register('runtimeImageContractTest', Test) {
    dependsOn rootProject.tasks.named('buildRuntimeImageForTest')
    testClassesDirs = sourceSets.runtimeImageContractTest.output.classesDirs
    classpath = sourceSets.runtimeImageContractTest.runtimeClasspath
    useJUnitPlatform()
}
```

### 23.2 Klasse `RuntimeImageContractTest`

Datei:

```text
gretl-test-support/src/runtimeImageContractTest/java/ch/so/agi/gretl/test/runtime/RuntimeImageContractTest.java
```

Methoden:

```java
@Test
void imageIdFileReferencesExistingLocalImage();

@Test
void imageTagAndImageIdReferenceTheSameImage();

@Test
void imageUsesExpectedEntrypoint();

@Test
void imageUsesExpectedWorkingDirectory();

@Test
void imageRunsAsNonRootUser();

@Test
void imageDeclaresExpectedGretlVersionLabel();

@Test
void buildInfoMatchesProjectVersionGradleAndJava();

@Test
void imageContainsRuntimeInitScriptAndLauncher();

@Test
void imageContainsBothPluginMarkerPublications();

@Test
void imageContainsBothPluginImplementationArtifacts();

@Test
void imageContainsGeoToolsWorkerRuntimeArtifacts();

@Test
void imageContainsExpectedDuckDbExtensions();

@Test
void imageDoesNotContainSourceOrJavadocJars();

@Test
void launcherPropagatesGradleFailureExitCode();

@Test
void mountedProjectDirectoryIsWritableAndHostReadable();
```

### 23.3 Konkrete Assertions

#### `imageUsesExpectedEntrypoint()`

Prüfe `docker image inspect` strukturiert gegen den dokumentierten Image-Vertrag.

Zulässige Zielbilder sind insbesondere:

```text
Config.Entrypoint == ["gretl"]
```

oder ein neutraler Entry Point beziehungsweise kein fester Entry Point, sofern der dokumentierte Aufruf `docker run ... gretl ...` funktioniert.

Der Test darf nicht bloss einen hart codierten historischen Wert prüfen. Er muss beweisen, dass:

- der dokumentierte One-shot-Aufruf funktioniert;
- der Launcher im Image vorhanden und ausführbar ist;
- der Servicebetrieb den Entry Point kontrolliert überschreiben kann.

#### `imageRunsAsNonRootUser()`

Prüfe:

```text
Config.User ist gesetzt
Config.User ist nicht "0"
Config.User ist nicht "root"
```

Zusätzlich:

```text
docker run --rm --entrypoint id <image-id> -u
```

Erwartung: Ausgabe ungleich `0`.

#### `buildInfoMatchesProjectVersionGradleAndJava()`

Lies `/home/gradle/build.info` im Container und vergleiche exakt mit den Gradle-System-Properties.

#### `imageContainsBothPluginMarkerPublications()`

Prüfe Markerpfade für:

```text
ch.so.agi.gretl
ch.so.agi.gretl.geotools
```

Die Prüfung muss die tatsächliche Maven-Pfadkonvention verwenden und POM-Inhalte lesen.

#### `imageContainsGeoToolsWorkerRuntimeArtifacts()`

Prüfe nicht nur Dateinamen in einem historischen `libs`-Verzeichnis. Ermittle die tatsächliche Distribution über `RuntimeImageDescriptor`, öffne das `gretl-geotools`-Artefakt ausserhalb oder innerhalb des Containers und verifiziere:

```text
gretl-geotools-worker-classpath/
gretl-geotools-worker-classpath/lib/
worker-runtime JAR
mindestens die erwarteten GeoTools-Kernmodule
```

#### `mountedProjectDirectoryIsWritableAndHostReadable()`

Lasse einen minimalen Gradle-Task eine Datei schreiben. Prüfe anschliessend auf dem Host:

- Datei existiert;
- Inhalt stimmt;
- Datei kann gelöscht werden.

### 23.4 Klasse `RuntimeImageDaemonReuseTest`

Datei:

```text
gretl-test-support/src/runtimeImageContractTest/java/ch/so/agi/gretl/test/runtime/RuntimeImageDaemonReuseTest.java
```

Verbindliche Methoden:

```java
@Test
void longLivedContainerReusesSameCompatibleDaemonPid();

@Test
void secondJobDirectoryCanUseSameDaemonWithoutStateLeak();

@Test
void changedBuildScriptIsObservedByReusedDaemon();

@Test
void stopCommandStopsDaemonButKeepsServiceContainerAlive();

@Test
void nextBuildStartsNewDaemonAfterStop();

@Test
void explicitNoDaemonBuildDoesNotReplaceOrDependOnIdleDaemon();

@Test
void daemonUsesExpectedGradleUserHomeAndContainerUser();
```

### 23.5 Klasse `RuntimeImageServiceContainer`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/runtime/RuntimeImageServiceContainer.java
```

API:

```java
public final class RuntimeImageServiceContainer implements AutoCloseable {
    public static RuntimeImageServiceContainer start(
            RuntimeImageDescriptor image,
            Path jobsRoot,
            Path gradleUserHome,
            Optional<String> network,
            Optional<String> user);

    public ProcessResult execGretl(Path relativeProjectDir, List<String> arguments);
    public ProcessResult execGradle(List<String> arguments);
    public Set<Long> daemonPids();
    public boolean isRunning();
    public String containerId();
    @Override public void close();
}
```

`daemonPids()` soll bevorzugt `gradle --status` strukturiert auswerten. Ein installierter JDK-`jps`-Befehl oder `ps` darf ergänzend genutzt werden, ist aber nicht alleinige Grundlage.

### 23.6 Zusatzplugin-Kompositionstest

Datei:

```text
gretl-test-support/src/runtimeImageContractTest/java/ch/so/agi/gretl/test/runtime/RuntimeImageAdditionalPluginTest.java
```

Szenario:

1. Publiziere während des Test-Builds ein minimales Fixture-Plugin in ein separates lokales Maven-Repository.
2. Mounte dieses Repository read-only in den Container.
3. Erzeuge ein Consumer-Projekt, das GRETL und das Fixture-Plugin anwendet.
4. Führe je einen Task beider Plugins aus.
5. Prüfe semantische Ausgaben beider Tasks.
6. Wiederhole mindestens einen Lauf im langlebigen Daemon-Container.

Dieser Test darf weder Gradle Plugin Portal noch Maven Central benötigen.

---

## 24. Offline-Bootstrap-Tests

### 24.1 Klasse `RuntimeImageOfflineResolutionTest`

Datei:

```text
gretl-test-support/src/runtimeImageOfflineTest/java/ch/so/agi/gretl/test/runtime/RuntimeImageOfflineResolutionTest.java
```

Methoden:

```java
@Test
void appliesCorePluginWithVersionlessGroovyDslOffline();

@Test
void appliesCorePluginWithExplicitVersionGroovyDslOffline();

@Test
@Test
void appliesGeotoolsPluginWithVersionlessGroovyDslOffline();

@Test
void appliesGeotoolsPluginWithExplicitVersionGroovyDslOffline();

@Test
@Test
void executesGzipWithEmptyGradleHomeAndNoNetwork();

@Test
void executesReadShapefileWithEmptyGradleHomeAndNoNetwork();

@Test
void executesDuckDbSpatialQueryWithoutDownloadingExtensions();

@Test
void failsWithoutEmbeddedPluginRepositoryAndRuntimeLibraries();

@Test
void outputContainsNoDependencyDownloadAttempt();

@Test
void independentRunsDoNotShareGradleCaches();
```

### 24.2 Testprojekt-Erzeugung

Implementiere Hilfsklasse:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/project/GradleTestProject.java
```

Methoden:

```java
public static GradleTestProject create(Path directory);
public GradleTestProject settingsGroovy(String content);
public GradleTestProject buildGroovy(String content);
public GradleTestProject file(String path, String content);
public Path path(String relativePath);
```

### 24.3 Keine Projekt-Repositories nötig für GRETL

Die minimalen Plugin-Projekte dürfen für die GRETL-Plugins keine externen Repositories konfigurieren.

Das Image-Init-Script muss die lokale Auflösung ermöglichen.

### 24.4 Negative Isolation

`failsWithoutEmbeddedPluginRepositoryAndRuntimeLibraries()` startet das Image mit:

```text
-Dgretl.mavenRepo=/tmp/does-not-exist
-Dgretl.libsDir=/tmp/does-not-exist
--offline
--network none
```

Erwartung:

- Build schlägt fehl;
- Fehler benennt fehlendes Plugin oder Repository;
- kein Source-Fallback;
- kein Downloadversuch;
- kein Erfolg durch Host-Cache.

### 24.5 Download-Assertion

Die Tests müssen mindestens nach folgenden Mustern suchen und deren Abwesenheit prüfen:

```text
Downloading
services.gradle.org
repo.maven.apache.org
plugins.gradle.org
jars.sogeo.services
repo.osgeo.org
maven.geo-solutions.it
```

Diese Logprüfung ergänzt, ersetzt aber nicht `--network none`.

---

## 25. Docker-Netzwerke für Service-E2E

### 25.1 Grundprinzip

Host-gemappte Ports sind innerhalb eines zweiten Containers nicht mit `localhost` erreichbar.

Verwende deshalb pro Testklasse ein isoliertes Testcontainers-Netzwerk:

```java
static final Network NETWORK = Network.newNetwork();
```

Servicecontainer erhalten Netzwerk-Aliase:

```text
postgis
ftp
s3
http
```

Der GRETL-Runtime-Container wird mit derselben Docker-Network-ID gestartet.

### 25.2 Keine Host-Netzwerkannahme

Unzulässig:

```text
--network host
```

Das muss insbesondere auf Docker Desktop portabel bleiben.

### 25.3 Host- und Container-Endpunkte trennen

Support-Klassen müssen zwei Endpunkte kennen:

```text
Host-Endpunkt: für Assertions aus dem JUnit-Prozess
Container-Endpunkt: für GRETL im Runtime-Image
```

Beispiel PostGIS:

```text
Host: POSTGIS.getJdbcUrl()
Container: jdbc:postgresql://postgis:5432/gretl
```

### 25.4 Klasse `DualEndpoint`

Datei:

```text
gretl-test-support/src/main/java/ch/so/agi/gretl/test/runtime/DualEndpoint.java
```

```java
public record DualEndpoint(String hostValue, String containerValue) {
    public String forMode(GretlExecutionMode mode);
}
```

`forMode()` liefert für `RUNTIME_IMAGE` den Containerwert, sonst den Hostwert.

---

## 26. PostGIS-Support

### 26.1 Refactoring `PostgisIntegrationTestSupport`

Die bestehende Klasse muss auf die gemeinsame Ausführungsabstraktion umgestellt werden.

Füge statisches Netzwerk hinzu:

```java
static final Network NETWORK = Network.newNetwork();
```

Konfiguriere PostGIS:

```java
.withNetwork(NETWORK)
.withNetworkAliases("postgis")
```

### 26.2 Methoden

Ergänze oder refaktoriere:

```java
String hostPgUrl();
String containerPgUrl();
String effectivePgUrl();
String pgUser();
String pgPassword();

@Override
RuntimeImageRunOptions runtimeImageRunOptions();

@Override
List<String> defaultArguments();
```

`effectivePgUrl()` verwendet `DualEndpoint`.

### 26.3 Runtime-Image-Option

```java
@Override
protected RuntimeImageRunOptions runtimeImageRunOptions() {
    return RuntimeImageRunOptions.onNetwork(NETWORK.getId());
}
```

### 26.4 Assertions bleiben hostseitig

Datenbankprüfungen verwenden weiterhin den Host-JDBC-Endpunkt.

Damit wird nachgewiesen, dass der Container Daten in denselben Testcontainer geschrieben hat.

### 26.5 Transaktions- und Fehlerfälle

Mindestens folgende bestehende Szenarien müssen im Image-Modus laufen:

- `SqlExecutor` vollständiger Rollback;
- `SqlExecutor` Rollback über Parameter-Sets;
- `Db2Db` Kopierkette;
- `Db2Db` Geometrie/SRID;
- ili2pg Import-/Export-/Validate-Kette.

---

## 27. HTTP-Support

### 27.1 Kein Host-Only-JDK-Server für Runtime-Modus

Ein `HttpServer` im JUnit-Prozess ist für Containerzugriff nur mit zusätzlicher Host-Port-Exposition geeignet.

Bevorzugt wird ein kleiner gepinnter HTTP-Testcontainer im gemeinsamen Netzwerk.

Zulässige Varianten:

- WireMock-Container mit fixer Version;
- nginx/httpd mit statischen Fixtures;
- einfacher eigener Testcontainer, falls bereits vorhanden.

Keine `latest`-Tags.

### 27.2 Alias

```text
http
```

Container-URL:

```text
http://http:<interner-port>/...
```

### 27.3 Testklasse

```text
gretl-core/src/test/java/ch/so/agi/gretl/CurlFunctionalTest.java
```

Muss im Runtime-Modus mindestens folgende Methoden ausführen:

```java
void downloadsBinaryResponse();
void executesGetWithExpectedStatusAndBody();
void executesPostRequest();
void executesMultipartUpload();
void sendsBasicAuthentication();
void doesNotLogAuthorizationSecret();
```

Die vorhandenen fachlichen Assertions sind zu erhalten.

---

## 28. FTP-Support

### 28.1 Servicecontainer

Verwende den bereits vorhandenen gepinnten FTP-Testcontainer oder eine gleichwertige gepinnte Variante.

Netzwerkalias:

```text
ftp
```

### 28.2 Endpunkte

Hostseite:

- gemappter Port für Assertions und Setup.

Containerseite:

```text
ftp:21
```

### 28.3 Testklasse

Die bestehende Docker-FTP-Integration muss im Runtime-Modus alle öffentlichen FTP-Tasks prüfen:

```text
FtpUpload
FtpList
FtpDownload
FtpDelete
```

Methoden oder Szenarien:

```java
void uploadsListsDownloadsAndDeletesFile();
void preservesBinaryFileContent();
void failsClearlyForMissingRemoteFile();
void doesNotLogPassword();
```

### 28.4 Passive Ports

Falls der FTP-Server passive Ports benötigt, müssen diese im gemeinsamen Docker-Netzwerk korrekt konfiguriert werden. Verlasse dich nicht auf Host-Port-Ranges, wenn Container-zu-Container-Kommunikation möglich ist.

---

## 29. S3-Support

### 29.1 Lokaler S3-Dienst

Verwende die vorhandene Floci-/Testcontainers-Lösung oder eine gleichwertige gepinnte S3-kompatible Runtime.

Netzwerkalias:

```text
s3
```

### 29.2 Container-Endpunkt

Sinngemäss:

```text
http://s3:<interner-port>
```

### 29.3 Pfadstil

Konfiguriere den S3-Client explizit für Path-Style-Zugriff, falls der lokale Dienst dies benötigt.

### 29.4 Tests

Die Runtime-Image-Suite muss semantisch prüfen:

```text
S3Upload
S3Download
S3Delete
S3Bucket2Bucket
```

Mindestens:

```java
void uploadsFileWithExpectedContentTypeAndMetadata();
void uploadsDirectoryTree();
void downloadsObjectByteIdentically();
void copiesBucketContents();
void deletesSingleObject();
void deletesBucketContents();
void failsClearlyForMissingBucket();
void doesNotLogAccessSecret();
```

Keine echten AWS-/Exoscale-Credentials verwenden.

---

## 30. GeoTools-Worker-E2E

### 30.1 Beweisziel

Die Source-Tests können GeoTools über den Test-Classpath bereitstellen. Der Runtime-Image-Test muss beweisen, dass die im `gretl-geotools`-JAR eingebettete Worker-Laufzeit im Image vollständig ist.

### 30.2 Zwingende Tests

Klasse:

```text
gretl-geotools/src/test/java/ch/so/agi/gretl/geotools/GretlGeotoolsFunctionalTest.java
```

oder aufgeteilte bestehende Klassen.

Mindestens folgende Methoden müssen im Runtime-Modus laufen:

```java
void appliesGeotoolsPluginFromRuntimeImage();
void readsShapefileInIsolatedWorker();
void reclassifiesRasterAndPreservesExpectedGridMetadata();
void vectorizesSelectedRasterValuesIntoGeoPackage();
void reportsValidationErrorForMissingInput();
void supportsGroovyDslInRuntimeImage();
```

### 30.3 Semantische Assertions

#### ReadShapefile

Prüfe mindestens:

- Feature-Anzahl;
- Geometrietyp;
- CRS oder SRID;
- erwartete Attributnamen.

#### RasterReclassify

Prüfe mindestens:

- Ausgabedatei existiert;
- Rasterdimensionen;
- NoData-Behandlung;
- erwartete Klassenwerte an mehreren Zellen;
- CRS bleibt erhalten.

#### Vectorize

Prüfe mindestens:

- GeoPackage existiert;
- Layername;
- Feature-Anzahl;
- Geometrien nicht leer;
- erwartete Klassenattribute;
- SRID.

### 30.4 Worker-Isolation

Der Test muss durch Log oder funktionalen Nachweis sicherstellen, dass der Worker-Classpath aus dem Plugin-JAR verwendet wird.

Es darf kein GeoTools-JAR aus dem Host-Test-Classpath in den Container gemountet werden.

---

## 31. DuckDB-E2E

### 31.1 Zwingende Offline-Prüfung

Mit `--network none`:

```sql
INSTALL spatial;
LOAD spatial;
SELECT ST_AsText(ST_Point(2600000, 1200000));
```

Je nach DuckDB-Verhalten kann `INSTALL` offline auf bereits vorhandene Artefakte zugreifen. Falls nur `LOAD` korrekt ist, muss der Test exakt zum produktiven Contract passen und dokumentiert werden.

### 31.2 Erweiterungen

Prüfe mindestens:

```text
spatial
postgres
excel
```

Nicht nur Dateiexistenz, sondern reale Ladefähigkeit.

### 31.3 Tasks

Runtime-Image-E2E für:

```text
DuckDbSqlExecutor
Ili2duckdbImportSchema
Ili2duckdbImport
Ili2duckdbExport
```

Mindestens eine Kette:

1. Schema erzeugen;
2. XTF importieren;
3. SQL-Abfrage/Transformation;
4. XTF exportieren;
5. Ausgabe validieren.

---

## 32. INTERLIS-/ili2pg-E2E

### 32.1 Lokale Fixtures

Alle Modelle und Transferdateien müssen aus Testressourcen kommen.

Vermeide öffentliche Repository-Downloads. Falls ili2pg standardmässig Repositories kontaktiert, konfiguriere lokale Model-Repositories explizit.

### 32.2 Verpflichtende Kette

Eine Image-E2E-Kette muss mindestens folgende Tasks umfassen:

```text
Ili2pgImportSchema
Ili2pgImport
Ili2pgValidate
Ili2pgExport
Ili2pgReplace oder Ili2pgUpdate
Ili2pgDelete
```

Die Kette darf auf mehrere Testmethoden verteilt werden, muss aber isoliert und reproduzierbar bleiben.

### 32.3 Assertions

- Tabellen und Schemas vorhanden;
- erwartete Anzahl Objekte;
- Dataset-/Basket-IDs;
- Geometrie-SRID;
- Validation erfolgreich;
- exportierte XTF vorhanden und parsbar;
- Replace/Update verändert die erwarteten Werte;
- Delete entfernt nur das Ziel-Dataset.

### 32.4 Fehlerfall

Mindestens ein absichtlich ungültiges XTF muss:

- Nichtnull-Exit-Code erzeugen;
- einen verständlichen Validierungsfehler liefern;
- keine teilweise importierten Daten hinterlassen, wenn atomare Semantik erwartet wird.

---

## 33. Coverage-Matrix für alle öffentlichen Tasks

Lege an:

```text
docs/testing/runtime-image-coverage.yaml
```

Schema sinngemäss:

```yaml
version: 1
tasks:
  Gzip:
    classification: DIRECT_E2E
    testClass: ch.so.agi.gretl.GzipFunctionalTest
    testMethods:
      - compressesPlanregisterFixtureAndCreatesParentDirectory
  CsvExport:
    classification: COVERED_BY_CHAIN
    testClass: ch.so.agi.gretl.RuntimeImageDatabasePipelineTest
    testMethods:
      - importsTransformsAndExportsCsv
```

Jeder Eintrag enthält:

```text
classification
testClass
testMethods
module
reason (nur falls nötig)
```

### 33.1 Verbindliche Taskliste

Die Matrix muss mindestens die aktuell dokumentierten öffentlichen Tasks enthalten:

```text
Av2ch
Av2geobau
Csv2Excel
CsvExport
CsvImport
CsvValidator
Curl
Db2Db
DuckDbSqlExecutor
FtpDelete
FtpDownload
FtpList
FtpUpload
Gpkg2Dxf
Gpkg2Shp
GpkgExport
GpkgImport
GpkgValidator
Gzip
Ili2duckdbExport
Ili2duckdbImport
Ili2duckdbImportSchema
Ili2gpkgImport
Ili2pgDelete
Ili2pgExport
Ili2pgImport
Ili2pgImportSchema
Ili2pgReplace
Ili2pgUpdate
Ili2pgValidate
IliValidator
JsonImport
JsonValidator
RasterReclassify
ReadShapefile
S3Bucket2Bucket
S3Delete
S3Download
S3Upload
ShpExport
ShpImport
ShpValidator
SqlExecutor
Vectorize
XslTransformer
```

### 33.2 Inventory-Test

Klasse:

```text
gretl-test-support/src/test/java/ch/so/agi/gretl/test/coverage/RuntimeImageCoverageInventoryTest.java
```

Methoden:

```java
@Test
void everyPublicTaskHasCoverageEntry();

@Test
void everyCoverageEntryReferencesExistingTask();

@Test
void everyDirectEntryReferencesExistingTestClassAndMethod();

@Test
void everyCoveredByChainEntryHasNonEmptyReasonAndScenario();

@Test
void noTaskIsMarkedNotApplicableWithoutReason();

@Test
void noRuntimeImageTestMethodIsMissingRuntimeImageTag();
```

Die öffentliche Taskliste soll nach Möglichkeit aus derselben Task-Metadatenquelle abgeleitet werden, die auch die Dokumentation erzeugt. Eine zweite hart codierte Java-Liste ist zu vermeiden.

---

## 34. Verbindliche fachliche Szenarien nach Taskfamilie

Die Coverage-Matrix muss mindestens folgende Szenarien enthalten.

### 34.1 Datei- und Transformationsaufgaben

| Task | Mindestnachweis |
|---|---|
| Gzip | Byte-identische Dekompression, Parent-Verzeichnis wird erzeugt. |
| XslTransformer | XML wird mit Fixture-XSLT transformiert; Inhalt strukturell geprüft. |
| Csv2Excel | XLSX existiert; Sheetname, Zellen und Datentypen stimmen. |
| Av2ch | Output vorhanden und fachlich parsbar; erwartete Objekte. |
| Av2geobau | DXF vorhanden; erwartete Layer/Elemente. |
| Gpkg2Dxf | DXF aus Fixture-GeoPackage mit erwarteten Elementen. |
| Gpkg2Shp | vollständiges Shapefile-Set und erwartete Features. |

### 34.2 Datenbankaufgaben

| Task | Mindestnachweis |
|---|---|
| SqlExecutor | SQL-Reihenfolge, Parameter-Sets, vollständiger Rollback. |
| Db2Db | Zeilenanzahl, Geometrie, SRID, Fetch-Size-/Chain-Szenario. |
| CsvImport | importierte Zeilen und Datentypen. |
| CsvExport | Header, Reihenfolge, Werte, Encoding. |
| JsonImport | JSON-Text vollständig und korrekt gespeichert. |
| GpkgImport | Feature-Anzahl und Geometrie. |
| GpkgExport | Layer, Feature-Anzahl, SRID. |
| ShpImport | importierte Features und Attribute. |
| ShpExport | `.shp/.shx/.dbf/.prj`, Feature-Anzahl und CRS. |

### 34.3 Validatoren

| Task | Mindestnachweis |
|---|---|
| CsvValidator | gültige Datei erfolgreich; ungültige Datei mit klarer Meldung. |
| GpkgValidator | gültig/ungültig. |
| ShpValidator | gültig/ungültig. |
| JsonValidator | gültig/ungültig. |
| IliValidator | gültig/ungültig und lokales Modell. |

### 34.4 Netzwerkaufgaben

| Task | Mindestnachweis |
|---|---|
| Curl | GET, POST, Binary, Multipart, Auth, Secret-Redaktion. |
| FtpUpload/List/Download/Delete | vollständiger Roundtrip. |
| S3Upload/Download/Delete/Bucket2Bucket | vollständiger Roundtrip inkl. Metadaten. |

### 34.5 INTERLIS-Datenbanken

| Task | Mindestnachweis |
|---|---|
| Ili2gpkgImport | GeoPackage-Struktur und Objekte. |
| Ili2duckdbImportSchema/Import/Export | vollständige lokale Kette. |
| Ili2pgImportSchema/Import/Export/Validate/Replace/Update/Delete | vollständige PostGIS-Kette. |

### 34.6 GeoTools

| Task | Mindestnachweis |
|---|---|
| ReadShapefile | Worker lädt Shapefile, Feature-/CRS-Prüfung. |
| RasterReclassify | Zellwerte, Dimensionen, CRS. |
| Vectorize | GeoPackage-Layer, Features, Attribute, CRS. |

---

## 35. Smoke-Testauswahl

Die mit `runtime-image-smoke` markierte Teilmenge muss mindestens enthalten:

```text
Gzip
SqlExecutor gegen SQLite
DuckDbSqlExecutor mit spatial
Db2Db gegen PostGIS
Curl GET
FTP Roundtrip
S3 Upload/Download
Ili2pg ImportSchema + Import + Export
ReadShapefile
```

Ziel der Smoke-Suite:

- erkennt Packaging- und Connectivity-Fehler schnell;
- läuft vor der vollständigen Matrix;
- ersetzt die vollständige Matrix nicht.

---

## 36. Fehler- und Negativtests

### 36.1 Image fehlt

Wenn `image-id.txt` auf ein nicht existentes Image zeigt:

- Test schlägt vor Containerstart fehl;
- Meldung enthält Datei und Image-ID;
- kein Pull wird versucht.

### 36.2 Falsche GRETL-Version

Manipuliere für einen Test `-Dgretl.version=<nicht-existente-version>`.

Erwartung:

- Plugin-Auflösung schlägt fehl;
- keine andere Version wird verwendet;
- Meldung enthält angeforderte Version.

### 36.3 Fehlendes lokales Repository

Siehe Offline-Negativtest.

### 36.4 Fehlende Runtime-Libs

Starte mit leerem `gretl.libsDir`.

Erwartung:

- ein Task mit externer Runtime-Abhängigkeit schlägt fehl;
- Fehler benennt fehlende Klasse/Abhängigkeit;
- kein Download und kein Host-Fallback.

Dieser Test darf nicht auf fragilen exakten Stacktrace-Text festgelegt werden.

### 36.5 Fehlender GeoTools-Worker

Erzeuge für den Negativtest kein modifiziertes Produktimage im normalen Buildpfad. Stattdessen darf ein temporäres abgeleitetes Testimage gebaut werden, das die Worker-Ressourcen entfernt.

Erwartung:

- `ReadShapefile` schlägt reproduzierbar fehl;
- Fehler verweist auf fehlende Worker-Laufzeit;
- damit wird bewiesen, dass der positive Test tatsächlich diese Ressourcen verwendet.

Der abgeleitete Negativtest darf nicht publiziert werden.

### 36.6 Container-Exit-Code

Ein absichtlich fehlschlagender GRETL-Task muss:

- Gradle-Fehler erzeugen;
- Container-Exit-Code ungleich `0`;
- `executeAndExpectFailure()` erfolgreich zurückkehren;
- `execute()` mit klarer Assertion/Exception fehlschlagen.

### 36.7 Timeout

Erzeuge einen Test-Gradle-Task, der länger als ein kurzer Testtimeout läuft.

Prüfe:

- Prozess wird beendet;
- Container wird entfernt;
- Fehlermeldung nennt Timeout und Containername;
- kein Zombie-Container bleibt zurück.

### 36.8 Secret-Redaktion

Mindestens für:

```text
PostGIS password
FTP password
S3 secret key
HTTP Authorization
```

Prüfe:

- Secret nicht in JUnit-Ausgabe;
- Secret nicht in `GretlBuildResult.output()`;
- Secret nicht in sanitized command;
- fachliche Authentisierung funktioniert trotzdem.

---

## 37. Dateisystem- und Mount-Tests

### 37.1 Pfade mit Leerzeichen

Mindestens ein Testprojekt liegt in einem Pfad mit Leerzeichen und Nicht-ASCII-Zeichen:

```text
GRETL E2E äöü
```

Prüfe, dass Mount und Gradle-Build funktionieren.

### 37.2 Relative Pfade

GRETL-Builds müssen relative Fixturepfade verwenden, soweit dies dem Benutzervertrag entspricht.

### 37.3 Schreibrechte

Prüfe:

- `build/` im gemounteten Projekt wird erzeugt;
- Output ist hostseitig lesbar;
- Host kann Output löschen;
- keine Root-owned Dateien auf Linux, wenn Benutzerstrategie aktiv ist.

### 37.4 Read-only-Mount

Mindestens ein Fixture-Verzeichnis soll read-only gemountet werden, falls zusätzliche Mounts verwendet werden. Der Task darf Input nicht verändern.

### 37.5 Kein Zugriff ausserhalb des Projekt-Mounts

Das Testprojekt darf keine Host-Pfade ausserhalb expliziter Mounts sehen.

---

## 38. Reproduzierbarkeit und Cache-Isolation

### 38.1 Zwei unabhängige One-shot-Läufe

Führe denselben Gzip- oder SQL-Test zweimal in getrennten kurzlebigen Containern mit getrenntem Gradle-Home aus.

Prüfe:

- beide Läufe erfolgreich;
- zweiter Lauf verwendet keinen Cache des ersten;
- Resultate byte-identisch beziehungsweise semantisch identisch.

### 38.1.1 Bewusster Gegenpol: Service-Reuse

Führe zusätzlich zwei kompatible Builds im selben langlebigen Servicecontainer mit demselben Gradle-Home aus.

Prüfe:

- derselbe Daemon-PID wird verwendet;
- Buildskriptänderungen werden erkannt;
- fachliche Resultate bleiben korrekt;
- der Service-Test ist der einzige Testpfad, der diesen Zustand bewusst teilt.

### 38.2 Keine Up-to-date-Verdeckung

Standardmässig `--rerun-tasks` verwenden.

Ein separater Test darf Up-to-date-Verhalten bewusst prüfen.

### 38.3 Keine Abhängigkeit vom Host

Vor Runtime-Teststart darf kein `publishToMavenLocal` notwendig sein.

CI muss auf frischem Runner funktionieren.

### 38.4 Zeitzone und Locale

Setze in den Runtime-Tests deterministisch:

```text
TZ=Europe/Zurich
LANG=C.UTF-8
LC_ALL=C.UTF-8
```

Falls das Base-Image `C.UTF-8` nicht unterstützt, wähle eine im Image vorhandene UTF-8-Locale und dokumentiere sie.

Tests mit Datum/Zeit müssen exakte erwartete Werte verwenden.

---

## 39. Testberichte und Diagnostik

### 39.1 Separate Reports

Jeder Runtime-Task erhält eigene JUnit-XML- und HTML-Reports.

Beispiele:

```text
gretl-core/build/reports/tests/runtimeImageFunctionalTest
gretl-core/build/reports/tests/runtimeImageIntegrationTest
gretl-geotools/build/reports/tests/runtimeImageFunctionalTest
gretl-test-support/build/reports/tests/runtimeImageContractTest
gretl-test-support/build/reports/tests/runtimeImageOfflineTest
gretl-test-support/build/reports/tests/runtimeImageServiceTest
```

### 39.2 Containerdiagnostik bei Fehler

Bei Fehler muss die Meldung enthalten:

- Testklasse und Methode;
- Image-ID;
- Image-Tag;
- Containername;
- Docker-Netzwerk;
- sanitisiertes Kommando;
- Exit-Code;
- Timeout;
- stdout;
- stderr;
- Projektpfad;
- Gradle-Home-Pfad.

Secrets müssen redigiert bleiben.

### 39.3 Image-Metadaten als CI-Artefakt

CI lädt bei Erfolg und Fehler mindestens hoch:

```text
build/runtime-image/test/image-id.txt
build/runtime-image/docker/build.info
alle Runtime-Image-Testreports
```

Optional:

```text
docker image inspect JSON
```

Kein vollständiges Image-Tar als Standardartefakt hochladen, ausser dies wird für Job-Splitting benötigt.

---

## 40. GitHub-Actions-CI

### 40.1 Build-Job

Erweitere `.github/workflows/ci.yml`.

Empfohlene Reihenfolge:

```yaml
- name: Build and source tests
  run: ./gradlew clean check

- name: Integration tests
  run: ./gradlew :gretl-core:integrationTest

- name: Published artifact tests
  run: ./gradlew publishedArtifactTest

- name: Runtime image contract and offline tests
  run: ./gradlew verifyRuntimeImageContract runtimeImageOfflineTest

- name: Runtime image smoke tests
  run: ./gradlew runtimeImageSmokeTest

- name: Runtime image full E2E tests
  run: ./gradlew runtimeImageE2eTest
```

Die exakte Aufteilung darf optimiert werden, aber die fachliche Reihenfolge muss erhalten bleiben.

### 40.2 Kein Publish bei Image-Testfehler

Der Publish-Job muss von dem Job abhängen, der `runtimeImageTest` vollständig ausgeführt hat.

Bei einem Fehler darf `publishSnapshots` nicht starten.

### 40.3 Docker-Informationen

CI soll vor den Tests ausgeben:

```text
docker version
docker info
```

Keine Secrets ausgeben.

### 40.4 Testreports

Upload mit `if: always()`.

Eindeutiger Artefaktname:

```text
runtime-image-test-reports
```

### 40.5 Kein externer Registry-Login

Für lokale Runtime-Image-Tests ist kein Docker-Registry-Login erforderlich.

### 40.6 Concurrency

Verwende keine CI-Konfiguration, die Image-Build und Tests in verschiedene frische Runner trennt, ohne das Image explizit zu übertragen.

Bevorzugt laufen Build und Tests im selben Job.

Falls Job-Splitting notwendig ist:

1. `docker save`;
2. komprimiertes Artefakt hochladen;
3. im Folgejob laden;
4. Image-ID erneut prüfen.

Dies ist nicht die bevorzugte P0-Variante.

---

## 41. Root-Aggregation

### 41.1 Task `verifyRuntimeImageContract`

Root-Alias:

```groovy
tasks.register('verifyRuntimeImageContract') {
    group = 'verification'
    dependsOn ':gretl-test-support:runtimeImageContractTest'
}
```

### 41.2 Task `runtimeImageOfflineTest`

Root-Alias auf den gleichnamigen Test-Support-Task.

### 41.3 Task `runtimeImageSmokeTest`

Abhängigkeiten:

```text
verifyRuntimeImageContract
runtimeImageOfflineTest
:gretl-core:runtimeImageFunctionalSmokeTest
:gretl-core:runtimeImageIntegrationSmokeTest
:gretl-geotools:runtimeImageFunctionalSmokeTest
```

### 41.4 Task `runtimeImageE2eTest`

Abhängigkeiten:

```text
:gretl-core:runtimeImageFunctionalTest
:gretl-core:runtimeImageIntegrationTest
:gretl-geotools:runtimeImageFunctionalTest
```

### 41.5 Task `runtimeImageTest`

Abhängigkeiten:

```text
verifyRuntimeImageContract
runtimeImageOfflineTest
runtimeImageE2eTest
```

Smoke muss nicht zusätzlich abhängen, wenn seine Tests echte Teilmenge der vollständigen Tasks sind. Die CI darf Smoke vorher separat ausführen, um schneller zu scheitern.

---

## 42. Gradle-Properties und Provider API

Definiere zentrale Provider im Root-Build:

```text
gretlRuntimeImageTestTag
gretlRuntimeImageGradleVersion
gretlRuntimeImageJavaVersion
gretlRuntimeImageTestTimeoutSeconds
gretlRuntimeImageUser
```

Verwende:

```groovy
providers.gradleProperty(...)
providers.environmentVariable(...)
layout.buildDirectory
```

Keine Konfigurationszeit-Zugriffe auf noch nicht vorhandene Dateien.

Die Image-ID-Datei wird als Pfad-Property an Testtasks weitergegeben; der Inhalt wird erst zur Testlaufzeit gelesen.

### 42.1 System-Properties für Testtasks

Jeder Runtime-Image-Testtask erhält:

```text
gretl.test.executionMode=RUNTIME_IMAGE
gretl.test.runtimeImage.idFile=<absolute-path>
gretl.test.runtimeImage.tag=<tag>
gretl.test.runtimeImage.version=<project.version>
gretl.test.runtimeImage.gradleVersion=<version>
gretl.test.runtimeImage.javaVersion=17
```

### 42.2 Configuration Cache

Die neue Gradle-Konfiguration soll möglichst Configuration-Cache-kompatibel sein.

Vermeide:

- Zugriff auf `Project` in Task-Actions;
- nicht serialisierbare Closures als Task-Zustand;
- `doLast` mit direktem Projektzugriff für Kernlogik;
- Konfigurationszeit-Prozessaufrufe.

Führe den Runtime-Testtask zweimal aus:

```text
./gradlew runtimeImageSmokeTest --configuration-cache
./gradlew runtimeImageSmokeTest --configuration-cache
```

Falls Docker-Taskklassen eine vollständige Configuration-Cache-Unterstützung im ersten P0-Schritt objektiv verhindern, muss dies exakt dokumentiert werden. Die Testtasks und übrige Konfiguration müssen dennoch sauber modelliert sein.

---

## 43. Unit-Tests der Testinfrastruktur

Im Modul `gretl-test-support` müssen mindestens folgende Unit-Testklassen existieren.

### 43.1 `GretlExecutionModeTest`

```java
void defaultsToTestkitClasspath();
void parsesAllSupportedValues();
void rejectsUnknownValueClearly();
```

### 43.2 `SecretRedactorTest`

```java
void redactsSecretInPlainText();
void redactsMultipleAndOverlappingSecrets();
void ignoresEmptySecrets();
void doesNotMutateInputList();
```

### 43.3 `DockerRunCommandBuilderTest`

```java
void buildsOfflineCommandWithNetworkNone();
void buildsServiceCommandWithNamedNetwork();
void mountsProjectAndIsolatedGradleHome();
void usesImmutableImageId();
void addsPullNever();
void passesArgumentsWithoutShellQuoting();
void rejectsConflictingNetworkOptions();
void redactsSecretsInDiagnosticCommand();
```

### 43.4 `RuntimeImageDescriptorTest`

```java
void readsValidImageIdFile();
void rejectsMissingImageIdFile();
void rejectsEmptyImageId();
void rejectsNonSha256Reference();
```

### 43.5 `GradleTaskOutputParserTest`

```java
void parsesSuccessOutcome();
void parsesFailedOutcome();
void parsesUpToDateAndNoSource();
void returnsUnknownForUnparseableOutput();
```

### 43.6 `ProcessExecutorTest`

```java
void capturesStdoutAndStderr();
void returnsExitCode();
void timesOutAndTerminatesProcess();
void preservesArgumentsWithSpaces();
void redactsSecrets();
```

Tests müssen auf Java 17 und den unterstützten CI-Systemen stabil sein.

---

## 44. Performance und Laufzeit

### 44.1 Image nur einmal bauen

Innerhalb eines Gradle-Aufrufs darf `buildRuntimeImageForTest` nur einmal ausgeführt werden.

Alle Testtasks verwenden dieselbe Image-ID-Datei.

### 44.2 Getrennte Reuse-Strategien

Für die fachlichen One-shot-, Offline- und Fehlerfalltests bleibt der zuverlässige Standard:

```text
ein kurzlebiger Container pro externem GRETL-Build
```

Zusätzlich ist für `runtimeImageServiceTest` verbindlich:

```text
ein langlebiger Container pro Service-Testsuite
mehrere docker-exec-Builds im selben Container
wiederverwendeter kompatibler Gradle-Daemon
```

Die Service-Suite darf ihren Container und ihr Gradle-User-Home innerhalb der Suite wiederverwenden, muss sie danach aber vollständig entfernen. Andere Tests dürfen von diesem Zustand nicht abhängen.

### 44.3 Testcontainers-Reuse deaktiviert

Verlasse dich nicht auf globale Testcontainers-Reuse-Einstellungen eines Entwicklerrechners.

### 44.4 Gepinnte Images

Alle Servicecontainer verwenden feste Versionen oder Digests.

Keine `latest`-Tags.

### 44.5 Timeouts

Empfohlene Defaults:

```text
lokaler Datei-/SQLite-Test: 2 Minuten
GeoTools-Test: 5 Minuten
PostGIS/ili2pg-Test: 10 Minuten
vollständige Taskkette: 15 Minuten
```

Timeouts über System-/Gradle-Property konfigurierbar machen, aber nicht unendlich.

---

## 45. Portabilität

### 45.1 Primärplattformen

P0 muss mindestens funktionieren auf:

```text
Linux x86_64 mit Docker Engine
macOS mit Docker Desktop
```

Windows/Docker Desktop soll durch argumentlistenbasierte Prozessausführung und Path-Handling nicht unnötig ausgeschlossen werden.

### 45.2 Pfadbehandlung

Verwende:

```java
Path
Path.toAbsolutePath()
Path.normalize()
```

Keine manuelle String-Konkatenation mit `/` oder `:` für Host-Pfade.

Docker-Mounts werden als getrennte Argumente gebaut.

### 45.3 Symlinks

Projektverzeichnisse für Mounts nach Möglichkeit mit `toRealPath()` auflösen, damit Docker Desktop und Linux konsistent arbeiten.

### 45.4 SELinux

Eine optionale Property für Mount-Label (`z`/`Z`) darf vorgesehen werden, ist aber kein P0-Muss. Keine Linux-spezifische Syntax standardmässig auf macOS/Windows anwenden.

---

## 46. Sicherheitsanforderungen

### 46.1 Kein Docker-Socket im GRETL-Container

Der Docker-Socket darf nicht in das Runtime-Image gemountet werden.

### 46.2 Keine privilegierten Container

Unzulässig:

```text
--privileged
--cap-add=ALL
```

### 46.3 Keine echten Secrets

Alle Test-Credentials sind zufällige oder statische lokale Testwerte.

### 46.4 Keine Secrets in Reports

Redaktion gilt auch für:

- Exception-Messages;
- JUnit XML;
- HTML-Reports;
- CI-Logs;
- sanitisiertes Kommando.

### 46.5 Containerlabels

Setze Testcontainer-Labels, beispielsweise:

```text
ch.so.agi.gretl.test=true
ch.so.agi.gretl.test.run=<run-id>
```

Dies erleichtert Diagnose und Cleanup.

---

## 47. Dokumentation

Erstelle oder erweitere:

```text
docs/testing/runtime-image-tests.adoc
```

Die Dokumentation enthält:

- Beweisziel der Image-E2E-Tests;
- Unterschied zu Source- und Published-Artifact-Tests;
- Voraussetzungen;
- lokale Befehle;
- Task-Graph;
- Image-ID-Datei;
- Offline-Prinzip;
- Docker-Netzwerke;
- Debugging;
- relevante Gradle-Properties;
- Coverage-Matrix;
- typische Fehlerbilder.

README-Kurzabschnitt:

```text
./gradlew buildRuntimeImageForTest
./gradlew runtimeImageSmokeTest
./gradlew runtimeImageTest
./gradlew ciCheck
```

Keine Anweisung zum manuellen `publishToMavenLocal` aufnehmen.

---

## 48. Auszuführende Befehle

Der Coding Agent muss mindestens folgende Befehle ausführen und im Abschlussbericht dokumentieren.

### 48.1 Testinfrastruktur

```bash
./gradlew :gretl-test-support:test
```

### 48.2 Bestehende Tests

```bash
./gradlew clean check
./gradlew :gretl-core:integrationTest
```

### 48.3 Image-Build und Vertrag

```bash
./gradlew buildRuntimeImageForTest
./gradlew verifyRuntimeImageContract
```

### 48.4 Offline, Service und Smoke

```bash
./gradlew runtimeImageOfflineTest
./gradlew runtimeImageServiceTest
./gradlew runtimeImageSmokeTest
```

### 48.5 Vollständig

```bash
./gradlew runtimeImageE2eTest
./gradlew runtimeImageTest
./gradlew ciCheck
```

### 48.6 Wiederholung und Parallelität

```bash
./gradlew runtimeImageSmokeTest --rerun-tasks
./gradlew runtimeImageTest --parallel
```

### 48.7 Configuration Cache

```bash
./gradlew runtimeImageSmokeTest --configuration-cache
./gradlew runtimeImageSmokeTest --configuration-cache
```

### 48.8 Gezielt

Mindestens je ein gezielter Lauf:

```bash
./gradlew :gretl-core:runtimeImageFunctionalTest --tests '*GzipFunctionalTest'
./gradlew :gretl-core:runtimeImageIntegrationTest --tests '*Db2DbPostgisIntegrationTest'
./gradlew :gretl-geotools:runtimeImageFunctionalTest --tests '*GretlGeotoolsFunctionalTest'
```

---

## 49. Akzeptanzkriterien

### 49.1 Image-Identität

- `buildRuntimeImageForTest` erzeugt eine neue `image-id.txt`.
- Image-ID ist `sha256:...`.
- alle Tests verwenden die ID, nicht nur den Tag.
- `--pull=never` ist aktiv.

### 49.2 Isolation

- kein `withPluginClasspath()` im Runtime-Image-Executor;
- kein Source-Mount;
- kein `mavenLocal()` im Standard-Init-Script;
- leeres Gradle-Home pro Lauf;
- keine Host-Gradle-Caches;
- alle Gradle-Läufe `--offline`;
- lokale Tests `--network none`.

### 49.3 Funktionalität

- Core-Plugin mit Groovy erfolgreich;
- GeoTools-Plugin mit Groovy erfolgreich;
- Gzip fachlich erfolgreich;
- SQL/SQLite fachlich erfolgreich;
- DuckDB-Erweiterungen offline erfolgreich;
- PostGIS-Kette erfolgreich;
- HTTP erfolgreich;
- FTP-Roundtrip erfolgreich;
- S3-Roundtrip erfolgreich;
- ili2pg-Kette erfolgreich;
- GeoTools-Worker erfolgreich.

### 49.4 Fehlersemantik

- Nichtnull-Exit-Codes werden korrekt erkannt;
- Timeout beendet Container;
- Secrets fehlen in Logs;
- Rollback-Assertions bleiben erhalten;
- Negativtest ohne eingebettete Runtime schlägt erwartungsgemäss fehl.

### 49.5 Coverage

- jede öffentliche Taskklasse ist in YAML klassifiziert;
- Inventory-Test ist grün;
- kein unbegründeter `NOT_APPLICABLE`-Eintrag;
- direkte Einträge referenzieren existierende Testmethoden.

### 49.6 CI

- Runtime-Image-E2E läuft auf Pull Requests und Hauptbranch-Pushes;
- Publish hängt vom vollständigen Gate ab;
- Reports werden immer hochgeladen;
- keine Registry-Credentials für lokale Tests;
- frischer Runner ohne Maven-Local-Vorbereitung funktioniert.

### 49.7 Bestehende Tests

- `clean check` bleibt grün;
- `integrationTest` bleibt grün;
- Published-Artifact-Tests bleiben grün, sofern vorhanden;
- keine Tests wurden deaktiviert oder Assertions abgeschwächt.

---

## 50. Definition of Done

Die Aufgabe gilt nur als abgeschlossen, wenn alle folgenden Punkte erfüllt sind:

- [ ] `gretl-test-support` ist implementiert und nicht publiziert.
- [ ] gemeinsames Ausführungsmodell unterstützt `RUNTIME_IMAGE`.
- [ ] bestehende funktionale Tests können über den Image-Executor laufen.
- [ ] Image wird über `--iidfile` gebaut.
- [ ] alle Tests verwenden die unveränderliche Image-ID.
- [ ] Version im Image entspricht exakt `project.version`.
- [ ] harte Default-Version im Init-Script ist entfernt.
- [ ] `mavenLocal()` ist im Standardpfad entfernt.
- [ ] leeres Gradle-Home wird pro Lauf gemountet.
- [ ] Offline-Tests laufen mit `--network none`.
- [ ] servicebasierte Tests verwenden isolierte Docker-Netzwerke.
- [ ] PostGIS verwendet getrennte Host-/Container-Endpunkte.
- [ ] FTP-, S3- und HTTP-Tests laufen containerintern.
- [ ] GeoTools-Worker wird fachlich aus dem Image geprüft.
- [ ] DuckDB-Erweiterungen werden offline geladen und verwendet.
- [ ] Coverage-Matrix umfasst alle öffentlichen Tasks.
- [ ] Inventory-Test verhindert neue unklassifizierte Tasks.
- [ ] Contract-, Offline-, Smoke- und Full-E2E-Tasks existieren.
- [ ] `check` bleibt Docker-frei.
- [ ] `ciCheck` enthält das vollständige Runtime-Image-Gate.
- [ ] CI blockiert Publikation bei Image-Testfehlern.
- [ ] alle Reports werden hochgeladen.
- [ ] keine echten Secrets oder externen Cloud-Dienste werden verwendet.
- [ ] kein privilegierter Container und kein Docker-Socket-Mount.
- [ ] alle geforderten Befehle wurden ausgeführt.
- [ ] keine deaktivierten Tests, Platzhalter oder unfertigen TODOs verbleiben.
- [ ] Dokumentation und Coverage-Matrix sind aktuell.

---

## 51. Erwartete geänderte und neue Dateien

Die exakten Pfade dürfen an den aktuellen Repository-Stand angepasst werden. Erwartet werden mindestens:

```text
settings.gradle
build.gradle
.github/workflows/ci.yml
docker/Dockerfile
docker/init.gradle
docker/gretl
docs/testing/runtime-image-tests.adoc
docs/testing/runtime-image-coverage.yaml

gretl-test-support/build.gradle
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlExecutionMode.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlBuildExecutor.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlBuildRequest.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlBuildResult.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/GretlBuildExecutors.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/execution/RuntimeImageBuildExecutor.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/process/ProcessExecutor.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/process/ProcessRequest.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/process/ProcessResult.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/process/SecretRedactor.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/docker/DockerCli.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/docker/DockerRunRequest.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/docker/DockerRunCommandBuilder.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/docker/ContainerUserResolver.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/runtime/RuntimeImageDescriptor.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/runtime/RuntimeImageRunOptions.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/runtime/DualEndpoint.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/project/GradleTestProject.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/support/AbstractGretlBuildTestSupport.java
gretl-test-support/src/main/java/ch/so/agi/gretl/test/tags/GretlTestTags.java

gretl-test-support/src/test/java/.../GretlExecutionModeTest.java
gretl-test-support/src/test/java/.../SecretRedactorTest.java
gretl-test-support/src/test/java/.../DockerRunCommandBuilderTest.java
gretl-test-support/src/test/java/.../RuntimeImageDescriptorTest.java
gretl-test-support/src/test/java/.../ProcessExecutorTest.java
gretl-test-support/src/test/java/.../RuntimeImageCoverageInventoryTest.java

gretl-test-support/src/runtimeImageContractTest/java/.../RuntimeImageContractTest.java
gretl-test-support/src/runtimeImageOfflineTest/java/.../RuntimeImageOfflineResolutionTest.java

buildSrc/src/main/java/ch/so/agi/gretl/build/VerifyRuntimeImagePrerequisitesTask.java
buildSrc/src/main/java/ch/so/agi/gretl/build/BuildRuntimeImageTask.java
```

Zusätzlich sind bestehende Support- und Testklassen in `gretl-core` und `gretl-geotools` anzupassen.

---

## 52. Nichtziele dieses P0

Folgende Punkte sind sinnvoll, aber nicht Teil dieser Umsetzung, sofern sie nicht bereits ohne Zusatzaufwand möglich sind:

- Publikation eines Multi-Arch-Manifestes;
- Tests auf ARM64 und x86_64 im selben PR;
- Registry-Push des Testimages;
- Kubernetes-/OpenShift-E2E;
- Performance-Benchmarking;
- langfristige Container-Wiederverwendung;
- Rootless-Podman-Kompatibilität als eigenes Gate;
- vollständige Supply-Chain-Signierung/SBOM.

Die Architektur darf diese späteren Schritte nicht unnötig verhindern.

---

## 53. Verbotene Abkürzungen

Der Coding Agent darf die Aufgabe nicht durch folgende Massnahmen „grün“ machen:

- nur `docker build` testen;
- nur `gretl tasks` testen;
- Runtime-Image-Tests mit `@Disabled` versehen;
- Docker-Tests bei CI pauschal skippen;
- Tests gegen den Source-Classpath statt das Image laufen lassen;
- Host-Gradle-Cache mounten;
- `mavenLocal()` verwenden;
- öffentliches Internet als versteckte Dependency nutzen;
- echte Cloud-Credentials verwenden;
- nur Exit-Code statt Resultat prüfen;
- problematische Taskfamilien aus der Coverage-Matrix weglassen;
- neue Tests als „flaky“ mehrfach automatisch wiederholen, ohne Ursache zu beheben;
- `--network host` verwenden;
- Container privilegiert starten;
- Docker-Socket mounten;
- Fehlermeldungen oder Logs mit Secrets akzeptieren;
- Assertions entfernen oder abschwächen;
- bestehende Tests löschen, um Refactoring zu vereinfachen.

---

## 54. Abschlussbericht des Coding Agents

Nach der Umsetzung muss der Agent einen strukturierten Abschlussbericht liefern.

### 54.1 Architektur

- Ausführungsmodi;
- Image-Build und Image-ID;
- Docker-Runner;
- Cache-Isolation;
- Netzwerkmodell;
- Host-/Container-Endpunkte;
- Testtags und Task-Graph.

### 54.2 Geänderte Dateien

Für jede Datei:

- Zweck;
- wesentliche Änderung;
- mögliche Auswirkungen.

### 54.3 Tests

Auflisten:

- Contract-Tests;
- Offline-Tests;
- Smoke-Tests;
- vollständige E2E-Tests;
- PostGIS;
- HTTP;
- FTP;
- S3;
- DuckDB;
- INTERLIS;
- GeoTools.

### 54.4 Ausgeführte Befehle

Für jeden Befehl:

- Exit-Code;
- Testanzahl;
- Ergebnis;
- relevante Laufzeit;
- bei Fehlern Ursache und Korrektur.

### 54.5 Isolationsnachweis

Explizit erklären:

- weshalb keine Source-Klassen verwendet wurden;
- weshalb kein `mavenLocal()` verwendet wurde;
- weshalb kein Host-Gradle-Cache verwendet wurde;
- weshalb kein öffentliches Internet notwendig war;
- wie die unveränderliche Image-ID verwendet wurde.

### 54.6 Coverage

- Anzahl öffentlicher Tasks;
- `DIRECT_E2E`;
- `COVERED_BY_CHAIN`;
- begründete Ausnahmen;
- Link/Pfad zur Matrix.

### 54.7 CI

- Reihenfolge;
- Publish-Gate;
- Reports;
- Docker-Voraussetzungen.

### 54.8 Abweichungen

Nur technisch zwingende Abweichungen von dieser Spezifikation nennen und detailliert begründen.

### 54.9 Verbleibende Risiken

Nur konkrete, reale Restprobleme. Keine allgemeinen Standardhinweise.

---

## 55. Schlussanforderung

Der Coding Agent soll diese Spezifikation vollständig lesen, bevor er die erste Änderung vornimmt.

Er soll die Arbeit nicht nach Analyse, Gerüstbau oder einzelnen Smoke-Tests beenden.

Das Ergebnis muss eine belastbare Release-Aussage ermöglichen:

> Das lokal aus dem aktuellen Commit gebaute GRETL-Runtime-Image wurde als unveränderliches Image ausgeführt. Es führt gemountete Gradle-Projekte mit moderner `plugins {}`-DSL aus, kann GRETL über die reguläre Plugin-Auflösung mit zusätzlichen Plugins kombinieren, löst seine gebündelten Laufzeitabhängigkeiten ohne Host-Cache und ohne Internet auf, unterstützt sowohl isolierte One-shot-Läufe als auch einen langlebigen Container mit nachweislich wiederverwendetem Gradle-Daemon, führt Core-, Datenbank-, Netzwerk-, INTERLIS- und GeoTools-Tasks fachlich korrekt aus und blockiert die Publikation, wenn irgendein Teil dieser Distribution nicht funktioniert. Die Migration oder Kompatibilitätsprüfung bestehender Jobs ist ausdrücklich nicht Bestandteil dieses Nachweises.


---

## 56. Revisionshinweis und Vorrangregel

Diese Revision berücksichtigt den am 30. Juli 2026 geprüften Stand von `sogis/gretljobs` und `sogis/gretl` ausschliesslich hinsichtlich Container-Lebenszyklus, Gradle-Daemon und historischer Verpackungsrisiken.

Bei einem Widerspruch innerhalb dieses Dokuments haben folgende Anforderungen Vorrang:

1. Die Migration, Änderung oder Kompatibilitätsprüfung bestehender Jobs aus `sogis/gretljobs` ist dauerhaft ausserhalb des Projektumfangs.
2. Der verbindliche Consumer-Vertrag verwendet ausschliesslich die moderne Gradle-Plugin-DSL `plugins {}`.
3. Legacy-Anwendung über `apply plugin`, manuelle Buildscript-Classpaths oder historische Task-APIs muss weder unterstützt noch getestet werden.
4. Der Launcher ist daemonneutral und erzwingt kein `--no-daemon`.
5. Ein langlebiger Servicecontainer mit wiederverwendetem Gradle-Daemon ist ein verbindliches Ziel.
6. One-shot- und Offline-Tests dürfen explizit `--no-daemon` verwenden.
7. Die interne Dependency-Distribution darf vom historischen Image abweichen.
8. `flatDir` ist kein Zielvertrag; bevorzugt wird eine strukturierte, metadatenfähige Distribution.
9. Gemountete moderne Gradle-Projekte sowie die reguläre Kombination mit zusätzlichen Plugins bilden den verbindlichen Consumer-Vertrag.
10. `sogis/gretljobs` darf nicht als Testfixture, Migrationsziel oder Abnahmekriterium in diesen Auftrag einbezogen werden.
