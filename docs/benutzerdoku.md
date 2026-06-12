# Benutzerdokumentation

## Zweck der aktuellen Version

`CADas` ist aktuell ein CAD-MVP für Gebäude-Grundrisse mit kombinierter 2D- und 3D-Workbench. Der Schwerpunkt liegt auf einem robusten Grundrisskern für Etagen, Wände, Räume, Türen, Fenster und Treppen. Die 3D-Ansicht dient als direkte räumliche Kontrolle desselben Fachmodells.

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

### Anwendung mit lokalem Automatisierungszugriff starten

```bash
./gradlew runMitAutomatisierung
```

Damit startet `CADas` zusätzlich mit einem lokalen HTTP-Zugriff auf `127.0.0.1:17845`. Dieser Modus ist für agentische oder manuelle Funktionstests gedacht.

Beispiele:

```bash
curl http://127.0.0.1:17845/health
curl http://127.0.0.1:17845/state
curl "http://127.0.0.1:17845/tool?value=WALL"
curl "http://127.0.0.1:17845/canvas/drag?fromX=120&fromY=120&toX=320&toY=120&button=PRIMARY"
curl "http://127.0.0.1:17845/invoke?action=exportProjectDxf&path=/tmp/haus.dxf"
```

## Aufbau der Oberfläche

Die Oberfläche besteht aktuell aus fünf Hauptbereichen:

* Menüleiste
* kompakte Werkzeugleiste
* linke Eigenschaftenleiste
* gemeinsamer Mittelbereich für 2D-Zeichenfläche oder 3D-Ansicht
* untere Statusleiste

### Menüleiste

Die Menüleiste bündelt Datei-, Bearbeitungs-, Ansichts- und Werkzeugaktionen. Dazu gehören insbesondere:

* Etage hinzufügen
* Projekt leeren
* Gebäude-DXF importieren und exportieren
* Etagen-DXF importieren und exportieren
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

* allgemeine Zeichenwerte wie Rasterweite, Länge, Winkel und Nordwinkel
* fachliche Eigenschaften für Wand, Raum, Tür, Fenster und Treppe
* Ebenen- und Belagswerte für Innenwand, Außenwand, Boden, Decke und Dachflächen
* eine Auswahlzusammenfassung
* eine Übersicht registrierter externer CAD-Bibliotheken

Die Eigenschaftenleiste blendet nur die Bereiche ein, die zum aktuellen Werkzeug oder zur aktiven Auswahl passen.

### 2D-Zeichenfläche

In der Mitte liegt die eigentliche Grundrissfläche. Dort werden Wände, Räume, Treppen, Türen und Fenster gezeichnet oder bearbeitet.

### 3D-Ansicht

Im selben Mittelbereich kann statt der 2D-Zeichenfläche auch die 3D-Ansicht eingeblendet werden. Sie basiert direkt auf denselben Domänendaten wie die 2D-Ansicht. Auswahl, Sichtbarkeit und Hervorhebung sind mit der 2D-Ansicht gekoppelt.

### Statusleiste

Unten zeigt die Anwendung:

* aktive Ansicht
* aktuelle Etage
* Zoomfaktor
* Cursorposition
* Werkzeug- oder Zeichenstatus

## Grundbedienung

## Fachliches Modell einer Etage

Für das Verständnis der aktuellen Version ist jetzt ein anderer Punkt zentral: `Wände` sind die primäre bauliche Geometrie, `Räume` werden daraus automatisch abgeleitet.

Das bedeutet im aktuellen Stand:

* Wände werden über ihre Achse, Stärke und Höhe gezeichnet.
* Die Wandachse ist eine Konstruktionslinie, nicht die Raumkante.
* Der Raum beginnt an der inneren sichtbaren Wandkante, also bei der Wandachse plus beziehungsweise minus halber Wandstärke.
* Türen und Fenster sind weiter direkt an genau eine Wand gebunden.
* Räume entstehen automatisch aus geschlossenen orthogonalen Wandzügen.
* Wenn du Wände verschiebst, veränderst oder schließt, wird die Raumkontur automatisch nachgeführt.
* Räume dürfen dadurch auch polygonal werden und müssen nicht rechteckig bleiben.

Praktisch heißt das für Anwender:

