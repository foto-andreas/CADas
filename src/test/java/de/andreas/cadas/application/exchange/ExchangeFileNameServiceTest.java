package de.andreas.cadas.application.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ExchangeFileNameServiceTest {

    private final ExchangeFileNameService fileNameService = new ExchangeFileNameService();

    @Test
    void sorgtFuerGenauEineDxfExtension() {
        assertEquals(
                Path.of("Erdgeschoss.dxf"),
                fileNameService.ensureSingleExtension(Path.of("Erdgeschoss.dxf.dxf"), ".dxf")
        );
        assertEquals(
                Path.of("Erdgeschoss.dxf"),
                fileNameService.ensureSingleExtension(Path.of("Erdgeschoss"), ".dxf")
        );
    }

    @Test
    void entferntMehrfachAnhangeBeimEtagennamen() {
        assertEquals("Erdgeschoss", fileNameService.stripRepeatedExtension(Path.of("Erdgeschoss.dxf.dxf"), ".dxf"));
        assertEquals("Haus", fileNameService.stripRepeatedExtension(Path.of("Haus.DXF"), ".dxf"));
    }
}
