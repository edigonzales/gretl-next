# GRETL-Referenzdokumentation

Die redaktionelle Referenz steht in [reference.adoc](reference.adoc). Die
Property-Tabellen unter [generated/](generated/) werden aus den öffentlichen
GRETL-Task-Annotationen erzeugt und eingecheckt. Diese Dateien werden nicht
manuell bearbeitet.

Die [Typografie und technischen Schreibweisen](STYLE_GUIDE.md) sind für
redaktionelle und generierte Referenzinhalte verbindlich. Die Richtlinie ist
eine Autorenunterlage und wird nicht in die öffentliche Seitennavigation
aufgenommen.

## Task-Dokumentation aktualisieren

Nach Änderungen an Task-Klassen, `@GretlTaskDoc` oder `@GretlDslMethod`:

```bash
./gradlew generateTaskDocs
./gradlew verifyTaskDocs
./gradlew verifyReferenceStyle
```

`verifyTaskDocs` und `verifyReferenceStyle` sind Teil von `check`.
`verifyTaskDocs` schlägt fehl, wenn die eingecheckten Dateien nicht mehr dem
aktuellen Code entsprechen oder ein Task in `reference.adoc` nicht eingebunden
ist. `verifyReferenceStyle` prüft die Rollen-Whitelist und verbietet
`+…+`-Auszeichnung ausserhalb von Source-Blöcken.

## Thoth Biblios lokal starten

Voraussetzung ist ein gebautes Thoth-Biblios-JAR aus dem benachbarten
`../thoth`-Repository. Für uncommittete Änderungen kopiert man einmal
`biblios.local.yml.example` nach `biblios.local.yml` und passt den lokalen
Repository-Pfad an. Der Serve-Modus verwendet mit
`--use-local-working-tree` dann den aktuellen Working Tree:

```bash
cd docs/reference
java -jar ../../../thoth/thoth-biblios/build/libs/thoth-biblios-<version>-all.jar \
  serve --config biblios.local.yml --use-local-working-tree
```

Die erzeugte Website liegt unter `build/thoth-site/`. Für eine reproduzierbare
Prüfung des eingecheckten Branches kann der Build aus dem Repository-Stand
ausgeführt werden:

```bash
cd docs/reference
java -jar ../../../thoth/thoth-biblios/build/libs/thoth-biblios-<version>-all.jar \
  build --config biblios.yml --clean
```

Die Thoth-Arbeitsdaten und die Website sind lokale Build-Artefakte und werden
nicht eingecheckt.