* Zuerst werden die Wände gezeichnet.
* Daraus entsteht die fachliche Innenfläche des Raums automatisch.
* Namen, Raumhöhe, Boden, Decke und Dachschräge hängen dann am automatisch erkannten Raum.

Wichtig:

* Ohne zusätzliche Innenlagen basiert die automatische Raumkontur auf der reinen Wandstärke.
* Sichtbare Innenbeläge auf Wänden verschieben die Raumkante zusätzlich nach innen.
* Dadurch ändern sich Raumkontur, Fläche und Volumen unmittelbar mit.

## Empfohlener Zeichenablauf pro Etage

Die aktuelle Version arbeitet am stabilsten mit der folgenden Reihenfolge:

1. Etage anlegen oder auswählen.
2. Mit dem Werkzeug `Wand` die baulichen Außen- und Innenwände zeichnen.
3. Mit `Tür` und `Fenster` Öffnungen direkt auf die passenden Wände setzen.
4. Prüfen, ob aus den geschlossenen Wandzügen die Räume automatisch erkannt wurden.
5. Bei Bedarf Treppen platzieren.
6. Danach Ansichten, Bemaßung, Flächen und 3D zur Kontrolle nutzen.

Wichtig:

* Für eine fachlich saubere Etage musst du die Wände vollständig und geschlossen zeichnen.
* Ein Raum entsteht nicht aus einer separaten Rechteckzeichnung, sondern aus der Innenfläche des Wandzugs.
* Türen und Fenster benötigen immer zuerst eine vorhandene Wand.

## Was genau gehört in die Wände, was in den Raum?

### Wände

Wände tragen aktuell:

* Verlauf der Wandachse
* Wandstärke
* Wandhöhe
* Türen und Fenster als wandgebundene Öffnungen

### Räume

Räume tragen aktuell:

* automatisch abgeleitete Innenkontur
* Raumname
* Raumfläche
* Raumvolumen
* lichte Raumhöhe auf der hohen Seite
* Bodenstärke
* Deckenstärke
* optional eine innere Dachschräge oder schräge Decke

## Dachgeschoss und Dachschrägen

Die aktuelle Lösung für Dachschrägen ist jetzt primär wand- und eckbasiert.

Das bedeutet:

* Die Höhe der Decke entsteht aus den Höhen der verbundenen Wand-Endpunkte.
* Ein gemeinsamer Wand-Endpunkt ist fachlich ein Höhenknoten.
* Aus diesen Eckhöhen werden die obere Wandkante, die polygonale Raumdecke und das Raumvolumen automatisch neu berechnet.
* Die schräge Innenfläche bleibt dadurch direkt als fachliche Deckenfläche erhalten und kann später mit Ebenen, Platten oder anderen Belägen weiter genutzt werden.

### So gibst du eine schräge Decke im Dachgeschoss ein

1. Zeichne zuerst die Wände des Dachgeschosses ganz normal und schließe den Raum vollständig.
2. Warte, bis der Raum aus dem Wandzug automatisch erkannt wurde.
3. Wechsle in das Werkzeug `Bearbeiten`.
4. Klicke die gemeinsame Wandecke an, an der die Decke niedriger oder höher werden soll.
5. Trage links bei `Eckhöhe` die gewünschte lichte Höhe dieser Ecke ein.
6. Klicke auf `Eckhöhe anwenden`.
7. Wiederhole das für weitere Ecken, bis die gewünschte Dach- oder Deckenform entstanden ist.

Aus diesen Werten ergibt sich automatisch:

* die obere Schräge der betroffenen Wände
* die polygonale Raumdecke
* das angepasste Raumvolumen
* die Darstellung in Seitenansichten und 3D

### Typischer Anwendungsfall im Dachgeschoss

Ein Raum hat entlang einer Traufseite einen Kniestock und steigt zur Firstseite an.

Dann gehst du typischerweise so vor:

1. Zeichne den geschlossenen Wandzug des Raums.
2. Wähle die beiden Ecken an der niedrigen Traufseite nacheinander aus.
3. Setze dort jeweils zum Beispiel `1,00 m` oder `1,20 m` als `Eckhöhe`.
4. Lasse die gegenüberliegenden hohen Ecken auf der normalen Raumhöhe, zum Beispiel `2,80 m` oder `3,10 m`.

