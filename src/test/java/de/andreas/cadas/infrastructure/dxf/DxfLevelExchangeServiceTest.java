package de.andreas.cadas.infrastructure.dxf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DxfLevelExchangeServiceTest {

    private final DxfLevelExchangeService exchangeService = new DxfLevelExchangeService();

    @TempDir
    Path tempDir;

    @Test
    void exportiertUndImportiertEinenLevelMitAllenGrundobjekten() throws Exception {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addRoom(Room.rectangular(
                "Küche",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3500),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        level.addDoor(Door.create(
                wall.id(),
                Length.of(1.0, LengthUnit.METER),
                Length.of(1.01, LengthUnit.METER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero()
        ));
        level.addWindow(WindowElement.create(
                wall.id(),
                Length.of(2.0, LengthUnit.METER),
                Length.of(1.2, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        ));

        Path file = tempDir.resolve("grundriss.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(1, imported.walls().size());
        assertEquals(1, imported.rooms().size());
        assertEquals(1, imported.doors().size());
        assertEquals(1, imported.windows().size());
        assertEquals("Küche", imported.rooms().getFirst().name());
        assertEquals(14.0, imported.rooms().getFirst().areaSquareMeters(), 0.001);
    }
}
