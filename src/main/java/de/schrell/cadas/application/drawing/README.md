# Zeichnen und Bearbeiten

Dieses Paket übersetzt Bedienabsichten in überprüfbare Geometrieänderungen. Es kennt das
Domänenmodell, aber keine JavaFX-Ereignisse oder Pixelkoordinaten.

## Dienstgruppen

* `DraftingService`, `SnapService`, `GuideSnapService` und `WallSnapService` bestimmen neue
  Zeichenpunkte unter Raster-, Winkel- und Hilfslinienbedingungen.
* `OpeningPlacementService` projiziert Türen und Fenster auf eine geeignete Host-Wand und lehnt
  Geometrien außerhalb von Achslänge oder Wandprofil ab.
* `WallEditingService`, `EdgeResizeService`, `SelectionTranslationService`,
  `QuarterTurnRotationService` und `OrthogonalCorrectionService` verändern vorhandene Bauteile.
* `SelectionQueryService` priorisiert überdeckte Treffer; die UI entscheidet nur über den
  gewünschten Trefferindex.
* Die Bemaßungsdienste trennen Fachmaße, Platzierung, Textblockierung und Linienlayout.

## Invarianten

Änderungsdienste liefern neue oder vollständig validierte Domänenobjekte. Gemeinsame Wandenden
müssen gemeinsam bewegt werden. Snap-Toleranzen werden in Weltmillimetern oder explizit
umgerechneten Bildschirmwerten übergeben. Jede geometrische Mutation muss über die UI genau einen
Undo-Snapshot erhalten.

## Review

Randfälle sind Nullsegmente, fast orthogonale Winkel, Kreuzungen auf Segmentenden, überdeckte
Auswahltreffer, negative Verschiebungen und Öffnungen an schrägen Wandprofilen. Tests sollen jeweils
zulässige Grenzwerte und die erste unzulässige Position abdecken.
