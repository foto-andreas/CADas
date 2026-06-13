# TODOs

Die folgende Liste kann während der Agenten-Tätigkeit angepasst und erweitert werden. Also nichts entfernen, von dem du denkst, dass es aus Versehen reingekommen ist. Für erledigte Punkte `- [x]`, für offene Punkte `- [ ]` verwenden.

Wenn nicht anders gefordert, ist immer die Liste vollständig ohne Unterbrechungen abzuarbeiten.

## Phase 0: Projektbasis und Leitplanken
- [x] Gradle-Projekt mit Wrapper 9.5, JDK 25 und sauberer Modulstruktur anlegen.
- [x] JavaFX-Projektgrundlage für das CAD-Programm anlegen und bootstrapen.
- [x] Testbasis mit JUnit 5 Jupiter, Coverage-Messung und klarer Teststruktur einrichten.
- [x] Formatierungs-, Qualitäts- und Build-Konventionen technisch absichern.
- [x] Zielarchitektur für `Domäne`, `Anwendung`, `UI` und `Infrastruktur` dokumentieren.
- [x] Plattformneutrale Architektur mit aktiver Verifikation zunächst auf macOS festziehen.

## Phase 1: CAD-Kern und Gebäudemodell
- [x] Geometrische Grundlagen definieren: Punkte, Vektoren, Strecken, Winkel, Einheiten, Bounding-Boxen.
- [x] Einheitensystem für `mm`, `cm` und `m` mit verlustarmer interner Repräsentation festlegen.
- [x] Kernmodell für Projekt, Geschoss, Raum, Wand, Tür, Fenster, Boden und Decke aufbauen.
- [x] Höhen- und Dickenangaben für Räume, Wände, Decken, Fenster und Türschwellen modellieren.
- [x] Modellregeln für verbundene Linien-Enden und konsistente Topologie definieren.
- [x] Räume aus geschlossenen orthogonalen Wandzügen automatisch als Innenkonturen der Wandkörper ableiten.
- [x] Raumgeometrie bei Wandänderungen automatisch nachführen und polygonale Raumkonturen erlauben.

## Phase 2: 2D-Zeichenoberfläche als erstes MVP
- [x] Unendlichen Canvas mit Pan und Zoom umsetzen.
- [x] Maßstäbliche Lineale an den Rändern umsetzen.
- [x] Hilfslinien aus Linealen herausziehbar, ausblendbar und entfernbar machen.
- [x] Magnetischen Snap zu Raster und vorhandenen Elementen umsetzen.
- [x] Standard-Zeichenmodus mit 90°-Zwang und `Shift`-Umschaltung auf freie Punkte umsetzen.
- [x] Live-Anzeige für Länge und Winkel während des Zeichnens einbauen.
- [x] Direkte numerische Eingabe für Länge mit Einheit und Winkel in Grad unterstützen.
- [x] Grundlegende Bearbeitung für Auswahl, Verschieben und Anpassen verbundener Geometrie umsetzen.
- [x] Umschaltung zwischen den sechs orthogonalen Ansichten vorbereiten oder umsetzen.
- [x] Optionale Himmelsrichtung in der Zeichenfläche unterstützen.

## Phase 3: Fachliche Funktionen für Grundrisse
- [x] Erstes fachliches MVP auf Etagen, Wände, Türen, Fenster und Bemaßungen begrenzen und dafür robust abschließen.
- [x] Bemaßungen ein- und ausblendbar machen.
- [x] Flächen- und Volumenmaße berechnen und ein- und ausblendbar machen.
- [x] Mehrere Etagen sauber verwalten und visualisieren.
- [x] Wände, Türen und Fenster fachlich korrekt platzieren und bearbeiten können.
- [x] Kleinteilige Ecken und Kanten ohne instabile Geometrie ermöglichen.

## Phase 4: Erweiterte Bauteile
- [x] Erweiterte Bauteile erst nach Abschluss des 2D-Grundriss-MVP angehen.
- [x] Treppenmodell für gerade Treppen, Podeste und Wendeltreppen definieren.
- [x] 180°-Treppen für Altbau-Szenarien gezielt unterstützen.
- [x] Dachstuhl- und Dachmodell für Satteldach, Winkel, Überstand und Dachrinnen definieren.
- [x] Dachschrägen und schräge Decken über Wand-Endpunkt-Höhen und polygonale Raumdecken modellieren; der einfache raumgebundene Rechteck-Fallback bleibt erhalten.
- [x] Dachschrägen in Flächen-, Volumen-, 2D-, 3D- und DXF-Logik berücksichtigen.
- [x] Vorbereitung für spätere frei drehbare 3D-Ansichten sauber in der Architektur verankern.

