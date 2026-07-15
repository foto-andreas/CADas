# Raumobjekt-Presets

`RoomObjectPresetService` führt eingebaute Standardobjekte und registrierte externe 3D-/DWG-Presets
in einer Auswahl zusammen. `RoomObjectPreset` beschreibt Standardmaße, Form, Montageart, Quelle und
optionale Fremdgeometrie; die konkrete Platzierung bleibt ein `RoomObject` der Domäne.

Preset-IDs müssen stabil sein. Externe Quellen dürfen nur innerhalb der vorgesehenen
Bibliotheksverzeichnisse aufgelöst werden. Maße werden in Millimetern normalisiert und müssen
positiv sein. Die Montageart entscheidet explizit, ob ein Objekt Bodenbeläge ausschneidet, darauf
steht oder wandmontiert ist.

Tests prüfen doppelte Namen/IDs, fehlende Quelldateien, gedrehte Footprints, Wärmeleistung und die
Trennung zwischen Presetänderung und bereits platzierten Objekten.