Damit entsteht keine symbolische Schräge mehr, sondern eine fachlich wirksame Innenfläche mit echter Volumenänderung.

### Was du in 2D und 3D siehst

* In Seitenansichten werden Wände mit schräger Oberkante dargestellt.
* Räume erscheinen dort nicht nur rechteckig, sondern mit polygonaler Deckenkontur.
* In 3D werden die Raumvolumina und Wandkörper aus diesen Eckhöhen abgeleitet.

### Einfacher Fallback für Rechteckräume

Für einfache Rechteckräume gibt es weiterhin die ältere raumgebundene Eingabe:

* `Dachschräge = Mit Dachschräge`
* `Niedrige Seite`
* `Sockelhöhe`

Dieser Modus ist als einfacher Fallback noch vorhanden. Für echte Dachgeschoss-Geometrien mit konkreten Wand- und Eckhöhen ist aber die Bearbeitung über `Eckhöhe` der fachlich führende Weg.

## Aktuelle Grenzen der Dachschrägen

Derzeit gilt:

* Die automatische Deckenform entsteht aus Wand-Endpunkten und interpoliert dazwischen. Komplexe Dachdetails wie Dachgauben oder mehrfach geknickte Dachflächen sind noch nicht als eigene Dachobjekte modelliert.
* Der ältere Rechteck-Fallback für `Dachschräge` bleibt bestehen, sollte aber nur für einfache Räume verwendet werden.

### Navigation in 2D

* Mit dem Mausrad zoomst du in die Zeichnung hinein oder heraus.
* Mit der rechten Maustaste verschiebst du die Zeichenfläche.
* `2D zentrieren` setzt Zoom und Position der 2D-Ansicht zurück.
* Über die Etagenauswahl wechselst du zwischen vorhandenen Geschossen.
* Mit `Etage hinzufügen` legst du ein weiteres Geschoss an.
* Mit `Projekt leeren` setzt du das aktuelle Projekt nach einer Sicherheitsabfrage auf eine leere Startetage zurück.

### Navigation in 3D

* Mit gedrückter linker Maustaste drehst du das Modell um seine Mitte.
* Mit gedrückter rechter Maustaste verschiebst du die 3D-Ansicht entlang der Bildschirmachsen.
* Mit dem Mausrad zoomst du in der 3D-Ansicht.
* Die 3D-Ansicht nutzt dafür eine feste Kamera mit Modell-Orbit; `Modell einpassen` und `Ansicht zentrieren` halten das Modell im Blick.
* Über `Projektion` schaltest du zwischen orthografischer und perspektivischer Darstellung um.
* `Oberflächenrendering` blendet die transparenten Raumvolumina aus und zeigt stattdessen die sichtbaren Flächen, Beläge und Öffnungen als baunähere Ansicht.
* Über `Iso`, `Oben`, `Vorne` und `Rechts` wechselst du auf schnelle 3D-Kamerapresets.
* Über die Geschoss-Checkboxen blendest du einzelne Etagen in der 3D-Ansicht ein oder aus.
* Die Option `3D Ebenen` blendet zusätzliche Oberflächen-Schichten ein oder aus.
* `Modell einpassen` richtet Abstand und Mittelpunkt auf das aktuell sichtbare Modell aus.
* `Ansicht zentrieren` setzt die Kamera auf die räumliche Standardperspektive zurück.

### Auswahl, Mehrfachauswahl und Kontextmenü

* Im Werkzeug `Bearbeiten` kannst du Bauteile auswählen.
* Ein Klick auf leeren Raum hebt die Auswahl wieder auf.
* Mit `Shift` oder `Cmd` beziehungsweise `Strg` erweiterst oder verringerst du die Auswahl.
* Die Auswahl wird in 2D und 3D gemeinsam hervorgehoben.
* Über `Auswahl aufheben` entfernst du die aktuelle Hervorhebung.
* Über `Auswahl löschen` entfernst du alle aktuell markierten Bauteile.
* Ein Rechtsklick auf eine Auswahl öffnet ein Kontextmenü mit passenden Aktionen.
* Aktuell bietet das Kontextmenü insbesondere Eigenschaften übernehmen, Auswahl löschen, Auswahl aufheben und 90°-Drehung für rotierbare Bauteile.