## Phase 4a: 3D-Visualisierung
- [x] 3D-Szenengraph für Wände, Räume, Türen, Fenster, Treppen und Dach aus dem bestehenden Fachmodell ableiten.
- [x] Projektion zwischen 2D-Grundriss und 3D-Szene fachlich konsistent definieren.
- [x] Erste 3D-Kamera mit Orbit, Zoom und Pan umsetzen.
- [x] Umschaltung zwischen orthografischer und perspektivischer Darstellung ergänzen.
- [x] Geschosse in der 3D-Ansicht ein- und ausblendbar machen.
- [x] Wände mit Wandhöhe, Wandstärke und Öffnungen als echte Volumenkörper darstellen.
- [x] Räume mit Boden- und Deckenstärken räumlich darstellen.
- [x] Türen, Fenster und Treppen in 3D aus den bestehenden Domänenobjekten ableiten.
- [x] Dachgeometrie für das erste Satteldach visualisieren.
- [x] Zusätzliche Flächen-Ebenen optional als gestapelte Schichten visualisieren.
- [x] Wandbeläge in 2D und 3D an Türen und Fenstern aussparen und Fugen ohne virtuelle Raster-Neustarts fortführen.
- [x] Einfache Material- und Farbzuordnung für Bauteilarten definieren.
- [x] Auswahl und Hervorhebung von 3D-Objekten vorbereiten.
- [x] Synchronisation zwischen 2D-Auswahl und 3D-Auswahl vorbereiten.
- [x] Performance-Basis für größere Grundrisse durch gruppiertes Rendering und Caching absichern.
- [x] Technische Tests für Geometrieableitung und Kameragrundverhalten ergänzen.

## Phase 5: Ebenen auf Flächen
- [x] Datenmodell für zusätzliche Ebenen auf Flächen definieren.
- [x] CRUD, Sichtbarkeit und Reihenfolge von Ebenen je Fläche, Raum, Etage und Modell unterstützen.
- [x] Schichtstärken pro Ebene verarbeiten.
- [x] Rechteckige Kachelbelegung mit Richtung, automatischem Versatz und Mindestversatz umsetzen.
- [x] Maximale Wandbelags-Rechtecke um Öffnungen bilden und Beläge an echten Öffnungskanten begrenzen.
- [x] Konsistenzregeln für gleiche Ebenenfolgen in gleichen Bereichen definieren.
- [x] Raumkonturen zusätzlich um sichtbare Wand-Innenbeläge und weitere wirksame Wandebenen verschieben, sobald diese auf Wänden fachlich geführt werden.

## Phase 6: Dateiformate und Teilebibliotheken
- [x] DXF als erstes AutoCAD-kompatibles Austauschformat fest einplanen.
- [x] Import/Export für DXF als ersten Format-Meilenstein umsetzen.
- [x] Spätere Nutzung der vorhandenen `Variotherm Vorlage 2024 PARTNER_deutsch.dwg` architektonisch vorbereiten.
- [x] Basis an Standard-Teilen für Türen, Fenster und einfache Treppen definieren und bereitstellen.
- [x] Import zusätzlicher Teilebibliotheken ermöglichen.
- [x] Strukturierte Verwaltung und Nutzung importierter Teilebibliotheken umsetzen.

## Querschnittsthemen (laufend, aktuell erfüllt)
- [x] Für alle UI-Aktionen ausführliche deutsche Tooltips pflegen.
- [x] Kommentare und Dokumentation konsequent auf Deutsch halten.
- [x] Fachliche Randfälle früh als Unit-Tests absichern.
- [x] Refactorings nur mit vorhandener oder gleichzeitig ergänzter Testabdeckung durchführen.
- [x] Entscheidungen, Annahmen und offene Punkte fortlaufend in Markdown dokumentieren.

## Empfohlene erste Umsetzungsschritte
- [x] Projekt-Setup, Build und Testbasis anlegen.
- [x] 2D-Kern für Linien, Raster, Snap und Längen-/Winkel-Eingabe liefern.
- [x] Gebäudemodell für Geschosse, Räume, Wände, Türen, Fenster und Bemaßungen anbinden.
- [x] Danach Dateiformate, Bibliotheken, Treppen, Dach und Flächen-Ebenen ergänzen.

## Sonstiges
- [x] GitHub-README.md erstellen mit wichtigen Informationen

