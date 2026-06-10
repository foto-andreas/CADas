# TODOs

Die folgende Liste kann während der Agenten-Tätigkeit angepasst und erweitert werden. Also nichts entfernen, von dem du denkst, dass es aus Versehen reingekommen ist. Tausch bei abgearbeiteten Punkten das `*` durch ein Check-Symbol aus.

## Phase 0: Projektbasis und Leitplanken
✓ Gradle-Projekt mit Wrapper 9.5, JDK 25 und sauberer Modulstruktur anlegen.
✓ JavaFX-Projektgrundlage für das CAD-Programm anlegen und bootstrapen.
✓ Testbasis mit JUnit 5 Jupiter, Coverage-Messung und klarer Teststruktur einrichten.
✓ Formatierungs-, Qualitäts- und Build-Konventionen technisch absichern.
✓ Zielarchitektur für `Domäne`, `Anwendung`, `UI` und `Infrastruktur` dokumentieren.
✓ Plattformneutrale Architektur mit aktiver Verifikation zunächst auf macOS festziehen.

## Phase 1: CAD-Kern und Gebäudemodell
✓ Geometrische Grundlagen definieren: Punkte, Vektoren, Strecken, Winkel, Einheiten, Bounding-Boxen.
✓ Einheitensystem für `mm`, `cm` und `m` mit verlustarmer interner Repräsentation festlegen.
✓ Kernmodell für Projekt, Geschoss, Raum, Wand, Tür, Fenster, Boden und Decke aufbauen.
✓ Höhen- und Dickenangaben für Räume, Wände, Decken, Fenster und Türschwellen modellieren.
* Modellregeln für verbundene Linien-Enden und konsistente Topologie definieren.

## Phase 2: 2D-Zeichenoberfläche als erstes MVP
✓ Unendlichen Canvas mit Pan und Zoom umsetzen.
✓ Maßstäbliche Lineale an den Rändern umsetzen.
✓ Hilfslinien aus Linealen herausziehbar, ausblendbar und entfernbar machen.
✓ Magnetischen Snap zu Raster und vorhandenen Elementen umsetzen.
✓ Standard-Zeichenmodus mit 90°-Zwang und `Shift`-Umschaltung auf freie Punkte umsetzen.
✓ Live-Anzeige für Länge und Winkel während des Zeichnens einbauen.
✓ Direkte numerische Eingabe für Länge mit Einheit und Winkel in Grad unterstützen.
✓ Grundlegende Bearbeitung für Auswahl, Verschieben und Anpassen verbundener Geometrie umsetzen.
✓ Umschaltung zwischen den sechs orthogonalen Ansichten vorbereiten oder umsetzen.
✓ Optionale Himmelsrichtung in der Zeichenfläche unterstützen.

## Phase 3: Fachliche Funktionen für Grundrisse
✓ Erstes fachliches MVP auf Etagen, Wände, Türen, Fenster und Bemaßungen begrenzen und dafür robust abschließen.
✓ Bemaßungen ein- und ausblendbar machen.
✓ Flächen- und Volumenmaße berechnen und ein- und ausblendbar machen.
✓ Mehrere Etagen sauber verwalten und visualisieren.
✓ Wände, Türen und Fenster fachlich korrekt platzieren und bearbeiten können.
* Kleinteilige Ecken und Kanten ohne instabile Geometrie ermöglichen.

## Phase 4: Erweiterte Bauteile
* Erweiterte Bauteile erst nach Abschluss des 2D-Grundriss-MVP angehen.
* Treppenmodell für gerade Treppen, Podeste und Wendeltreppen definieren.
* 180°-Treppen für Altbau-Szenarien gezielt unterstützen.
* Dachstuhl- und Dachmodell für Satteldach, Winkel, Überstand und Dachrinnen definieren.
* Vorbereitung für spätere frei drehbare 3D-Ansichten sauber in der Architektur verankern.

## Phase 5: Ebenen auf Flächen
* Datenmodell für zusätzliche Ebenen auf Flächen definieren.
* CRUD, Sichtbarkeit und Reihenfolge von Ebenen je Fläche, Raum, Etage und Modell unterstützen.
* Schichtstärken pro Ebene verarbeiten.
* Rechteckige Kachelbelegung mit Richtung, automatischem Versatz und Mindestversatz umsetzen.
* Konsistenzregeln für gleiche Ebenenfolgen in gleichen Bereichen definieren.

## Phase 6: Dateiformate und Teilebibliotheken
* DXF als erstes AutoCAD-kompatibles Austauschformat fest einplanen.
* Import/Export für DXF als ersten Format-Meilenstein umsetzen.
* Spätere Nutzung der vorhandenen `Variotherm Vorlage 2024 PARTNER_deutsch.dwg` architektonisch vorbereiten.
* Basis an Standard-Teilen für Türen, Fenster und einfache Treppen definieren und bereitstellen.
* Import zusätzlicher Teilebibliotheken ermöglichen.
* Strukturierte Verwaltung und Nutzung importierter Teilebibliotheken umsetzen.

## Querschnittsthemen
* Für alle UI-Aktionen ausführliche deutsche Tooltips pflegen.
* Kommentare und Dokumentation konsequent auf Deutsch halten.
* Fachliche Randfälle früh als Unit-Tests absichern.
* Refactorings nur mit vorhandener oder gleichzeitig ergänzter Testabdeckung durchführen.
* Entscheidungen, Annahmen und offene Punkte fortlaufend in Markdown dokumentieren.

## Empfohlene erste Umsetzungsschritte
✓ Projekt-Setup, Build und Testbasis anlegen.
✓ 2D-Kern für Linien, Raster, Snap und Längen-/Winkel-Eingabe liefern.
✓ Gebäudemodell für Geschosse, Räume, Wände, Türen, Fenster und Bemaßungen anbinden.
* Danach Dateiformate, Bibliotheken, Treppen, Dach und Flächen-Ebenen ergänzen.
