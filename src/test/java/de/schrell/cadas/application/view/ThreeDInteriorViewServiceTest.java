package de.schrell.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.Wall;
import org.junit.jupiter.api.Test;

class ThreeDInteriorViewServiceTest {

    private final ThreeDInteriorViewService service = new ThreeDInteriorViewService();

    @Test
    void setztKameraInDieRaummitteAufAugenhoehe() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        Room room = Room.rectangular(
                "Bad",
                new PlanPoint(100, 200),
                new PlanPoint(3100, 2200),
                Length.of(2.4, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        project.primaryLevel().addRoom(room);

        ThreeDInteriorViewService.InteriorViewTarget target = service.targetFor(project, project.primaryLevel(), room);

        assertEquals("Erdgeschoss", target.levelName());
        assertEquals("Bad", target.roomName());
        assertEquals(1600.0, target.eyeXMillimeters(), 0.001);
        assertEquals(1200.0, target.eyeZMillimeters(), 0.001);
        assertEquals(1780.0, target.eyeYMillimeters(), 0.001);
        assertEquals(1600.0, target.eyeHeightAboveFloorMillimeters(), 0.001);
    }

    @Test
    void beruecksichtigtVorherigeEtagenBeiDerAugenhoehe() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        project.primaryLevel().addWall(Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        ));
        var obergeschoss = project.createLevel("Obergeschoss");
        Room room = Room.rectangular(
                "Kind",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(1, LengthUnit.MILLIMETER)
        );
        obergeschoss.addRoom(room);

        ThreeDInteriorViewService.InteriorViewTarget target = service.targetFor(project, obergeschoss, room);

        assertEquals(4580.0, target.eyeYMillimeters(), 0.001);
    }
}
