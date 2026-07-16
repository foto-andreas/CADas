# Leitfaden für das menschliche Review von CADas

## Zweck und Ergebnis des Reviews

Dieser Leitfaden führt ein fachliches und technisches Vier-Augen-Review durch die sicherheits- und
berechnungsrelevanten Teile von CADas. Ein Review gilt erst als abgeschlossen, wenn nicht nur der
Code plausibel wirkt, sondern auch die zugehörigen Invarianten, Randfälle, Tests und erzeugten
Artefakte nachvollzogen wurden. Die Paket-`README.md`-Dateien erläutern jeweils Verantwortung,
Datenfluss und Fehlerverhalten. Die zentrale Systemübersicht zeigt
[docs/diagramme/systemarchitektur.svg](docs/diagramme/systemarchitektur.svg).

Das Review soll für jeden Befund eine der folgenden Aussagen ermöglichen:

1. Die Implementierung ist fachlich richtig und durch einen aussagekräftigen Test belegt.
2. Die Implementierung ist technisch vertretbar, eine ausdrücklich benannte fachliche Entscheidung
   fehlt aber noch.
3. Der Befund verlangt eine Korrektur mit eigenem Test und eigenem Commit.
4. Die vorgeschlagene Änderung wäre nur eine Geschmacksfrage und wird deshalb nicht umgesetzt.

## Geprüfter Ist-Stand des automatisierten Audits

Stand: 16. Juli 2026. `./gradlew clean check` ist erfolgreich. Der Lauf prüft sämtliche Tests,
Compilerwarnungen, Dokumentationskonventionen und die in `build.gradle.kts` festgelegten
90-Prozent-Grenzen der fachlichen Kernpakete sowie die globale 85-Prozent-Grenze.

Die globale JaCoCo-Zeilenabdeckung beträgt 22.694 von 26.678 ausführbaren Zeilen, also 85,1 Prozent.
Der Build erzwingt dieses Projektziel ohne Dateiausschlüsse. Die zuvor benannten Schulden wurden
gezielt bearbeitet: Die Gelände-Anwendungslogik erreicht nach Entfernung der unerreichbaren
Konturableitung 93,4 Prozent, die Heizungs-Anwendungslogik nach den Rasterrouting-Grenzfällen
80,4 Prozent und die JavaFX-Workbench nach den realen Bedien-, Automatisierungs-, Export- und
3D-Abläufen 77,3 Prozent. Die Bereichswerte sind keine Ersatzgrenzen für das globale Ziel; sie dienen
Reviewern als Hinweis auf die weiterhin unterschiedlich hohen Testkosten von Fachlogik und
plattformgebundenen Dialogen. Die beiden Startklassen werden im Unit-Test weiterhin nicht gestartet.

Im automatisierten Audit wurden bereits folgende konkrete Fehler- und Wartbarkeitsklassen bearbeitet:

- DXF-Struktur, vollständige Einheitenauswertung, rettender Import bei beschädigten Metadaten und
  atomarer Projekt- beziehungsweise Etagenexport
- exakte Raumvolumenintegration unter Dachschrägen und geometrische Wohnflächengewichtung nach WoFlV
- vollständige Prüfung von Wandöffnungen gegen lokale Höhenprofile
- Erhalt konkaver Konturen und von Innenlöchern beim IFC- sowie ACIS-Import
- kollisionsfreie parallele DWG-Konverterausgaben
- Begrenzung der Undo-Historie auf 100 Snapshots und endliche Wartezeiten in JavaFX-Tests
- token-, methoden-, origin- und workspace-geschützte lokale Automatisierung
- sichere JavaScript-Stringkodierung für Dokumentnavigation und -suche
- vollständige Tooltips für Menü- und Bedienelemente sowie Beseitigung von Compilerwarnungen
- Entfernung eines unerreichbaren SVG-Berichtsrenders, einer wirkungslosen Ein-Wert-Option und einer
  doppelt deklarierten transitiven Abhängigkeit
- Entfernung der unerreichbaren, parallelen Gelände-Konturableitung zugunsten des aktiven
  Rasterverfahrens
- geometrische Charakterisierung des Heizungs-Rasterroutings einschließlich Sperrknoten, Umwegen,
  Lücken und leerer Geometrie
