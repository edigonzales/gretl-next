# Typografie und technische Schreibweisen

Diese Richtlinie ist für alle redaktionellen und generierten Inhalte der
GRETL-Referenz verbindlich. Ihr Grundsatz lautet: **semantisch reich, visuell
ruhig**. Technische Begriffe werden nach ihrer Bedeutung ausgezeichnet, aber
nicht mit einer eigenen Syntaxfarbe versehen. Blau bleibt Links vorbehalten.

## Semantische Rollen

Technische Tokens werden in AsciiDoc mit einer Rolle und, wo in der folgenden
Tabelle verlangt, zusätzlich mit Backticks ausgezeichnet. Die Rolle umfasst
immer nur den exakten technischen Token, nicht Artikel, Satzzeichen oder
deutsche Wortbestandteile.

| Element | Rolle | Verbindliches Beispiel |
| --- | --- | --- |
| Produkt, Werkzeug oder ausgeschriebener Formatname | `product` | `[.product]#GRETL#`, `[.product]#GeoPackage#` |
| Akronym, Standard, Format oder Protokoll | `acronym` | `[.acronym]#SQL#`, `[.acronym]#JDBC#-URL` |
| Task-Typ beziehungsweise Java-Task-Klasse | `task-type` | `[.task-type]#\`SqlExecutor\`#` |
| registrierter Gradle-Taskname | `task-name` | `[.task-name]#\`importData\`#` |
| DSL-Methodenname | `dsl-method` | `[.dsl-method]#\`schema\`#` |
| vollständiger Methodenaufruf | `dsl-method` | `[.dsl-method]#\`schema('agi')\`#` |
| Methodensignatur | `dsl-signature` | `[.dsl-signature]#*sqlFiles*(Object\... paths)#` |
| sonstiger Java-Typ | `java-type` | `[.java-type]#\`HttpClient\`#` |
| Parameter | `parameter` | `[.parameter]#\`paths\`#` |
| Dateiname | `file-name` | `[.file-name]#\`build.gradle\`#` |
| Dateierweiterung | `file-ext` | `[.file-ext]#\`.sql\`#` |
| Pfad oder Verzeichnis | `path` | `[.path]#\`docs/reference/\`#` |
| Dateimuster | `glob` | `[.glob]#\`*.xtf\`#` |
| Kommando oder Programmaufruf | `command` | `[.command]#\`./gradlew check\`#` |
| Kommandozeilenoption | `cli-option` | `[.cli-option]#\`--project-dir\`#` |
| Konfigurationsschlüssel | `config-key` | `[.config-key]#\`gretl.version\`#` |
| Environment-Variable | `env-var` | `[.env-var]#\`GRETL_HOME\`#` |
| technischer Literalwert | `literal` | `[.literal]#\`false\`#` |
| fachlicher oder technischer Identifikator | `domain-id` | `[.domain-id]#\`DM01AVCH24LV95D\`#` |
| Platzhalter | `placeholder` | `[.placeholder]#\`${paramName}\`#` |
| kurzes Codefragment | `code-fragment` | `[.code-fragment]#\`failOnError false\`#` |
| Pflichtstatus | `required` | `[.required]#ja#` |
| optionaler Status | `optional` | `[.optional]#nein#` |

## Bedeutungsgrenzen

Ein **Task-Typ** ist die Klasse, welche die DSL bereitstellt, zum Beispiel
`[.task-type]#\`SqlExecutor\`#`. Ein **Taskname** ist der im Build registrierte
Name einer konkreten Instanz, zum Beispiel
`[.task-name]#\`executeSql\`#` in
`tasks.register('executeSql', SqlExecutor)`. Überschriften von Task-Kapiteln
bleiben normale Überschriften und erhalten keine Monospace-Auszeichnung.

Ein **Methodenname** bezeichnet nur den Bezeichner, zum Beispiel
`[.dsl-method]#\`schema\`#`. Ein **Aufruf** enthält Argumente und bleibt als
Ganzes dieselbe Rolle, zum Beispiel
`[.dsl-method]#\`schema('agi')\`#`. Eine **Signatur** dokumentiert dagegen Typen
und Parameternamen; in ihr wird nur der Methodenname stark gesetzt:
`[.dsl-signature]#*schema*(String name)#`.

Generische Fachwörter wie «Task», «Datei», «Datenbank», «Tabelle» und «Schema»
bleiben normaler Fliesstext. Auch offizielle Produktschreibweisen werden nicht
verändert. Die Rolle `product` liefert Semantik, aber bewusst keine auffällige
Darstellung.

## Allgemeine Schreibregeln

- Die AsciiDoc-Schreibweise `+…+` ist für technische Auszeichnung verboten.
  Backticks liefern die Code-Semantik; die Rolle liefert die fachliche
  Semantik.
