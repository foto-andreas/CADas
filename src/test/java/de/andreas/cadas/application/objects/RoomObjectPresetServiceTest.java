package de.andreas.cadas.application.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.model.RoomObjectShape;
import de.andreas.cadas.domain.model.RoomObjectType;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RoomObjectPresetServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void liefertFachlicheStandardobjekte() {
        RoomObjectPresetService service = new RoomObjectPresetService(tempDir);

        var presets = service.defaults();

        assertTrue(presets.stream().anyMatch(preset -> preset.type() == RoomObjectType.TOILET));
        assertTrue(presets.stream().anyMatch(preset -> preset.shape() == RoomObjectShape.HALF_ROUND));
        assertTrue(presets.stream().anyMatch(RoomObjectPreset::cutsFloorCovering));
    }

    @Test
    void importiertDwgDateienAusObjektverzeichnisAlsPresets() throws Exception {
        Files.writeString(tempDir.resolve("Toilette.dwg"), "DWG");
        Files.writeString(tempDir.resolve("Notiz.txt"), "kein Objekt");
        RoomObjectPresetService service = new RoomObjectPresetService(tempDir);

        var presets = service.loadDwgPresets();

        assertEquals(1, presets.size());
        assertEquals("DWG-Objekt: Toilette", presets.getFirst().name());
        assertEquals(RoomObjectType.DWG_REFERENCE, presets.getFirst().type());
        assertTrue(presets.getFirst().source().endsWith("Toilette.dwg"));
    }
}