- reale Workbench-Prüfung von Automatisierungsendpunkten, Heizkreisfenster, Belagsebenen,
  Etagen-Rundlauf, Bauteildrehung, PDF- und 3D-Snapshot-Export
- Zusammenführung wiederholter DXF-Metadatenkonvertierung in `DxfMetadataCodec`

Diese Liste ersetzt kein unabhängiges Review der geänderten Algorithmen. Sie markiert vielmehr die
Stellen, an denen ein menschlicher Reviewer zuerst die neuen Tests gegen die fachliche Erwartung
prüfen sollte.

## Vorbereitung

1. Mit JDK 25 arbeiten und `./gradlew --version` prüfen.
2. `./gradlew clean check` ausführen. Der Lauf umfasst Kompilierung, Tests, JaCoCo-Bericht und die
   globale Mindestabdeckung von 85 Prozent sowie die paketbezogene Mindestabdeckung von 90 Prozent
   für die in `build.gradle.kts` festgelegten Kernpakete.
3. Den HTML-Bericht unter `build/reports/jacoco/test/html/index.html` öffnen. Grüne Zeilen allein sind
   kein Qualitätsnachweis; entscheidend sind fachlich relevante Eingaben und geprüfte Ergebnisse.
4. `git status --short` prüfen und vorhandene Anwenderdateien nicht in Review-Commits aufnehmen.
5. Änderungen pro Fehler, Refactoring oder Dokumentationsthema getrennt halten. Ein Commit muss
   eigenständig verständlich, testbar und bei Bedarf rücknehmbar sein.

## Priorisierung

| Stufe | Bedeutung | Beispiele | Erwartete Reaktion |
|---|---|---|---|
| P0 | Datenverlust, unkontrollierte Ausführung oder falsche sicherheitskritische Ergebnisse | Überschreiben einer Zieldatei nach Teilfehler, Verlassen des Workspace, Befehlsinjektion | Arbeit stoppen, reproduzierenden Test schreiben, sofort korrigieren |
| P1 | Falsche CAD-Geometrie oder fachlich falsche Mengen | verlorene DXF-Öffnung, falsche Wohnfläche, fehlendes Innenloch | vor Freigabe korrigieren und Ergebnis unabhängig prüfen |
| P2 | Robusterheits-, Bedien- oder Wartungsproblem | blockierter JavaFX-Test, unverständliche Fehlermeldung, unbegrenzte Historie | im aktuellen Arbeitszyklus korrigieren oder begründet terminieren |
| P3 | Vereinfachung ohne unmittelbare Fehlwirkung | doppelte Konvertierung, unnötige Abstraktion, tote Variante | nur mit messbarem Nutzen und ausreichender Testabdeckung ändern |

## Empfohlene Review-Reihenfolge

1. Domäneninvarianten und Einheiten
2. Geometrische Algorithmen
3. Persistenz und Austauschformate
4. Mengen, Berichte und fachliche Regelwerke
5. 3D-Ableitung und externe Konverter
6. JavaFX-Zustand, Undo/Redo und Bedienbarkeit
7. Automatisierungsschnittstelle und Dateisicherheit
8. Build, Distribution, Abhängigkeiten und Dokumentation

Diese Reihenfolge folgt dem Datenfluss: Ein Fehler im Modell oder Geometriekern vervielfacht sich in
Export, Bericht und Darstellung. Oberflächenfehler sollten daher nicht zuerst kaschiert werden.

## 1. Domänenmodell und Einheiten

### Prüffragen

- Sind Millimeter, Quadratmillimeter, Kubikmillimeter, Grad und dimensionslose Faktoren an jeder
  Schnittstelle eindeutig? Werden Umrechnungen genau einmal vorgenommen?
- Lehnen Konstruktoren und Änderungsmethoden `NaN`, unendliche Werte, negative Längen, leere Namen,
  ungültige Polygone und widersprüchliche Höhen ab?
- Bleiben Objektkennungen beim Kopieren, Importieren und Undo erhalten, soweit sie fachliche
  Referenzen tragen?
- Verweisen Türen und Fenster immer auf eine vorhandene Host-Wand? Liegen Breite, Versatz,
  Brüstung und Höhe vollständig im lokalen Wandprofil?
- Sind Etagenreihenfolge, Bezugshöhen, Dachbezug und Geländeecken auch bei negativen Höhen korrekt?
- Werden Listen und Wertobjekte defensiv kopiert, sodass Aufrufer keine Invarianten nachträglich
  umgehen können?

