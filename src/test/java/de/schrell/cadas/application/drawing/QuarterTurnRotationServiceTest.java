package de.schrell.cadas.application.drawing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.application.heating.HeatingCircuitRoutingService;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuarterTurnRotationServiceTest {

    private final QuarterTurnRotationService service = new QuarterTurnRotationService();

    @Test
    void drehtWaendeRaeumeUndTreppenViertelweise() {
        Level level = new Level("Erdgeschoss");
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 2000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        Staircase staircase = Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(500, 500),
                new PlanPoint(1500, 3500),
                Length.of(2.8, LengthUnit.METER),
                16
        );
        level.addWall(wall);
        level.addRoom(room);
        level.addStaircase(staircase);

        QuarterTurnRotationService.RotationResult result = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.WALL, level.name(), wall.id().toString()),
                new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString()),
                new SelectionKey(RenderableKind.STAIR, level.name(), staircase.id().toString())
        ), true);

        assertTrue(result.changed());
        assertEquals(2000.0, result.walls().getFirst().axis().start().xMillimeters(), 0.001);
        assertEquals(2000.0, result.walls().getFirst().axis().end().xMillimeters(), 0.001);
        assertEquals(2000.0, result.rooms().getFirst().centerPoint().xMillimeters(), 0.001);
        assertEquals(1000.0, result.rooms().getFirst().centerPoint().yMillimeters(), 0.001);
        assertEquals(1, result.staircases().getFirst().rotationQuarterTurns());
    }

    @Test
    void drehtAuchDieNiedrigeSeiteEinerDachschraegeMit() {
        Level level = new Level("Dachgeschoss");
        Room room = Room.rectangular(
                "Dachzimmer",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 2000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(SlopedCeilingSide.NORTH, Length.of(1.0, LengthUnit.METER))
        );
        level.addRoom(room);

        QuarterTurnRotationService.RotationResult result = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.ROOM_VOLUME, level.name(), room.id().toString())
        ), true);

        assertEquals(SlopedCeilingSide.EAST, result.rooms().getFirst().slopedCeilingProfile().orElseThrow().lowSide());
    }

    @Test
    void drehtObjekteAusgehendVonFreiemWinkel() {
        Level level = new Level("Außenbereich");
        RoomObject roomObject = RoomObject.create(
                "tisch",
                "Tisch",
                RoomObjectType.TABLE,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(1000, 1000),
                Length.of(160, LengthUnit.CENTIMETER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(75, LengthUnit.CENTIMETER),
                25.0,
                RoomObjectMountingMode.STANDS_ON_COVERING,
                ""
        );
        level.addRoomObject(roomObject);

        QuarterTurnRotationService.RotationResult result = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.ROOM_OBJECT, level.name(), roomObject.id().toString())
        ), false);

        assertTrue(result.changed());
        assertEquals(295.0, result.roomObjects().getFirst().rotationDegrees(), 0.001);
    }

    @Test
    void drehtAusgewaehlteHeizkreisRechteckeMitSprachrouting() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = HydronicHeating.create(
                java.util.UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.VARIO,
                Length.of(10, LengthUnit.CENTIMETER),
                Length.of(1.6, LengthUnit.CENTIMETER),
                Length.of(80, LengthUnit.METER),
                Length.of(10, LengthUnit.CENTIMETER),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        );
        HeatingZone zone = new HeatingCircuitRoutingService().regenerate(new HeatingZone(
                java.util.UUID.randomUUID(),
                "HK 1",
                List.of(
                        new PlanPoint(1_000, 1_000),
                        new PlanPoint(3_000, 1_000),
                        new PlanPoint(3_000, 2_000),
                        new PlanPoint(1_000, 2_000)
                ),
                HeatingLayoutPattern.VARIO,
                false
        ), heating);
        level.addHydronicHeating(heating.withZones(List.of(zone)));

        QuarterTurnRotationService.RotationResult result = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.HEATING_ZONE, level.name(), zone.id().toString())
        ), true);

        HeatingZone rotated = result.hydronicHeatings().getFirst().zones().getFirst();
        assertTrue(result.changed());
        assertEquals(2_000_000.0, rotated.areaSquareMillimeters(), 0.001);
        assertTrue(rotated.hasRoutingCommands());
        assertEquals(1_500.0, rotated.outline().getFirst().xMillimeters(), 0.001);
    }

    @Test
    void drehtHeizkreisRoutingAuchBeiZweiVierteldrehungenWeiter() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = HydronicHeating.create(
                java.util.UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.VARIO,
                Length.of(10, LengthUnit.CENTIMETER),
                Length.of(1.6, LengthUnit.CENTIMETER),
                Length.of(80, LengthUnit.METER),
                Length.of(10, LengthUnit.CENTIMETER),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        );
        HeatingZone zone = new HeatingCircuitRoutingService().regenerate(new HeatingZone(
                java.util.UUID.randomUUID(),
                "HK 1",
                List.of(
                        new PlanPoint(1_000, 1_000),
                        new PlanPoint(3_000, 1_000),
                        new PlanPoint(3_000, 2_000),
                        new PlanPoint(1_000, 2_000)
                ),
                HeatingLayoutPattern.VARIO,
                false
        ), heating);
        level.addHydronicHeating(heating.withZones(List.of(zone)));
        var originalRouting = new HeatingCircuitRoutingService().placedRoutingResult(zone, heating);

        QuarterTurnRotationService.RotationResult first = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.HEATING_ZONE, level.name(), zone.id().toString())
        ), true);
        level.replaceHydronicHeatings(first.hydronicHeatings());
        QuarterTurnRotationService.RotationResult second = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.HEATING_ZONE, level.name(), zone.id().toString())
        ), true);

        HeatingZone rotatedTwice = second.hydronicHeatings().getFirst().zones().getFirst();
        var rotatedRouting = new HeatingCircuitRoutingService().placedRoutingResult(rotatedTwice, second.hydronicHeatings().getFirst());
        assertTrue(rotatedTwice.routingQuarterTurns() != 0
                || rotatedTwice.routingMirroredHorizontally()
                || rotatedTwice.routingMirroredVertically());
        assertTrue(Math.abs(originalRouting.supplyPath().endPoint().xMillimeters()
                - rotatedRouting.supplyPath().endPoint().xMillimeters()) > 0.001
                || Math.abs(originalRouting.supplyPath().endPoint().yMillimeters()
                - rotatedRouting.supplyPath().endPoint().yMillimeters()) > 0.001);
    }

    @Test
    void drehtHkvAnschlusspaarUndFreiflaeche() {
        Level level = new Level("Erdgeschoss");
        HydronicHeating heating = HydronicHeating.create(
                java.util.UUID.randomUUID(),
                HeatingSurfacePosition.FLOOR,
                HeatingLayoutPattern.VARIO,
                Length.of(10, LengthUnit.CENTIMETER),
                Length.of(1.6, LengthUnit.CENTIMETER),
                Length.of(80, LengthUnit.METER),
                Length.of(10, LengthUnit.CENTIMETER),
                new PlanPoint(0, 0),
                new PlanPoint(50, 0)
        ).withManifoldFreeArea(Length.ofMillimeters(600), Length.ofMillimeters(1_000));
        level.addHydronicHeating(heating);

        QuarterTurnRotationService.RotationResult result = service.rotate(level, Set.of(
                new SelectionKey(RenderableKind.HEATING_MANIFOLD, level.name(), heating.id().toString())
        ), true);

        HydronicHeating rotated = result.hydronicHeatings().getFirst();
        assertTrue(result.changed());
        assertEquals(25.0, rotated.supplyPoint().xMillimeters(), 0.001);
        assertEquals(25.0, rotated.supplyPoint().yMillimeters(), 0.001);
        assertEquals(25.0, rotated.returnPoint().xMillimeters(), 0.001);
        assertEquals(-25.0, rotated.returnPoint().yMillimeters(), 0.001);
        assertEquals(1_000.0, rotated.manifoldFreeAreaWidth().toMillimeters(), 0.001);
        assertEquals(600.0, rotated.manifoldFreeAreaDepth().toMillimeters(), 0.001);
    }
}
