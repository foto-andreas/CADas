# Benutzerdokumentation

## Zweck der aktuellen Version

Die aktuelle Version ist ein erstes 2D-Zeichen-MVP für Gebäude-Grundrisse. Es lassen sich bereits Wände auf einer pann- und zoombaren Zeichenfläche mit Raster, Snap, manueller Maßeingabe und orthogonalem Zeichnen erfassen.

![Zeichenablauf](diagramme/zeichenablauf.svg)

## Anwendung starten

```bash
./gradlew run
```

## Grundbedienung

### Navigation

* Mit dem Mausrad zoomst du in die Zeichnung hinein oder heraus.
* Mit der rechten Maustaste verschiebst du die Zeichenfläche.
* Mit `Ansicht zentrieren` setzt du Zoom und Position auf die Ausgangslage zurück.
* Über die Etagenauswahl wechselst du zwischen vorhandenen Geschossen.
* Mit `Etage hinzufügen` legst du ein weiteres Geschoss an.

### Wände zeichnen

* Linke Maustaste drücken und ziehen, um eine Wand zu zeichnen.
* Ohne `Shift` wird die Wand automatisch orthogonal ausgerichtet.
* Mit gedrückter `Shift`-Taste wird frei gezeichnet.
* Während des Zeichnens werden Länge und Winkel live angezeigt.
* Ist im Feld `Länge` ein Wert eingetragen, wird die Wand auf diese Länge gesetzt.
* Ist im Feld `Winkel` ein Wert eingetragen, wird die Wand auf diesen Winkel gesetzt.

### Raster und Snap

* `Raster` blendet das Zeichenraster ein oder aus.
* `Snap Raster` lässt Punkte auf das aktuelle Raster einrasten.
* `Snap Punkte` lässt Punkte auf vorhandene Linien-Enden einrasten.
* Über `Rasterweite` legst du die Rasterdichte in `mm`, `cm` oder `m` fest.

### Zusätzliche Anzeigen

* `Bemaßung` blendet Längenbeschriftungen für Wände ein oder aus.
* `Nordpfeil` zeigt die Himmelsrichtung in der Zeichenfläche an.
* Über die sechs Ansichtsbuttons kann zwischen den orthogonalen Ansichten umgeschaltet werden.

## Aktuelle Grenzen

Die aktuelle Version konzentriert sich bewusst auf den 2D-Grundrisskern. Noch nicht umgesetzt sind unter anderem:

* Räume, Türen und Fenster als bearbeitbare Fachobjekte
* Hilfslinien aus Linealen
* Bearbeiten und Verschieben bereits verbundener Linienketten
* Treppen, Dächer und zusätzliche Flächen-Ebenen
* DXF- und DWG-Verarbeitung

## Nächste fachliche Ausbaustufen

Die nächsten geplanten Schritte sind:

* Erweiterung des Fachmodells um Räume, Türen und Fenster
* strukturierter DXF-Import und -Export
* interne Standardbibliothek für Bauteile
* robuste Mehr-Etagen-Unterstützung
