# Systemdokumentation

## Zielbild

`CADas` ist aktuell als Java-Desktop-Anwendung für Gebäude-Grundrisse mit kombinierter 2D- und 3D-Workbench aufgesetzt. Der technische Schwerpunkt liegt weiterhin auf einem sauberen Geometriekern, damit DXF-Import/Export, Teilebibliotheken und die 3D-Ableitung auf konsistenten Fachobjekten aufbauen.

![Systemarchitektur](diagramme/systemarchitektur.svg)

## Aktueller Architekturstand

### Technologien

* `JDK 25`
* `Gradle Wrapper 9.5.0`
* `JavaFX` für die Desktop-Oberfläche
* `JUnit 5 Jupiter` für Unit-Tests
* `JaCoCo` für Testreports

### Start- und Paketierungsmodell

Die Anwendung läuft inzwischen als modulare JavaFX-Anwendung mit `module-info.java`, `mainModule` und einem Launcher auf Modulpfad-Basis. Dadurch sind die generierten Startskripte stabiler und die bisherigen JavaFX-Warnungen zur nicht unterstützten Klassenpfad-Konfiguration entfallen.

Für macOS gibt es zusätzlich Gradle-Aufgaben auf Basis von `jpackage`:

* `installDist` für ein lokales Startverzeichnis
* `packageMacOsAppImage` für ein App-Image
* `packageMacOsDmg` für ein DMG-Installationspaket
* `macosInstall` als kompatibler Alias für den DMG-Bau ausschließlich im Workspace; Installation und externe Verzeichnislinks sind bewusst ausgeschlossen

### Paketstruktur

* `de.schrell.cadas`
  Einstiegspunkte der Anwendung.
* `de.schrell.cadas.ui`
  JavaFX-Workbench, Umschaltung zwischen 2D- und 3D-Arbeitsbereich sowie Interaktion mit der Zeichenfläche.
* `de.schrell.cadas.ui.AutomationBridgeServer`
  Lokaler HTTP-Zugriff für direkte Tests gegen eine laufende JavaFX-Anwendung.
* `de.schrell.cadas.application.history`
  Allgemeine Rückgängig-/Wiederherstellen-Verwaltung auf Snapshot-Basis.
* `de.schrell.cadas.application.drawing`
  Anwendungslogik für orthogonales Zeichnen, manuelle Längen- und Winkeleingabe, Snap-Verhalten, Öffnungsplatzierung und Bearbeitung verbundener Wand-Endpunkte.
* `de.schrell.cadas.application.exchange`
  Formatunabhängige Schnittstellen für Import und Export.
* `de.schrell.cadas.application.parts`
  Interne Standard-Teilebibliothek sowie Import zusätzlicher Presets für Türen, Fenster und Treppen.
* `de.schrell.cadas.application.objects`
  Standard-Presets sowie referenzierte DWG- und dreidimensionale DXF-Presets für Raum- und Außenobjekte.
* `de.schrell.cadas.application.dwg`
  DWG-Analyse über externe Konverter sowie direkte Auswertung von ACIS-v1-`3DSOLID`-Körpern aus DXF-Dateien.
* `de.schrell.cadas.application.reports`
  Fachliche Berichte, Markdown-Erzeugung und HTML-Rendering auf Basis einer Standard-Markdown-Bibliothek.
* `de.schrell.cadas.domain.geometry`
  Geometrische Grundbausteine wie Längen, Winkel, Raster, Punkte und Segmente.
* `de.schrell.cadas.domain.model`
  Fachliches Projektmodell für Etagen, Räume, Wände, Türen, Fenster, Treppen und Raumobjekte.
* `de.schrell.cadas.infrastructure.dxf`
  Konkreter Adapter für ASCII-DXF-Import und -Export.
* `de.schrell.cadas.application.layers`
  Belags-Presets, Kachelbelegung, Schichtwirkungen auf Raumgeometrie und Konsistenz zusätzlicher Oberflächen-Ebenen.
* `de.schrell.cadas.application.view`
  Ableitung renderbarer 3D-Modelle, Kamerasteuerung, Innenansicht und Belags-Ausschnitte für Wandöffnungen.

## Verantwortlichkeiten

### UI

