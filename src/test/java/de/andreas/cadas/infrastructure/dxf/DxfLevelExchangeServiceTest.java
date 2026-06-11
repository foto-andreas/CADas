package de.andreas.cadas.infrastructure.dxf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SlopedCeilingProfile;
import de.andreas.cadas.domain.model.SlopedCeilingSide;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.nio.file.Path;
import java.nio.file.Files;

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
        level.addStaircase(new Staircase(
                java.util.UUID.randomUUID(),
                StairType.SWITCHBACK,
                new PlanPoint(500, 500),
                new PlanPoint(2200, 4200),
                Length.of(2.9, LengthUnit.METER),
                18,
                1
        ));

        Path file = tempDir.resolve("grundriss.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(1, imported.walls().size());
        assertEquals(1, imported.rooms().size());
        assertEquals(1, imported.doors().size());
        assertEquals(1, imported.windows().size());
        assertEquals(1, imported.staircases().size());
        assertEquals("Küche", imported.rooms().getFirst().name());
        assertEquals(14.0, imported.rooms().getFirst().areaSquareMeters(), 0.001);
        assertEquals(StairType.SWITCHBACK, imported.staircases().getFirst().stairType());
        assertEquals(1, imported.staircases().getFirst().rotationQuarterTurns());
    }

    @Test
    void exportiertMetrischeHeaderUndModelSpaceKennung() throws Exception {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(1000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addDoor(Door.create(
                wall.id(),
                Length.of(10, LengthUnit.CENTIMETER),
                Length.of(80, LengthUnit.CENTIMETER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero()
        ));
        level.addWindow(WindowElement.create(
                wall.id(),
                Length.of(10, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.20, LengthUnit.METER)
        ));
        level.addStaircase(Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(0, 0),
                new PlanPoint(1000, 2000),
                Length.of(2.75, LengthUnit.METER),
                14
        ));

        Path file = tempDir.resolve("header.dxf");
        exchangeService.exportLevel(level, file);
        String dxf = Files.readString(file);

        assertTrue(dxf.contains("$INSUNITS\n70\n4"));
        assertTrue(dxf.contains("$MEASUREMENT\n70\n1"));
        assertTrue(dxf.contains("\n67\n0\n410\nModel\n"));
        assertTrue(dxf.contains("\n2\nTABLES\n"));
        assertTrue(dxf.contains("\n2\nBLOCKS\n"));
        assertTrue(dxf.contains("\n2\nOBJECTS\n"));
        assertTrue(dxf.contains("\n2\nLAYER\n"));
        assertTrue(dxf.contains("\n2\nBLOCK_RECORD\n"));
        assertTrue(dxf.contains("\n0\nINSERT\n"));
        assertTrue(dxf.contains("\n2\nCADAS_DOOR\n"));
        assertTrue(dxf.contains("\n2\nCADAS_WINDOW\n"));
        assertTrue(dxf.contains("\n2\nCADAS_STAIR\n"));
        assertTrue(dxf.contains("\n0\nDICTIONARY\n"));
        assertTrue(dxf.contains("\n0\nLAYOUT\n"));
    }

    @Test
    void exportiertUndImportiertRaeumeMitDachschraege() throws Exception {
        Level level = new Level("Dachgeschoss");
        level.addRoom(Room.rectangular(
                "Studio",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3000),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(SlopedCeilingSide.SOUTH, Length.of(1.1, LengthUnit.METER))
        ));

        Path file = tempDir.resolve("dachgeschoss.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(SlopedCeilingSide.SOUTH, imported.rooms().getFirst().slopedCeilingProfile().orElseThrow().lowSide());
        assertEquals(1100.0, imported.rooms().getFirst().slopedCeilingProfile().orElseThrow().kneeWallHeight().toMillimeters(), 0.001);
    }

    @Test
    void exportiertUndImportiertWandEndpunktHoehenUndPolygonaleDecken() throws Exception {
        Level level = new Level("Dachgeschoss");
        level.addWall(new Wall(
                java.util.UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(2.4, LengthUnit.METER),
                Length.of(3.1, LengthUnit.METER)
        ));
        level.addRoom(new Room(
                java.util.UUID.randomUUID(),
                "Ausbau",
                java.util.List.of(
                        new PlanPoint(0, 0),
                        new PlanPoint(4000, 0),
                        new PlanPoint(4000, 3000),
                        new PlanPoint(0, 3000)
                ),
                Length.of(3.1, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                null,
                java.util.List.of(
                        Length.of(2.4, LengthUnit.METER),
                        Length.of(3.1, LengthUnit.METER),
                        Length.of(3.1, LengthUnit.METER),
                        Length.of(2.6, LengthUnit.METER)
                )
        ));

        Path file = tempDir.resolve("decke.dxf");
        exchangeService.exportLevel(level, file);
        Level imported = exchangeService.importLevel(file, "Import");

        assertEquals(2400.0, imported.walls().getFirst().startHeight().toMillimeters(), 0.001);
        assertEquals(3100.0, imported.walls().getFirst().endHeight().toMillimeters(), 0.001);
        assertEquals(4, imported.rooms().getFirst().ceilingVertexHeights().size());
        assertEquals(2600.0, imported.rooms().getFirst().ceilingVertexHeights().get(3).toMillimeters(), 0.001);
    }
}
