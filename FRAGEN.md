# Entscheidungen

Die zuvor offenen Punkte sind entschieden und gelten bis auf Weiteres als verbindlicher Projektstand.

## Festlegungen
* Erstes AutoCAD-kompatibles Austauschformat ist `DXF`.
* Neue produktive DXF-Dateien zielen auf `AutoCAD 2000` / `$ACADVER = AC1015`, metrische Einheiten und CADas-Metadatenmarker `CADAS_DXF|2`.
* Die grafische Oberfläche wird mit `JavaFX` umgesetzt.
* Die Architektur wird plattformneutral angelegt, aktiv verifiziert wird zunächst auf `macOS`.
* Das erste fachliche MVP umfasst Etagen, Wände, Türen, Fenster und Bemaßungen.
* Treppen, Dächer und zusätzliche Flächen-Ebenen folgen nach dem robusten 2D-Grundrisskern.
* Zunächst wird eine kleine interne Standardbibliothek für Türen, Fenster und einfache Treppen bereitgestellt.
* Externe Teilebibliotheken werden danach ergänzt.
* Die vorhandene Datei `Variotherm Vorlage 2024 PARTNER_deutsch.dwg` muss später zwingend nutzbar gemacht werden.

## Auswirkung auf die Umsetzung
* Die Formatarchitektur muss von Anfang an so geschnitten sein, dass `DXF` zuerst geliefert und `DWG` später ergänzbar wird.
* DXF-Metadaten müssen textuelle Fachwerte verlustfrei kodieren und alte CADas-DXF-Dateien ohne Versionsmarker weiter importieren.
* Die UI-Architektur kann direkt auf `JavaFX` und eine Desktop-Anwendung mit Zeichenfläche ausgerichtet werden.
* Das erste MVP wird bewusst fachlich eingegrenzt, damit der 2D-Grundrisskern stabil wird, bevor er um komplexere Bauteile erweitert wird.

## Noch zu klären
* Eine echte 3D-Innenansicht braucht eine fachliche Festlegung für Raumkamera, Clipping der umgebenden Bauteile und Navigation. Bis dahin bleibt die 3D-Ansicht eine Orbit-Kontrollansicht mit sichtbaren Innenwand-Fugen.