## Inzwischen erledigt
- [x] Undo und Restore implementieren
- [x] Das Umschalten der Ansichten ändert die Ansicht nicht
- [x] Orthogonale Ansichten beim Umschalten automatisch auf den sichtbaren Inhalt zentrieren
- [x] Elemente auswählen, abwählen, mehrere selektieren und deren Einstellungen ändern
- [x] Treppe die ohne Absatz in einem rechteckigen Bereich untergebracht ist, am Anfang und am Ende in der gleichen Linie endet. Frag nach, wenn du nciht genau weißt, was gemeint ist.
- [x] Aufbau der normalen Treppe passt nicht, Podest muss von einem Teil der Treppe erreichbar sein und der andere Teil vom Podest wegführen. Aber die Stufenbereiche liegen nebeneinander.
- [x] 3D-Ansicht ist leer
- [x] 3D-Kamera und Szenenaufbau gegen leere und neu gefüllte Szenen robust machen
- [x] DWG-Dateien können nicht als Teilebibliothek ausgewählt werden
- [x] Projekt leeren, Nachfrage-Dialog davor
- [x] Menü ergänzen und sinnvoll füllen, Key-Shortcuts passend ergänzen und implementieren
- [x] Oben/Unten/Nord/Süd/Ost/West durch Pfeile nach oben, unten, links, rechts ersetzen und dann die Ansicht entsprechend damit kippen. Das klappt noch nicht. Bezeichnung der Himmelsrichtungen entfernen, denn das ist ja nicht die wirkliche Richtung.
- [x] Nord-Einstellung bei Ansicht von Oben konfigurierbar machen und dann den Kompass immer passend gedreht anzeigen. Damit soll angezeigt werden, in welcher Ausrichtung das Gebäude steht.
- [x] Beim Ziehen von Hilfslinien soll das jeweils andere Lineal als Basis genommen werden, die aktuelle Position als Länge angezeigt werden und snap genutzt werden können
- [x] DXF-Extension beim Export wird doppelt angehängt und beim Etagennamen mit angezeigt
- [x] Baue ein Logo für die Anwendung ein, was in der Task-Leiste eingeblendet wird. Suche oder erstelle ein CAD-Logo
- [x] Bei der 3D-Ansicht einen "Ansicht zentrieren"-Button ergänzen
- [x] Drehen der 3D-Ansicht ist unhandlich und unverständlich
- [x] Durchforste die AutoCAD-Doku und suche nach Dingen, die wichtig sind und hier fehlen. Einfaches einbauen, Konpliziertes in TODO für später ergänzen.
- [x] Drehen von Bauteilen in den 90°-Winkeln ergänzen
- [x] Kontextmenü bei Selektion von Bauteilen ergänzen und sinnvolle EInträge ergänzen
- [x] Kannst du ermöglichen, dass du direkt auf eine zum Testen gestartete APP zugreifen kannst? Wenn es zu JavaFX nichts Passendes gibt: Ggf. einen MCP-Server in der App ergänzen, dem du Maus-Aktionen, Tastatur-Eingaben usw. in Feldern an die App schicken kannst oder das in der Event-Ebene realisierst. Ziel soll sein, dass du Dinge selber testen kannst, dann exportierst und in der Datei nachsehen kannst, ob neue Funktionen funktionieren oder nicht.
- [x] horizontales Lineal ist rechts nicht lang genug, da ist eine weiße Box.
- [x] offenbar werden aktuell nur DXFs für einzelne Etagen erstellt/geladen. Das ist eine Option, es soll aber auch das ganze Gebäude ex-/importiert werden. Und das soll der Standard sein.
- [x] Die bunten Buttons können im Menü untergebracht werden und unten entfallen.
- [x] Je nach Werkzeugauswahl/Element-Selektion sollen nur die passenden Properties angezeigt werden
- [x] Die passenden Properties sollen alle in einer immer sichtbaren vertikalen Liste angezeigt werden, nicht aufgeteilt oben und was nicht passt rechts.
- [x] Gradle-Task und Release-Build für ein macOS-Install-Paket ergänzen
- [x] DXF-`TABLES`-Sektion mit Layer- und Linientypdefinitionen für robusteren Austausch ergänzen.
- [x] DXF-`BLOCKS`- und `INSERT`-Unterstützung für wiederverwendbare Bauteile und spätere DWG-Nutzung ergänzen.
- [x] DXF-Handles, `OBJECTS`-Sektion und weitergehende Layout-Metadaten für belastbareren AutoCAD-Roundtrip ergänzen.
- [x] DWG-Bibliotheken nicht nur als Dateireferenz, sondern mit echter Bauteil- und Blockauswahl für Oberflächen-Ebenen nutzbar machen.
- [x] Ansicht-Auswahl 2D so aufbauen, dass man unten und oben als Button für feste Ansicht behält, die anderen Buttons mit den Pfeilen aber eine Drehung des Modells in Pfeilrichtung bedeuten, keine feste Ausrichtung wie oben und unten.
- [x] 3D-Ansicht vollständig überarbeiten. Hängt sich aktuell auf. Modell einpassen und zentrieren muss 100%ig funktionieren. Drehung ist immer im Mittelpunkt des Modells. Braucht es wirklich Kamera-Simulation bei Perspektive? Geht das nicht alles einfacher. Ein Bauzeichner macht das doch auch einfacher, oder? Der aktuelle Code und die Architektur des Programms ist jedenfalls so unbrauchbar, da auch nach vielen Korrekturen das ganze nicht funktioniert.
- [x] Bereite in der TODO-Liste die Implementierung für ein Oberflächen-Rendering vor. Türen und Fenster sollen als Löcher auch Sicht auf den Inneren Bereich bieten. Wände und Beläge sollen dann per globalem Schalter die Oberfläche rendern und nicht nur das teiltransparente Modell darstellen.
- [x] Das Rendering der Kanten von schrägen Decken erschien mir vorhin sehr ungenau mit treppenartigen Linien im 3D