### Rückgängig und Wiederherstellen

Die aktuelle Version besitzt einen Projektverlauf für fachliche Bearbeitungsschritte.

* `Rückgängig` stellt den letzten Schritt wieder her.
* `Wiederherstellen` stellt einen zuvor zurückgenommenen Schritt erneut her.
* Der Verlauf umfasst unter anderem Bauteil-Anlage, Löschen, Etagenanlage, Projektleeren, DXF-Import, Hilfslinien und Eigenschaftsübernahmen.

## Tastaturkürzel

Wichtige Kürzel der aktuellen Oberfläche:

* `Cmd+N` oder `Strg+N`: Etage hinzufügen
* `Cmd+L` oder `Strg+L`: Projekt leeren
* `Cmd+Shift+E` oder `Strg+Shift+E`: Gebäude als DXF exportieren
* `Cmd+Shift+I` oder `Strg+Shift+I`: Gebäude aus DXF importieren
* `Cmd+Shift+B` oder `Strg+Shift+B`: Teilebibliothek laden
* `Cmd+Z` oder `Strg+Z`: Rückgängig
* `Cmd+Shift+Z` oder `Strg+Shift+Z`: Wiederherstellen
* `Cmd+Shift+P` oder `Strg+Shift+P`: sichtbare Eigenschaften auf Auswahl anwenden
* `Cmd+Shift+→` oder `Strg+Shift+→`: Auswahl 90° rechts drehen
* `Cmd+Shift+←` oder `Strg+Shift+←`: Auswahl 90° links drehen
* `Entf`: Auswahl löschen
* `Esc`: Auswahl aufheben
* `Cmd+0` oder `Strg+0`: 2D-Ansicht zentrieren
* `Cmd+Shift+0` oder `Strg+Shift+0`: 3D-Ansicht zentrieren
* `Cmd+E`, `Cmd+W`, `Cmd+R`, `Cmd+T`, `Cmd+D`, `Cmd+F`: Werkzeuge `Bearbeiten`, `Wand`, `Raum`, `Treppe`, `Tür`, `Fenster`

## Ansichten umschalten

Die sechs orthogonalen Ansichten werden oberhalb der Zeichenfläche über feste Tasten für `Oben` und `Unten` sowie über relative Pfeiltasten umgeschaltet:

* `⤒ Oben` setzt die Draufsicht.
* `⤓ Unten` setzt die Ansicht von unten.
* `↑` kippt das Modell aus der aktuellen Sicht nach oben.
* `↓` kippt das Modell aus der aktuellen Sicht nach unten.
* `←` dreht das Modell aus der aktuellen Sicht nach links.
* `→` dreht das Modell aus der aktuellen Sicht nach rechts.

Die Umschaltung wirkt auf die 2D-Projektion und auf die Kompassdarstellung. Besonders bei `Oben` ist der Nordwinkel relevant.
Die 3D-Kamera bleibt davon bewusst unabhängig und wird über die 3D-Bedienelemente gesteuert.

## Zwischen 2D und 3D umschalten

Oberhalb des Mittelbereichs gibt es jetzt die Umschalter `2D` und `3D`.

* `2D` zeigt die Zeichenfläche mit Linealen und Grundrisswerkzeugen.
* `3D` nutzt denselben Platz für die räumliche Modellansicht.
* Dadurch bleibt mehr Platz für die aktive Arbeitsansicht, statt 2D und 3D ständig parallel einzublenden.

## DWG-Blöcke für Oberflächen verwenden

Für geladene `DWG`-Bibliotheken gibt es jetzt zwei Wege, konkrete Blöcke als Oberflächen-Presets auswählbar zu machen:

* automatisch über eine optionale Katalogdatei neben der `DWG`
* manuell über das Feld `DWG-Block` in der Ebenenverwaltung

### Automatischer Katalog

Lege neben die Bibliothek eine Textdatei mit dem Namen `<dateiname>.blocks` oder `<basisname>.blocks`.

Beispiel:

```text
Rigips_1250x2000
OSB_2500x675
Daemmplatte_1200x600
```

