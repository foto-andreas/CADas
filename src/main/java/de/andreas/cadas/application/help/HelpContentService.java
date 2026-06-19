package de.andreas.cadas.application.help;

public final class HelpContentService {

    public String createMarkdown() {
        return """
                # CADas-Hilfe

                ## Aktuelle Möglichkeiten

                * Grundrisse mit Etagen, Wänden, Türen, Fenstern, Räumen, Treppen und Raumobjekten bearbeiten
                * Räume automatisch oder aus ausgewählten Wänden erkennen
                * Raster, Endpunkte, Hilfslinien und Wände als Fangziele verwenden
                * 2D-, 3D- und Innenansicht gemeinsam mit der Auswahl nutzen
                * Gebäude und einzelne Etagen als DXF importieren und exportieren
                * Oberflächenebenen und Materiallisten einschließlich Raum- und Mietflächen verwalten

                ## Keymap

                | Eingabe | Aktion |
                |---|---|
                | F1 | Diese Hilfe öffnen |
                | ⌘/Strg+Z | Rückgängig |
                | ⌘/Strg+Umschalt+Z | Wiederherstellen |
                | Entf | Auswahl löschen |
                | Esc | Auswahl aufheben oder Aktion abbrechen |
                | ⌘/Strg+0 | 2D-Ansicht zentrieren |
                | E / W / T / D / F / O | Bearbeiten / Wand / Treppe / Tür / Fenster / Objekt |
                | Umschalt beim Wandzeichnen | Freien Winkel zulassen |
                | Alt beim Klicken | Alternative Pick-Aktion anzeigen |
                | Leertaste+Ziehen | 2D-Ansicht verschieben, auch über Bauteilen |
                | Mittlere oder rechte Maustaste+Ziehen | 2D-Ansicht verschieben |
                | Mausrad | 2D zoomen |
                | Ziehen an rundem Handle | Endpunkt verschieben |
                | Ziehen an eckigem Handle | Bauteilkante verschieben und einrasten |

                Die Keymap kann über die Schaltfläche **Drucken** ausgegeben oder als PDF gedruckt werden.
                """;
    }
}