- Kursivschrift ist ausschliesslich sprachlicher Hervorhebung vorbehalten. Sie
  kennzeichnet keine Produkte, Typen, Dateien oder Werte.
- Rollen umfassen nur den technischen Token:
  `[.acronym]#SQL#-Datei`, nicht `[.acronym]#SQL-Datei#`.
- Folgt direkt eine alphanumerische Endung, wird die unconstrained Form
  verwendet: `[.product]##Shapefile##s` und `[.acronym]##ID##s`.
- Satzzeichen stehen ausserhalb der Rolle, sofern sie nicht Teil eines
  Literals, Pfads, Aufrufs oder Codefragments sind.
- Quelltext in Source-Blöcken erhält keine Inline-Rollen. Dort übernimmt Prism
  die Darstellung.

## Links

Links bleiben in jeder technischen Rolle blau und werden beim Hover
unterstrichen. Ein fachlicher Begriff erhält nie allein wegen seiner Kategorie
eine Farbe. Der Link umfasst den sinnvoll anklickbaren Text; die technische
Rolle umfasst weiterhin nur den exakten Token. Sichtbare rohe URLs sind zu
vermeiden, wenn ein sprechender Linktext möglich ist.

## Tabellen

Tabellen verwenden Rollen genauso wie Fliesstext. DSL-Signaturen stehen in der
Spalte «DSL-Methode» als `dsl-signature`; Pflichtstatus wird immer sichtbar als
`required` beziehungsweise `optional` ausgegeben. Inline-Code in echten
Referenztabellen hat keinen zusätzlichen Hintergrund. Hinweisboxen sind keine
Referenztabellen und behalten die normale Inline-Code-Darstellung.

Dokumentüberschriften aller Ebenen und Tabellenüberschriften verwenden
Recursive mit Schriftgewicht `900`. Alle DSL-Methodennamen werden mit
Schriftgewicht `700` gesetzt: sowohl
`dsl-method`-Tokens im Fliesstext als auch der starke Methodenname innerhalb
einer `dsl-signature`. Typen und Parameter innerhalb der Signatur bleiben bei
Schriftgewicht `400`.

Lange Signaturen, Pfade und Codefragmente müssen auf schmalen Viewports
umbrechen dürfen. Autoren erzwingen deshalb weder geschützte Leerzeichen noch
manuelle Zeilenumbrüche in technischen Tokens.

## Hinweise und Codeblöcke

Hinweise erklären Randbedingungen, Gefahren oder Abweichungen; sie ersetzen
keine reguläre Beschreibung. Technische Tokens in einem Hinweis erhalten
dieselben Rollen wie im Fliesstext.

Ausführbare oder mehrzeilige Beispiele stehen in einem Source-Block mit
passender Sprache. Inline-Rollen werden innerhalb des Blocks nicht verwendet.
Prism-Sprachfarben und der Copy-Button gehören ausschliesslich zur
Codeblock-Darstellung.

## Generierte Dateien

Dateien unter `docs/reference/generated/` werden nie manuell bearbeitet. Sie
werden ausschliesslich mit `./gradlew generateTaskDocs` erzeugt.

Annotationen und Javadocs bleiben frei von rohem AsciiDoc-Rollenmarkup wie
`[.acronym]#SQL#`. Sie müssen als Plain Text beziehungsweise mit dem bereits
unterstützten Javadoc-/Markdown-kompatiblen Inline-Code verständlich sein. Nur
der AsciiDoc-Renderer ergänzt technische Rollen. Dadurch bleiben insbesondere
LSP-Beschreibungen frei von ausgabespezifischem Markup.

## Checkliste für Autorinnen und Autoren

- Ist jedes ausgezeichnete Token der präzisesten Rolle zugeordnet?
- Sind Task-Typ und registrierter Taskname korrekt unterschieden?
- Sind Methodenname, Aufruf und Signatur korrekt unterschieden?
- Umfasst jede Rolle nur den exakten technischen Token?
- Werden Backticks statt `+…+` verwendet?
- Ist Kursivschrift ausschliesslich sprachlich begründet?
- Bleiben Source-Blöcke frei von Inline-Rollen?
- Können lange Tokens auf schmalen Viewports umbrechen?
- Wurden generierte Dateien mit `generateTaskDocs` statt manuell geändert?

## Checkliste für Reviews

- Enthält `reference.adoc` ausserhalb von Source-Blöcken kein `+…+`?
- Werden ausschliesslich die in dieser Richtlinie dokumentierten Rollen
  verwendet?
- Bleiben Links blau und fachliche Kategorien farbneutral?
- Sind Hinweisboxen von Tabellenregeln unbeeinflusst?
- Sind Pflicht- und Optionalstatus sichtbar und verständlich?
- Enthalten LSP-Metadaten kein AsciiDoc-Rollenmarkup?
- Wurden `verifyReferenceStyle`, `verifyTaskDocs` und die relevanten
  Doclet-Tests ausgeführt?