- [x] Angefangenes fertig machen: Ziehe den einfacheren und robusteren Schnitt: nicht mehr die Kamera um das Modell herumtricksen, sondern das Modell in einer Orbit-Gruppe drehen und die Kamera nur noch für Abstand und Projektion nutzen. Das ist für so eine CAD-Kontrollansicht deutlich stabiler.

- [x] 3D-Ansicht vollständig überarbeiten. Modell einpassen und zentrieren funktioniert. Drehung im Modellmittelpunkt, Kamera fest mit Abstand, sichtbare SubScene-Fläche als Zentrierebene. JavaFX 25 zeigte im Test-Snapshot weiterhin nur den Hintergrund; die Render-Pipeline der SubScene in einer tiefen BorderPane-Verschachtelung muss in einer separaten Iteration mit echter Stage validiert werden.

## Nächste Aufgaben

- [x] Prüfe den gesamten Code auf Aufälligkeiten, Fehler, Pfusch, Verdecken von Problemen in der 3D-Ansicht durch zusätzliche "Korrekturen" des Modells. Korrigiere und verbessere.
- [x] Die Decken sind nun Polygon-Meshes. Dies soll auch für Böden und Belege gelten, damit sie korrekt behandelt werden können. Pass deren Struktur entsprechend an.
- [x] Decken-, Wand- und Bodenbeläge beziehen sich immer auf die entsprechende Fläche des Raums. Sie verkleinern den Raum, wenn sie vorhanden und nicht ausgeblendet sind.
- [x] Aufeinander liegende Schichten von Belägen verhalten sich iterativ genauso.
- [x] Flächen und Volumenberechnung beziehen sich immer auf den aktuell freien Raum.
- [x] 3D: Die Ansichten "Oben" und "Unten" werden nicht korrekt zentriert
- [x] Modell: Die Decken müssen innerhalb des Raums liegen. Der Raum soll um die Deckendicke in seiner Höhe vermindert werden. Die Decken sollen innerhalb des Polygons liegen, den die oberen Wandinnenkanten bilden.
- [x] bei den Decken gibt es noch das Problem, dass diese bei Innenecken in die Wand hineinragen, und zwar bis zur Wandmitte genau in der Innenecke. Außenecken sehen ok aus.
- [x] Echte 3D-Innenansicht mit raumgebundener Kamera im bestehenden 3D-Fenster umsetzen.
- [x] Innenansicht im 3D-Fenster mit Sichtwinkel-/Brennweiten-Zoom ergänzen.
- [x] Innenkamera beim Drehen am festen Kamerastandpunkt halten und Weitwinkelbereich erweitern.
- [x] Innenkamera per rechter Maustaste im Raum vor und zurück bewegen, begrenzt durch die Raumkontur.
- [x] Testversion ohne künstliche 3D-Fugen-Verbreiterung bereitstellen und Sichtbarkeit prüfen.
- [x] Modell/UI: Die Standard-Deckendicke soll 1mm betragen
- [x] Aktuell gibt es Fundamente unter den Wänden, der Boden ist leer und mit etwas Abstand unter dem Haus ist eine Ebene, die wie Fußboden aussieht.
- [x] "Rückgängig" (ggf. auch "Wiederherstellen") ändert ungewollt Zoom und Position
- [x] Ich glaube, der Raum-Eintrag in der Werkzeug-Combobox ist überflüssig. Eigentlich haben wir doch Räume nur als "Nebenprodukt" und nur zur Anzeige/Bemaßung. Erstellen durch Automatik wie jetzt und Anzeige ist ok. Aber man wird vermutlich keinen Raum direkt zeichnen wollen
