package de.schrell.cadas.application.heating;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HydronicHeatingLayoutServiceTest {

    private final HydronicHeatingLayoutService service = new HydronicHeatingLayoutService();

    @Test
    void verlegtMeanderZwischenVorlaufUndRücklauf() {
        Room room = rectangularRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 80_000)
                .withZones(List.of(HeatingZone.create("Heizkreis 1", room.outline())));

        HydronicHeatingLayoutService.CircuitLayout circuit = service.layout(heating).getFirst();

        assertEquals(heating.supplyPoint(), circuit.pipePath().getFirst());
        assertEquals(heating.returnPoint(), circuit.pipePath().getLast());
        assertEquals(100.0, circuit.bendRadius().toMillimeters(), 0.001);
        assertTrue(circuit.pipeLength().toMillimeters() > 40_000);
    }

    @Test
    void erzeugtFürSchneckeAnderenRohrverlauf() {
        Room room = rectangularRoom();
        HydronicHeating meander = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 80_000)
                .withZones(List.of(HeatingZone.create("Heizkreis 1", room.outline())));
        HydronicHeating spiral = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.SPIRAL, 80_000)
                .withZones(List.of(HeatingZone.create("Heizkreis 1", room.outline(), HeatingLayoutPattern.SPIRAL)));

        List<PlanPoint> meanderPath = service.layout(meander).getFirst().pipePath();
        List<PlanPoint> spiralPath = service.layout(spiral).getFirst().pipePath();

        assertFalse(spiralPath.isEmpty());
        assertNotEquals(meanderPath, spiralPath);
        assertNoSelfIntersections(spiralPath.subList(1, spiralPath.size() - 1));
    }

    @Test
    void teiltRaumBisZurMaximalenRohrlängeInHeizkreise() {
        Room room = rectangularRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 35_000);

        HydronicHeatingLayoutService.PlanningResult result = service.suggest(room, heating);

        assertTrue(result.heating().zones().size() > 1);
        assertEquals(result.heating().zones().size(), result.circuits().size());
        assertTrue(result.circuits().stream()
                .allMatch(circuit -> circuit.pipeLength().compareTo(heating.maximumPipeLength()) <= 0));
    }

    @Test
    void liefertTeilplanungWennEinAnschlussNichtRoutbarBleibt() {
        Room room = rectangularRoom();
        HydronicHeating heating = HydronicHeating.create(
                room.id(), HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER,
                Length.ofMillimeters(200), Length.ofMillimeters(16), Length.ofMillimeters(25_000),
                Length.ofMillimeters(100), new PlanPoint(100, 100), new PlanPoint(300, 100)
        );

        HydronicHeatingLayoutService.PlanningResult result = service.suggest(room, heating);

        assertTrue(result.validationReport().valid());
        assertFalse(result.validationReport().warnings().isEmpty());
        assertTrue(result.validationReport().warnings().stream()
                .anyMatch(warning -> warning.type() == HydronicHeatingLayoutService.ValidationErrorType.PARTIAL_LAYOUT));
        assertEquals(result.heating().zones().size(), result.circuits().size());
        assertTrue(result.circuits().stream()
                .allMatch(circuit -> circuit.pipeLength().compareTo(heating.maximumPipeLength()) <= 0));
    }

    @Test
    void ordnetJedemHeizkreisEinEigenesHkvPaarZu() {
        Room room = rectangularRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 35_000);

        HydronicHeatingLayoutService.PlanningResult result = service.suggest(room, heating);

        assertTrue(result.validationReport().valid());
        assertTrue(result.circuits().size() > 1);
        assertEquals(result.circuits().size(), result.circuits().stream()
                .map(HydronicHeatingLayoutService.CircuitLayout::supplyPort)
                .distinct()
                .count());
        for (HydronicHeatingLayoutService.CircuitLayout circuit : result.circuits()) {
            assertEquals(circuit.supplyPort(), circuit.supplyConnectorPath().getFirst());
            assertEquals(circuit.returnPort(), circuit.returnConnectorPath().getLast());
            assertEquals(circuit.fieldSupplyPath().getFirst(), circuit.supplyConnectorPath().getLast());
            assertEquals(circuit.fieldReturnPath().getLast(), circuit.returnConnectorPath().getFirst());
            assertFalse(circuit.fieldSupplyPath().isEmpty());
            assertFalse(circuit.fieldReturnPath().isEmpty());
        }
    }

    @Test
    void invertiertHeizkreisrollenImLayout() {
        Room room = rectangularRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 300_000)
                .withZones(List.of(HeatingZone.create("Heizkreis 1", room.outline())));
        HydronicHeating inverted = heating.withZones(List.of(heating.zones().getFirst().withFlowInverted(true)));

        HydronicHeatingLayoutService.CircuitLayout normalCircuit = service.layout(heating).getFirst();
        HydronicHeatingLayoutService.CircuitLayout invertedCircuit = service.layout(inverted).getFirst();

        assertEquals(reversed(normalCircuit.fieldReturnPath()), invertedCircuit.fieldSupplyPath());
        assertEquals(reversed(normalCircuit.fieldSupplyPath()), invertedCircuit.fieldReturnPath());
        assertNotEquals(normalCircuit.pipePath(), invertedCircuit.pipePath());
    }

    @Test
    void erzeugtMaßstabsgerechtesSvgMitRinnenUndRollenfarben() {
        Room room = rectangularRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.SPIRAL, 300_000)
                .withZones(List.of(HeatingZone.create("Heizkreis 1", room.outline(), HeatingLayoutPattern.SPIRAL)));

        String svg = service.toSvg(room, heating);

        assertTrue(svg.contains("viewBox=\"-200.000 -200.000 6400.000 4400.000\""));
        assertTrue(svg.contains("id=\"variotherm-rinnen\""));
        assertTrue(svg.contains("r=\"44.200\""));
        assertTrue(svg.contains("class=\"vorlauf\""));
        assertTrue(svg.contains("class=\"ruecklauf\""));
        assertTrue(svg.contains("V1"));
        assertTrue(svg.contains("R1"));
    }

    @Test
    void erhältLFormAlsVeränderbarenHeizbereich() {
        Room room = lShapedRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.CEILING, HeatingLayoutPattern.MEANDER, 200_000);

        HydronicHeatingLayoutService.PlanningResult result = service.suggest(room, heating);

        assertEquals(1, result.heating().zones().size());
        assertEquals(room.outline(), result.heating().zones().getFirst().outline());
        assertTrue(result.circuits().getFirst().pipePath().size() > 4);
    }

    @Test
    void plantBodenUndDeckeGeometrischGleich() {
        Room room = rectangularRoom();
        HydronicHeating floor = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.SPIRAL, 80_000);
        HydronicHeating ceiling = heating(room, HeatingSurfacePosition.CEILING, HeatingLayoutPattern.SPIRAL, 80_000);

        HydronicHeatingLayoutService.PlanningResult floorResult = service.suggest(room, floor);
        HydronicHeatingLayoutService.PlanningResult ceilingResult = service.suggest(room, ceiling);

        assertEquals(
                floorResult.circuits().stream().map(HydronicHeatingLayoutService.CircuitLayout::pipeLength).toList(),
                ceilingResult.circuits().stream().map(HydronicHeatingLayoutService.CircuitLayout::pipeLength).toList()
        );
        assertEquals(
                floorResult.circuits().stream().map(HydronicHeatingLayoutService.CircuitLayout::pipePath).toList(),
                ceilingResult.circuits().stream().map(HydronicHeatingLayoutService.CircuitLayout::pipePath).toList()
        );
    }

    @Test
    void führtRohreAuchInUFormNurDurchDenHeizbereich() {
        Room room = new Room(
                java.util.UUID.randomUUID(), "U-Raum", List.of(
                new PlanPoint(0, 0), new PlanPoint(6_000, 0), new PlanPoint(6_000, 5_000),
                new PlanPoint(4_000, 5_000), new PlanPoint(4_000, 2_000), new PlanPoint(2_000, 2_000),
                new PlanPoint(2_000, 5_000), new PlanPoint(0, 5_000)
        ), Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200), null);
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 300_000)
                .withZones(List.of(HeatingZone.create("U-Heizkreis", room.outline())));

        List<PlanPoint> pipePath = service.layout(heating).getFirst().pipePath();

        for (int index = 2; index + 2 < pipePath.size(); index++) {
            assertSegmentInside(room.outline(), pipePath.get(index - 1), pipePath.get(index));
        }
    }

    @Test
    void spartTreppenbereicheBeimVorschlagenAus() {
        Room room = rectangularRoom();
        Staircase staircase = Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(2_400, 1_000),
                new PlanPoint(3_500, 2_700),
                Length.ofMillimeters(2_800),
                15
        );
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 300_000);

        HydronicHeatingLayoutService.PlanningResult result = service.suggest(room, heating, List.of(staircase));

        assertTrue(result.validationReport().valid());
        assertTrue(result.heating().zones().size() > 1);
        for (HydronicHeatingLayoutService.CircuitLayout circuit : result.circuits()) {
            for (HydronicHeatingLayoutService.PipeSegment segment : circuit.segments()) {
                assertSegmentOutsideStaircase(staircase, segment.start(), segment.end());
            }
        }
    }

    @Test
    void nutztBodenöffnungenAutomatischAlsFbhSperrfläche() {
        Room room = rectangularRoom();
        FloorOpening opening = FloorOpening.create(
                room.id(),
                FloorOpeningShape.RECTANGLE,
                new PlanPoint(3_000, 2_000),
                Length.ofMillimeters(1_200),
                Length.ofMillimeters(1_000)
        );
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 300_000);

        HydronicHeatingLayoutService.PlanningResult result = service.suggest(
                room, heating, List.of(), List.of(opening), List.of()
        );

        assertTrue(result.validationReport().valid());
        assertTrue(result.heating().zones().size() > 1);
        assertNoSegmentInsideRectangle(result.circuits(), 2_400, 1_500, 3_600, 2_500);
    }

    @Test
    void nutztManuelleFbhSperrflächenBeimVorschlagen() {
        Room room = rectangularRoom();
        HeatingExclusionArea exclusionArea = HeatingExclusionArea.create(
                room.id(),
                "Schrank",
                new PlanPoint(2_000, 1_000),
                new PlanPoint(3_000, 2_000)
        );
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.SPIRAL, 300_000);

        HydronicHeatingLayoutService.PlanningResult result = service.suggest(
                room, heating, List.of(), List.of(), List.of(exclusionArea)
        );

        assertTrue(result.validationReport().valid());
        assertTrue(result.heating().zones().size() > 1);
        assertNoSegmentInsideRectangle(result.circuits(), 2_000, 1_000, 3_000, 2_000);
    }

    @Test
    void lehntManuellÜberRaumgrenzeGezogenenHeizbereichAb() {
        Room room = rectangularRoom();
        HydronicHeating heating = heating(room, HeatingSurfacePosition.FLOOR, HeatingLayoutPattern.MEANDER, 300_000)
                .withZones(List.of(HeatingZone.create("Zu groß", List.of(
                        new PlanPoint(100, 100), new PlanPoint(6_100, 100),
                        new PlanPoint(6_100, 3_900), new PlanPoint(100, 3_900)
                ))));

        assertThrows(IllegalArgumentException.class, () -> service.validateZones(room, heating));
    }

    private void assertSegmentInside(List<PlanPoint> outline, PlanPoint start, PlanPoint end) {
        for (int step = 0; step <= 20; step++) {
            double ratio = step / 20.0;
            PlanPoint point = new PlanPoint(
                    start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * ratio,
                    start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * ratio
            );
            assertTrue(contains(outline, point), () -> "Rohr außerhalb bei " + point);
        }
    }

    private void assertSegmentOutsideStaircase(Staircase staircase, PlanPoint start, PlanPoint end) {
        for (int step = 0; step <= 20; step++) {
            double ratio = step / 20.0;
            PlanPoint point = new PlanPoint(
                    start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * ratio,
                    start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * ratio
            );
            assertFalse(
                    point.xMillimeters() > staircase.minX() + 0.001
                            && point.xMillimeters() < staircase.maxX() - 0.001
                            && point.yMillimeters() > staircase.minY() + 0.001
                            && point.yMillimeters() < staircase.maxY() - 0.001,
                    () -> "Rohr im Treppenbereich bei " + point
            );
        }
    }

    private void assertNoSegmentInsideRectangle(
            List<HydronicHeatingLayoutService.CircuitLayout> circuits,
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {
        for (HydronicHeatingLayoutService.CircuitLayout circuit : circuits) {
            for (HydronicHeatingLayoutService.PipeSegment segment : circuit.segments()) {
                for (int step = 0; step <= 20; step++) {
                    double ratio = step / 20.0;
                    PlanPoint point = new PlanPoint(
                            segment.start().xMillimeters()
                                    + (segment.end().xMillimeters() - segment.start().xMillimeters()) * ratio,
                            segment.start().yMillimeters()
                                    + (segment.end().yMillimeters() - segment.start().yMillimeters()) * ratio
                    );
                    assertFalse(
                            point.xMillimeters() > minX + 0.001
                                    && point.xMillimeters() < maxX - 0.001
                                    && point.yMillimeters() > minY + 0.001
                                    && point.yMillimeters() < maxY - 0.001,
                            () -> "Rohr in Sperrfläche bei " + point
                    );
                }
            }
        }
    }

    private boolean contains(List<PlanPoint> polygon, PlanPoint point) {
        boolean inside = false;
        int previousIndex = polygon.size() - 1;
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint current = polygon.get(index);
            PlanPoint previous = polygon.get(previousIndex);
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters()) + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside || polygon.stream().anyMatch(vertex -> vertex.distanceTo(point).toMillimeters() < 0.001);
    }

    private void assertNoSelfIntersections(List<PlanPoint> path) {
        for (int firstIndex = 1; firstIndex < path.size(); firstIndex++) {
            PlanPoint firstStart = path.get(firstIndex - 1);
            PlanPoint firstEnd = path.get(firstIndex);
            for (int secondIndex = firstIndex + 2; secondIndex < path.size(); secondIndex++) {
                PlanPoint secondStart = path.get(secondIndex - 1);
                PlanPoint secondEnd = path.get(secondIndex);
                assertFalse(segmentsIntersect(firstStart, firstEnd, secondStart, secondEnd),
                        () -> "Schneckenrohre kreuzen sich: " + firstStart + " / " + secondStart);
            }
        }
    }

    private boolean segmentsIntersect(PlanPoint firstStart, PlanPoint firstEnd, PlanPoint secondStart, PlanPoint secondEnd) {
        double firstOrientation = orientation(firstStart, firstEnd, secondStart);
        double secondOrientation = orientation(firstStart, firstEnd, secondEnd);
        double thirdOrientation = orientation(secondStart, secondEnd, firstStart);
        double fourthOrientation = orientation(secondStart, secondEnd, firstEnd);
        return firstOrientation * secondOrientation < -0.001 && thirdOrientation * fourthOrientation < -0.001;
    }

    private double orientation(PlanPoint first, PlanPoint second, PlanPoint third) {
        return (second.xMillimeters() - first.xMillimeters()) * (third.yMillimeters() - first.yMillimeters())
                - (second.yMillimeters() - first.yMillimeters()) * (third.xMillimeters() - first.xMillimeters());
    }

    private Room rectangularRoom() {
        return Room.rectangular(
                "Wohnen", new PlanPoint(0, 0), new PlanPoint(6_000, 4_000),
                Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
    }

    private Room lShapedRoom() {
        return new Room(
                java.util.UUID.randomUUID(), "Wohnen", List.of(
                new PlanPoint(0, 0), new PlanPoint(6_000, 0), new PlanPoint(6_000, 2_000),
                new PlanPoint(3_000, 2_000), new PlanPoint(3_000, 5_000), new PlanPoint(0, 5_000)
        ), Length.ofMillimeters(2_500), Length.ofMillimeters(180), Length.ofMillimeters(200), null);
    }

    private List<PlanPoint> reversed(List<PlanPoint> points) {
        return points.reversed();
    }

    private HydronicHeating heating(
            Room room,
            HeatingSurfacePosition position,
            HeatingLayoutPattern pattern,
            double maximumPipeLength
    ) {
        return HydronicHeating.create(
                room.id(), position, pattern, Length.ofMillimeters(200), Length.ofMillimeters(16),
                Length.ofMillimeters(maximumPipeLength), Length.ofMillimeters(150),
                new PlanPoint(100, 100), new PlanPoint(300, 100)
        );
    }
}
