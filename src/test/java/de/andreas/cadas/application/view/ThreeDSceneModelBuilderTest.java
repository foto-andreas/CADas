package de.andreas.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import de.andreas.cadas.application.room.AutoRoomGenerationService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ThreeDSceneModelBuilderTest {

    private final ThreeDSceneModelBuilder builder = new ThreeDSceneModelBuilder();
    private final AutoRoomGenerationService roomGenerationService = new AutoRoomGenerationService();

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
        assertFalse(nurObergeschoss.meshes().stream().anyMatch(mesh -> "Erdgeschoss".equals(mesh.levelName())));
        assertTrue(mitEbenen.meshes().stream().anyMatch(mesh -> mesh.kind() == RenderableKind.ROOM_FLOOR));
        assertTrue(mitEbenen.meshes().stream().anyMatch(mesh -> mesh.kind() == RenderableKind.SURFACE_LAYER));
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
    void reduziertRaumvolumenIn3dUmSichtbareBodenUndDeckenlagen() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3000),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);

        SurfaceLayerStack floor = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        floor.addLayer(SurfaceLayer.create("Parkett", Length.of(1.2, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero()));
        floor.addLayer(SurfaceLayer.create("Ausgleich", Length.of(0.8, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero()));
        SurfaceLayerStack ceiling = new SurfaceLayerStack(SurfaceType.CEILING, room.id().toString());
        ceiling.addLayer(SurfaceLayer.create("Paneel", Length.of(1.5, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero()));
        level.addSurfaceLayerStack(floor);
        level.addSurfaceLayerStack(ceiling);

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), false);

        RenderableBox roomVolume = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.ROOM_VOLUME)
                .findFirst()
                .orElseThrow();
        assertEquals(2765.0, roomVolume.height(), 0.001);
        assertEquals(1582.5, roomVolume.centerY(), 0.001);
    }

    @Test
    void rendertKeineFundamentBoxenMehrUnterWaenden() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(5000, 0),
                new PlanPoint(5000, 4000),
                new PlanPoint(0, 4000)
        ));
        level.addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(100, 100),
                new PlanPoint(4900, 3900),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), false);

        assertTrue(sceneModel.meshes().stream().anyMatch(mesh -> mesh.kind() == RenderableKind.ROOM_FLOOR));
        assertFalse(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.ROOM_FLOOR));
    }

    @Test
    void flacheDeckenmeshBleibtBeiLRaeumenAusDerInneneckeHeraus() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        level.addRoom(new Room(
                UUID.randomUUID(),
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

        RenderableMesh ceilingMesh = sceneModel.meshes().stream()
                .filter(mesh -> mesh.kind() == RenderableKind.ROOM_CEILING)
                .findFirst()
                .orElseThrow();
        float[] points = ceilingMesh.points();
        for (int index = 0; index < points.length; index += 3) {
            double x = points[index];
            double z = points[index + 2];
            assertFalse(x > 2900.0 && z > 1400.0,
                    "Deckenpunkt liegt unzulässig im ausgesparten Inneneck: " + x + "/" + z);
        }
    }

    @Test
    void flacheDeckenmeshBleibtAuchBeiAutomatischAbgeleitetemLRaumAusDerInneneckeHeraus() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        addLoop(level, List.of(
                new PlanPoint(0, 0),
                new PlanPoint(5000, 0),
                new PlanPoint(5000, 1500),
                new PlanPoint(3000, 1500),
                new PlanPoint(3000, 4000),
                new PlanPoint(0, 4000)
        ));
        level.replaceRooms(roomGenerationService.synchronize(level, defaults()));

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), false);

        RenderableMesh ceilingMesh = sceneModel.meshes().stream()
                .filter(mesh -> mesh.kind() == RenderableKind.ROOM_CEILING)
                .findFirst()
                .orElseThrow();
        float[] points = ceilingMesh.points();
        for (int index = 0; index < points.length; index += 3) {
            double x = points[index];
            double z = points[index + 2];
            assertFalse(x > 2900.0 && z > 1400.0,
                    "Automatisch abgeleiteter Deckenpunkt liegt im ausgesparten Inneneck: " + x + "/" + z);
        }
    }

    private AutoRoomGenerationService.RoomDefaults defaults() {
        return new AutoRoomGenerationService.RoomDefaults(
                "Raum",
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                null
        );
    }

    private void addLoop(de.andreas.cadas.domain.model.Level level, List<PlanPoint> points) {
        for (int index = 0; index < points.size(); index++) {
            PlanPoint start = points.get(index);
            PlanPoint end = points.get((index + 1) % points.size());
            level.addWall(Wall.create(new PlanSegment(start, end), Length.of(20, LengthUnit.CENTIMETER), Length.of(2.8, LengthUnit.METER)));
        }
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

    @Test
    void leitetPolygonaleDeckenhoehenUndSchraegeWandkoerperIn3dAb() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Dachgeschoss");
        var level = project.primaryLevel();
        level.addWall(new Wall(
                UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(3.1, LengthUnit.METER),
                Length.of(2.4, LengthUnit.METER),
                Length.of(3.1, LengthUnit.METER)
        ));
        level.addRoom(new Room(
                UUID.randomUUID(),
                "Ausbau",
                List.of(
                        new PlanPoint(100, 100),
                        new PlanPoint(3900, 100),
                        new PlanPoint(3900, 2900),
                        new PlanPoint(100, 2900)
                ),
                Length.of(3.1, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                null,
                List.of(
                        Length.of(2.4, LengthUnit.METER),
                        Length.of(3.1, LengthUnit.METER),
                        Length.of(3.1, LengthUnit.METER),
                        Length.of(2.6, LengthUnit.METER)
                )
        ));

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Dachgeschoss"), false);

        assertTrue(sceneModel.boxes().stream().filter(box -> box.kind() == RenderableKind.ROOM_VOLUME).count() > 1);
        assertNotEquals(
                sceneModel.boxes().stream()
                        .filter(box -> box.kind() == RenderableKind.WALL)
                        .mapToDouble(RenderableBox::height)
                        .min()
                        .orElseThrow(),
                sceneModel.boxes().stream()
                        .filter(box -> box.kind() == RenderableKind.WALL)
                        .mapToDouble(RenderableBox::height)
                        .max()
                        .orElseThrow(),
                0.001
        );
    }
}
