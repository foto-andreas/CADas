package de.schrell.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.application.dwg.DwgBlockDefinition;
import de.schrell.cadas.application.dwg.DwgBounds;
import de.schrell.cadas.application.dwg.DwgUnit;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class SurfaceCoveringPresetServiceTest {

    private final SurfaceCoveringPresetService service = new SurfaceCoveringPresetService();

    @Test
    void enthältVariothermTrockenbauplatteAlsBodenbelag() {
        SurfaceCoveringPreset preset = service.defaults().stream()
                .filter(candidate -> candidate.id().equals("variotherm-trockenbau-fbh-60x100"))
                .findFirst()
                .orElseThrow();

        assertEquals("Variotherm Trockenbau-FBH-Platte 60 x 100 cm", preset.name());
        assertEquals(18.0, preset.thickness().toMillimeters(), 0.001);
        assertEquals(600.0, preset.tileWidth().toMillimeters(), 0.001);
        assertEquals(1_000.0, preset.tileHeight().toMillimeters(), 0.001);
        assertEquals(SurfaceCoveringPresetService.VARIOTHERM_DRY_PANEL_SOURCE, preset.coveringSource());
    }

    @Test
    void leitetBelagsPresetAusDwgBlockmaßenAb() {
        DwgBlockDefinition block = new DwgBlockDefinition(
                Path.of("/tmp/Bibliothek.dwg"),
                "OSB 2500x675",
                DwgUnit.MILLIMETER,
                0.0,
                0.0,
                new DwgBounds(0.0, 0.0, 2500.0, 675.0),
                List.of("OSB"),
                List.of("1A"),
                List.of(),
                1,
                0,
                List.of()
        );

        SurfaceCoveringPreset preset = service.fromDwgBlock(block);

        assertEquals("DWG-Belag: OSB 2500x675", preset.name());
        assertEquals(2500.0, preset.tileWidth().toMillimeters(), 0.001);
        assertEquals(675.0, preset.tileHeight().toMillimeters(), 0.001);
        assertEquals(100.0, preset.minimumEdgeWidth().toMillimeters(), 0.001);
        assertEquals("/tmp/Bibliothek.dwg#OSB 2500x675", preset.coveringSource());
    }
}