### Besonders zu provozierende Randfälle

- Länge und Winkel exakt an ihren erlaubten Grenzwerten
- Nullfläche, kollineare Punkte, doppelter erster/letzter Polygonpunkt und Selbstüberschneidung
- sehr große und sehr kleine Koordinaten sowie negative Weltkoordinaten
- Dachschräge, die eine Wandöffnung nur an einer Ecke schneidet
- zwei Fachobjekte mit identischem Anzeigenamen, aber unterschiedlichen IDs
- Entfernen einer Wand mit referenzierten Öffnungen und anschließendes Undo/Redo

## 2. Geometriekern und Zeichenlogik

Die Reviewerin oder der Reviewer sollte Ergebnisse nicht nur aus derselben Implementierung ableiten.
Für einfache Fälle sind Handrechnung, Skizze und bekannte Kontrollgeometrien geeigneter.

### Prüffragen

- Verwenden Punkt-, Segment- und Polygonvergleiche eine zur Einheit passende Toleranz?
- Sind horizontale, vertikale, diagonale und nahezu parallele Segmente symmetrisch behandelt?
- Unterscheiden Schnittalgorithmen sauber zwischen Kreuzung, Berührung und Überdeckung?
- Bleiben Orientierung und Innen-/Außenseite bei Uhrzeiger- und Gegenuhrzeigerpolygonen korrekt?
- Erzeugen Snap, orthogonale Korrektur und Wandteilung keine Nullsegmente oder Doppelwände?
- Ist das Ergebnis unabhängig von der Reihenfolge fachlich gleichwertiger Eingaben?
- Werden Dimensionstexte und Maßhilfslinien auch bei sehr kurzen Wänden lesbar und kollisionsarm
  platziert?

### Kontrollfälle

- Rechteck, L-Form, U-Form und konkaves orthogonales Polygon
- T-Knoten, Kreuzung, gemeinsamer Endpunkt und teilweise deckungsgleiche Wände
- Öffnung exakt am Wandanfang beziehungsweise Wandende
- mehrfache Teilung derselben Wand in verschiedener Bearbeitungsreihenfolge
- Rotation um 90, 180, 270 und 360 Grad mit anschließendem Rückweg
- Raster- und Endpunktsnap bei gleichem Abstand sowie deaktiviertem Snap

## 3. DXF-, DWG-, IFC- und RFA-Austausch

### DXF-Export

- Mit einem unabhängigen Parser prüfen, dass `HEADER`, `TABLES`, `BLOCKS`, `ENTITIES`, `OBJECTS`
  und `EOF` strukturell lesbar sind.
- `$INSUNITS`, `$MEASUREMENT`, Model-Space-Kennzeichnung, Handles und Referenzen auf Block-Records
  kontrollieren.
- Sicherstellen, dass `CADAS_DXF|5` geschrieben und ältere Metadatensätze weiterhin gelesen werden.
- Umlaute, Leerzeichen, Schrägstriche, senkrechte Striche und Steuerzeichen in Namen prüfen.
- Projekt- und Etagenexport jeweils auf einen bestehenden und einen neuen Zielpfad testen. Ein Fehler
  vor dem atomaren Ersetzen darf die vorhandene Datei nicht verändern.

### DXF-Import

- Vollständigen Rundlauf `Modell → DXF → Modell` objektweise vergleichen; zusätzlich die sichtbare
  Geometrie unabhängig prüfen, damit identische Fehler in Export und Import nicht unentdeckt bleiben.
- Fehlenden Versionsmarker, unbekannte Version, beschädigte einzelne Metadatenzeile, fehlende
  Host-Wand und reine Fremd-DXF-Geometrie getrennt testen.
- Alle unterstützten DXF-Zeichnungseinheiten mit derselben Sollgeometrie vergleichen.
- Doppelte Metadaten, unbekannte Layer, leere Blöcke, verschachtelte `INSERT`-Transformationen und
  negative Skalierung einbeziehen.
- Prüfen, dass rettbare sichtbare Geometrie ergänzt wird, ohne bereits rekonstruierte Fachobjekte zu
  verdoppeln.

### Externe Formate und Prozesse