Jede nichtleere Zeile wird dann beim Laden der `DWG` als eigener auswählbarer `DWG-Block` registriert.

### Manuelle Registrierung

1. Lade die `DWG` über `Teilebibliothek laden`.
2. Wechsle zu einer Wand- oder Raumfläche im Bereich `Ebenen`.
3. Trage unter `DWG-Block` einen konkreten Blocknamen ein.
4. Klicke auf `DWG-Block hinzufügen`.
5. Das neue Preset erscheint danach direkt in der Preset-Auswahl.

## Werkzeuge

### Wand

Mit dem Werkzeug `Wand` zeichnest du lineare Wände.

* Linke Maustaste drücken und ziehen.
* Ohne `Shift` wird orthogonal gezeichnet.
* Mit gedrückter `Shift`-Taste wird frei gezeichnet.
* Wandstärke und Wandhöhe kommen aus den aktuellen Eingabefeldern.

### Raum

Mit dem Werkzeug `Raum` zeichnest du keinen separaten Rechteckraum mehr.

* Räume werden automatisch aus geschlossenen Wandzügen erzeugt.
* Mit dem Werkzeug `Raum` klickst du in einen automatisch erkannten Raum, um ihn gezielt auszuwählen.
* Raumname, Raumhöhe, Bodenstärke, Deckenstärke und Dachschräge kommen aus den aktuellen Eingaben und können dann auf die Auswahl angewendet werden.
* Wenn du eine gemeinsame Wandecke verschiebst, wird die Raumkontur automatisch nachgeführt und darf dabei auch schräg oder polygonal werden.

### Ebenen auf Flächen

Ebenen werden immer auf der aktuell ausgewählten Wand- oder Raumfläche gepflegt.

* Wähle im Werkzeug `Bearbeiten` zuerst eine Wand oder einen Raum aus.
* Öffne links den Bereich `Ebenen`.
* Wähle den Flächentyp, zum Beispiel `WALL_INTERIOR`, `WALL_EXTERIOR`, `FLOOR` oder `CEILING`.
* Wähle optional ein Preset wie `Fliese`, `Dämmplatte`, `Rigipsplatte`, `OSB-Platte`, `Tapete` oder eine registrierte `DWG-Referenz`.
* Lege Name, Dicke, Platten- oder Fliesenmaß, Verlegeart, Mindestversatz und Mindestbreite fest.
* `Ebene hinzufügen` legt den Belag auf der gewählten Fläche an.
* `Ebene aktualisieren`, `ausblenden`, `nach oben` und `nach unten` ändern Bestand und Reihenfolge.

Für Anwender wichtig:

* Sichtbare Innenlagen auf Wänden verkleinern den Raum entlang der Innenkante.
* Sichtbare Boden- und Deckenlagen verändern die lichte Raumhöhe und damit das Volumen.
* Die 3D-Ansicht zeigt diese Ebenen auf Wunsch als gestapelte Schichten an.
* Registrierte `DWG`-Bibliotheken erscheinen zusätzlich als auswählbare Referenz-Presets für Ebenen.

### Treppe

Mit dem Werkzeug `Treppe` ziehst du die rechteckige Grundfläche einer Treppe auf.

* Das Treppen-Preset bestimmt den Grundtyp.
* Treppenhöhe und Stufenanzahl kannst du zusätzlich manuell anpassen.
* Unterstützt sind aktuell gerade Treppen, 180°-Treppen, gegenläufige Treppen und Wendeltreppen.

### Tür

Mit dem Werkzeug `Tür` klickst du auf eine bestehende Wand.

* Die Tür wird wandgebunden gespeichert.
* Türbreite, Türhöhe und Schwelle kommen aus den aktuellen Eingaben.
* `Tür-Preset` übernimmt Standardmaße aus der internen Bibliothek.

### Fenster

Mit dem Werkzeug `Fenster` klickst du auf eine bestehende Wand.

* Das Fenster wird wandgebunden gespeichert.
* Fensterbreite, Fensterhöhe und Brüstungshöhe kommen aus den aktuellen Eingaben.
* `Fenster-Preset` übernimmt Standardmaße aus der internen Bibliothek.

### Bearbeiten

Im Werkzeug `Bearbeiten` verschiebst du aktuell vor allem verbundene Wand-Endpunkte und wählst Bauteile zur Bearbeitung oder Kontrolle aus.

