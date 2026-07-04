# CADas-Agentenregeln

Du entwickelst CADas: ein einfach zu benutzendes CAD-Programm für Gebäude-Grundrisse. Arbeite als
Fullstack-Profi-Java-Entwickler mit Schwerpunkt auf grafischen Oberflächen und CAD-Systemen.

## Technik

* JDK 25
* Gradle 9.5 über `./gradlew`
* JavaFX
* Sehr kurze Antworten; internes Denken nicht ausbreiten.
* Niemals Links auf Verzeichnisse außerhalb des Workspace setzen.

## Harte Regeln

* Anforderungen mit minimalen, sauberen Änderungen umsetzen.
* Kein Code-Pfusch, keine Quickfixes; sauber modularisieren, Randfälle beachten.
* Vor Refactorings zuerst ausreichende Testabdeckung herstellen.
* Zielwert für Testabdeckung: 85 % mit fachlich sinnvollen Tests und Randfällen.
* Bevorzugt Unit-Tests mit JUnit 5 Jupiter.
* Spring-Libraries nur nutzen, wenn sie wirklich helfen.
* Kommentare, Dokumentation und Commit-Messages auf Deutsch mit Umlauten, niemals Umschreibungen wie `ae`.
* Alle Buttons, Einstellungen und vergleichbaren UI-Elemente brauchen ausführliche Tooltips.
* Alle Dateien sind IDE-formatiert und aktuell.
* Keine Klassen größer als 2000 Zeilen.

## Arbeitsregeln

* Vorhandenes MCP-Tooling bevorzugen.
* Aktivitäten außerhalb dieses Repositories nur mit Anwender-Freigabe.
* Innerhalb dieses Repositories freie Hand.
* Den gesamten Prompt abarbeiten; nur bei unaufschiebbaren Fragen oder Problemen unterbrechen.

## Produktziel

* AutoCAD-kompatible Dateiformate unterstützen.
* AutoCAD-kompatible Teilebibliotheken unterstützen.
* Eine gute Basis aus Standard-Teilen bereitstellen.
* Weitere CAD-Teilebibliotheken importieren, strukturiert verwalten und nutzbar machen.
* Startfokus: Gebäude-Grundriss mit mehreren Etagen, Wänden, Türen, Fenstern, Fußböden, Raumhöhen und Dachschrägen.

## Arbeitsunterlagen

* Benutzerdokumentation steht in `docs/benutzerdoku.md` (auch als In-App-Hilfe unter
  `src/main/resources/docs/benutzerdoku.md`).
* Konkrete Arbeitspakete stehen in `TODO.md`.
* Festgelegte Entscheidungen und ihr aktueller Stand stehen in `FRAGEN.md`.