- Konverterpfad, Argumente und Ausgabepfad getrennt behandeln; keine Shell-Zeichenkette erzeugen.
- Erfolg ohne Ausgabedatei, Abbruch, Zeitüberschreitung, nicht ausführbare Datei und parallele
  Konvertierungen testen.
- Temporäre Ausgaben eindeutig ableiten und zuverlässig aufräumen.
- Bei ACIS-, IFC- und RFA-Geometrie konkave Außenkonturen, mehrere Innenlöcher, umgekehrte
  Flächennormalen, entartete Dreiecke und gemischte Einheiten prüfen.
- Eine fehlgeschlagene Teilfläche darf nicht stillschweigend ein plausibel aussehendes, aber
  topologisch falsches Gesamtmodell erzeugen.

## 4. Fachliche Berechnungen und Berichte

### Wohnfläche

- Die Flächengewichtung nach WoFlV an den Höhen 1,00 m und 2,00 m sowie unmittelbar davor und danach
  kontrollieren.
- Dachschrägen über konkaven Räumen und mit wechselnder Schräge geometrisch zerlegen; eine reine
  Mittelwertbildung ist nicht ausreichend.
- Balkone, Terrassen und sonstige Sonderflächen nur mit einer dokumentierten fachlichen Einstufung
  gewichten.
- Summen gegen Einzelpositionen und Rundung erst an der Ausgabestelle prüfen.

### Raumvolumen und Materialmengen

- Volumen unter horizontaler Decke, einfacher Dachschräge, abgeschnittener Schräge und negativer
  Bezugshöhe per Handrechnung vergleichen.
- Öffnungen, Innenlöcher, Verschnitt, Fugenbreite, Plattenformat und Oberflächenschichten getrennt
  kontrollieren.
- Leere Projekte, leere Materialgruppen und mehrere Materialien mit gleichem Namen müssen einen
  gültigen Bericht ergeben.
- Markdown-, HTML- und PDF-Ausgabe auf identische Werte, Einheiten und Sortierung vergleichen.
- PDF-Datei nach dem Schreiben erneut öffnen; bloßes Vorhandensein oder eine positive Dateigröße
  genügt nicht.

### Heizung

- Heizlast, belegbare Fläche, Verlegeabstand, Rohrlänge, Kreisaufteilung und Verteilerzuordnung mit
  dokumentierten Beispielrechnungen prüfen.
- Ausschlussflächen, Innenlöcher, sehr schmale Restflächen, überlappende Zonen und Flächen außerhalb
  des Raumes einbeziehen.
- Vor- und Rücklauf müssen verbunden, innerhalb erlaubter Bereiche und frei von unzulässigen
  Kreuzungen sein.
- Unterschiedliche Verlegemuster und Routing-Sprachen müssen fachlich gleichwertige Randbedingungen
  einhalten.

## 5. 3D-Ableitung

- 2D-Grundriss, Höhenprofil und erzeugte 3D-Maschen anhand gemeinsamer Referenzpunkte vergleichen.
- Dreiecksorientierung, Normalen, geschlossene Flächen und Innenlöcher kontrollieren.
- Wandöffnungen dürfen weder in Vorder-/Rückfläche noch in den Laibungsflächen versehentlich
  geschlossen werden.
- Dach, Treppenunterbau, Bodenöffnungen und Gelände bei negativen sowie versetzten Höhen prüfen.
- Kamera-Fit, orthogonale und perspektivische Projektion für leere, sehr kleine und sehr große Szenen
  testen.
- Auswahlkennungen müssen nach Neuaufbau der Szene weiterhin eindeutig dem Fachobjekt entsprechen.

## 6. JavaFX, Bedienung und Zustand

- Jeder Button, Menüpunkt, Schalter, Regler und jedes Eingabefeld benötigt einen ausführlichen
  Tooltip, der Wirkung, Einheit und wichtige Randbedingung beschreibt.
- Alle Modelländerungen müssen auf dem JavaFX-Thread oder über eine klar definierte Übergabe erfolgen;
  lang laufende Datei- und Konverterarbeit darf den UI-Thread nicht blockieren.
- Dialogabbrüche dürfen weder Modell noch Historie ändern.
- Ungültige Eingaben müssen den alten gültigen Zustand erhalten und eine verständliche deutsche
  Meldung liefern.
