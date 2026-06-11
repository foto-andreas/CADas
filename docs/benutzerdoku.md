# Benutzerdokumentation

## Zweck der aktuellen Version

`CADas` ist aktuell ein CAD-MVP für Gebäude-Grundrisse mit kombinierter 2D- und 3D-Ansicht. Der Schwerpunkt liegt auf einem robusten Grundrisskern für Etagen, Wände, Räume, Türen, Fenster und Treppen. Die 3D-Ansicht dient als direkte räumliche Kontrolle des 2D-Modells.

![Zeichenablauf](diagramme/zeichenablauf.svg)

## Anwendung starten

### Direkt aus dem Gradle-Projekt

```bash
./gradlew run
```

### Lokales Startverzeichnis erzeugen

```bash
./gradlew installDist
```

Danach kann die Anwendung über das erzeugte Startskript im Verzeichnis `build/install/CADas/bin` gestartet werden.

### macOS-Paket erzeugen

```bash
./gradlew packageMacOsAppImage
./gradlew packageMacOsDmg
```

Diese Aufgaben laufen nur auf `macOS`.

## Aufbau der Oberfläche

Die Oberfläche besteht aktuell aus fünf Hauptbereichen:

* Menüleiste
* kompakte Werkzeugleiste
* linke Eigenschaftenleiste
* 2D-Zeichenfläche mit Linealen
* rechte 3D-Ansicht
* untere Statusleiste

### Menüleiste

Die Menüleiste bündelt Datei-, Bearbeitungs-, Ansichts- und Werkzeugaktionen. Dazu gehören insbesondere:

* Etage hinzufügen
* Projekt leeren
* DXF importieren und exportieren
* Teilebibliothek laden
* Rückgängig und Wiederherstellen
* Werkzeugwechsel per Menü
* Ansichtswechsel und Zentrierfunktionen

### Werkzeugleiste

Die obere Werkzeugleiste ist bewusst kompakt gehalten. Dort liegen:

* Werkzeugauswahl
* Etagenauswahl
* Etage hinzufügen
* Rückgängig und Wiederherstellen
* Auswahl löschen und Auswahl aufheben
* Anzeige-Optionen wie Raster, Snap, Bemaßung, Fläche und Hilfslinien

### Eigenschaftenleiste

Links befindet sich eine dauerhaft sichtbare vertikale Liste mit Properties. Dort stehen:

* allgemeine Zeichenwerte wie Rasterweite, Länge und Winkel
* fachliche Eigenschaften für Wand, Raum, Tür, Fenster und Treppe
* eine kurze Zusammenfassung der aktuellen Auswahl

Die Eigenschaftenleiste blendet nur die Bereiche ein, die zum aktuellen Werkzeug oder zur aktiven Auswahl passen.

### 2D-Zeichenfläche

In der Mitte liegt die eigentliche Grundrissfläche. Dort werden Wände, Räume, Treppen, Türen und Fenster gezeichnet oder bearbeitet.

### 3D-Ansicht

Rechts siehst du eine räumliche Ableitung des aktuellen Modells. Sie basiert direkt auf denselben Domänendaten wie die 2D-Ansicht.

### Statusleiste

Unten zeigt die Anwendung:

* aktive Ansicht
* aktuelle Etage
* Zoomfaktor
* Cursorposition
* Werkzeug- oder Zeichenstatus

## Grundbedienung

### Navigation in 2D

* Mit dem Mausrad zoomst du in die Zeichnung hinein oder heraus.
* Mit der rechten Maustaste verschiebst du die Zeichenfläche.
* `2D zentrieren` setzt Zoom und Position der 2D-Ansicht zurück.
* Über die Etagenauswahl wechselst du zwischen vorhandenen Geschossen.
* Mit `Etage hinzufügen` legst du ein weiteres Geschoss an.
* Mit `Projekt leeren` setzt du das aktuelle Projekt nach einer Sicherheitsabfrage auf eine leere Startetage zurück.

### Navigation in 3D

* Mit gedrückter linker Maustaste drehst du die Kamera um das Modell.
* Mit der rechten Maustaste verschiebst du die 3D-Kamera seitlich.
* Mit dem Mausrad zoomst du in der 3D-Ansicht.
* Über `Projektion` schaltest du zwischen orthografischer und perspektivischer Darstellung um.
* Über die Geschoss-Checkboxen blendest du einzelne Etagen in der 3D-Ansicht ein oder aus.
* Die Option `3D Ebenen` blendet zusätzliche Oberflächen-Schichten ein oder aus.
* `Ansicht zentrieren` in der 3D-Ansicht setzt die Kamera auf die Standardansicht zurück.

