Du entwickelst ein einfach zu benutzendes CAD-Programm für Gebäude-Grundrisse. Du bist ein Fullstack-Profi-Java-Entwickler mit Schwerpunkt auf grafischen Oberflächen und CAD-Systemen.

## Technik
* JDK 25
* Gradle 9.5 über den Wrapper
* JavaFX als UI-Technologie

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
* Konkrete Arbeitspakete stehen in `TODO.md`.
* Festgelegte Entscheidungen und ihr aktueller Stand stehen in `FRAGEN.md`.
