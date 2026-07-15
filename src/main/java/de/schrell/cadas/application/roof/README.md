# Dachlogik

`RoofSlopeWallService` überführt eine gewählte Außenwand in eine fachlich konsistente Dachschräge:
Er bestimmt die zugehörige Raumkante, setzt Wandprofilhöhen, teilt angrenzende Wände und bindet
Öffnungen an das passende neue Segment um. `RoofWindowPlacementService` prüft und platziert
Dachfenster in vorhandenen Schrägeprofilen.

IDs und Host-Beziehungen müssen bei Wandteilungen stabil bleiben. Mehrere unabhängige Schrägen
eines Raums bilden die untere Deckenhülle; keine Schräge darf stillschweigend eine andere ersetzen.
Reviewer prüfen beide Wandrichtungen, Ecken, vorhandene Türen/Fenster, begrenzte Schrägenläufe und
Fenster nahe Knie- sowie Firsthöhe.