### Auswahl und Bearbeiten

* Im Werkzeug `Bearbeiten` kannst du Bauteile auswählen.
* Die Auswahl wird in 2D und 3D gemeinsam hervorgehoben.
* `Auswahl aufheben` entfernt die aktuelle Hervorhebung.
* `Auswahl löschen` entfernt das aktuell markierte Bauteil.
* Wand-Endpunkte, die fachlich verbunden sind, werden gemeinsam verschoben.
* Bei der Auswahl gilt aktuell folgende Priorität:
  * Türen vor Fenstern
  * Fenster vor Treppen
  * Treppen vor Wänden
  * Wände vor Räumen

### Rückgängig und Wiederherstellen

Die aktuelle Version besitzt einen Projektverlauf für fachliche Bearbeitungsschritte.

* `Rückgängig` stellt den letzten Schritt wieder her.
* `Wiederherstellen` stellt einen zuvor zurückgenommenen Schritt erneut her.
* Der Verlauf umfasst derzeit unter anderem Bauteil-Anlage, Löschen, Etagenanlage, Projektleeren, DXF-Import und Hilfslinien.

## Tastaturkürzel

Wichtige Kürzel der aktuellen Oberfläche:

* `Cmd+N` oder `Strg+N`: Etage hinzufügen
* `Cmd+L` oder `Strg+L`: Projekt leeren
* `Cmd+Shift+E` oder `Strg+Shift+E`: DXF exportieren
* `Cmd+Shift+I` oder `Strg+Shift+I`: DXF importieren
* `Cmd+Shift+B` oder `Strg+Shift+B`: Teilebibliothek laden
* `Cmd+Z` oder `Strg+Z`: Rückgängig
* `Cmd+Shift+Z` oder `Strg+Shift+Z`: Wiederherstellen
* `Entf`: Auswahl löschen
* `Esc`: Auswahl aufheben
* `Cmd+0` oder `Strg+0`: 2D-Ansicht zentrieren
* `Cmd+Shift+0` oder `Strg+Shift+0`: 3D-Ansicht zentrieren
* `Cmd+E`, `Cmd+W`, `Cmd+R`, `Cmd+T`, `Cmd+D`, `Cmd+F`: Werkzeuge `Bearbeiten`, `Wand`, `Raum`, `Treppe`, `Tür`, `Fenster`

## Ansichten umschalten

Die sechs orthogonalen Ansichten werden jetzt über Pfeil-Buttons oberhalb der Zeichenfläche umgeschaltet:

* `↑ Nord`
* `↓ Süd`
* `← West`
* `→ Ost`
* `⤒ Oben`
* `⤓ Unten`

Die Umschaltung wirkt aktuell vor allem auf die gekoppelte 3D-Kamera. Dadurch kannst du das Modell direkt aus den wichtigsten Hauptrichtungen kontrollieren.

## Werkzeuge

### Wand

Mit dem Werkzeug `Wand` zeichnest du lineare Wände.

* Linke Maustaste drücken und ziehen.
* Ohne `Shift` wird orthogonal gezeichnet.
* Mit gedrückter `Shift`-Taste wird frei gezeichnet.
* Die Wandstärke und Wandhöhe kommen aus den aktuellen Eingabefeldern.

### Raum

Mit dem Werkzeug `Raum` ziehst du aktuell einen rechteckigen Raum auf.

* Linke Maustaste drücken und diagonal ziehen.
* Raumname, Raumhöhe, Bodenstärke und Deckenstärke kommen aus den aktuellen Eingaben.

### Treppe

Mit dem Werkzeug `Treppe` ziehst du die rechteckige Grundfläche einer Treppe auf.

* Das Treppen-Preset bestimmt den Grundtyp.
* Treppenhöhe und Stufenanzahl kannst du zusätzlich manuell anpassen.

### Tür

Mit dem Werkzeug `Tür` klickst du auf eine bestehende Wand.

* Die Tür wird an die Wand gebunden gespeichert.
* Türbreite, Türhöhe und Schwelle kommen aus den aktuellen Eingaben.
* `Tür-Preset` übernimmt Standardmaße aus der internen Bibliothek.

### Fenster

Mit dem Werkzeug `Fenster` klickst du auf eine bestehende Wand.

* Das Fenster wird an die Wand gebunden gespeichert.
* Fensterbreite, Fensterhöhe und Brüstungshöhe kommen aus den aktuellen Eingaben.
* `Fenster-Preset` übernimmt Standardmaße aus der internen Bibliothek.

### Bearbeiten

