# TODOs

Die folgende Liste kann während der Agenten-Tätigkeit angepasst und erweitert werden. Also nichts entfernen, von dem du denkst, dass es aus Versehen reingekommen ist. Für erledigte Punkte `- [x]`, für offene Punkte `- [ ]` verwenden.

## Phase 0: Projektbasis und Leitplanken
- [x] Gradle-Projekt mit Wrapper 9.5, JDK 25 und sauberer Modulstruktur anlegen.
- [x] JavaFX-Projektgrundlage für das CAD-Programm anlegen und bootstrapen.
- [x] Testbasis mit JUnit 5 Jupiter, Coverage-Messung und klarer Teststruktur einrichten.
- [x] Formatierungs-, Qualitäts- und Build-Konventionen technisch absichern.
- [x] Zielarchitektur für `Domäne`, `Anwendung`, `UI` und `Infrastruktur` dokumentieren.
- [x] Plattformneutrale Architektur mit aktiver Verifikation zunächst auf macOS festziehen.

## Phase 1: CAD-Kern und Gebäudemodell
- [x] Geometrische Grundlagen definieren: Punkte, Vektoren, Strecken, Winkel, Einheiten, Bounding-Boxen.
- [x] Einheitensystem für `mm`, `cm` und `m` mit verlustarmer interner Repräsentation festlegen.
- [x] Kernmodell für Projekt, Geschoss, Raum, Wand, Tür, Fenster, Boden und Decke aufbauen.
- [x] Höhen- und Dickenangaben für Räume, Wände, Decken, Fenster und Türschwellen modellieren.
- [x] Modellregeln für verbundene Linien-Enden und konsistente Topologie definieren.

## Phase 2: 2D-Zeichenoberfläche als erstes MVP
- [x] Unendlichen Canvas mit Pan und Zoom umsetzen.
- [x] Maßstäbliche Lineale an den Rändern umsetzen.
- [x] Hilfslinien aus Linealen herausziehbar, ausblendbar und entfernbar machen.
- [x] Magnetischen Snap zu Raster und vorhandenen Elementen umsetzen.
- [x] Standard-Zeichenmodus mit 90°-Zwang und `Shift`-Umschaltung auf freie Punkte umsetzen.
- [x] Live-Anzeige für Länge und Winkel während des Zeichnens einbauen.
- [x] Direkte numerische Eingabe für Länge mit Einheit und Winkel in Grad unterstützen.
- [x] Grundlegende Bearbeitung für Auswahl, Verschieben und Anpassen verbundener Geometrie umsetzen.
- [x] Umschaltung zwischen den sechs orthogonalen Ansichten vorbereiten oder umsetzen.
- [x] Optionale Himmelsrichtung in der Zeichenfläche unterstützen.

## Phase 3: Fachliche Funktionen für Grundrisse
- [x] Erstes fachliches MVP auf Etagen, Wände, Türen, Fenster und Bemaßungen begrenzen und dafür robust abschließen.
- [x] Bemaßungen ein- und ausblendbar machen.
- [x] Flächen- und Volumenmaße berechnen und ein- und ausblendbar machen.
- [x] Mehrere Etagen sauber verwalten und visualisieren.
- [x] Wände, Türen und Fenster fachlich korrekt platzieren und bearbeiten können.
- [x] Kleinteilige Ecken und Kanten ohne instabile Geometrie ermöglichen.

## Phase 4: Erweiterte Bauteile
- [x] Erweiterte Bauteile erst nach Abschluss des 2D-Grundriss-MVP angehen.
- [x] Treppenmodell für gerade Treppen, Podeste und Wendeltreppen definieren.
- [x] 180°-Treppen für Altbau-Szenarien gezielt unterstützen.
- [x] Dachstuhl- und Dachmodell für Satteldach, Winkel, Überstand und Dachrinnen definieren.
- [x] Vorbereitung für spätere frei drehbare 3D-Ansichten sauber in der Architektur verankern.

## Phase 4a: 3D-Visualisierung
- [x] 3D-Szenengraph für Wände, Räume, Türen, Fenster, Treppen und Dach aus dem bestehenden Fachmodell ableiten.
- [x] Projektion zwischen 2D-Grundriss und 3D-Szene fachlich konsistent definieren.
- [x] Erste 3D-Kamera mit Orbit, Zoom und Pan umsetzen.
- [x] Umschaltung zwischen orthografischer und perspektivischer Darstellung ergänzen.
- [x] Geschosse in der 3D-Ansicht ein- und ausblendbar machen.
- [x] Wände mit Wandhöhe, Wandstärke und Öffnungen als echte Volumenkörper darstellen.
- [x] Räume mit Boden- und Deckenstärken räumlich darstellen.
- [x] Türen, Fenster und Treppen in 3D aus den bestehenden Domänenobjekten ableiten.
- [x] Dachgeometrie für das erste Satteldach visualisieren.
- [x] Zusätzliche Flächen-Ebenen optional als gestapelte Schichten visualisieren.
- [x] Einfache Material- und Farbzuordnung für Bauteilarten definieren.
- [x] Auswahl und Hervorhebung von 3D-Objekten vorbereiten.
- [x] Synchronisation zwischen 2D-Auswahl und 3D-Auswahl vorbereiten.
- [x] Performance-Basis für größere Grundrisse durch gruppiertes Rendering und Caching absichern.
- [x] Technische Tests für Geometrieableitung und Kameragrundverhalten ergänzen.

