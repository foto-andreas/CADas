# CADas

`CADas` ist eine JavaFX-basierte Desktop-Anwendung für Gebäude-Grundrisse und angrenzende CAD-Funktionen.

## Status

Achtung: Wir sind in einem frühen Status der Programmierung. Nix ist fest, wenig getestet. Alles wird per KI erstellt.


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
* Etagenverwaltung
* Raster, Snap, Hilfslinien und Bearbeitung verbundener Wand-Endpunkte
* Menü, Tastaturkürzel, Rückgängig/Wiederherstellen und kontextabhängige Properties-Leiste
* modulare JavaFX-Startskripte ohne die bisherigen Startwarnungen
* erste gekoppelte 3D-Ansicht mit Auswahlrückkopplung
* DXF-Import und -Export
* interne und externe Teilebibliotheken

Weitere fachliche Leitplanken und Entscheidungen stehen in [AGENTS.md](AGENTS.md) und [TODO.md](TODO.md).
