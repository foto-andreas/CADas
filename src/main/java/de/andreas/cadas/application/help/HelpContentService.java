package de.andreas.cadas.application.help;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class HelpContentService {

    private static final String BENUTZERDOKU_PFAD = "/docs/benutzerdoku.md";

    public String createMarkdown() {
        return ladeRessource(BENUTZERDOKU_PFAD);
    }

    public String createKeymapMarkdown() {
        return """
                # Tastaturkürzel und Mausbedienung

                ## Tastaturkürzel

                | Eingabe | Aktion |
                |---|---|
                | F1 | Hilfe öffnen |
                | ⌘/Strg+S | Gebäude sichern |
                | ⌘/Strg+Umschalt+S | Gebäude sichern unter ... |
                | ⌘/Strg+Z | Rückgängig |
                | ⌘/Strg+Umschalt+Z | Wiederherstellen |
                | ⌘/Strg+N | Etage hinzufügen |
                | ⌘/Strg+L | Projekt leeren |
                | ⌘/Strg+Umschalt+I | Gebäude laden |
                | ⌘/Strg+Umschalt+B | Teilebibliothek laden |
                | ⌘/Strg+Umschalt+P | Bauzeichnung als PDF exportieren |
                | ⌘/Strg+Umschalt+→ | Auswahl 90° rechts drehen |
                | ⌘/Strg+Umschalt+← | Auswahl 90° links drehen |
                | Entf | Auswahl löschen |
                | Esc | Auswahl aufheben oder Aktion abbrechen |
                | ⌘/Strg+0 | 2D-Ansicht zentrieren |
                | ⌘/Strg+Umschalt+0 | 3D-Ansicht zentrieren |
                | E | Werkzeug Bearbeiten |
                | W | Werkzeug Wand |
                | T | Werkzeug Treppe |
                | D | Werkzeug Tür |
                | F | Werkzeug Fenster |
                | O | Werkzeug Objekt |
                | G | Werkzeug Fußbodenverlängerung |

                ## Mausbedienung

                | Eingabe | Aktion |
                |---|---|
                | Linke Maustaste + Ziehen | Zeichnen oder Auswahl verschieben |
                | Rechte Maustaste + Ziehen | 2D-Ansicht verschieben |
                | Mittlere Maustaste + Ziehen | 2D-Ansicht verschieben |
                | Leertaste + Ziehen | 2D-Ansicht verschieben, auch über Bauteilen |
                | Mausrad | 2D zoomen |
                | Umschalt beim Wandzeichnen | Freien Winkel zulassen |
                | Alt + Linksklick | Zwischen übereinanderliegenden Treffern wechseln |
                | Alt + rechte Maustaste | Nahe Hilfslinie entfernen |
                | Ziehen an rundem Handle | Endpunkt verschieben |
                | Ziehen an eckigem Handle | Bauteilkante verschieben und einrasten |
                | Rechtsklick auf Auswahl | Kontextmenü öffnen |

                ## 3D-Navigation

                | Eingabe | Aktion |
                |---|---|
                | Linke Maustaste + Ziehen | Modell um die Mitte drehen (Orbit) |
                | Rechte Maustaste + Ziehen | Ansicht verschieben (Panning) |
                | Mausrad | Zoomen |
                | Mausrad in Innenansicht | Brennweite/Sichtwinkel anpassen |
                | Rechte Maustaste + Ziehen in Innenansicht | Vor und zurück bewegen |

                Die Keymap kann über die Schaltfläche **Drucken** ausgegeben oder als PDF gedruckt werden.
                """;
    }

    private String ladeRessource(String pfad) {
        try (InputStream stream = HelpContentService.class.getResourceAsStream(pfad)) {
            return new String(Objects.requireNonNull(stream, "Ressource " + pfad + " nicht gefunden.").readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Ressource " + pfad + " konnte nicht gelesen werden.", exception);
        }
    }
}