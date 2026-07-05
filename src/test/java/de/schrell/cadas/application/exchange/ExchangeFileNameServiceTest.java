package de.schrell.cadas.application.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ExchangeFileNameServiceTest {

    @Test
    void sorgtFuerGenauEineDxfExtension() {
        assertEquals(
                Path.of("Erdgeschoss.dxf"),
                ExchangeFileNameService.ensureSingleExtension(Path.of("Erdgeschoss.dxf.dxf"), ".dxf")
        );
        assertEquals(
                Path.of("Erdgeschoss.dxf"),
                ExchangeFileNameService.ensureSingleExtension(Path.of("Erdgeschoss"), ".dxf")
        );
        assertEquals(
                Path.of("Materialliste.md"),
                ExchangeFileNameService.ensureSingleExtension(Path.of("Materialliste.md.md"), ".md")
        );
    }

    @Test
    void entferntMehrfachAnhangeBeimEtagennamen() {
        assertEquals("Erdgeschoss", ExchangeFileNameService.stripRepeatedExtension(Path.of("Erdgeschoss.dxf.dxf"), ".dxf"));
        assertEquals("Haus", ExchangeFileNameService.stripRepeatedExtension(Path.of("Haus.DXF"), ".dxf"));
    }
}