## Phase 5: Ebenen auf Flächen
- [x] Datenmodell für zusätzliche Ebenen auf Flächen definieren.
- [x] CRUD, Sichtbarkeit und Reihenfolge von Ebenen je Fläche, Raum, Etage und Modell unterstützen.
- [x] Schichtstärken pro Ebene verarbeiten.
- [x] Rechteckige Kachelbelegung mit Richtung, automatischem Versatz und Mindestversatz umsetzen.
- [x] Konsistenzregeln für gleiche Ebenenfolgen in gleichen Bereichen definieren.

## Phase 6: Dateiformate und Teilebibliotheken
- [x] DXF als erstes AutoCAD-kompatibles Austauschformat fest einplanen.
- [x] Import/Export für DXF als ersten Format-Meilenstein umsetzen.
- [x] Spätere Nutzung der vorhandenen `Variotherm Vorlage 2024 PARTNER_deutsch.dwg` architektonisch vorbereiten.
- [x] Basis an Standard-Teilen für Türen, Fenster und einfache Treppen definieren und bereitstellen.
- [x] Import zusätzlicher Teilebibliotheken ermöglichen.
- [x] Strukturierte Verwaltung und Nutzung importierter Teilebibliotheken umsetzen.

## Querschnittsthemen (laufend)
- [ ] Für alle UI-Aktionen ausführliche deutsche Tooltips pflegen.
- [ ] Kommentare und Dokumentation konsequent auf Deutsch halten.
- [ ] Fachliche Randfälle früh als Unit-Tests absichern.
- [ ] Refactorings nur mit vorhandener oder gleichzeitig ergänzter Testabdeckung durchführen.
- [ ] Entscheidungen, Annahmen und offene Punkte fortlaufend in Markdown dokumentieren.

## Empfohlene erste Umsetzungsschritte
- [x] Projekt-Setup, Build und Testbasis anlegen.
- [x] 2D-Kern für Linien, Raster, Snap und Längen-/Winkel-Eingabe liefern.
- [x] Gebäudemodell für Geschosse, Räume, Wände, Türen, Fenster und Bemaßungen anbinden.
- [x] Danach Dateiformate, Bibliotheken, Treppen, Dach und Flächen-Ebenen ergänzen.

## Sonstiges
- [x] GitHub-README.md erstellen mit wichtichen Informationen

## Nächste Aktionen
- [ ] Undo und Restore implementieren
- [ ] Die Ansichten lassen sich noch nicht umschalten
- [ ] Elemente auswählen, abwählen, mehrere selektieren und deren Einstellungen ändern
- [ ] Treppe die ohne Absatz in einem rechteckigen Bereich untergebracht ist, am Anfang und am Ende in der gleichen Linie endet. Frag nach, wenn du nciht genau weißt, was gemeint ist.
- [ ] Aufbau der normalen Treppe passt nicht, Podest muss von einem Teil der Treppe erreichbar sein und der andere Teil vom Podest wegführen. Aber die Stufenbereiche liegen nebeneinander.
- [ ] 3D-Ansicht ist leer
- [ ] DWG-Dateien könne nicht als Teilebibliothek ausgewählt werden
- [ ] Projekt leeren, Nachfrage-Dialog davor
- [ ] Menü ergänzen und sinnvoll füllen, Key-Shortcuts passend ergänzen und implementieren
- [ ] Oben/Unten/Nord/Süd/Ost/West durch Pfeile nach oben, unten, links, rechts ersetzen und dann die Ansicht entsprechend damit kippen.
- [ ] Nord-Einstellung bei Ansicht von Oben konfigurierbar machen und dann den Kompass immer passend gedreht anzeigen. Damit soll angezeigt werden, in welcher Ausrichtung das Gebäude steht.
- [ ] Beim Ziehen von Hilfslinien soll das jeweils andere Lineal als Basis genommen werden, die aktuelle Position als Länge angezeigt werden und snap genutzt werden können
- [ ] DXF-Extension beim Export wird doppelt angehängt und beim Etagennamen mit angezeigt
- [ ] Baue ein Logo für die Anwendung ein, was in der Task-Leiste eingeblendet wird. Suche oder erstelle ein CAD-Logo
- [ ] Bei der 3D-Ansicht einen "Ansicht zentrieren"-Button ergänzen
- [ ] Drehen der 3D-Ansicht ist unhandlich und unverständlich
- [ ] Durchforste die AutoCAD-Doku und suche nach Dingen, die wichtig sind und hier fehlen. Einfaches einbauen, Konpliziertes in TODO für später ergänzen.
- [ ] Drehen von Bauteilen in den 90°-Winkeln ergänzen
- [ ] Kontextmenü bei Selektion von Bauteilen ergänzen und sinnvolle EInträge ergänzen
- [ ] Kannst du ermöglichen, dass du direkt auf eine zum Testen gestartete APP zugreifen kannst? Wenn es zu JavaFX nichts Passendes gibt: Ggf. einen MCP-Server in der App ergänzen, dem du Maus-Aktionen, Tastatur-Eingaben usw. in Feldern an die App schicken kannst oder das in der Event-Ebene realisierst. Ziel soll sein, dass du Dinge selber testen kannst, dann exportierst und in der Datei nachsehen kannst, ob neue Funktionen funktionieren oder nicht.
- [ ] horizontales Lineal ist rechts nicht lang genug, da ist eine weiße Box.
- [ ] offenbar werden aktuell nur DXFs für einzelne Etagen erstellt/geladen. Das ist eine Option, es soll aber auch das ganze Gebäude ex-/importiert werden. Und das soll der Standard sein.
- [ ] Die bunten Buttons können im Menü untergebracht werden und unten entfallen.
- [ ] Je nach Werkzeugauswahl/Element-Selektion sollen nur die passenden Properties angezeigt werden
- [ ] Die passenden Properties sollen alle in einer immer sichtbaren vertikalen Liste angezeigt werden, nicht aufgeteilt oben und was nicht passt rechts.