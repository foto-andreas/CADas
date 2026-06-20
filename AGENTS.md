Du entwickelst ein einfach zu benutzendes CAD-Programm für Gebäude-Grundrisse. Du bist ein Fullstack-Profi-Java-Entwickler mit Schwerpunkt auf grafischen Oberflächen und CAD-Systemen.

## Technik
* JDK 25
* Gradle 9.5 über den Wrapper
* JavaFX als UI-Technologie
* Fass dich extrem kurz bei deinen Antworten und internen Erklärungen und dem Auflisten deines Denkens im Chat.

## Nicht verhandelbar
* Anforderungen immer mit minimalen, sauberen Änderungen umsetzen.
* Kein Code-Pfusch, keine Quickfixes, saubere Modularisierung, Clean Code.
* Vor Refactorings zuerst ausreichende Testabdeckung herstellen.
* Zielwert für Testabdeckung: 85 % mit fachlich sinnvollen Tests und Randfällen.
* Bevorzugt Unit-Tests mit JUnit 5 Jupiter.
* Wenn sinnvoll, Spring-Libraries nutzen.
* Kommentare, Dokumentation und Commit-Messages auf Deutsch.
* Immer Umlaute benutzen, niemals Umschreibungen wie `ae`.
* Alle Buttons, Einstellungen und vergleichbaren UI-Elemente brauchen ausführliche Tooltips.
* Alle Dateien sind IDE-formatiert und aktuell.

## Arbeitsregeln
* Vorhandenes MCP-Tooling bevorzugen.
* Wenn installiert, Shell-Befehle mit `rtk` ausführen.
* Aktivitäten außerhalb dieses Repositories nur mit Anwender-Freigabe.
* Innerhalb dieses Repositories freie Hand.

## Produktziel
* AutoCAD-kompatible Dateiformate unterstützen.
* AutoCAD-kompatible Teilebibliotheken unterstützen.
* Eine gute Basis aus Standard-Teilen bereitstellen.
* Weitere CAD-Teilebibliotheken importieren, strukturiert verwalten und nutzbar machen.
* Startfokus ist der Gebäude-Grundriss mit mehreren Etagen, Wänden, Türen, Fenstern, Fußböden, Raumhöhen und Dachschrägen.

