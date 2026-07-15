# CADas

`CADas` ist eine modulare JavaFX-Desktop-Anwendung für Gebäude-Grundrisse und angrenzende CAD-Funktionen.

## Technik

* `JDK 25`
* `Gradle Wrapper 9.6.0`
* `JavaFX`
* `JUnit 5`
* `JaCoCo`

## Start

```bash
./gradlew run
```

Für einen lokalen HTTP-Testzugriff auf die laufende Anwendung:

```bash
./gradlew runMitAutomatisierung
```

Der Start gibt ein zufälliges Bearer-Token aus. Alternativ wird ein mindestens 32 Zeichen langes
`CADAS_AUTOMATION_TOKEN` verwendet. Zustandsänderungen benötigen authentifizierte `POST`-Aufrufe;
Dateizugriffe bleiben auf den Workspace begrenzt. Vollständige Beispiele stehen in der
[Benutzerdokumentation](docs/benutzerdoku.md).

## Distribution

Lokale Installations- und Distributionsaufgaben:

```bash
./gradlew installDist
./gradlew packageMacOsAppImage
./gradlew packageMacOsDmg
./gradlew packageMacOsPkg
./gradlew packageMacOsInstallers
./gradlew macosInstall
```

Hinweis: Die macOS-Paketierungsaufgaben laufen nur auf `macOS`, weil dafür `jpackage` des lokalen JDK genutzt wird. `macosInstall` baut `CADas.app` und ersetzt ausschließlich eine vorhandene App mit der Bundle-ID `de.schrell.cadas` atomar unter `/Applications/CADas.app`. Symbolische Ziele werden abgelehnt.

## Tests

```bash
./gradlew check
```

`check` führt die vollständigen fachlichen und technischen Tests, Compilerprüfungen und die paketbezogene
JaCoCo-Mindestabdeckung aus. Der HTML-Abdeckungsbericht liegt anschließend unter
`build/reports/jacoco/test/html/index.html`.

## Dokumentation

* Systemdokumentation: [docs/systemdoku.md](docs/systemdoku.md)
* Benutzerdokumentation: [docs/benutzerdoku.md](docs/benutzerdoku.md)
* Leitfaden für menschliche Reviews: [REVIEW.md](REVIEW.md)
* Paketdokumentation: jeweilige `README.md` direkt im Produktions- und Testpaket

## Aktueller Stand

Der aktuelle Schwerpunkt liegt auf dem 2D-Grundrisskern:

* Wände, Räume, Türen, Fenster und Treppen
* erste Raumobjekte wie Dusche, Toilette, Waschbecken, Schränke und Tische
* Etagenverwaltung
* Raster, Snap, Hilfslinien, Nordwinkel und Bearbeitung verbundener Wand-Endpunkte
* Mehrfachauswahl, Kontextmenü, Eigenschaftenübernahme auf Auswahl und 90°-Drehung rotierbarer Bauteile
* Menü, Tastaturkürzel, Rückgängig/Wiederherstellen und kontextabhängige Properties-Leiste
* modulare JavaFX-Startskripte ohne die bisherigen Startwarnungen
* gekoppelte 3D-Ansicht mit Auswahlrückkopplung, Kamerahilfe und Modell-Einpassung
* Materialliste für Beläge mit gerenderter Markdown-Ansicht, Druck und Markdown-Export
* Gebäude-DXF als Standard sowie Etagen-DXF als Zusatzoption
* interne und externe Teilebibliotheken einschließlich registrierbarer `.dwg`-Referenzen
* lokaler Automatisierungszugriff für direkte App-Tests

Weitere fachliche Leitplanken und Entscheidungen stehen in [AGENTS.md](AGENTS.md) und [TODO.md](TODO.md).
