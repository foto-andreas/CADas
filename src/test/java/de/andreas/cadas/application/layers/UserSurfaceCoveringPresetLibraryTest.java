package de.andreas.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.model.SurfaceCutRestriction;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserSurfaceCoveringPresetLibraryTest {

    @TempDir
    Path tempDir;

    @Test
    void speichertUndLaedtBelagspresetMitAllenVerlegewerten() throws Exception {
        UserSurfaceCoveringPresetLibrary library = new UserSurfaceCoveringPresetLibrary(tempDir);
        SurfaceCoveringPreset preset = new SurfaceCoveringPreset(
                "keramik-terracotta",
                "Keramik Terracotta",
                Length.of(9, LengthUnit.MILLIMETER),
                Length.of(45, LengthUnit.CENTIMETER),
                Length.of(90, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.FIXED,
                Length.of(22.5, LengthUnit.CENTIMETER),
                Length.of(7, LengthUnit.CENTIMETER),
                Length.of(6, LengthUnit.CENTIMETER),
                Length.of(12, LengthUnit.CENTIMETER),
                Length.of(3, LengthUnit.MILLIMETER),
                SurfaceCutRestriction.OUTER_CUTS_ROTATABLE,
                "Standard: Test"
        );

        SurfaceCoveringPreset savedPreset = library.savePreset(preset, false);
        List<SurfaceCoveringPreset> loadedPresets = library.loadPresets();

        assertEquals(1, loadedPresets.size());
        SurfaceCoveringPreset loadedPreset = loadedPresets.getFirst();
        assertEquals(savedPreset.coveringSource(), loadedPreset.coveringSource());
        assertEquals("Keramik Terracotta", loadedPreset.name());
        assertEquals(Length.of(9, LengthUnit.MILLIMETER), loadedPreset.thickness());
        assertEquals(Length.of(45, LengthUnit.CENTIMETER), loadedPreset.tileWidth());
        assertEquals(Length.of(90, LengthUnit.CENTIMETER), loadedPreset.tileHeight());
        assertEquals(SurfaceLayoutMode.FIXED, loadedPreset.layoutMode());
        assertEquals(Length.of(22.5, LengthUnit.CENTIMETER), loadedPreset.offset());
        assertEquals(Length.of(7, LengthUnit.CENTIMETER), loadedPreset.minimumOffset());
        assertEquals(Length.of(6, LengthUnit.CENTIMETER), loadedPreset.minimumEdgeWidth());
        assertEquals(Length.of(12, LengthUnit.CENTIMETER), loadedPreset.minimumStartEndMargin());
        assertEquals(Length.of(3, LengthUnit.MILLIMETER), loadedPreset.jointWidth());
        assertEquals(SurfaceCutRestriction.OUTER_CUTS_ROTATABLE, loadedPreset.cutRestriction());
    }

    @Test
    void verhindertÜberschreibenOhneFreigabeUndErsetztMitFreigabe() throws Exception {
        UserSurfaceCoveringPresetLibrary library = new UserSurfaceCoveringPresetLibrary(tempDir);
        SurfaceCoveringPreset ersterBelag = preset("Naturstein", Length.of(12, LengthUnit.MILLIMETER));
        SurfaceCoveringPreset geänderterBelag = preset("Naturstein", Length.of(18, LengthUnit.MILLIMETER));

        library.savePreset(ersterBelag, false);

        assertThrows(FileAlreadyExistsException.class, () -> library.savePreset(geänderterBelag, false));

        library.savePreset(geänderterBelag, true);
        assertEquals(Length.of(18, LengthUnit.MILLIMETER), library.loadPresets().getFirst().thickness());
    }

    @Test
    void übernimmtDwgBibliothekMitBlockkatalogInBelagsverzeichnis() throws Exception {
        UserSurfaceCoveringPresetLibrary library = new UserSurfaceCoveringPresetLibrary(tempDir.resolve("ziel"));
        Path sourceDirectory = tempDir.resolve("quelle");
        Files.createDirectories(sourceDirectory);
        Path sourceDwg = sourceDirectory.resolve("Partner.dwg");
        Path sourceCatalog = sourceDirectory.resolve("Partner.blocks");
        Files.writeString(sourceDwg, "DWG-Platzhalter");
        Files.writeString(sourceCatalog, "Fliese_A");

        Path copiedDwg = library.copyCadLibrary(sourceDwg, false);

        assertTrue(Files.exists(copiedDwg));
        assertTrue(Files.exists(copiedDwg.resolveSibling("Partner.blocks")));
        assertEquals(List.of(copiedDwg), library.loadCadLibraries());
    }

    private SurfaceCoveringPreset preset(String name, Length thickness) {
        return new SurfaceCoveringPreset(
                "test-" + name.toLowerCase(),
                name,
                thickness,
                Length.of(30, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.AUTOMATIC,
                Length.zero(),
                Length.of(5, LengthUnit.CENTIMETER),
                Length.of(5, LengthUnit.CENTIMETER),
                Length.of(5, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.MILLIMETER),
                ""
        );
    }
}