Im Werkzeug `Bearbeiten` verschiebst du aktuell vor allem verbundene Wand-Endpunkte. Außerdem kannst du damit Bauteile für die optische Kontrolle auswählen.

## Arbeiten mit Eingabewerten

Die Werteingabe ist einer der wichtigsten Teile der aktuellen Oberfläche. Sie steuert sowohl die Geometrie neuer Bauteile als auch das Verhalten während des Zeichnens.

## Allgemeine Regeln für Werteingaben

* Leere Felder verwenden den fachlichen Standardwert.
* Dezimalwerte dürfen mit Komma oder Punkt eingegeben werden.
* Einheiten werden nicht direkt in das Textfeld geschrieben, sondern über das zugehörige Einheitenfeld daneben gewählt.
* Ungültige Eingaben werden aktuell still verworfen. Dann greift der jeweilige Standardwert.

### Beispiele

* `1,20` mit Einheit `m` bedeutet `1,20 Meter`
* `120` mit Einheit `cm` bedeutet ebenfalls `1,20 Meter`
* `900` mit Einheit `mm` bedeutet `0,90 Meter`

## Rasterweite

Mit `Rasterweite` legst du das Zeichenraster fest.

* Das Zahlenfeld enthält den Wert.
* Das Einheitenfeld daneben bestimmt, ob der Wert in `mm`, `cm` oder `m` verstanden wird.
* Das Raster beeinflusst die Darstellung und bei aktivem `Snap Raster` auch die Fangpunkte.

### Typische Nutzung

* `25 cm` für grobe Wandplanung
* `10 cm` für detailliertere Innenwände
* `1 m` für sehr grobe Vorplanung

## Länge und Winkel beim Zeichnen

Die Felder `Länge` und `Winkel` wirken auf das gerade entstehende lineare Element, vor allem auf Wände.

### Länge

Wenn `Länge` leer bleibt:

* Die tatsächliche Mausposition bestimmt die Länge.

Wenn `Länge` gefüllt ist:

* Die Wand wird auf genau diese Länge gesetzt.
* Die Einheit rechts daneben bestimmt die Interpretation.

### Winkel

Wenn `Winkel` leer bleibt:

* Ohne `Shift` bleibt das orthogonale Verhalten aktiv.
* Mit `Shift` richtet sich das Segment frei nach der Maus aus.

Wenn `Winkel` gefüllt ist:

* Der Winkel wird als Gradwert verwendet.
* Damit kann eine Wand auch ohne freie Mausführung exakt in einem bestimmten Winkel erzeugt werden.

### Praktische Beispiele

* `Länge = 4,50 m`, `Winkel leer`:
  Eine Wand wird exakt 4,50 m lang, aber weiter orthogonal ausgerichtet.
* `Länge = 3,20 m`, `Winkel = 35`:
  Eine Wand wird 3,20 m lang und im Winkel von 35° erzeugt.

## Wandparameter

### Wandstärke

`Wandstärke` gilt für neue Wände.

Typische Beispiele:

* `11,5 cm` für leichte Innenwand
* `17,5 cm` für häufige massive Innenwand
* `24 cm` oder mehr für stärkere Außenwände

### Wandhöhe

`Wandhöhe` bestimmt die räumliche Höhe neuer Wände und damit auch die 3D-Darstellung.

Typische Beispiele:

* `2,50 m`
* `2,60 m`
* `2,75 m`

## Raumparameter

### Raumname

`Raum` ist das Namensfeld des nächsten Raums.

Typische Eingaben:

* `Wohnen`
* `Küche`
* `Bad`
* `Flur`

### Raumhöhe

`Raumhöhe` ist die lichte Höhe des nächsten Raums.

### Boden

`Boden` ist die Boden- oder Fußbodenstärke des nächsten Raums.

### Decke

`Decke` ist die Deckenstärke des nächsten Raums.

### Zusammenspiel in 3D

Die Kombination aus Raumhöhe, Boden und Decke beeinflusst direkt:

* Bodenplatte des Raums
* Raumvolumen
* Deckenplatte
* abgeleitete Geschosshöhen in der 3D-Darstellung

## Türparameter

### Türbreite

Steuert die Breite der nächsten Türöffnung.

Typische Werte:

* `88 cm` für Innentür
* `101 cm` für breitere Tür
* `110 cm` für barriereärmere Durchgänge

### Türhöhe

Steuert die Höhe der nächsten Tür.

### Schwelle

`Schwelle` ist der Höhenversatz der Türschwelle.

* `0 cm` bedeutet schwellenlos
* positive Werte bedeuten einen Höhenunterschied

### Tür-Preset

Das Preset setzt mehrere Werte gleichzeitig.

