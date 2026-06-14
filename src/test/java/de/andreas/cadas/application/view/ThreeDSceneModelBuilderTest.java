package de.andreas.cadas.application.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import de.andreas.cadas.domain.model.RoomObject;
import de.andreas.cadas.domain.model.RoomObjectShape;
import de.andreas.cadas.domain.model.RoomObjectType;
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
import de.andreas.cadas.application.layers.WallSurfaceTargetKey;
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
        level.addRoomObject(RoomObject.create(
                "toilet",
                "Toilette",
                RoomObjectType.TOILET,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(2500, 1500),
                Length.of(40, LengthUnit.CENTIMETER),
                Length.of(70, LengthUnit.CENTIMETER),
                Length.of(80, LengthUnit.CENTIMETER),
                false,
                ""
        ));
        project.defineRoof(new Roof(RoofType.SADDLE, Angle.ofDegrees(38), Length.of(45, LengthUnit.CENTIMETER), true));

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), false);
        ThreeDSceneModel ohneObjekte = builder.build(project, Set.of("Erdgeschoss"), false, false, false);

        assertFalse(sceneModel.boxes().isEmpty());
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.WALL));
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.DOOR));
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.WINDOW));
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.STAIR));
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.ROOM_OBJECT));
        assertTrue(sceneModel.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.ROOF));
        assertFalse(ohneObjekte.boxes().stream().anyMatch(box -> box.kind() == RenderableKind.ROOM_OBJECT));
    }

    @Test
    void rendertGedrehteRaumobjekteMitOriginalmaßenUndBoxDrehung() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        project.primaryLevel().addRoomObject(new RoomObject(
                UUID.randomUUID(),
                "toilet",
                "Toilette",
                RoomObjectType.TOILET,
                RoomObjectShape.RECTANGLE,
                new PlanPoint(1000, 1200),
                Length.of(40, LengthUnit.CENTIMETER),
                Length.of(70, LengthUnit.CENTIMETER),
                Length.of(80, LengthUnit.CENTIMETER),
                1,
                false,
                true,
                ""
        ));

        RenderableBox box = builder.build(project, Set.of("Erdgeschoss"), false).boxes().stream()
                .filter(renderableBox -> renderableBox.kind() == RenderableKind.ROOM_OBJECT)
                .findFirst()
                .orElseThrow();

        assertEquals(400.0, box.width(), 0.001);
        assertEquals(700.0, box.depth(), 0.001);
        assertEquals(90.0, box.rotationDegrees(), 0.001);
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
        RenderableMesh ceilingMesh = sceneModel.meshes().stream()
                .filter(mesh -> mesh.kind() == RenderableKind.ROOM_CEILING)
                .findFirst()
                .orElseThrow();
        RenderableMesh ceilingLayerMesh = sceneModel.meshes().stream()
                .filter(mesh -> mesh.kind() == RenderableKind.SURFACE_LAYER)
                .filter(mesh -> mesh.selectionKey().elementId().equals(decke.layers().getFirst().id().toString()))
                .findFirst()
                .orElseThrow();
        assertTrue(hasMeshHeightDifference(ceilingMesh, 1000.0));
        assertTrue(hasMeshHeightDifference(ceilingLayerMesh, 1000.0));
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
    void rendertBodenkoerperInnenUndTragschichtUnterWaenden() {
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
        List<RenderableBox> floorBoxes = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.ROOM_FLOOR)
                .toList();
        assertFalse(floorBoxes.isEmpty());
        assertTrue(floorBoxes.stream().anyMatch(box -> box.selectionKey().elementId().equals(level.rooms().getFirst().id().toString())));
        assertTrue(floorBoxes.stream().anyMatch(box -> box.selectionKey().elementId().startsWith("wall-support-")));
        floorBoxes.forEach(box -> assertEquals(180.0, box.centerY() + box.height() / 2.0, 0.001));
    }

    @Test
    void legtFugenVonBodenbelaegenSichtbarUeberDieBelagsoberflaeche() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3000),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);
        SurfaceLayerStack floor = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        floor.addLayer(SurfaceLayer.create(
                "Fliese",
                Length.of(12, LengthUnit.MILLIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(30, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.MILLIMETER)
        ));
        level.addSurfaceLayerStack(floor);

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), true);

        List<RenderableBox> joints = sceneModel.boxes().stream()
                .filter(box -> "joint".equals(box.materialKey()))
                .toList();
        assertFalse(joints.isEmpty());
        joints.forEach(box -> assertTrue(box.centerY() - box.height() / 2.0 > 192.0));
        assertTrue(sceneModel.boxes().stream().anyMatch(box ->
                box.kind() == RenderableKind.SURFACE_LAYER
                        && "surface-layer".equals(box.materialKey())
                        && Math.abs(box.height() - 12.0) < 0.001
        ));
        long horizontaleFugenSegmente = joints.stream()
                .filter(box -> box.width() > box.depth())
                .count();
        assertTrue(horizontaleFugenSegmente > 1, "Horizontale Fugen dürfen nicht auf ein Segment pro Reihe kollabieren.");
    }

    @Test
    void rendertLegacyInnenwandBelagNichtAufDerAussenseite() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(wall);
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(100, 100),
                new PlanPoint(3900, 2900),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, wall.id().toString());
        SurfaceLayer layer = SurfaceLayer.create("Putz", Length.of(12, LengthUnit.MILLIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(30, LengthUnit.CENTIMETER), Length.zero());
        stack.addLayer(layer);
        level.addSurfaceLayerStack(stack);

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), true);

        List<RenderableBox> wallLayers = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.SURFACE_LAYER)
                .filter(box -> box.selectionKey().elementId().equals(layer.id().toString()))
                .filter(box -> "surface-layer".equals(box.materialKey()))
                .toList();
        assertEquals(1, wallLayers.size());
        assertTrue(wallLayers.getFirst().centerZ() > 0.0);
    }

    @Test
    void legtFugenVonInnenwandFliesenSichtbarVorDieBelagsoberflaeche() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(wall);
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(100, 100),
                new PlanPoint(3900, 2900),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, wall.id().toString());
        SurfaceLayer layer = SurfaceLayer.create(
                "Fliese",
                Length.of(12, LengthUnit.MILLIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(30, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.MILLIMETER)
        );
        stack.addLayer(layer);
        level.addSurfaceLayerStack(stack);

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), true);

        List<RenderableBox> wallJoints = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.SURFACE_LAYER)
                .filter(box -> box.selectionKey().elementId().equals(layer.id().toString()))
                .filter(box -> "joint".equals(box.materialKey()))
                .toList();
        assertFalse(wallJoints.isEmpty());
        assertTrue(wallJoints.stream().allMatch(box -> box.centerZ() > 100.0));
        assertTrue(wallJoints.stream().anyMatch(box -> box.width() > box.height()));
        assertTrue(wallJoints.stream().anyMatch(box -> box.height() > box.width()));
    }

    @Test
    void spartÖffnungenAusInnenwandBelägenUndFugenAus() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(wall);
        level.addRoom(Room.rectangular(
                "Wohnen",
                new PlanPoint(100, 100),
                new PlanPoint(3900, 2900),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        ));
        level.addDoor(Door.create(
                wall.id(),
                Length.of(1.0, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(2.1, LengthUnit.METER),
                Length.zero()
        ));
        level.addWindow(WindowElement.create(
                wall.id(),
                Length.of(2.4, LengthUnit.METER),
                Length.of(1.0, LengthUnit.METER),
                Length.of(90, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        ));
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, wall.id().toString());
        SurfaceLayer layer = SurfaceLayer.create(
                "Dämmplatte",
                Length.of(20, LengthUnit.MILLIMETER),
                Length.of(125, LengthUnit.CENTIMETER),
                Length.of(62.5, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.MILLIMETER)
        );
        stack.addLayer(layer);
        level.addSurfaceLayerStack(stack);

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), true);

        List<RenderableBox> wallLayerBodies = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.SURFACE_LAYER)
                .filter(box -> box.selectionKey().elementId().equals(layer.id().toString()))
                .filter(box -> "surface-layer".equals(box.materialKey()))
                .toList();
        List<RenderableBox> wallJoints = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.SURFACE_LAYER)
                .filter(box -> box.selectionKey().elementId().equals(layer.id().toString()))
                .filter(box -> "joint".equals(box.materialKey()))
                .toList();

        assertEquals(5, wallLayerBodies.size());
        assertFalse(wallLayerBodies.stream().anyMatch(box -> Math.abs(box.width() - 4000.0) < 0.001
                && Math.abs(box.height() - 2800.0) < 0.001));
        assertFalse(wallLayerBodies.stream().anyMatch(box -> overlapsWallOpening(box, 180.0, 1000.0, 1900.0, 0.0, 2100.0)));
        assertFalse(wallLayerBodies.stream().anyMatch(box -> overlapsWallOpening(box, 180.0, 2400.0, 3400.0, 900.0, 2100.0)));
        assertFalse(wallJoints.isEmpty());
        assertFalse(wallJoints.stream().anyMatch(box -> overlapsWallOpening(box, 180.0, 1000.0, 1900.0, 0.0, 2100.0)));
        assertFalse(wallJoints.stream().anyMatch(box -> overlapsWallOpening(box, 180.0, 2400.0, 3400.0, 900.0, 2100.0)));
        assertTrue(wallJoints.stream().anyMatch(box -> Math.abs(box.width() - 2.0) < 0.001 || Math.abs(box.height() - 2.0) < 0.001));
    }

    @Test
    void rendertRaumbezogenenInnenwandBelagNurAufDerSeiteDesAusgewaehltenRaums() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall wall = Wall.create(
                new PlanSegment(new PlanPoint(2000, 0), new PlanPoint(2000, 3000)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(wall);
        Room leftRoom = Room.rectangular(
                "Links",
                new PlanPoint(100, 100),
                new PlanPoint(1900, 2900),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        Room rightRoom = Room.rectangular(
                "Rechts",
                new PlanPoint(2100, 100),
                new PlanPoint(3900, 2900),
                Length.of(2.6, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(leftRoom);
        level.addRoom(rightRoom);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, WallSurfaceTargetKey.interior(wall.id(), leftRoom.id()));
        SurfaceLayer layer = SurfaceLayer.create("Putz", Length.of(12, LengthUnit.MILLIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(30, LengthUnit.CENTIMETER), Length.zero());
        stack.addLayer(layer);
        level.addSurfaceLayerStack(stack);

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), true);

        List<RenderableBox> wallLayers = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.SURFACE_LAYER)
                .filter(box -> box.selectionKey().elementId().equals(layer.id().toString()))
                .filter(box -> "surface-layer".equals(box.materialKey()))
                .toList();
        assertEquals(1, wallLayers.size());
        assertTrue(wallLayers.getFirst().centerX() < 2000.0);
    }

    @Test
    void passtWandbelagMeshAnSchrägeWandoberkanteAn() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Dachgeschoss");
        var level = project.primaryLevel();
        Wall wall = new Wall(
                UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(3.2, LengthUnit.METER),
                Length.of(3.2, LengthUnit.METER),
                Length.of(2.6, LengthUnit.METER)
        );
        level.addWall(wall);
        Room room = Room.rectangular(
                "Ausbau",
                new PlanPoint(100, 100),
                new PlanPoint(3900, 2500),
                Length.of(3.2, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, WallSurfaceTargetKey.interior(wall.id(), room.id()));
        SurfaceLayer layer = SurfaceLayer.create(
                "Dämmplatte",
                Length.of(8, LengthUnit.CENTIMETER),
                Length.of(120, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.zero()
        );
        stack.addLayer(layer);
        level.addSurfaceLayerStack(stack);

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Dachgeschoss"), true);

        RenderableMesh surfaceMesh = sceneModel.meshes().stream()
                .filter(mesh -> mesh.kind() == RenderableKind.SURFACE_LAYER)
                .filter(mesh -> mesh.selectionKey().elementId().equals(layer.id().toString()))
                .findFirst()
                .orElseThrow();
        assertTrue(hasMeshHeightBetween(surfaceMesh, 3150.0, 3250.0));
        assertTrue(hasMeshHeightBetween(surfaceMesh, 2550.0, 2650.0));
    }

    @Test
    void unterbrichtWandbelagUndFugenAnEinbindenderInnenwand() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall exteriorWall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        Wall interiorWall = Wall.create(
                new PlanSegment(new PlanPoint(2000, 0), new PlanPoint(2000, 1500)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        level.addWall(exteriorWall);
        level.addWall(interiorWall);
        Room room = Room.rectangular(
                "Raum",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 2500),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);
        SurfaceLayerStack stack = addInteriorWallStack(level, exteriorWall, room);
        SurfaceLayer layer = stack.layers().getFirst();

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), true);

        List<RenderableBox> wallLayerBodies = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.SURFACE_LAYER)
                .filter(box -> box.selectionKey().elementId().equals(layer.id().toString()))
                .filter(box -> "surface-layer".equals(box.materialKey()))
                .toList();
        List<RenderableBox> wallJoints = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.SURFACE_LAYER)
                .filter(box -> box.selectionKey().elementId().equals(layer.id().toString()))
                .filter(box -> "joint".equals(box.materialKey()))
                .toList();
        assertEquals(2, wallLayerBodies.size());
        assertFalse(wallLayerBodies.stream().anyMatch(box -> overlapsX(box, 1900.0, 2100.0)));
        assertFalse(wallJoints.stream().anyMatch(box -> overlapsX(box, 1900.0, 2100.0)));
    }

    @Test
    void unterbrichtWandbelagAnHalbhoherEinbindenderWandNurBisZuDerenHoehe() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        var level = project.primaryLevel();
        Wall exteriorWall = Wall.create(
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(4000, 0)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(2.8, LengthUnit.METER)
        );
        Wall halfHeightWall = Wall.create(
                new PlanSegment(new PlanPoint(2000, 0), new PlanPoint(2000, 1500)),
                Length.of(20, LengthUnit.CENTIMETER),
                Length.of(1.2, LengthUnit.METER)
        );
        level.addWall(exteriorWall);
        level.addWall(halfHeightWall);
        Room room = Room.rectangular(
                "Raum",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 2500),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);
        SurfaceLayerStack stack = addInteriorWallStack(level, exteriorWall, room);
        SurfaceLayer layer = stack.layers().getFirst();

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Erdgeschoss"), true);

        List<RenderableBox> wallLayerBodies = sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.SURFACE_LAYER)
                .filter(box -> box.selectionKey().elementId().equals(layer.id().toString()))
                .filter(box -> "surface-layer".equals(box.materialKey()))
                .toList();
        assertEquals(3, wallLayerBodies.size());
        assertTrue(wallLayerBodies.stream().anyMatch(box -> Math.abs(box.width() - 4000.0) < 0.001
                && Math.abs(box.height() - 1600.0) < 0.001
                && overlapsX(box, 1900.0, 2100.0)));
    }

    @Test
    void verlängertWandbelagMeshAnKonkaverInneneckeBisZumNachbarbelag() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Dachgeschoss");
        var level = project.primaryLevel();
        Wall horizontal = new Wall(
                UUID.randomUUID(),
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(-2000, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(3.2, LengthUnit.METER),
                Length.of(3.2, LengthUnit.METER),
                Length.of(2.6, LengthUnit.METER)
        );
        Wall vertical = Wall.create(
                new PlanSegment(new PlanPoint(0, 1000), new PlanPoint(0, 0)),
                Length.of(17.5, LengthUnit.CENTIMETER),
                Length.of(3.2, LengthUnit.METER)
        );
        level.addWall(horizontal);
        level.addWall(vertical);
        Room room = new Room(
                UUID.randomUUID(),
                "Raum",
                List.of(
                        new PlanPoint(-2000, -1500),
                        new PlanPoint(1500, -1500),
                        new PlanPoint(1500, 1000),
                        new PlanPoint(0, 1000),
                        new PlanPoint(0, 0),
                        new PlanPoint(-2000, 0)
                ),
                Length.of(3.2, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                null
        );
        level.addRoom(room);
        SurfaceLayerStack horizontalStack = addInteriorWallStack(level, horizontal, room);
        addInteriorWallStack(level, vertical, room);
        SurfaceLayer layer = horizontalStack.layers().getFirst();

        ThreeDSceneModel sceneModel = builder.build(project, Set.of("Dachgeschoss"), true);

        RenderableMesh surfaceMesh = sceneModel.meshes().stream()
                .filter(mesh -> mesh.kind() == RenderableKind.SURFACE_LAYER)
                .filter(mesh -> mesh.selectionKey().elementId().equals(layer.id().toString()))
                .findFirst()
                .orElseThrow();
        assertTrue(maximumMeshX(surfaceMesh) > 100.0);
        assertTrue(sceneModel.boxes().stream()
                .filter(box -> box.kind() == RenderableKind.SURFACE_LAYER)
                .filter(box -> box.selectionKey().elementId().equals(layer.id().toString()))
                .filter(box -> "joint".equals(box.materialKey()))
                .anyMatch(box -> box.centerX() + box.width() / 2.0 > 100.0));
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

    private SurfaceLayerStack addInteriorWallStack(de.andreas.cadas.domain.model.Level level, Wall wall, Room room) {
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.WALL_INTERIOR, WallSurfaceTargetKey.interior(wall.id(), room.id()));
        stack.addLayer(SurfaceLayer.create(
                "Dämmplatte",
                Length.of(8, LengthUnit.CENTIMETER),
                Length.of(120, LengthUnit.CENTIMETER),
                Length.of(60, LengthUnit.CENTIMETER),
                Length.of(2, LengthUnit.MILLIMETER)
        ));
        level.addSurfaceLayerStack(stack);
        return stack;
    }

    private boolean overlapsWallOpening(
            RenderableBox box,
            double baseHeight,
            double openingStart,
            double openingEnd,
            double openingLower,
            double openingUpper
    ) {
        double localStart = box.centerX() - box.width() / 2.0;
        double localEnd = box.centerX() + box.width() / 2.0;
        double localLower = box.centerY() - baseHeight - box.height() / 2.0;
        double localUpper = box.centerY() - baseHeight + box.height() / 2.0;
        return localStart < openingEnd - 0.001
                && localEnd > openingStart + 0.001
                && localLower < openingUpper - 0.001
                && localUpper > openingLower + 0.001;
    }

    private boolean hasMeshHeightDifference(RenderableMesh mesh, double minimumDifference) {
        return maximumMeshHeight(mesh) - minimumMeshHeight(mesh) > minimumDifference;
    }

    private double maximumMeshHeight(RenderableMesh mesh) {
        double maximum = Double.NEGATIVE_INFINITY;
        float[] points = mesh.points();
        for (int index = 1; index < points.length; index += 3) {
            maximum = Math.max(maximum, points[index]);
        }
        return maximum;
    }

    private double minimumMeshHeight(RenderableMesh mesh) {
        double minimum = Double.POSITIVE_INFINITY;
        float[] points = mesh.points();
        for (int index = 1; index < points.length; index += 3) {
            minimum = Math.min(minimum, points[index]);
        }
        return minimum;
    }

    private boolean hasMeshHeightBetween(RenderableMesh mesh, double minimum, double maximum) {
        float[] points = mesh.points();
        for (int index = 1; index < points.length; index += 3) {
            if (points[index] >= minimum && points[index] <= maximum) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapsX(RenderableBox box, double minimumX, double maximumX) {
        return box.centerX() - box.width() / 2.0 < maximumX - 0.001
                && box.centerX() + box.width() / 2.0 > minimumX + 0.001;
    }

    private double maximumMeshX(RenderableMesh mesh) {
        double maximum = Double.NEGATIVE_INFINITY;
        float[] points = mesh.points();
        for (int index = 0; index < points.length; index += 3) {
            maximum = Math.max(maximum, points[index]);
        }
        return maximum;
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
        RenderableMesh wallMesh = sceneModel.meshes().stream()
                .filter(mesh -> mesh.kind() == RenderableKind.WALL)
                .findFirst()
                .orElseThrow();
        assertTrue(hasMeshHeightBetween(wallMesh, 2350.0, 2450.0));
        assertTrue(hasMeshHeightBetween(wallMesh, 3050.0, 3150.0));
    }
}
