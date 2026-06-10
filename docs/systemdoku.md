# Systemdokumentation

## Zielbild

`CADas` ist aktuell als Java-Desktop-Anwendung für Gebäude-Grundrisse aufgesetzt. Der erste technische Schwerpunkt liegt auf einer stabilen 2D-Zeichenfläche mit sauberem Geometriekern, damit spätere fachliche Ausbaustufen wie Räume, Öffnungen, DXF-Import/Export, Teilebibliotheken und 3D-Projektionen nicht auf instabilen Grundlagen aufbauen.

![Systemarchitektur](diagramme/systemarchitektur.svg)

## Aktueller Architekturstand

### Technologien

* `JDK 25`
* `Gradle Wrapper 9.5.0`
* `JavaFX` für die Desktop-Oberfläche
* `JUnit 5 Jupiter` für Unit-Tests
* `JaCoCo` für Testreports

### Paketstruktur

* `de.andreas.cadas`
  Einstiegspunkte der Anwendung.
* `de.andreas.cadas.ui`
  JavaFX-Workbench, Ansichten und Interaktion mit der Zeichenfläche.
* `de.andreas.cadas.application.drawing`
  Anwendungslogik für orthogonales Zeichnen, manuelle Längen- und Winkeleingabe, Snap-Verhalten, Öffnungsplatzierung und Bearbeitung verbundener Wand-Endpunkte.
* `de.andreas.cadas.domain.geometry`
  Geometrische Grundbausteine wie Längen, Winkel, Raster, Punkte und Segmente.
* `de.andreas.cadas.domain.model`
  Fachliches Projektmodell für Etagen, Räume, Wände, Türen und Fenster.

## Verantwortlichkeiten

### UI

Die Klasse `CadWorkbench` kapselt die aktuelle Workbench. Sie stellt bereit:

* pann- und zoombare Zeichenfläche
* Rasterdarstellung
* Hilfslinien aus Linealen
* magnetisches Snap auf Raster und Endpunkte
* sechs orthogonale Ansichtsumschalter
* optionale Himmelsrichtung
* Live-Anzeige von Länge und Winkel
* ein- und ausblendbare Bemaßung für Wände
* Werkzeugmodus für Wände, Räume, Türen, Fenster und Bearbeitung
* Flächen- und Volumenanzeige für Räume

### Anwendungslogik

`DraftingService` erzwingt je nach Eingabemodus orthogonales Zeichnen oder übernimmt manuelle Längen- und Winkelvorgaben. `SnapService` entscheidet, ob auf bestehende Endpunkte oder auf das Raster eingerastet wird. `OpeningPlacementService` bindet Türen und Fenster an bestehende Wände. `WallEditingService` verschiebt verknüpfte Wand-Endpunkte gemeinsam.

### Domäne

`Length` speichert Maßangaben in Millimetern auf Basis von `BigDecimal`, um Einheiten konsistent zu halten. `ProjectModel`, `Level`, `Wall`, `Room`, `Door` und `WindowElement` bilden den aktuellen Grundrisskern ab. Etagen lassen sich bereits dynamisch anlegen und getrennt voneinander bearbeiten.

## Rendering-Modell

Die Zeichenfläche arbeitet intern in Millimetern und transformiert diese Weltkoordinaten mit Offset und Zoom auf Bildschirmkoordinaten. Dadurch bleiben Raster, Snap und Bemaßung konsistent, auch wenn die Ansicht verschoben oder skaliert wird.

## Qualitätssicherung

Aktuell abgesichert sind unter anderem:

* Einheitenumrechnung und Formatierung
* orthogonales Zeichnen
* freie Längen- und Winkelvorgaben
* Snap auf Raster und Endpunkte
* Platzierung von Türen und Fenstern auf Wänden
* Verschieben verbundener Wand-Endpunkte
* Flächen- und Volumenberechnung von Räumen
* Grundverhalten des Projektmodells

Build und Tests laufen über:

```bash
./gradlew test
```

## Erweiterungsstrategie

Die bestehende Struktur ist absichtlich so geschnitten, dass die nächsten Ausbauschritte sauber ergänzt werden können:

* Räume, Türen, Fenster und Decken im Fachmodell ergänzen
* Bearbeitungslogik für verbundene Linienenden ergänzen
* DXF zuerst als Import-/Export-Format anbinden
* die vorhandene `DWG`-Datei später über eine separat gekapselte Formatadapter-Schicht nutzbar machen
* zusätzliche Flächen-Ebenen, Treppen und Dachaufbauten als weitere Domänenmodule ergänzen

## Plattformstrategie

Die Anwendung wird plattformneutral aufgebaut. Aktive Verifikation erfolgt zunächst auf `macOS`, die Architektur trennt jedoch bereits UI, Anwendungslogik und Domäne so, dass spätere Plattformtests auf `Windows` und `Linux` nicht an vermischten Zuständigkeiten scheitern.
