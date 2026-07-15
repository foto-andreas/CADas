# Fremdformate und 3D-Objekte

## Verantwortung

CADas linkt keine proprietäre DWG-Bibliothek. `ExternalDwgToDxfConverter` findet einen explizit
konfigurierten oder lokal installierten Konverter, führt ihn mit Zeitgrenze aus und sammelt dessen
Ausgabe parallel. `DwgLibraryAnalyzer` interpretiert das erzeugte DXF und liefert Blöcke, Maße,
Layer, Inserts und Diagnoseinformationen.

Die 3D-Leser importieren DXF/ACIS-SAT, IFC und RFA in das gemeinsame Modell aus `Dxf3dMesh` und
`Dxf3dBounds`. `AcisSatMeshTessellator` löst SAT-Flächen einschließlich konkaver Konturen und
Innenlöcher auf. `Ifc3dObjectGeometryReader` tesselliert extrudierte Profile ebenfalls mit
Innenringen. RFA wird nur über verfügbare externe Konvertierungspfade gelesen.

## Sicherheits- und Robustheitsregeln

* Externe Prozesse erhalten keine Shell-Kommandozeichenfolge, sondern getrennte Argumente.
* Standardausgabe und Fehlerausgabe müssen gleichzeitig geleert und Diagnosegrößen begrenzt werden.
* Zeitüberschreitungen beenden den Prozess und seine Nachkommen.
* Importierte Indizes, Schleifen und Zahlen werden vor Netzerzeugung validiert.
* Nicht tessellierbare Fremdgeometrie darf einen klar gekennzeichneten Bounds-Fallback nutzen, aber
  keine erfundene Detailgeometrie vortäuschen.

## Review

Konverter nicht im `PATH`, Leerzeichen im Dateinamen, sehr große Ausgabe, Timeout, falsche Einheiten,
verschachtelte Inserts, konkave Profile, Innenlöcher und beschädigte SAT-Topologie prüfen.
