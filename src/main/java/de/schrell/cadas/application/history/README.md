# Rückgängig und Wiederherstellen

`UndoRedoStack<T>` verwaltet vergangene und wiederherstellbare Snapshots. CADas speichert darin
vollständige Workbench-Zustände, nicht einzelne Gegenoperationen. Der Standard begrenzt den Verlauf
auf 100 Einträge, damit lange Sitzungen keinen unbegrenzten Speicherbedarf erzeugen.

Ein neuer fachlicher Schritt leert den Redo-Zweig. Undo und Redo auf leerem Verlauf verändern
nichts. Der Stack kennt keine JavaFX-Eigenschaften und synchronisiert nicht selbst; alle Zugriffe
erfolgen im Workbench-Kontext auf dem JavaFX-Thread.

Reviewer prüfen Kapazität null beziehungsweise eins, Überlauf, Verzweigung nach Undo und die
Unabhängigkeit gespeicherter Snapshots von späteren Mutationen.