## Arbeiten mit Eingabewerten

Die Werteingabe ist einer der wichtigsten Teile der aktuellen Oberfläche. Sie steuert sowohl die Geometrie neuer Bauteile als auch das Verhalten während des Zeichnens und beim Bearbeiten ausgewählter Elemente.

## Allgemeine Regeln für Werteingaben

* Leere Felder verwenden den fachlichen Standardwert des jeweiligen Bauteils.
* Dezimalwerte dürfen mit Komma oder Punkt eingegeben werden.
* Einheiten werden nicht direkt in das Textfeld geschrieben, sondern über das zugehörige Einheitenfeld daneben gewählt.
* Ungültige Eingaben werden aktuell verworfen. Dann greift der jeweilige Standardwert.
* Wenn eine Auswahl aktiv ist, kannst du die sichtbaren Werte mit `Werte auf Auswahl anwenden` auf alle passenden markierten Bauteile übernehmen.

### Beispiele

* `1,20` mit Einheit `m` bedeutet `1,20 Meter`.
* `120` mit Einheit `cm` bedeutet ebenfalls `1,20 Meter`.
* `900` mit Einheit `mm` bedeutet `0,90 Meter`.

## Zeichenwerte im Detail

### Rasterweite

Mit `Rasterweite` legst du das Zeichenraster fest.

* Das Zahlenfeld enthält den Wert.
* Das Einheitenfeld daneben bestimmt, ob der Wert in `mm`, `cm` oder `m` verstanden wird.
* Das Raster beeinflusst die Darstellung und bei aktivem `Snap Raster` auch die Fangpunkte.

Typische Nutzung:

* `25 cm` für grobe Wandplanung
* `10 cm` für detailliertere Innenwände
* `1 m` für grobe Vorplanung

### Länge

`Länge` wirkt auf das gerade entstehende lineare Element, vor allem auf Wände.

Wenn `Länge` leer bleibt:

* Die tatsächliche Mausposition bestimmt die Länge.

Wenn `Länge` gefüllt ist:

* Die Wand wird auf genau diese Länge gesetzt.
* Die Einheit rechts daneben bestimmt die Interpretation.

Typische Beispiele:

* `4,50` mit Einheit `m` erzeugt eine Wand mit exakt `4,50 m`.
* `240` mit Einheit `cm` erzeugt eine Wand mit exakt `2,40 m`.

### Winkel

`Winkel` wirkt auf die Richtung des aktuell gezogenen linearen Elements.

Wenn `Winkel` leer bleibt:

* Ohne `Shift` bleibt das orthogonale Verhalten aktiv.
* Mit `Shift` richtet sich das Segment frei nach der Maus aus.

Wenn `Winkel` gefüllt ist:

* Der Winkel wird als Gradwert verwendet.
* Damit kann eine Wand auch ohne freie Mausführung exakt in einem bestimmten Winkel erzeugt werden.

Praktische Beispiele:

* `Länge = 4,50 m`, `Winkel leer`:
  Eine Wand wird exakt 4,50 m lang und orthogonal ausgerichtet.
* `Länge = 3,20 m`, `Winkel = 35`:
  Eine Wand wird 3,20 m lang und im Winkel von 35° erzeugt.

### Nordwinkel

`Nordwinkel` legt fest, wie das Gebäude relativ zur Planansicht ausgerichtet ist.

* `0` bedeutet: oben im Plan ist Norden.
* `90` bedeutet: Norden liegt nach rechts.
* `180` bedeutet: Norden liegt unten.
* Der Kompass in der Zeichenfläche wird entsprechend gedreht.

Diese Einstellung ist vor allem in der Draufsicht hilfreich, wenn das Gebäude nicht exakt achsparallel gezeichnet wurde.

## Bauteilwerte im Detail

### Wand

`Wandstärke` und `Wandhöhe` gelten für neue Wände und für ausgewählte Wände beim Anwenden von Eigenschaften.

Typische Wandstärken:

* `11,5 cm` für leichte Innenwände
* `17,5 cm` für häufige massive Innenwände
* `24 cm` oder mehr für stärkere Außenwände

Typische Wandhöhen:

