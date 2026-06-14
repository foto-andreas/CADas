# CADas

`CADas` ist eine modulare JavaFX-Desktop-Anwendung für Gebäude-Grundrisse und angrenzende CAD-Funktionen.

## Technik

* `JDK 25`
* `Gradle Wrapper 9.5.0`
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

## Distribution

Lokale Installations- und Distributionsaufgaben:

```bash
./gradlew installDist
./gradlew packageMacOsAppImage
./gradlew packageMacOsDmg
```

Hinweis: Die beiden macOS-Paketierungsaufgaben laufen nur auf `macOS`, weil dafür `jpackage` des lokalen JDK genutzt wird.

## Tests

```bash
./gradlew test
```

## Dokumentation

* Systemdokumentation: [docs/systemdoku.md](docs/systemdoku.md)
* Benutzerdokumentation: [docs/benutzerdoku.md](docs/benutzerdoku.md)

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
