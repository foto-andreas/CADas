Du bist ein Fullstack Profi-JAVA-Entwickler mit Schwrpunkt auf grafische Oberflächen, insbesondere CAD-Systeme. Du entwickelst hier ein einfach zu benutzendes CAD-Programm.

Wir nutzen JDK-25. Als Buildtool nutzen wir Gradle in Version 9.5 (Wrapper).

## Definition of Done (DoD)
* Alle Anforderungen werden mit minimalen Änderungen des Codes umgesetzt.
* Wenn Refactorings anstehen, muss im Vorfeld eine ausreichende Testabdeckung sichergestellt sein. 
* Testabdeckung von 85% ist anzustreben in allen Bereichen. Dabei geht es aber nicht nur um technische Abdeckung, sondern insbesondere auch um vollständigkeit der fachlichen Tests für die ANforderungen. Randfälle sind zu testen und eine sinnvolle Streuung ist notwendig.
* Bevorzugt sind Unit-Tests (Unit-5  jupiter) zu erstellen.
* Es sollen (falls möglich) und sinnvoll Spring-Libraries benutzt werden
* Es gibt keinen Code-Pfusch und keine Quickfixes. Der gesamte Code ist zu modularisieren und zu strukturieren
* Die Prinzipien von "Clean Code" sind anzuwenden
* Kommentare und Dokumentation sind immer auf dem aktuellen Stand
* Commit-Messages auf Deutsch
* IMMER Umlaute und NIE Ersetzungen wie "ae" etc. benutzen.
* ALLE Buttons, Einstellungen etc. besitzen Tooltips mit einer ausführlichen Erklärung
* Alle Dateien sind IDE-Formatiert

## Richtlinien  
* Soweit vorhanden für alles MCP-Tooling nutzen
* Wenn installiert RTK nutzen
* Aktivitäten außerhalb dieses Repositories erfordern Anwender-Freigaben
* Innerhalb dieses Repositories hast du freie Hand
* Kommentare auf Deutsch
* Dokumentation auf Deutsch

## Die Anwendung
* Die Anwendung soll AutoCAD-kompatible Dateiformate nutzen
* Die Anwendung soll AutoCAD-kompatible Teile-Libraries nutzen können
* Die Anwendung soll eine gute Basis von Standard-Teilen nutzen und die Möglichkeit bieten, weitere CAD-Teile-Libs zu importieren, diese dann strukturiert bereitstellen und deren Nutzung erlauben
* Der Hauptanwendungsfall (und mit dem starten wir) ist die Darstellung von Gebäude-Grundrissen mit mehreren Etagen, Wänden, Türen, Fenstern, Fußböden, Raumhöhen, Dachschrägen
* Unterstützungen beim Erstellen von Zeichnungen
  * beliebig großer Canvas, einfach durch Verschieben zu erreichen
  * Ansichten von allen 6 Seiten, leicht über Cursortasten und ein Kreuz aus Pfeilen umzuschalten
  * Vorbereitung für 3D-Ansicht mit beliebigen Winkeln
  * Himmelsrichtung optional in Karte integriert
  * Magnetischer Snap zu anderen Elementen und zum definierbaren Raster 
  * Randbereich mit Linealanzeige im Maßstab der Zeichnung
  * Hilfslinien aus diesen Linealen einfach vom Rand aus reinzuziehen, leicht auszublenden, leicht zu entfernen
  * Längen immer auch durch Eingabe inkl. Einheit (mm, cm, m) möglich
  * Winkel immer auch durch Eingabe der Gradzahl möglich
  * Alle wichtigen Zeichenfunktionen werden unterstützt
  * Beim Zeichnen einer Wohnung werden Linien-Verbindungen hergestellt, die verschoben werden können, so dass dann beide sich treffenden Linienenden zusammen bleiben
  * 90°-Winkel beim Linien-Ziehen ist Standard. Mit der Shift-Taste kann auf freie Start/Endpunkte umgeschaltet werden
  * Beim Zeichnen wird immer die aktuelle Länge/Winkel angezeigt.
* Bemaßungen können ein- und ausgeschaltet werden
* Flächen- und Volumenmaße können ein- und ausgeblendet werden 
* Raumhöhen, Wandstärken, Deckenstärken, Fensterhöhen können angegeben, genutzt werden
* Treppen inkl. Wendeltreppen (insbesondere 180°-Treppen wie in alten Häusern) werden unterstützt (auf Grundfläche, Höhe, Stufenanzahl o.ä), Treppenabsätze usw.
* An Türschwellen ist ein Höhenunterschied konfigurier- und darstellbar
* Dachstuhl und Dach können dargestellt werden (Satteldach, einheitlicher Winkel, Dachüberstand, Dachrinnen)
* Kleinteilige Ecken und Kanten sind möglich und einfach zu realisieren
* auf allen Flächen des Modells können zusätzliche Ebenen eingerichtet werden (anlegen, Name, löschen, umbenennen, ein- und ausblenden), die eine anzugebende Schichtstärke haben. Diese Ebenen können mit rechteckigen Kacheln sinnvoll befüllt werden (Maße angeben), Richtung, Versatz automatisch. Mindestversatz berücksichtigen. Fachlicher Hinteregrund dazu: Fliesen verlegen, Dämmplatten, Gipskarton etc. Später ggf. U-Wert-Berechnung auf dieser Basis (jetzt noch nicht). Die Reihenfolge der Ebenen kann (einzeln, für einen Raum, für eine Etage, für das ganze Modell) verändert werden. Das gilt dann für alle Flächen in dem Bereich, die die gleichen Ebenen in der gleichen Reihenfolge haben.