* `2,50 m`
* `2,60 m`
* `2,75 m`

### Raum

`Name`, `Raumhöhe`, `Boden` und `Decke` gelten für neue Räume und für ausgewählte Räume.

Typische Raumnamen:

* `Wohnen`
* `Küche`
* `Bad`
* `Flur`

Das Zusammenspiel in 3D:

* `Raumhöhe` beeinflusst das Raumvolumen.
* `Boden` beeinflusst die Bodenplatte.
* `Decke` beeinflusst die Deckenplatte.
* Aus diesen Werten werden später auch Geschosshöhen abgeleitet.

### Tür

`Türbreite`, `Türhöhe` und `Schwelle` gelten für neue Türen und für ausgewählte Türen.

Typische Werte:

* `88 cm` für Innentüren
* `101 cm` für breitere Türen
* `110 cm` für barriereärmere Durchgänge
* `0 cm` Schwelle für schwellenlose Übergänge

Das Tür-Preset setzt mehrere Werte gleichzeitig und kann danach manuell überschrieben werden.

### Fenster

`Fensterbreite`, `Fensterhöhe` und `Brüstung` gelten für neue Fenster und für ausgewählte Fenster.

Typische Werte:

* `90 cm` Brüstung für klassische Fenster
* `0 cm` Brüstung für bodentiefe Elemente

Das Fenster-Preset setzt Breite, Höhe und Brüstungshöhe gemeinsam.

### Treppe

`Treppen-Preset`, `Treppenhöhe` und `Stufen` gelten für neue Treppen und für ausgewählte Treppen.

Empfohlene Reihenfolge:

* Erst Preset wählen.
* Danach Höhe und Stufenzahl fachlich anpassen.
* Danach Grundfläche in 2D aufziehen.
* Bei Bedarf die fertige Treppe über Auswahl oder Kontextmenü um 90° drehen.

## Eigenschaften auf Auswahl anwenden

Die Schaltfläche `Werte auf Auswahl anwenden` wirkt auf alle aktuell gewählten Bauteile, deren Typ zu den sichtbaren Feldern passt.

Beispiele:

* Mehrere Wände auswählen und anschließend `Wandstärke` und `Wandhöhe` gemeinsam ändern.
* Mehrere Räume auswählen und allen dieselbe `Raumhöhe` geben.
* Mehrere Treppen auswählen und die `Stufen` oder `Treppenhöhe` gemeinsam anpassen.

Wichtig:

* Die Anwendung übernimmt nur sinnvolle Felder für den gerade aktiven Auswahltyp.
* Bei gemischten Auswahlen solltest du gezielt nur gleichartige Bauteile zusammen bearbeiten.

## Drehen ausgewählter Bauteile

Aktuell lassen sich folgende Bauteile in 90°-Schritten drehen:

* Wände
* Räume
* Treppen

Die Drehung ist erreichbar über:

* Menü `Werkzeuge`
* Kontextmenü der Auswahl
* Tastaturkürzel `Cmd+Shift+→` oder `Cmd+Shift+←`

Türen und Fenster bleiben wandgebunden. Wenn du eine Wand drehst, folgen ihre Öffnungen automatisch über die Wandbindung.

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

* Ziehe aus dem oberen Lineal eine horizontale Hilfslinie in die Zeichnung.
* Ziehe aus dem linken Lineal eine vertikale Hilfslinie in die Zeichnung.
* Während des Ziehens wird die aktuelle Zielposition als Länge angezeigt.
* Beim Platzieren wird derselbe Snap-Mechanismus genutzt wie beim Zeichnen.
* Die Position wird auf Basis des jeweils anderen Lineals in die Zeichnungsfläche projiziert.
* Mit `Alt` + rechter Maustaste entfernst du eine nahe Hilfslinie.
* Über `Hilfslinien` blendest du alle vorhandenen Hilfslinien aus oder ein.

## DXF-Import und -Export

### Gebäude-DXF als Standard

Die Standardfunktionen arbeiten auf Gebäudeebene:

* `Gebäude als DXF exportieren`
* `Gebäude aus DXF importieren`

Damit werden alle aktuell vorhandenen Etagen gemeinsam verarbeitet.

### Etagen-DXF als Zusatzfunktion

