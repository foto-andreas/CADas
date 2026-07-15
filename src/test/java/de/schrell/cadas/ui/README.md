# Tests: JavaFX-Workbench

Die UI-Suite prüft Workbench-Automation, Werkzeuge, Rendering-Snapshots, Menü, Tastatur, Tooltips,
Undo/Redo, Auswahl, Dialogadapter, 3D-Viewport und den lokalen Automatisierungsserver. Aufgaben
laufen über `JavaFxTestSupport` auf dem JavaFX-Thread und besitzen eine feste Zeitgrenze.

Tests sollen Fensterzustand nicht zwischen Methoden teilen, interaktive Dialoge deaktivieren und
fachliche Ergebnisse über öffentliche Automation oder sichtbare Knoten prüfen. HTTP-Tests decken
Token, Methoden, Origin, Pfadgrenzen, symbolische Links und JavaFX-Timeout ab.
