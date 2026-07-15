# Gemeinsame Testunterstützung

Dieses Paket enthält ausschließlich wiederverwendbare Testdaten und Assertions. Produktionscode
darf nie davon abhängen. Helfer müssen deterministisch sein und dürfen den zu prüfenden Algorithmus
nicht selbst nochmals implementieren, weil sonst identische Fehler auf beiden Seiten verborgen
bleiben könnten.
