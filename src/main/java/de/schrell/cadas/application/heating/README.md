# Flächenheizung

## Datenfluss

`HydronicHeatingLayoutService` zerlegt eine nutzbare Raumfläche unter Berücksichtigung von
Sperrflächen und Wandabstand in Heizkreise. `HeatingCircuitRoutingService` erzeugt oder richtet die
Rohrführung aus. `HeatingCircuitCommandRouter` interpretiert die kompakte manuelle Routing-Sprache.
`HydronicHeatingLayoutSvgRenderer` erzeugt die vektorielle Berichtsdarstellung, während
`RoomHeatingOutputService` Leistungen nach Heizart summiert.

## Fachregeln

* Vorlauf und Rücklauf müssen an gültigen Randpunkten beginnen und dürfen die Heizfläche nicht
  unkontrolliert verlassen.
* Ein Kreis darf die konfigurierte maximale Rohrlänge nicht überschreiten.
* Meander und Schnecke müssen innerhalb frei polygonaler Flächen und außerhalb aller Sperrflächen
  bleiben.
* Spiegelung, Rollenwechsel und neuer Startpunkt müssen Kommandosprache und gerenderte Geometrie
  gemeinsam ändern.
* Boden- und Deckenheizung teilen Algorithmen, bleiben aber über `HeatingSurfacePosition` getrennt.

## Review

Besonders kritisch sind konkave Räume, sehr schmale Restflächen, mehrere Sperrflächen, unroutebare
Anschlüsse und Rundungsfehler am Raster. Leistungswerte müssen endlich und nicht negativ sein.
