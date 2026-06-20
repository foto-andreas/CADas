# KI-Projektübersicht

Kompakte Orientierung für KI-Assistenten. Diese Datei ergänzt `AGENTS.md`, `docs/systemdoku.md` und `docs/benutzerdoku.md` – sie wiederholt nicht deren Inhalt, sondern verweist dorthin und fasst nur das Zusammen, was für einen schnellen Einstieg nötig ist.

## Schnellstart

```bash
./gradlew test              # alle Tests (JUnit 5, JaCoCo)
./gradlew runApp            # App über CADas.app-Bundle starten (macOS zeigt korrekten App-Namen)
./gradlew runMitAutomatisierung  # App mit HTTP-Testzugriff auf 127.0.0.1:17845
./gradlew installDist       # Startskripte und Libs nach build/install/CADas
```

`./gradlew run` ist deaktiviert, weil die nackte JVM als App-Name „java" in der macOS-Menüleiste zeigen würde. `runApp` baut das App-Bundle und startet es.

## Architektur in einem Satz

Vier-Schicht-Architektur: `domain` (Fachmodell, Geometrie) → `application` (Services, Berichte, Austausch) → `infrastructure` (DXF-Adapter) → `ui` (JavaFX-Workbench). Alle Abhängigkeiten zeigen nach innen, die UI kennt die Domäne, nicht umgekehrt.

## Paketkarte

| Paket | Aufgabe | Wichtige Klassen |
|---|---|---|
| `domain.geometry` | Längen, Winkel, Punkte, Segmente | `Length`, `PlanPoint`, `PlanSegment`, `Angle` |
| `domain.model` | Fachmodell: Projekt, Etagen, Wände, Räume, Öffnungen, Beläge | `ProjectModel`, `Level`, `Wall`, `Room`, `Door`, `WindowElement`, `Staircase`, `SurfaceLayerStack` |
| `application.drawing` | Zeichnen, Snap, Auswahl, Bearbeitung | `DraftingService`, `SnapService`, `SelectionQueryService`, `EdgeResizeService` |
| `application.view` | 3D-Szenenmodell, Innenansicht, Kamera | `ThreeDSceneModelBuilder`, `ThreeDInteriorViewService` |
| `application.layers` | Belags-Presets, Kachelbelegung, Schichtwirkungen | `SurfaceLayerEffectService`, `TileLayoutService` |
| `application.reports` | Materialliste, Markdown-Rendering | `SurfaceMaterialListService`, `MarkdownHtmlRenderer` |
| `application.help` | Hilfe und Keymap | `HelpContentService` |
| `application.exchange` | Formatunabhängige Austauschschnittstellen | `ExchangeFileNameService` |
| `application.parts` | Interne Teilebibliothek | Standard-Presets für Türen, Fenster, Treppen |
| `application.objects` | Raumobjekt-Presets | `RoomObjectPresetService` |
| `application.room` | Automatische Raumerkennung aus Wandzügen | `AutoRoomGenerationService` |
| `application.history` | Rückgängig/Wiederherstellen | `UndoRedoStack` |
| `application.dwg` | DWG-Analyse über externe Konverter | `DwgLibraryAnalyzer` |
| `infrastructure.dxf` | DXF-Import/Export | `DxfProjectExchangeService`, `DxfLevelExchangeService` |
| `ui` | JavaFX-Workbench, 3D-Viewport, Automation | `CadWorkbench`, `ThreeDViewport`, `AutomationBridgeServer` |

`CadWorkbench` ist die zentrale UI-Klasse (~6900 Zeilen). `ThreeDViewport` steuert die 3D- und Innenansicht.

## Build-Konfiguration

| Element | Wert |
|---|---|
| JDK | 25 |
| Gradle | 9.5 (Wrapper) |
| UI | JavaFX 25 (controls, swing, web) |
| Tests | JUnit 5 Jupiter, JaCoCo |
| Markdown | commonmark + gfm-tables |
| PDF | Apache PDFBox 3.0.7 |
| Module | `de.andreas.cadas` mit `module-info.java` |
| macOS-Pakete | `jpackage` über `packageMacOsAppImage` / `packageMacOsDmg` |

Test-JVM-Arg: `--enable-native-access=ALL-UNNAMED` (unterdrückt Glass-Nativelibrary-Warnung).

## Dateiformate

* **`.cadas`**: Gebäude-Datei (intern DXF-Format, UI-Endung `.cadas`). Laden über „Laden", Sichern über „Sichern"/„Sichern als ...". Etagen laden/sichern analog über „Etage laden"/„Etage sichern"/„Etage sichern als ...".
* **`.dxf`**: Auch `.cadas`-Dateien können als `.dxf` gelesen werden, da das Format identisch ist. Etagen-Export/Import nutzt ebenfalls `.cadas`.
* **`.cadasparts`**: Externe Teilebibliothek (textbasiert).
* **`.cadasbelag`**: Eigene Belags-Presets unter `~/.config/CADas/Belag`.
* **`.blocks`**: Begleitkatalog für DWG-Blöcke.
* **`.md`**: Materiallisten-Export.

## Session-relevante Festlegungen (aktuell)

* Etagen lassen sich umbenennen und umsortieren („Etage hoch" = größerer Index = im Gebäude nach oben).
* Innenansicht: Bodenklick wechselt durch Türen/Wände in den Nachbarräum.
* 3D-Wände und Fundamente enden bündig mit der Wandachse (keine Verlängerung über die Endpunkte hinaus).
* `EdgeResizeService` ignoriert Nicht-UUID-Element-IDs (z. B. `wall-support-<uuid>`).
* Menüleiste nutzt `setUseSystemMenuBar(true)` und ist unter macOS ausgeblendet (`setManaged(false)`/`setVisible(false)`).
* Hilfe zeigt die vollständige Benutzerdokumentation aus `src/main/resources/docs/benutzerdoku.md`; Keymap ist separater Menüpunkt.
* `./gradlew run` ist deaktiviert; `runApp` startet das App-Bundle.
* `./gradlew macosInstall` baut das App-Bundle + DMG und installiert CADas.app direkt nach /Applications (überschreibt bestehende Version).

## Wo steht was

| Thema | Datei |
|---|---|
| Projektregeln und Entscheidungen | `AGENTS.md` |
| Systemarchitektur und Verantwortlichkeiten | `docs/systemdoku.md` |
| Benutzerdokumentation (auch als In-App-Hilfe) | `docs/benutzerdoku.md` → `src/main/resources/docs/benutzerdoku.md` |
| Arbeitspakete | `TODO.md` |
| Entsprechene Entscheidungen und ihr Stand | `FRAGEN.md` |
| Build-Konfiguration | `build.gradle.kts` |
| Modul-Deskriptor | `src/main/java/module-info.java` |

## Test-Konventionen

* Tests in `src/test/java`, gleiche Paketstruktur wie Hauptcode.
* Fachliche Unit-Tests mit JUnit 5 Jupiter.
* JavaFX-Tests laufen über `aufFxThread()`-Helper in `CadWorkbenchTest`.
* Automation-Tests über `AutomationBridgeServer` (HTTP auf `127.0.0.1:17845`).
* Zielabdeckung: 85 % mit fachlich sinnvollen Tests und Randfällen.