Die Klasse `CadWorkbench` kapselt die aktuelle Workbench. Sie stellt bereit:

* Menüleiste mit Datei-, Bearbeitungs-, Ansichts- und Werkzeugaktionen
* kontextabhängige Eigenschaftenleiste als dauerhaft sichtbare vertikale Property-Spalte
* pann- und zoombare Zeichenfläche
* Rasterdarstellung
* Hilfslinien aus Linealen
* magnetisches Snap auf Raster und Endpunkte
* feste `Oben`- und `Unten`-Ansicht plus relative Pfeilrotationen für die übrigen 2D-Orthogonalansichten
* optionale Himmelsrichtung
* Live-Anzeige von Länge und Winkel
* ein- und ausblendbare Bemaßung für Wände
* Werkzeugmodus für Wände, Treppen, Türen, Fenster, Objekte und Bearbeitung; Räume werden automatisch abgeleitet und im Bearbeiten-Werkzeug ausgewählt
* umschaltbarer Mittelbereich für 2D-Zeichenfläche oder 3D-Ansicht
* 3D-Ansicht mit Orbit, Zoom, Pan und Auswahlrückkopplung
* Mehrfachauswahl mit Eigenschaftenübernahme auf mehrere passende Bauteile
* Kontextmenü für Auswahlaktionen und 90°-Drehung rotierbarer Bauteile
* Rückgängig und Wiederherstellen für fachliche Bearbeitungsschritte
* Gebäude-DXF als Standard sowie Etagen-DXF als Zusatzfunktion
* Standardteil-Presets für Türen, Fenster und Treppen
* erste Treppenplatzierung für gerade, 180°-, gegenläufige und Wendeltreppen
* erste Raumobjekte mit Standard-Presets und gemeinsamem Sichtbarkeitsschalter
* Flächen- und Volumenanzeige für Räume
* Ebenenverwaltung für ausgewählte Wand- und Raumflächen mit Presets, Reihenfolge, Sichtbarkeit, DWG-Referenzen und konkreten DWG-Block-Presets
* Materiallistenfenster mit gerendertem Markdown, Druck und Markdown-Export
* Raum-Kontextaktion zum Öffnen der Innenansicht am angeklickten 2D-Standort

### Anwendungslogik

`DraftingService` erzwingt je nach Eingabemodus orthogonales Zeichnen oder übernimmt manuelle Längen- und Winkelvorgaben. `SnapService` entscheidet, ob auf bestehende Endpunkte oder auf das Raster eingerastet wird. `OpeningPlacementService` bindet Türen und Fenster an bestehende Wände. `WallEditingService` verschiebt verknüpfte Wand-Endpunkte gemeinsam. `SelectionTranslationService` verschiebt ausgewählte Wände, Treppen und Raumobjekte parallel als Gruppe. `OrthogonalCorrectionService` richtet Wandachsen und Objektwinkel bis 10° Abweichung rastergebunden auf das nächste Vielfache von 90° aus und hält gemeinsame Wandenden verbunden. `RoofSlopeWallService` findet zur ausgewählten Wand die angrenzende Raum-Innenkante, setzt die Eckhöhen, teilt beide Seitenwände an der oberen Schrägenkante und bindet Öffnungen an das zutreffende Teilsegment um. Räume speichern mehrere unabhängige Schrägenprofile; `AutoRoomGenerationService` erhält diese bei erneuter Raumableitung. `QuarterTurnRotationService` kapselt die 90°-Drehung rotierbarer Bauteile testbar außerhalb der UI. `SurfaceLayerEffectService` berechnet die Wirkung sichtbarer Wand-, Boden- und Deckenlagen auf Innenkontur, lichte Höhe und Volumen. `WallSurfaceOpeningService` bildet aus Türen und Fenstern maximale sichtbare Wandbelags-Rechtecke sowie Draufsicht-Intervalle. `RoomObjectPresetService` liefert interne Objekt-Presets und referenzierte `DWG`-Objekte aus `~/.config/CADas/Objekte`. `SurfaceMaterialListService` erzeugt die Belags-Materialliste mit Reststückoptimierung, Schnittbeschränkungen und Komplexitätswerten; `MarkdownHtmlRenderer` rendert den erzeugten Markdown über `commonmark`. `ConstructionDrawingPdfService` erzeugt je Etage einen eigenen Grundriss und fasst alle Etagen in gemeinsamen 3D- und Seitenansichten zusammen. Er begrenzt den planabhängigen Tiefenversatz räumlicher PDF-Ansichten relativ zur kleinsten Geschosshöhe, damit dieselbe Etage nicht als mehrere Geschosse erscheint. `ThreeDSceneModelBuilder` leitet aus denselben Domänenobjekten einen renderbaren 3D-Szenengraphen ab, `ThreeDCameraController` kapselt Orbit-, Pan-, Zoom- und Projektionswechsel der 3D-Ansicht, und `ThreeDInteriorViewService` berechnet die raumgebundene Innenkamera.