Es überschreibt typischerweise:

* Türbreite
* Türhöhe
* Schwellenwert

Danach kannst du die einzelnen Felder weiter manuell anpassen.

## Fensterparameter

### Fensterbreite

Steuert die Breite des nächsten Fensters.

### Fensterhöhe

Steuert die Höhe des nächsten Fensters.

### Brüstung

Steuert die Brüstungshöhe des nächsten Fensters, also die Höhe der Fensterunterkante über dem Boden.

Typische Werte:

* `90 cm` für klassisches Fenster
* `0 cm` für bodentiefes Element

### Fenster-Preset

Ein Preset setzt Breite, Höhe und Brüstungshöhe gemeinsam.

## Treppenparameter

### Treppen-Preset

Das Preset bestimmt den Grundtyp:

* gerade Treppe
* 180°-Treppe
* Wendeltreppe

### Treppenhöhe

`Treppenhöhe` beschreibt die zu überwindende Gesamthöhe.

### Stufen

`Stufen` beschreibt die Anzahl der Stufen des nächsten Treppenobjekts.

### Typische Nutzung

* Erst Preset wählen
* Danach Höhe und Stufenzahl fachlich anpassen
* Danach Grundfläche in 2D aufziehen

## Raster, Snap und Präzision

### Raster

Blendet das Raster sichtbar ein oder aus.

### Snap Raster

Wenn aktiv:

* Punkte rasten auf das Raster ein.

Wenn deaktiviert:

* Das Raster bleibt optional sichtbar, wirkt aber nicht mehr als Fanghilfe.

### Snap Punkte

Wenn aktiv:

* vorhandene Wand-Enden werden als Fangpunkte bevorzugt berücksichtigt.

Das ist besonders wichtig, um geschlossene Grundrisskonturen konsistent aufzubauen.

## Hilfslinien

* Ziehe aus dem oberen Lineal eine vertikale Hilfslinie.
* Ziehe aus dem linken Lineal eine horizontale Hilfslinie.
* Mit `Alt` + rechter Maustaste entfernst du eine nahe Hilfslinie.
* Über `Hilfslinien` blendest du alle vorhandenen Hilfslinien aus oder ein.

## Ansichten

Oben gibt es Schalter für die orthogonalen Ansichten:

* Oben
* Unten
* Nord
* Süd
* Ost
* West

Der aktuelle Stand blendet diese Ansichten sichtbar um und beschreibt sie im Overlay. Die vollständige fachliche Kipp- und Darstellungslogik ist noch ein Ausbaupunkt.

## DXF-Import und -Export

### DXF exportieren

* Exportiert aktuell die aktive Etage.
* Die Anwendung sorgt nun dafür, dass die Dateiendung `.dxf` nicht doppelt angehängt wird.

### DXF importieren

* Importiert eine DXF-Datei als neue Etage.
* Mehrfach angehängte `.dxf`-Endungen werden für den Etagennamen bereinigt.

## Teilebibliotheken

### Interne Bibliothek

Die Anwendung bringt Standard-Presets für:

* Türen
* Fenster
* Treppen

### Externe Bibliothek

Über `Teilebibliothek laden` können zusätzliche Presets aus `.cadasparts` importiert werden.

## Aktuelle Grenzen

Die aktuelle Version konzentriert sich bewusst auf einen robusten Grundrisskern mit erster 3D-Ableitung. Noch nicht umgesetzt oder noch nicht vollständig ausgebaut sind unter anderem:

* freie 3D-Modellierung jenseits der aus dem Grundriss abgeleiteten Körper
* vollständige Projekt-Undo-/Redo-Funktion
* konfigurierbare Kontextmenüs und breitere Mehrfachselektion
* grafische Verwaltung von Dachmodellen und zusätzlichen Flächen-Ebenen
* vollständige DWG-Verarbeitung

## Typischer Arbeitsablauf

1. Etage wählen oder anlegen.
2. Rasterweite festlegen.
3. Werkzeug `Wand` wählen und Außenkontur zeichnen.
4. Innenwände ergänzen.
5. Werkzeug `Raum` wählen und Räume anlegen.
6. Türen und Fenster mit Presets oder freien Werten setzen.
7. Treppen platzieren.
8. Ergebnis in der 3D-Ansicht kontrollieren.
9. DXF exportieren.

## Nächste fachliche Ausbaustufen

Die nächsten geplanten Schritte sind:

* komplexere 3D-Geometrien und Materialdarstellungen
* grafische Verwaltung für Dach- und Oberflächen-Ebenen
* vollständige DWG-Anbindung
* weitere Bearbeitungs- und Selektionsfunktionen
