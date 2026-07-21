# Oberflächen und Beläge

Das Paket verbindet sichtbare Oberflächenlagen mit Raumgeometrie, Verlegeplanung und
Benutzerpresets. `SurfaceLayerConsistencyService` hält Trägerreferenzen gültig,
`SurfaceLayerEffectService` berechnet lichte Konturen und Höhen. Die Tile-Layout-Dienste legen
Platten unter Versatz-, Rand-, Fugen- und Schnittbedingungen aus.

## Persistente und abgeleitete Daten

`SurfaceMaterial` ist der zentrale Materialkatalog der Zeichnung. `SurfaceLayer` referenziert ihn
über eine stabile ID und hält nur die nutzungsspezifischen Verlegewerte. Die Materialwerte werden
zusätzlich an der Nutzung gespeichert, damit ältere DXF-Dateien weiterhin lesbar bleiben.
`SurfaceMaterialUsageService` ersetzt, ergänzt oder entfernt Nutzungen projektweit oder für einen
einzelnen Raum. Einzelne `TilePlacement`-Ergebnisse sind abgeleitet und werden neu berechnet.
`UserSurfaceCoveringPresetLibrary` liest und schreibt benutzerdefinierte Presets; DWG-Kataloge
liefern ergänzende Blockmaße, aber keine verdeckte Geometrieänderung.

## Invarianten

Unsichtbare Lagen beeinflussen Raummaße nicht. Innenwandlagen verschieben die lichte Kontur,
Boden- und Deckenlagen verringern die lichte Höhe. Fugenbreite und Mindeststücke dürfen nicht
negativ sein. Startecke, Rotation und Verlegerichtung müssen in beiden Achsen konsistent bleiben.

## Review

Für jede Änderung gerade und gedrehte Platten, alle vier Startecken, Reststreifen, Öffnungen,
freie Ränder und Schnittbeschränkungen prüfen. Bericht, 2D und 3D müssen dieselbe Lage verwenden.