## Festgelegte Entscheidungen
* Erste grafische Oberfläche auf Basis von JavaFX umsetzen.
* Architektur plattformneutral anlegen, aktive Verifikation zunächst auf macOS durchführen.
* Erstes AutoCAD-kompatibles Austauschformat ist DXF.
* Eine vorhandene `*.dwg`-Datei im Repository muss später zwingend nutzbar werden.
* Erstes fachliches MVP umfasst Etagen, Wände, Türen, Fenster und Bemaßung.
* Treppen, Dächer und zusätzliche Flächen-Ebenen folgen nach dem robusten 2D-Grundrisskern.
* Zuerst eine kleine interne Standardbibliothek für Türen, Fenster und einfache Treppen bereitstellen.
* Externe Teilebibliotheken folgen nach der internen Basisbibliothek.
* Räume werden fachlich aus geschlossenen orthogonalen Wandzügen automatisch aus der Innenkante der Wandkörper abgeleitet; Namen und Raum-Properties hängen am abgeleiteten Raum und werden bei Wandänderungen mitgeführt.
* Türen und Fenster werden im Modell wandgebunden über Offset und Breite gespeichert, damit Wandbearbeitungen diese Öffnungen mitführen können.
* Die erste Dateiformat-Schnittstelle wird als separater DXF-Adapter umgesetzt, damit spätere DWG-Unterstützung ohne Eingriff in die Fachlogik ergänzt werden kann.
* Der erste Treppenumfang basiert auf Presets für gerade Treppen, 180°-Treppen und Wendeltreppen mit platzierbarer Grundfläche.
* Treppen besitzen optionale linke und rechte Unterbauwände mit eigener Wandstärke sowie eine planare schräge Untersicht mit eigener Dicke. Unterbauwände sind normale polygonale Wände mit stabilen IDs, sodass Türen und Fenster regulär daran gebunden werden können.
* Zusätzliche Oberflächen-Ebenen werden zunächst als allgemeine Layer-Stacks mit rechteckiger Kachelbelegung modelliert, bevor dafür eine eigene UI-Verwaltung ergänzt wird.
* Der erste Dachumfang fokussiert das Satteldach als separates Domänenobjekt mit Winkel, Überstand und Dachrinne.
* Dachschrägen und schräge Decken werden primär über Eckhöhen an verbundenen Wand-Endpunkten abgeleitet; daraus entstehen polygonale Raumdecken und schräge Wandoberkanten. Die ältere raumgebundene Rechteck-Schräge bleibt nur als einfacher Fallback für manuelle Rechteckräume erhalten.
* Wände besitzen optional ein stückweise lineares Polygon-Höhenprofil entlang ihrer Achse. DXF-Persistenz, Längenänderungen, 3D-Körper und Seitenansichten erhalten und verwenden sämtliche Profilpunkte.
* Die Dachschrägen-Schnellfunktion startet an der Innenkante einer ausgewählten Wand, übernimmt Sockelhöhe und horizontale Breite, senkt die gewählte Wand ab, profiliert die angrenzenden Seitenwände polygonal und aktualisiert die Decke des erkannten Raums.
* Sichtbare Wand-Innenbeläge verschieben die effektive Raum-Innenkante bereits heute; weitere Wandebenen und komplexere Mehrschichtregeln müssen darauf fachlich konsistent aufbauen.
* Externe Teilebibliotheken werden im ersten Schritt über das textbasierte Format `.cadasparts` importiert und in die bestehende Preset-Verwaltung integriert.
* Die erste 3D-Visualisierung wird als JavaFX-`SubScene` mit gemeinsamem Auswahlmodell zwischen 2D und 3D sowie aus dem Fachmodell abgeleiteten Volumenkörpern umgesetzt.
* Die JavaFX-Anwendung wird modular mit `module-info.java` und `mainModule` betrieben, damit Launcher, Distribution und zukünftige Paketierung sauber zusammenpassen.
* Für macOS wird die erste Installationspaket-Erzeugung über `jpackage` aus dem Gradle-Build heraus bereitgestellt.
* Datei-, Projekt- und Bearbeitungsaktionen werden primär über Menü und Tastaturkürzel angeboten; die Werkzeugleiste bleibt auf den schnellen Kernzugriff reduziert.
* Eingabewerte und Bauteil-Properties werden in einer dauerhaft sichtbaren vertikalen Eigenschaftenleiste kontextabhängig nach Werkzeug oder Auswahl eingeblendet.
* Gebäude-DXF ist die Standardfunktion für Import und Export; Etagen-DXF bleibt als Zusatzoption erhalten.
* Für lokale App-Tests wird ein optionaler HTTP-Automatisierungszugriff auf `127.0.0.1:17845` bereitgestellt, startbar über `runMitAutomatisierung`.
* Die aktuelle DXF-Basis schreibt metrische Header-Werte mit `$INSUNITS = 4`, `$MEASUREMENT = 1`, eigene Handles, `TABLES`, `BLOCKS`, `INSERT`-Referenzen sowie eine einfache `OBJECTS`- und Layout-Grundstruktur.
* Rotierbare Bauteile werden zunächst über testbare 90°-Drehung für Wände, Räume und Treppen unterstützt.
* Die 2D-Auswahl lässt sich mit den Cursortasten um genau eine Rasterweite verschieben. Die Kontextaktion `90°-Korrektur` richtet ausgewählte Wände und Objektwinkel bei höchstens 10° Abweichung orthogonal aus, berücksichtigt das Raster und hält gemeinsame Wandenden verbunden.
* Die 2D- und 3D-Workbench teilen sich einen gemeinsamen Mittelbereich; zwischen beiden wird explizit umgeschaltet, damit die aktive Arbeitsansicht maximalen Platz erhält.
* In 2D bleiben `Oben` und `Unten` feste Ansichten, während die Pfeile `links`, `rechts`, `oben` und `unten` das Modell relativ zur aktuellen Sicht kippen.
* Die 3D-Ansicht dreht immer um die Modellmitte, nutzt kamerabezogenes Panning und startet standardmäßig in einer orthografischen räumlichen Kontrollansicht.
* Oberflächen-Ebenen können wahlweise als gestapelte 3D-Schichten oder im Oberflächenrendering ohne transparente Raumkörper visualisiert werden.
* Registrierte `DWG`-Bibliotheken können zusätzlich konkrete Blocknamen als Oberflächen-Presets führen, entweder über begleitende `.blocks`-Katalogdateien oder über manuelle Eingabe in der Workbench.
* Dreidimensionale DXF-Objekte mit ACIS-v1-`3DSOLID`-Körpern werden als skalierbare Objekt-Presets importiert, innen oder außen platziert und aus der entschlüsselten SAT-Topologie als gefüllte Dreiecksnetze dargestellt. Quadergrenzen sind ausschließlich der Fallback für nicht tessellierbare Fremdgeometrie.
* Jedes Raum- und Außenobjekt besitzt eine positive oder negative Basishöhe relativ zum Boden seiner Etage; Persistenz, Eigenschaftenleiste, Seitenansichten und 3D-Rendering berücksichtigen sie.
* Das Hanggelände wird über Höhen an den äußeren Gebäudeecken relativ zum Boden der untersten Etage modelliert. Die 3D-Ansicht zeigt es hellbraun als Fläche, orthogonale Seitenansichten zeigen seine Außenkante.
* Die installierte macOS-App erkennt LibreDWG unabhängig vom Finder-`PATH` in den üblichen Homebrew- und MacPorts-Verzeichnissen; `CADAS_DWG_CONVERTER` bleibt der explizite Vorrang.
* Automatische Bemaßungen liegen vollständig außerhalb des Gebäudes. Die Platzierungsseite folgt dem nächsten Schnitt der Wandnormalen mit der Außenkante einer Außenwand; kleinere Maße werden zuerst und größere Maße mit wachsendem Außenabstand angeordnet. Jede gesetzte Bemaßung ergänzt Sperrflächen gegen Überdeckungen. Fachlich identische Raummaße aus parallelen Wänden erscheinen nur einmal. Diese Regeln gelten identisch für die 2D-Ansicht und die PDF-Bauzeichnung.
* Räumliche PDF-Bauzeichnungsansichten begrenzen den planabhängigen Tiefenversatz relativ zur kleinsten Geschosshöhe, damit langgestreckte Etagen bei diagonalen Blickwinkeln nicht optisch als mehrere Geschosse erscheinen.
* PDF-Bauzeichnungen enthalten jede Etage als eigenen 2D-Grundriss. Die gemeinsamen 3D- und orthogonalen Seitenansichten enthalten immer alle Etagen des Gebäudes.
* Das Raum-Kontextmenü der 2D-Ansicht öffnet die Innenansicht am angeklickten Standort. Bodenklicks in der Innenansicht versetzen den Standort auf den gewählten Punkt und wechseln durch Türen oder Wände in den Nachbarräum.
* Automatisch abgeleitete Räume schließen auch über aneinandergereihte Teilwände; Anschlusslücken bis 10 mm werden für die Topologie als gemeinsamer Wandknoten behandelt, ohne die gezeichneten Wände zu verändern.
* Etagen lassen sich umbenennen und in der Gebäude-Reihenfolge umsortieren („Etage hoch" = größerer Index = im Gebäude nach oben).
* Gebäude-Dateien nutzen die Endung `.cadas` (intern DXF-Format). Laden heißt „Laden", Sichern heißt „Sichern" und „Sichern als ...". Etagen laden/sichern analog über „Etage laden"/„Etage sichern"/„Etage sichern als ...". Beim Laden wird der Dateiname als Projektname übernommen und beim Sichern vorbelegt. Beim „Sichern als ..." und „Etage sichern als ..." wird der gewählte Dateiname als Projekt- bzw. Etagenname übernommen.
* `./gradlew macosInstall` baut das App-Bundle inkl. DMG und installiert CADas.app direkt nach /Applications (überschreibt bestehende Version).
* 3D-Wände und Fundamente enden bündig mit der Wandachse; keine Verlängerung über die Endpunkte hinaus.
* Die Menüleiste nutzt `setUseSystemMenuBar(true)` und ist im Fenster unter macOS ausgeblendet (`setManaged(false)`/`setVisible(false)`).
* Die In-App-Hilfe zeigt die vollständige Benutzerdokumentation aus `docs/benutzerdoku.md`; Keymap und Mausbedienung sind separater Menüpunkt.
* Die In-App-Hilfe besitzt ein Inhaltsverzeichnis mit direkten Sprungmarken und eine Vorwärts-/Rückwärtssuche. „Über CADas“ ist zusätzlich zum nativen macOS-Info-Handler sichtbar im Hilfe-Menü erreichbar.
* `./gradlew run` ist deaktiviert; `runApp` startet das App-Bundle, damit macOS den App-Namen „CADas" in der Menüleiste zeigt.

## Empfohlene Umsetzungsreihenfolge
1. Technisches Grundgerüst, Architektur, Tests und Qualitätswerkzeuge.
2. 2D-Zeichenfläche für Grundrisse mit Navigation, Snap und präziser Eingabe.
3. Fachliches Gebäudemodell für Geschosse, Räume, Wände, Öffnungen und Maße.
4. Persistenz sowie Import/Export für AutoCAD-nahe Formate und Teilebibliotheken.
5. Erweiterte Bauteile wie Treppen, Dach und zusätzliche Flächen-Ebenen.
6. Vorbereitung und spätere Ergänzung einer frei drehbaren 3D-Ansicht.

## Muss-Funktionen der Zeichenoberfläche
* Beliebig großer Canvas, der einfach durch Verschieben erreichbar ist.
* Ansichten von allen sechs Seiten, umschaltbar per Cursortasten und Pfeilkreuz.
* Vorbereitung für eine 3D-Ansicht mit beliebigen Winkeln.
* Optionale Himmelsrichtung in der Karte.
* Magnetischer Snap zu anderen Elementen und zu einem konfigurierbaren Raster.
* Randbereiche mit Linealanzeige im Maßstab der Zeichnung.
* Hilfslinien aus den Linealen herausziehen, ausblenden und entfernen können.
* Längen zusätzlich direkt per Eingabe mit Einheit (`mm`, `cm`, `m`) setzen können.
* Winkel zusätzlich direkt per Gradzahl eingeben können.
* Alle wichtigen Zeichenfunktionen bereitstellen.
* Beim Zeichnen von Wohnungen Linien-Enden verbinden, damit verbundene Enden gemeinsam verschiebbar bleiben.
* Beim Linien-Ziehen standardmäßig 90°-Winkel erzwingen; mit `Shift` auf freie Start- und Endpunkte umschalten.
* Während des Zeichnens aktuelle Länge und aktuellen Winkel anzeigen.

## Muss-Funktionen des Modells
* Bemaßungen ein- und ausblendbar machen.
* Flächen- und Volumenmaße ein- und ausblendbar machen.
* Raumhöhen, Wandstärken, Deckenstärken und Fensterhöhen erfassen und nutzen.
* Treppen inklusive Wendeltreppen und 180°-Treppen unterstützen, inklusive Absätzen.
* Höhenunterschiede an Türschwellen konfigurier- und darstellbar machen.
* Dachstuhl und Dach darstellen, einschließlich Satteldach, einheitlichem Winkel, Dachüberstand und Dachrinnen.
* Kleinteilige Ecken und Kanten einfach realisieren können.

## Muss-Funktionen zusätzlicher Ebenen
* Auf allen Flächen des Modells zusätzliche Ebenen anlegen, benennen, umbenennen, löschen, ein- und ausblenden können.
* Jede Ebene besitzt eine konfigurierbare Schichtstärke.
* Ebenen mit rechteckigen Kacheln befüllen können, inklusive Maße, Richtung, automatischem Versatz und Mindestversatz.
* Fachlicher Hintergrund: Fliesen, Dämmplatten, Gipskarton und ähnliche Beläge.
* Reihenfolge der Ebenen je Fläche, Raum, Etage oder gesamtem Modell verändern können.
* Gleiche Ebenen in gleicher Reihenfolge innerhalb eines Bereichs konsistent behandeln.

## Arbeitsunterlagen
* Kompakte Einstiegsorientierung für KI-Assistenten steht in `KI.md`.
* Systemarchitektur und Verantwortlichkeiten stehen in `docs/systemdoku.md`.
* Benutzerdokumentation steht in `docs/benutzerdoku.md` (auch als In-App-Hilfe unter `src/main/resources/docs/benutzerdoku.md`).
* Konkrete Arbeitspakete stehen in `TODO.md`.
* Festgelegte Entscheidungen und ihr aktueller Stand stehen in `FRAGEN.md`.
