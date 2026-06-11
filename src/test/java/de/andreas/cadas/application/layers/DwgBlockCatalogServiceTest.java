package de.andreas.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DwgBlockCatalogServiceTest {

    private final DwgBlockCatalogService service = new DwgBlockCatalogService();

    @TempDir
    Path tempDir;

    @Test
    void liestOptionaleBlockKatalogeNebenDerDwgEin() throws Exception {
        Path dwg = tempDir.resolve("bibliothek.dwg");
        Files.writeString(dwg, "Dummy");
        Files.writeString(tempDir.resolve("bibliothek.blocks"), """
                # Kommentar
                Rigips_1250x2000
                OSB_2500x675

                Rigips_1250x2000
                """);

        List<String> blockNames = service.loadCatalog(dwg);

        assertEquals(List.of("Rigips_1250x2000", "OSB_2500x675"), blockNames);
    }
}