`SelectionQueryService` kapselt die fachliche Auswahlauflösung unter dem Cursor. Dadurch liegt die Priorisierung von Öffnungen, Treppen, Wänden, Raumobjekten und Räumen nicht mehr direkt in der JavaFX-Workbench. Die Workbench kann bei überdeckten Treffern über `Alt` + Linksklick durch die Trefferliste schalten.

`UndoRedoStack` kapselt den generischen Verlauf. In der Workbench werden darüber komplette Projektsnapshots einschließlich Hilfslinien, aktiver Etage und Auswahlzustand verwaltet.

`MarkdownNavigationService` extrahiert die Kapitelstruktur der eingebetteten Markdown-Dokumente. `MarkdownHtmlRenderer` versieht dieselben Überschriften mit stabilen Sprungmarken; die JavaFX-Hilfe kombiniert daraus Inhaltsverzeichnis, Volltextsuche und Druckfunktion.

`AutomationBridgeServer` bindet lokal auf `127.0.0.1:17845` und reicht Werkzeuge, Felder, Canvas-Aktionen sowie Import-/Export-Kommandos kontrolliert an die Workbench weiter. Dadurch lassen sich End-to-End-Tests der laufenden Desktop-Anwendung auch ohne manuelle Bedienung durchführen.

### Domäne

`Length` speichert Maßangaben in Millimetern auf Basis von `BigDecimal`, um Einheiten konsistent zu halten. `ProjectModel`, `Level`, `Wall`, `Room`, `Door`, `WindowElement`, `Staircase`, `RoomObject`, `Roof`, `Terrain` und `SurfaceLayerStack` bilden den aktuellen Grundrisskern ab. `WallProfilePoint` erweitert lineare Wandoberkanten zu stückweise linearen Polygonprofilen; Wandbearbeitung, DXF-Persistenz sowie 3D- und Seitenableitung erhalten diese Profile. `TerrainCornerService` leitet die äußeren Gebäudeecken aus der konvexen Hülle der untersten Etage ab und erhält bereits erfasste Geländehöhen. Etagen lassen sich bereits dynamisch anlegen und getrennt voneinander bearbeiten.

`StairUnderbuildService` leitet aus einer Treppe deterministische linke und rechte Unterbauwand-IDs ab. Die erzeugten Wände folgen der Treppensteigung als Polygonprofil und bleiben dadurch normale Hosts für Türen und Fenster. Beim Deaktivieren einer Seite werden nur deren Wand und gebundene Öffnungen entfernt. Die planare Untersicht wird im `ThreeDSceneModelBuilder` als eigener schräger Volumenkörper aus ihrer konfigurierten Dicke abgeleitet.

## Dach- und Ebenenmodell

Der aktuelle Domänenstand enthält bereits die fachlichen Grundlagen für die nächsten Ausbaustufen:

* `Roof` modelliert das erste Satteldach mit Winkel, Überstand und Dachrinne.
* `SurfaceLayer` und `SurfaceLayerStack` modellieren zusätzliche Aufbau-Ebenen auf Flächen.
* `TileLayoutService` berechnet rechteckige Kachel- beziehungsweise Plattenbelegungen mit Versatz und Mindestversatz.

Diese Teile sind inzwischen nicht nur im Modell abgesichert, sondern auch in der Workbench sichtbar:

* Ebenen lassen sich auf ausgewählten Wand- und Raumflächen anlegen, umbenennen, ein- und ausblenden, umsortieren und mit Presets vorbelegen.
* Sichtbare Wand-Innenlagen verschieben die automatisch abgeleitete Raum-Innenkante.
* Sichtbare Innen- und Außenwand-Beläge werden in der 2D-Grundrissansicht als Belagsstreifen, in Seitenansichten als Raster und in 3D mit Fugen auf der Wandoberfläche gerendert.
* Türen und Fenster schneiden Wandbeläge aus. Die sichtbaren Belagsflächen werden als maximale Rechtecke zerlegt; das Kachelraster läuft über virtuelle Rechteckgrenzen hinweg und wird nur an echten Öffnungskanten geklippt.
* Sichtbare Boden- und Deckenlagen verringern die lichte Raumhöhe und beeinflussen Volumen sowie 3D-Ableitung.
* `UserSurfaceCoveringPresetLibrary` speichert eigene Belags-Presets unter `~/.config/CADas/Belag` als `.cadasbelag` und lädt sie beim Start in die Preset-Auswahl.
* Registrierte `DWG`-Dateien werden in dasselbe Belagsverzeichnis übernommen, über externe Konverter analysiert und als auswählbare Referenz-Presets in die Ebenenverwaltung eingehängt.
* `DwgLibraryAnalyzer` kapselt die DWG-Konvertierung über externe Programme wie `dwg2dxf` oder `dwgread`; CADas linkt und bündelt keine DWG-Bibliotheken. `ExternalDwgToDxfConverter` prüft neben `PATH` und `CADAS_DWG_CONVERTER` explizit die üblichen Homebrew- und MacPorts-Verzeichnisse, weil per Finder gestartete App-Bundles keinen Shell-`PATH` erben.
* Aus der konvertierten DXF-Geometrie werden Einheiten, Blockursprünge, Skalierung, Rotation, Layer, Handles, Inserts und echte Blockmaße abgeleitet.
* Über optionale `.blocks`-Katalogdateien, manuelle Eingabe oder die analysierte Blockauswahl lassen sich konkrete DWG-Blöcke als Oberflächen- oder Objekt-Presets registrieren.
* `SurfaceMaterialListService` erzeugt aus den sichtbaren Belägen eine Materialliste mit Fläche, Stückzahl, Materialfläche, Schnitten und Raum-Komplexität. Boden- und Deckenflächen werden über die orthogonale Raumzerlegung bewertet; Wandbeläge nutzen die vorhandenen maximalen Wandrechtecke mit ausgesparten Türen, Fenstern und anstoßenden Innenwänden.
* Reststücke werden materialweit weitergeführt und vor dem Anschnitt eines neuen Werkstücks genutzt. Bei mehreren passenden Reststücken wird das mit dem geringsten Verschnitt bevorzugt.
* `SurfaceCutRestriction` unterscheidet freie Zuschnitte, außen begrenzte Schnittkanten und feste Verlegerichtung mit außen begrenzten Schnittkanten. Diese Information wird gespeichert und in der Materialliste berücksichtigt.
* Die Raum-Komplexität steigt mit dem Schnittanteil und mit kurzen Schnittkanten. Schnittkanten werden getrennt gegen die jeweilige Vollplattenkante in Breite oder Höhe bewertet.

## Dateiformatstrategie

Die konkreten Austauschadapter sind `DxfProjectExchangeService` und `DxfLevelExchangeService`. Sie kapseln den DXF-Import und -Export bewusst hinter `ProjectExchangeService` und `LevelExchangeService`. DWG-Bibliotheken werden nicht direkt als Austauschformat geschrieben, sondern über eine separate externe Konverter-Schicht gelesen und anschließend als CADas-Presets genutzt.

Für den aktuellen Stand gilt:

* Gebäude-DXF ist die Standardfunktion für den Austausch kompletter Modelle mit mehreren Etagen.
* Wände, Räume, Türen, Fenster und Treppen werden sichtbar als DXF-Geometrie exportiert.
* Zielversion für neue produktive DXF-Dateien ist `AutoCAD 2000` mit `$ACADVER = AC1015`.
* Zusätzlich schreibt CADas eine eigene Layer-Spur `CADAS_META`, um fachliche Zusatzinformationen verlustarm wieder einzulesen.
* Neue Exporte schreiben `CADAS_DXF|4` als Metadatenmarker; textuelle Fachfelder werden UTF-8-kodiert, damit Umlaute, `/`, `|` und Leerzeichen im Rundlauf erhalten bleiben. Türen, Fenster und Raumobjekte behalten in dieser Metadatenspur ihre Objekt-IDs; Objektwinkel, positive oder negative Basishöhen sowie Geländehöhen an den äußeren Gebäudeecken werden verlustfrei gespeichert.
* Der Import bleibt zu älteren CADas-DXF-Metadaten ohne Versionsmarker kompatibel und überspringt einzelne beschädigte Metadatensätze sowie Öffnungen ohne gültige Host-Wand, statt den gesamten Import abzubrechen.
* Der Export schreibt aktuell metrische Kopfvariablen über `$INSUNITS = 4` und `$MEASUREMENT = 1`.
* Exportierte Entities werden explizit als Model-Space-Elemente gekennzeichnet und mit eigenen Handles versehen.
* `TABLES` für Layer-, Linientyp-, Textstil- und Block-Record-Grunddaten werden geschrieben.
* `BLOCKS` und `INSERT` werden für wiederverwendbare Tür-, Fenster- und Treppenbausteine vorbereitet und exportiert.
* `OBJECTS` enthält eine kleine Grundstruktur für Dictionaries und Layout-Metadaten.
* Fällt diese Metadaten-Spur weg, importiert der Adapter zumindest einfache Wände und Räume aus der reinen Geometrie.

## Teilebibliotheken

Die Standardteilversorgung besteht aus drei Ebenen:

* interne Standard-Presets für Türen, Fenster und Treppen
* interne Standard-Presets für einfache Raumobjekte
* UI-Auswahllisten, die diese Presets direkt auf Eingabefelder anwenden
* externer Import über `.cadasparts`, damit weitere Bibliotheken ohne Codeänderung ergänzt werden können
* referenzierte `DWG`-Objekte aus `~/.config/CADas/Objekte` als erste objektbezogene Bibliotheksanbindung

## Rendering-Modell

Die 2D-Zeichenfläche arbeitet intern in Millimetern und transformiert diese Weltkoordinaten mit Offset und Zoom auf Bildschirmkoordinaten. Dadurch bleiben Raster, Snap und Bemaßung konsistent, auch wenn die Ansicht verschoben oder skaliert wird.

Die 3D-Ansicht nutzt dieselben Millimeterkoordinaten und leitet daraus Box-Geometrien für Wände, Räume, Öffnungen, Treppen, Raumobjekte, Dachflächen und optionale Oberflächen-Ebenen ab. Importierte ACIS-v1-`3DSOLID`-Objekte werden durch `Dxf3dObjectGeometryReader` entschlüsselt. Der `AcisSatMeshTessellator` löst Flächen, Schleifen, Kanten und Kurven der SAT-Topologie auf und erzeugt daraus skalierbare gefüllte Dreiecksnetze; Quadergrenzen bleiben ausschließlich der Fallback für nicht tessellierbare Fremdgeometrie. Wandbeläge erhalten zusätzlich dünne Fugen-Boxen, damit Fliesen und Platten auf Wänden nicht nur als glatte Schicht erscheinen. Türen und Fenster werden aus diesen Wandbelägen ausgespart; die Fugen werden auf dem vollständigen Wandraster berechnet und gegen die sichtbaren Maximalrechtecke geklippt. Fugen werden im JavaFX-Rendering ohne künstliche Mindestverbreiterung mit ihren skalierten Modellmaßen gezeichnet. Die Darstellung läuft als JavaFX-`SubScene` mit umschaltbarer orthografischer oder perspektivischer Kamera. Sichtbarkeit wird je Geschoss und für Raumobjekte zusätzlich global gesteuert, und die Auswahl ist zwischen 2D- und 3D-Darstellung synchronisiert. Für die Orbit-Steuerung wird das Modell in einer eigenen Orbit-Gruppe um die Modellmitte gedreht; die Kamera selbst übernimmt nur Abstand und Projektion. Die Innenansicht nutzt dasselbe 3D-Fenster, setzt die Kamera auf Augenhöhe in Weltkoordinaten und zeigt nur die Zielebene. Sie kann über das Raum-Kontextmenü der 2D-Ansicht direkt am angeklickten Standort geöffnet werden. Ein Bodenklick versetzt den Standort innerhalb des Raums; ein Türklick wechselt in einen angrenzenden Raum. Die Blickdrehung verändert nur die Orientierung am Kamerastandpunkt; rechte-Maus-Bewegung läuft entlang der Blickrichtung innerhalb der Raumkontur, und Zoom verändert den Sichtwinkel bis 115°, statt die Kamera aus dem Raum zu verschieben. Panning verschiebt die Orbitansicht bewusst entlang der Bildschirmachsen. `Modell einpassen` verändert den Abstand, ohne die aktuelle Blickrichtung zu verlieren.

