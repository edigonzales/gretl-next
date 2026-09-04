# GRETL next – Quarto-Webseite

Dies ist das Quarto-Webseitenprojekt für `next.gretl.app`.

## Lokal starten

Quarto installieren, im Terminal in diesen Ordner wechseln und ausführen:

```bash
quarto preview
```

Die statische Webseite wird erzeugt mit:

```bash
quarto render
```

Das Ergebnis liegt anschliessend in `_site/`.

## GitHub Pages

Die öffentliche Site wird mit `.github/workflows/website.yml` gebaut. Der Workflow kombiniert zwei statische Generatoren zu einem gemeinsamen GitHub-Pages-Artefakt:

- Quarto rendert die Hauptseite nach `website/_site/`.
- Thoth Biblios rendert die technische Referenz nach `website/_site/reference/`.
- Nur Pushes auf `main` werden nach GitHub Pages deployt. Pull Requests bauen dieselbe Site, deployen sie aber nicht.

Der Website-Workflow wird nur ausgelöst, wenn sich publizierbarer Dokumentationsinhalt oder der Workflow selbst ändert:

- `website/**` für die Quarto-Seiten,
- `docs/reference/**` für die Biblios-Referenz,
- `.github/workflows/website.yml` für Änderungen am Publikationsprozess.

Die inhaltliche Korrektheit der generierten Referenz gehört dagegen zur normalen CI in `.github/workflows/ci.yml`. `./gradlew check` führt dort unter anderem `verifyTaskDocs` und `verifyReferenceStyle` aus. Dadurch gilt für Änderungen an dokumentierten GRETL-Tasks der folgende Ablauf:

1. Java-Code ändern.
2. `./gradlew generateTaskDocs` ausführen.
3. Änderungen unter `docs/reference/generated/` zusammen mit dem Code committen.
4. Die normale CI prüft, dass die generierten Referenzdateien aktuell und stilistisch gültig sind.
5. Weil sich `docs/reference/**` geändert hat, baut der Website-Workflow Quarto und Biblios neu und publiziert nach dem Merge auf `main` die gemeinsame Site.

Eine reine Java-Änderung ohne aktualisierte generierte Referenz löst damit keinen unnötigen Website-Build aus; sie scheitert stattdessen in der normalen CI an `verifyTaskDocs`.

Thoth Biblios ist im Workflow auf einen konkreten Thoth-Commit gepinnt, damit Änderungen auf dem Thoth-`main` den GRETL-Website-Build nicht unerwartet verändern. Die Referenz wird im CI aus genau dem getesteten GRETL-Checkout erzeugt und nicht nochmals aus dem entfernten `main` geladen.

Für GitHub Pages muss im Repository unter **Settings → Pages** als Source **GitHub Actions** gewählt sein. Die Custom Domain für die Site ist `next.gretl.app`.

## Wo wird was bearbeitet?

- `index.qmd` – Titelseite
- `features.qmd` – Tab **Features**
- `getting-started.qmd` – Tab **Getting Started**
- `docs.qmd` – Tab **Docs** und Einstieg in die Biblios-Referenz
- `styles.scss` – Farben, Abstände, Schrift und responsives Layout
- `_quarto.yml` – Navigation und globale Quarto-Konfiguration
- `assets/gretl-mark.svg` – einfaches, editierbares Vektorlogo
- `../docs/reference/` – Quellen und `biblios.yml` für die technische Referenz

Die Webseite bindet **Recursive** lokal als woff2 ein. Überschriften und andere fette Texte verwenden Gewicht **800 (ExtraBold)**. Für Code-Blöcke wird die Monospace-Achse (`MONO`) des Recursive-Variable-Fonts verwendet.

## Schnelle visuelle Anpassungen

Die wichtigsten Farben stehen in `styles.scss` oben unter `:root`. Der Farbverlauf der grossen Überschrift ist in `.gradient-text` definiert. Der CSS-Abschnitt für die Titelseite beginnt beim Kommentar `Landing page`.

Der Code auf der Titelseite ist ein normaler Quarto-Codeblock. Er bleibt auswählbar und kopierbar; es wird kein Bildgenerator benötigt.