- Auswahl, aktives Werkzeug, 2D-/3D-Ansicht und Property-Spalte nach Import, Ebenenwechsel, Undo und
  Redo prüfen.
- Die Undo-Historie ist auf 100 Snapshots begrenzt. Am Grenzwert sowie nach neuer Änderung im
  zurückgesetzten Zustand muss das Verhalten kontrolliert werden.
- JavaFX-Tests benötigen eine endliche Wartezeit und müssen ursprüngliche Ausnahmen sichtbar machen;
  ein hängender Test ist kein akzeptables Fehlerbild.

## 7. Automatisierungsserver und Dateisicherheit

- Der Server darf nur an `127.0.0.1` binden und ausschließlich auf expliziten Start aktiviert werden.
- Das Sitzungstoken muss mindestens 32 Zeichen lang sein; bis auf `/health` muss jeder Endpunkt ein
  korrektes Bearer-Token fordern.
- Lesende Zugriffe verwenden `GET`, Änderungen `POST`. Andere Methoden, fremde Origin und fehlende
  beziehungsweise doppelte Parameter müssen abgewiesen werden.
- Eingabe- und Ausgabepfade werden kanonisch gegen die Workspace-Wurzel geprüft. Absolute Pfade
  außerhalb, `..`, symbolische Links und ein zwischen Prüfung und Zugriff ausgetauschtes Ziel sind
  Angriffs- beziehungsweise Robustheitsfälle.
- Gleichzeitige Anfragen, Zeitüberschreitung der JavaFX-Übergabe, geschlossene Workbench und
  unerwartete Runtime-Ausnahmen dürfen den Server nicht in einem inkonsistenten Zustand lassen.
- Antworten dürfen weder Token noch lokale Pfade oder interne Stacktraces offenlegen.

## 8. Struktur, Verständlichkeit und Vereinfachung

### Sofort anwendbare Regeln

- Eine Abstraktion behalten, wenn sie eine fachliche Grenze schützt, mehrere echte Implementierungen
  trägt oder einen schwer testbaren Adapter isoliert. Spekulative Erweiterungspunkte entfernen.
- Konvertierungs- und Kodierlogik nur an einer Stelle halten. Der gemeinsame
  `DxfMetadataCodec` ist das Muster für Punkte, Profile, Zahlen und IDs.
- Standardbibliothek und vorhandene JavaFX-/JDK-Funktionen vor neuen Hilfsklassen oder Abhängigkeiten
  verwenden.
- Private Methoden nach fachlichem Ablauf statt nach zufälliger Entstehungsreihenfolge gruppieren.
- Kommentare erklären Zweck, Einheit, Invariante und nicht offensichtliche Entscheidung. Sie sollen
  den Code nicht Zeile für Zeile wiederholen.
- Tote Varianten, unerreichbare Renderer und nur theoretisch konfigurierbare Ein-Wert-Modelle löschen,
  sofern Tests das aktuelle Verhalten festhalten.

### Bewusst nicht blind durchzuführende Refactorings

Die folgenden Punkte verdienen ein eigenes Architekturvorhaben, aber keine großflächige Änderung im
Rahmen eines Fehlerfixes:

- Die Workbench verteilt Zustand über eine tiefe Vererbungskette. Eine schrittweise Komposition aus
  Projekt-, Interaktions-, Render- und Dialogkomponenten wäre leichter prüfbar. Vorher sind jedoch
  Charakterisierungstests für Auswahl, Undo, Import und Ansichtswechsel erforderlich.
- `ThreeDSceneModelBuilder`, `HydronicHeatingLayoutService`, `SurfaceMaterialListService` und Teile der
  Workbench liegen nahe an der zulässigen Klassengröße. Eine Aufteilung darf nur entlang fachlich
  eigenständiger Algorithmen erfolgen; reine Zeilenzahlkosmetik verschlechtert die Navigation.
- `SurfaceLayer` besitzt viele Konstruktor- und Rekonfigurationsvarianten. Ein validiertes
  Konfigurations-Wertobjekt könnte Aufrufer vereinfachen, verändert aber eine breite API und benötigt
  zuerst vollständige Rundlauf- und Migrationsprüfungen.
- Weitere gemeinsame DXF-Entity-Schreibbausteine sind nur sinnvoll, wenn sie Handles, Eigentümer und
  Gruppencodes weiterhin sichtbar und formatnah halten. Eine generische Map-Abstraktion würde das
  Formatreview erschweren.

