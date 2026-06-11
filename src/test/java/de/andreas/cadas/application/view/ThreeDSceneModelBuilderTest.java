package de.andreas.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Angle;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Roof;
import de.andreas.cadas.domain.model.RoofType;
import de.andreas.cadas.domain.model.SlopedCeilingProfile;
import de.andreas.cadas.domain.model.SlopedCeilingSide;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ThreeDSceneModelBuilderTest {

    private final ThreeDSceneModelBuilder builder = new ThreeDSceneModelBuilder();

    @Test
    void leitetVolumenkoerperFuerMvpObjekteAb() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(2.75, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        level.addDoor(Door.create(
                wall.id(),
                Length.of(1.0, LengthUnit.METER),
                Length.of(1.0, LengthUnit.METER),
                Length.of(2.01, LengthUnit.METER),
                Length.zero()
        ));
        level.addWindow(WindowElement.create(
                wall.id(),
                Length.of(2.4, LengthUnit.METER),
                Length.of(1.2, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        ));
        level.addStaircase(Staircase.create(
                StairType.STRAIGHT,
                new PlanPoint(500, 500),
                new PlanPoint(1500, 3000),
                Length.of(2.8, LengthUnit.METER),
                14
        ));
        project.defineRoof(new Roof(RoofType.SADDLE, Angle.ofDegrees(38), Length.of(45, LengthUnit.CENTIMETER), true));

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), false);

        assertFalse(sceneModel.boxes().isEmpty());
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.WALL));
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.DOOR));
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.WINDOW));
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.STAIR));
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.ROOF));
    }

    @Test
    void beruecksichtigtGeschossfilterUndOberflaechenEbenen() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var erdgeschoss = project.primaryLevel();
        Room wohnen = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(5000, 4000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        erdgeschoss.addRoom(wohnen);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, wohnen.id().toString());
        stack.addLayer(SurfaceLayer.create(
                "Estrich",
                Length.of(6, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.zero()
        ));
        erdgeschoss.addSurfaceLayerStack(stack);

        var obergeschoss = project.createLevel("Obergeschoss");
        obergeschoss.addRoom(Room.rectangular(
                "Kind",
                new PlanPoint(1000, 1000),
                new PlanPoint(2000, 2000),
                Length.of(2.5, LengthUnit.METER),
                Length.of(16, LengthUnit.CENTIMETER),
                Length.of(18, LengthUnit.CENTIMETER)
        ));

        ThreeDSceneModel nurObergeschoss = builder.build(project, Set.of("Obergeschoss"), true);
        ThreeDSceneModel mitEbenen = builder.build(project, Set.of("Erdgeschoss"), true);

        assertFalse(nurObergeschoss.boxes().stream().anyMatch(box -> "Erdgeschoss".equals(box.levelName())));
        assertTrue(mitEbenen.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.SURFACE_LAYER));
    }

    @Test
    void leitetSchraegeRaumdeckenUndDeckenebenenIn3dAb() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Dachgeschoss");
        var level = project.primaryLevel();
        Room room = Room.rectangular(
                "Studio",
                new PlanPoint(0, 0),
                new PlanPoint(5000, 3000),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                new SlopedCeilingProfile(SlopedCeilingSide.NORTH, Length.of(1.0, LengthUnit.METER))
        );
        level.addRoom(room);
        SurfaceLayerStack decke = new SurfaceLayerStack(SurfaceType.CEILING, room.id().toString());
        decke.addLayer(SurfaceLayer.create(
                "Gipskarton",
                Length.of(1.25, LengthUnit.CENTIMETER),
                Length.of(125, LengthUnit.CENTIMETER),
                Length.of(62.5, LengthUnit.CENTIMETER),
                Length.zero()
        ));
        level.addSurfaceLayerStack(decke);

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Dachgeschoss"), true);

        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.ROOM_VOLUME));
        assertTrue(sceneModel.boxes().stream().filter(box -> box.kind() == RenderableKind.ROOM_VOLUME).count() > 1);
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.SURFACE_LAYER));
    }

    @Test
    void zerlegtPolygonaleRaeumeInMehrere3dVolumenkoerper() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        level.addRoom(new Room(
                java.util.UUID.randomUUID(),
                "L-Raum",
                List.of(
                        new PlanPoint(100, 100),
                        new PlanPoint(4900, 100),
                        new PlanPoint(4900, 1400),
                        new PlanPoint(2900, 1400),
                        new PlanPoint(2900, 3900),
                        new PlanPoint(100, 3900)
                ),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                null
        ));

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), false);

        assertTrue(sceneModel.boxes().stream().filter(box -> box.kind() == RenderableKind.ROOM_VOLUME).count() > 1);
    }
}
