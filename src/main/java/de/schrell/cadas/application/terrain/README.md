# Gelände

Die Geländedienste trennen fünf Aufgaben: Gebäudeaußenkontur bestimmen, relevante Gebäudeecken
ermitteln, Eingabepunkte projizieren und bearbeiten, Höhenprofile interpolieren sowie daraus
renderbare Flächen ableiten.

Gespeicherte Geländehöhen beziehen sich auf nachvollziehbare Positionen entlang der Außenkontur.
Eine neue Gebäudeableitung erhält zuordenbare Werte und entfernt nur nicht mehr vorhandene
Referenzen. Symbolische UI-Bänder und Klicktoleranzen gehören nicht in die gespeicherte Domäne.

Reviewer prüfen konvexe und konkave Gebäude, mehrere Etagen, identische Projektionen, Ecken auf
Segmentgrenzen, negative Höhen und Konturänderungen nach Wandbearbeitung.
