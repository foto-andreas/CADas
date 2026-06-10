# CADas

`CADas` ist eine JavaFX-basierte Desktop-Anwendung für Gebäude-Grundrisse und angrenzende CAD-Funktionen.

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
* DXF-Import und -Export
* interne und externe Teilebibliotheken

Weitere fachliche Leitplanken und Entscheidungen stehen in [AGENTS.md](AGENTS.md) und [TODO.md](TODO.md).
