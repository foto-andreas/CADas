# Austauschdateinamen

`ExchangeFileNameService` stellt sicher, dass ein Ziel genau eine gewünschte Dateiendung besitzt.
Die Normalisierung arbeitet unabhängig von Groß-/Kleinschreibung, erhält den Elternpfad und entfernt
wiederholt angehängte Endungen.

Der Dienst verändert keine Datei und prüft keine Formatdaten. Formatadapter liegen in
`infrastructure.dxf`, Zielauswahl und Überschreibbestätigung in `ui`. Tests müssen Namen ohne
Endung, gemischte Großschreibung, Mehrfachendungen und Pfade mit Elternverzeichnis abdecken.