Zusätzlich gibt es einen Oberflächenmodus: Transparente Raumkörper werden ausgeblendet, während Wände, Beläge, Boden- und Deckenflächen sichtbar bleiben. Öffnungen in Wänden geben dadurch den Blick in den Innenraum frei. Schräge Decken werden mit erhöhter Segmentdichte diskretisiert, damit die Kanten in 3D weniger treppenartig erscheinen.

## Qualitätssicherung

Aktuell abgesichert sind unter anderem:

* Einheitenumrechnung und Formatierung
* orthogonales Zeichnen
* freie Längen- und Winkelvorgaben
* Snap auf Raster und Endpunkte
* Platzierung von Türen und Fenstern auf Wänden
* Verschieben verbundener Wand-Endpunkte
* Flächen- und Volumenberechnung von Räumen
* DXF-Roundtrip für die Grundobjekte des MVP
* DXF-Header, Model-Space-Kennzeichnung, `TABLES`, `BLOCKS`, `INSERT` und `OBJECTS` für bessere AutoCAD-Kompatibilität
* DXF-Metadaten-Versionierung, Sonderzeichen-Rundlauf, stabile Öffnungs-IDs und toleranter Import beschädigter Metadaten
* Dateinamennormalisierung für DXF-Import und -Export
* DWG-Blockkataloge für Oberflächen-Presets
* Standardteil-Bibliothek für Türen, Fenster und Treppen
* Standardobjekt-Bibliothek und `DWG`-Objektreferenzen
* Dach- und Ebenendomäne für weitere Ausbaustufen
* 3D-Geometrieableitung für Wände, Räume, Öffnungen, Treppen, Raumobjekte und Dach
* Ausschnitt von Wandbelägen und Fugen an Türen und Fenstern
* Materialliste mit Reststückwiederverwendung, Schnittbeschränkungen, gerendertem Markdown und Druckpfad
* Kameragrundverhalten für Orbit, Pan, Zoom und Projektionswechsel
* raumgebundene 3D-Innenansicht mit Sichtwinkel-Zoom und begrenzter Vor-/Zurückbewegung
* Kamera-Presets für die sechs orthogonalen Ansichten
* Auswahlpriorisierung für Türen, Fenster, Treppen, Wände und Räume
* 90°-Drehung für Wände, Räume und Treppen
* Rückgängig-/Wiederherstellen-Verhalten des generischen Verlaufs
* Grundverhalten des Projektmodells

Build und Tests laufen über:

```bash
./gradlew test
```

## Erweiterungsstrategie

Die bestehende Struktur ist absichtlich so geschnitten, dass die nächsten Ausbauschritte sauber ergänzt werden können:

* weitere DWG-Konverter und AutoCAD-Elementtypen über dieselbe Adapter-Schicht ergänzen
* produktive DWG-Bibliotheken gegen die Blockgeometrie-, Einheiten- und Preview-Auswertung verifizieren
* komplexere 3D-Geometrie jenseits von Box-Ableitungen ergänzen
* grafische Verwaltungsoberflächen für Dach- und Oberflächen-Ebenen ergänzen

## Plattformstrategie

Die Anwendung wird plattformneutral aufgebaut. Aktive Verifikation erfolgt zunächst auf `macOS`, die Architektur trennt jedoch bereits UI, Anwendungslogik und Domäne so, dass spätere Plattformtests auf `Windows` und `Linux` nicht an vermischten Zuständigkeiten scheitern.
