package de.schrell.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceMaterial;
import de.schrell.cadas.domain.model.SurfaceType;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class SurfaceMaterialUsageServiceTest {

    private final SurfaceMaterialUsageService service = new SurfaceMaterialUsageService();

    @Test
    void ersetztErgaenztUndEntferntMaterialGlobal() {
        ProjectModel project = projectWithTwoRooms();
        addFloorLayer(project, project.primaryLevel().rooms().get(0), legacyLayer("Alt", 12));
        addFloorLayer(project, project.primaryLevel().rooms().get(1), legacyLayer("Alt", 12));
        project.normalizeSurfaceMaterials();
        UUID oldMaterialId = floorLayer(project, 0).materialId();

        SurfaceMaterial replacement = material("Neu", 20);
        assertEquals(2, service.replace(project, oldMaterialId, replacement,
                SurfaceMaterialUsageScope.ENTIRE_PROJECT, null, null));
        UUID replacementId = floorLayer(project, 0).materialId();
        assertEquals(replacementId, floorLayer(project, 1).materialId());
        assertEquals("Neu", floorLayer(project, 0).name());
        assertEquals(20.0, floorLayer(project, 0).thickness().toMillimeters());

        SurfaceMaterial additional = material("Dämmung", 40);
        assertEquals(2, service.insert(project, replacementId, additional,
                SurfaceMaterialUsageService.InsertionPosition.AFTER,
                SurfaceMaterialUsageScope.ENTIRE_PROJECT, null, null));
        assertEquals(2, floorStack(project, 0).layers().size());
        assertEquals("Dämmung", floorStack(project, 0).layers().get(1).name());

        assertEquals(2, service.remove(project, replacementId,
                SurfaceMaterialUsageScope.ENTIRE_PROJECT, null, null));
        assertEquals(1, floorStack(project, 0).layers().size());
        assertEquals("Dämmung", floorLayer(project, 0).name());
    }

    @Test
    void beschraenktMaterialoperationenAufDenAusgewaehltenRaum() {
        ProjectModel project = projectWithTwoRooms();
        Room firstRoom = project.primaryLevel().rooms().get(0);
        Room secondRoom = project.primaryLevel().rooms().get(1);
        addFloorLayer(project, firstRoom, legacyLayer("Parkett", 14));
        addFloorLayer(project, secondRoom, legacyLayer("Parkett", 14));
        SurfaceLayerStack interiorWall = new SurfaceLayerStack(
                SurfaceType.WALL_INTERIOR,
                WallSurfaceTargetKey.interior(UUID.randomUUID(), firstRoom.id())
        );
        interiorWall.addLayer(legacyLayer("Parkett", 14));
        project.primaryLevel().addSurfaceLayerStack(interiorWall);
        project.normalizeSurfaceMaterials();
        UUID materialId = floorLayer(project, 0).materialId();

        assertEquals(2, service.insert(project, materialId, material("Dämmung", 40),
                SurfaceMaterialUsageService.InsertionPosition.BEFORE,
                SurfaceMaterialUsageScope.SELECTED_ROOM, project.primaryLevel(), firstRoom.id()));
        assertEquals(2, floorStack(project, 0).layers().size());
        assertEquals(1, floorStack(project, 1).layers().size());
        assertEquals(2, interiorWall.layers().size());

        assertEquals(2, service.remove(project, materialId,
                SurfaceMaterialUsageScope.SELECTED_ROOM, project.primaryLevel(), firstRoom.id()));
        assertEquals("Dämmung", floorLayer(project, 0).name());
        assertEquals("Parkett", floorLayer(project, 1).name());
        assertEquals("Dämmung", interiorWall.layers().getFirst().name());
    }

    @Test
    void fasstAlteGleichnamigeMaterialienMitHaeufigsterAuspraegungZusammen() {
        ProjectModel project = projectWithTwoRooms();
        addFloorLayer(project, project.primaryLevel().rooms().get(0), legacyLayer("Eiche", 12));
        addFloorLayer(project, project.primaryLevel().rooms().get(1), legacyLayer("Eiche", 12));
        Room thirdRoom = Room.rectangular(
                "Arbeitszimmer", new PlanPoint(4_000, 0), new PlanPoint(6_000, 2_000),
                Length.ofMillimeters(2_600), Length.ofMillimeters(180), Length.ofMillimeters(200)
        );
        project.primaryLevel().addRoom(thirdRoom);
        addFloorLayer(project, thirdRoom, legacyLayer("Eiche", 20));

        project.normalizeSurfaceMaterials();

        UUID materialId = floorLayer(project, 0).materialId();
        assertEquals(1, project.surfaceMaterials().size());
        assertEquals(materialId, floorLayer(project, 1).materialId());
        assertEquals(materialId, floorLayer(project, 2).materialId());
        assertEquals(12.0, floorLayer(project, 2).thickness().toMillimeters());
        assertNotNull(materialId);
    }

    @Test
    void aktualisiertAlleNutzungenBeimSpeichernEinesGleichnamigenBibliotheksmaterials() {
        ProjectModel project = projectWithTwoRooms();
        addFloorLayer(project, project.primaryLevel().rooms().get(0), legacyLayer("Eiche", 12, "Bibliothek/Eiche"));
        addFloorLayer(project, project.primaryLevel().rooms().get(1), legacyLayer("Eiche", 12, "Bibliothek/Eiche"));
        project.normalizeSurfaceMaterials();
        UUID materialId = floorLayer(project, 0).materialId();

        SurfaceMaterial updated = service.registerMatchingMaterial(project, material("Eiche", 20, "Bibliothek/Eiche"));

        assertEquals(materialId, updated.id());
        assertEquals(20.0, floorLayer(project, 0).thickness().toMillimeters());
        assertEquals(20.0, floorLayer(project, 1).thickness().toMillimeters());
    }

    private ProjectModel projectWithTwoRooms() {
        ProjectModel project = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        project.primaryLevel().addRoom(Room.rectangular(
                "Wohnen", new PlanPoint(0, 0), new PlanPoint(2_000, 2_000),
                Length.ofMillimeters(2_600), Length.ofMillimeters(180), Length.ofMillimeters(200)
        ));
        project.primaryLevel().addRoom(Room.rectangular(
                "Schlafen", new PlanPoint(2_000, 0), new PlanPoint(4_000, 2_000),
                Length.ofMillimeters(2_600), Length.ofMillimeters(180), Length.ofMillimeters(200)
        ));
        return project;
    }

    private void addFloorLayer(ProjectModel project, Room room, SurfaceLayer layer) {
        SurfaceLayerStack stack = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        stack.addLayer(layer);
        project.primaryLevel().addSurfaceLayerStack(stack);
    }

    private SurfaceLayerStack floorStack(ProjectModel project, int roomIndex) {
        return project.primaryLevel().findSurfaceLayerStack(
                SurfaceType.FLOOR,
                project.primaryLevel().rooms().get(roomIndex).id().toString()
        );
    }

    private SurfaceLayer floorLayer(ProjectModel project, int roomIndex) {
        return floorStack(project, roomIndex).layers().getFirst();
    }

    private SurfaceLayer legacyLayer(String name, double thicknessMillimeters) {
        return legacyLayer(name, thicknessMillimeters, "");
    }

    private SurfaceLayer legacyLayer(String name, double thicknessMillimeters, String coveringSource) {
        return SurfaceLayer.create(
                name,
                Length.ofMillimeters(thicknessMillimeters),
                Length.of(120, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.zero(),
                Length.ofMillimeters(2),
                coveringSource
        );
    }

    private SurfaceMaterial material(String name, double thicknessMillimeters) {
        return material(name, thicknessMillimeters, "");
    }

    private SurfaceMaterial material(String name, double thicknessMillimeters, String coveringSource) {
        SurfaceLayer layer = legacyLayer(name, thicknessMillimeters, coveringSource);
        return SurfaceMaterial.fromLayer(layer);
    }
}