## 9. Teststrategie

Für jeden korrigierten Befund gilt die Reihenfolge:

1. Einen Test formulieren, der die fachliche Erwartung aus Eingaben und Sollwerten ausdrückt.
2. Prüfen, dass der Test ohne Korrektur aus dem richtigen Grund fehlschlägt.
3. Die kleinste saubere Korrektur umsetzen.
4. Den fokussierten Test und anschließend `./gradlew clean check` ausführen.
5. Bei Geometrie, Export oder PDF zusätzlich das erzeugte Artefakt unabhängig prüfen.

Gute Tests prüfen beobachtbares Verhalten. Tests, die private Methoden spiegeln, interne Listenlängen
ohne fachliche Bedeutung vergleichen oder nur „wirft keine Ausnahme“ erwarten, erschweren spätere
Vereinfachungen. Randomisierte Tests dürfen feste Seeds verwenden und müssen den kleinsten
reproduzierbaren Fehlerfall ausgeben.

### Abdeckungsmatrix pro Änderung

| Kategorie | Mindestens ein Fall |
|---|---|
| Normalfall | typischer gültiger Arbeitsablauf |
| Untere Grenze | leer, null oder kleinster erlaubter Wert |
| Obere Grenze | größter sinnvoller Wert oder Historienlimit |
| Knapp ungültig | unmittelbar außerhalb einer fachlichen Grenze |
| Strukturfehler | fehlende Referenz, beschädigter Datensatz oder ungültiges Polygon |
| Rundlauf | Schreiben und erneutes Lesen ohne Informationsverlust |
| Nebenwirkung | unveränderter Zustand nach Abbruch oder Fehler |

## 10. Dokumentation und Nachvollziehbarkeit

- Paket-`README.md`, `package-info.java`, öffentliche API-Kommentare und Implementierung müssen
  denselben Stand beschreiben.
- Die Benutzerhilfe liegt doppelt unter `docs/benutzerdoku.md` und
  `src/main/resources/docs/benutzerdoku.md`; beide Dateien müssen bytegleich bleiben.
- Mermaid-Quelle und eingebundenes SVG gemeinsam aktualisieren.
- Neue UI-Texte, Kommentare, Dokumentation und Commit-Nachrichten werden auf Deutsch mit Umlauten
  geschrieben.
- Ein fachlicher Algorithmus dokumentiert Quelle oder Regelwerk, Einheiten, Grenzwerte,
  Rundungszeitpunkt und Verhalten bei nicht klassifizierbaren Eingaben.

## Befundvorlage

Jeder Review-Befund sollte in dieser Form erfasst werden:

```text
Kennung: CADAS-REV-...
Priorität: P0 bis P3
Ort: Datei, Typ und möglichst kleine Zeilenspanne
Fachliche Erwartung:
Beobachtetes Verhalten:
Minimaler reproduzierbarer Fall:
Auswirkung auf gespeicherte Daten oder Folgefunktionen:
Vorgeschlagene Korrektur:
Erforderliche Tests und unabhängige Kontrolle:
Entscheidung und verantwortliche Person:
```

## Freigabecheckliste

- [ ] Alle P0- und P1-Befunde sind korrigiert oder die Freigabe ist ausdrücklich verweigert.
- [ ] P2-Befunde sind korrigiert oder mit Verantwortlichkeit und Zieltermin dokumentiert.
- [ ] `./gradlew clean check` ist auf einem sauberen Stand erfolgreich.
- [ ] Compiler meldet keine ungeprüften oder veralteten API-Verwendungen.
- [ ] Keine Produktionsklasse überschreitet 2000 Zeilen.
- [ ] Geänderte DXF-, PDF- und 3D-Artefakte wurden unabhängig geöffnet beziehungsweise geprüft.
- [ ] Benutzerhilfe, Systemdokumentation, Paketdokumentation und Diagramme stimmen mit dem Code überein.
- [ ] Tooltips und Fehlermeldungen sind für die geänderte Bedienoberfläche vollständig.
- [ ] Commits sind fachlich kleinteilig, deutsch benannt und enthalten keine Anwenderdateien.
- [ ] Verbleibende Risiken und bewusste Nicht-Änderungen sind im Review-Protokoll festgehalten.