Zusätzlich gibt es weiterhin:

* `Aktive Etage als DXF exportieren`
* `DXF als neue Etage importieren`

Diese Funktionen sind nützlich, wenn du gezielt nur ein Geschoss austauschen willst.

### Fachlicher Inhalt des Exports

Der Export enthält aktuell:

* sichtbare DXF-Geometrie für Wände, Räume, Öffnungen und Treppen
* CADas-Metadaten für verlustärmeren Re-Import
* metrische DXF-Kennzeichnung für Millimeter und Model-Space
* DXF-Tabellen für Layer, Linientypen, Textstil und Block-Records
* wiederverwendbare Tür-, Fenster- und Treppenblöcke mit `INSERT`-Referenzen
* Handles sowie eine einfache `OBJECTS`- und Layout-Grundstruktur

### Dateinamen

* Die Anwendung sorgt dafür, dass `.dxf` nicht doppelt angehängt wird.
* Wiederholte `.dxf`-Endungen werden beim Etagenimport für den Etagennamen bereinigt.

## Teilebibliotheken

### Interne Bibliothek

Die Anwendung bringt Standard-Presets für:

* Türen
* Fenster
* Treppen

### Externe Bibliotheken

Über `Teilebibliothek laden` können zwei Typen eingebunden werden:

* `.cadasparts` für direkt importierbare CADas-Presets
* `.dwg` als registrierte externe CAD-Bibliothek für Oberflächen-Presets und Blockquellen

Aktuell gilt:

* `.cadasparts` erweitert die auswählbaren Presets sofort.
* `.dwg` wird registriert und in der Eigenschaftenleiste dokumentiert.
* Konkrete `DWG`-Blöcke lassen sich über `.blocks`-Kataloge oder manuell als Oberflächen-Presets freischalten.

## Automatisierung für Tests

Der Automatisierungsmodus erlaubt direkte Tests gegen eine laufende App, ohne manuell zu klicken.

Aktuell verfügbar sind insbesondere:

* Werkzeug umschalten
* Eingabefelder setzen
* Einheiten umstellen
* Etagen wechseln
* Hilfslinien setzen
* auf der Zeichenfläche klicken oder ziehen
* Projekt- und Etagen-DXF importieren oder exportieren
* Teilebibliotheken registrieren oder laden

Damit kann ein Testablauf beispielsweise so aussehen:

1. App mit `./gradlew runMitAutomatisierung` starten.
2. Werkzeug per HTTP auf `WALL` setzen.
3. Eine Wand per `/canvas/drag` anlegen.
4. Projekt oder Etage per `/invoke` exportieren.
5. Die erzeugte DXF-Datei fachlich prüfen.

## Typischer Arbeitsablauf

1. Etage wählen oder anlegen.
2. Rasterweite festlegen.
3. Werkzeug `Wand` wählen und Außenkontur zeichnen.
4. Innenwände ergänzen.
5. Prüfen, ob die Räume automatisch aus dem geschlossenen Wandzug entstanden sind.
6. Türen und Fenster mit Presets oder freien Werten setzen.
7. Treppen platzieren und bei Bedarf drehen.
8. Ergebnis in der 3D-Ansicht kontrollieren.
9. Gebäude als DXF exportieren.

## Aktuelle Grenzen

Die aktuelle Version konzentriert sich bewusst auf einen robusten Grundrisskern mit erster 3D-Ableitung. Noch nicht umgesetzt oder noch nicht vollständig ausgebaut sind unter anderem:

* freie 3D-Modellierung jenseits der aus dem Grundriss abgeleiteten Körper
* vollständige binäre `DWG`-Verarbeitung direkt aus dem Dateiformat statt der heutigen Blockkatalog- und Referenznutzung
* grafische Verwaltung von Dachmodellen und zusätzlichen Flächen-Ebenen

## Nächste fachliche Ausbaustufen

Die nächsten geplanten Schritte sind:

* komplexere 3D-Geometrien und Materialdarstellungen
* grafische Verwaltung für Dach- und Oberflächen-Ebenen
* vollständige `DWG`-Anbindung direkt aus dem Format
* tiefere AutoCAD-Kompatibilität über zusätzliche DXF-Datentypen und weitergehende Zeichnungsmetadaten
