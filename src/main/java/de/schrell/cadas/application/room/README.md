# Raumerkennung

`AutoRoomGenerationService` leitet aus dem Wandnetz geschlossene Innenkonturen ab und gleicht sie
mit bestehenden Räumen ab. Dabei bleiben benutzergepflegte Namen, Höhen, Heizlasten und
Dachschrägen erhalten. `OrthogonalPolygonDecompositionService` zerlegt orthogonale Konturen in
überlappungsfreie Rechtecke für Belags- und Berichtsalgorithmen.

## Zu prüfende Eigenschaften

* Nur tatsächlich geschlossene Wandzüge erzeugen Räume.
* Gemeinsame Innenwände dürfen keine doppelten oder verschmolzenen Konturen erzeugen.
* Kleine Koordinatentoleranzen schließen technische Lücken, dürfen aber getrennte Wände nicht
  verbinden.
* Konkave L- und U-Formen müssen dieselbe Gesamtfläche vor und nach der Zerlegung besitzen.
* Eine Neuberechnung darf fachliche Raumdaten nur verlieren, wenn der Raum wirklich entfällt.
