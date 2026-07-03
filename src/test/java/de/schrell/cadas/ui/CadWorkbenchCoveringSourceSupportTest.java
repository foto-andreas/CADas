package de.schrell.cadas.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CadWorkbenchCoveringSourceSupportTest {

    @Test
    void liestDwgBibliothekUndBlockAusQuelle() {
        String source = "/tmp/Bibliothek.dwg#Fenster 120";

        assertEquals(Optional.of(Path.of("/tmp/Bibliothek.dwg")), CadWorkbenchCoveringSourceSupport.extractDwgLibraryPath(source));
        assertEquals(Optional.of("Fenster 120"), CadWorkbenchCoveringSourceSupport.extractDwgBlockName(source));
        assertEquals("Bibliothek.dwg → Fenster 120", CadWorkbenchCoveringSourceSupport.formatCoveringSourceLabel(source));
    }

    @Test
    void formatiertEigenesPresetKurz() {
        assertEquals(
                "Eigenes Preset: Bad",
                CadWorkbenchCoveringSourceSupport.formatCoveringSourceLabel("/tmp/Bad.cadasbelag")
        );
    }

    @Test
    void ignoriertLeereUndNichtDwgQuellen() {
        assertEquals(Optional.empty(), CadWorkbenchCoveringSourceSupport.extractDwgLibraryPath(""));
        assertEquals(Optional.empty(), CadWorkbenchCoveringSourceSupport.extractDwgBlockName("ohne-block"));
        assertEquals("Quelle | DWG/Block", CadWorkbenchCoveringSourceSupport.formatCoveringSourceLabel("Quelle | DWG/Block"));
    }
}
