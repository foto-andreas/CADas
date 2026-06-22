package de.schrell.cadas.application.drawing;

import de.schrell.cadas.application.view.RenderableKind;
import de.schrell.cadas.application.view.SelectionKey;
import de.schrell.cadas.application.stairs.StairUnderbuildService;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SelectionTranslationService {

    private static final double EPSILON = 0.001;
    private final StairUnderbuildService stairUnderbuildService = new StairUnderbuildService();

    public TranslationResult translate(Level level, Set<SelectionKey> selections, double deltaXMillimeters, double deltaYMillimeters) {
        Set<String> selectedStairs = selectedIds(selections, RenderableKind.STAIR);
        Set<String> selectedWalls = new LinkedHashSet<>(selectedIds(selections, RenderableKind.WALL));
        for (String staircaseId : selectedStairs) {
            UUID id = UUID.fromString(staircaseId);
            selectedWalls.add(stairUnderbuildService.wallId(id, StairUnderbuildService.Side.LEFT).toString());
            selectedWalls.add(stairUnderbuildService.wallId(id, StairUnderbuildService.Side.RIGHT).toString());
        }
        Set<String> selectedRoomObjects = selectedIds(selections, RenderableKind.ROOM_OBJECT);
        Set<String> selectedFloorOpenings = selectedIds(selections, RenderableKind.FLOOR_OPENING);
        Set<String> selectedHeatingExclusionAreas = selectedIds(selections, RenderableKind.HEATING_EXCLUSION);
        Set<String> selectedHeatingZones = selectedIds(selections, RenderableKind.HEATING_ZONE);
        Set<String> selectedHeatingManifolds = selectedIds(selections, RenderableKind.HEATING_MANIFOLD);
        List<PlanPoint> translatedWallEndpoints = selectedWallEndpoints(level, selectedWalls);
        List<Wall> translatedWalls = level.walls().stream()
                .map(wall -> selectedWalls.contains(wall.id().toString())
                        ? translateWall(wall, deltaXMillimeters, deltaYMillimeters)
                        : translateConnectedEndpoints(wall, translatedWallEndpoints, deltaXMillimeters, deltaYMillimeters))
                .toList();
        List<Staircase> translatedStairs = level.staircases().stream()
                .map(staircase -> selectedStairs.contains(staircase.id().toString()) ? translateStair(staircase, deltaXMillimeters, deltaYMillimeters) : staircase)
                .toList();
        List<RoomObject> translatedRoomObjects = level.roomObjects().stream()
                .map(roomObject -> selectedRoomObjects.contains(roomObject.id().toString()) ? translateRoomObject(roomObject, deltaXMillimeters, deltaYMillimeters) : roomObject)
                .toList();
        List<FloorOpening> translatedFloorOpenings = level.floorOpenings().stream()
                .map(opening -> selectedFloorOpenings.contains(opening.id().toString())
                        ? opening.withCenter(translatePoint(opening.center(), deltaXMillimeters, deltaYMillimeters))
                        : opening)
                .toList();
        List<HeatingExclusionArea> translatedHeatingExclusionAreas = level.heatingExclusionAreas().stream()
                .map(area -> selectedHeatingExclusionAreas.contains(area.id().toString())
                        ? area.translated(deltaXMillimeters, deltaYMillimeters)
                        : area)
                .toList();
        List<HydronicHeating> translatedHydronicHeatings = level.hydronicHeatings().stream()
                .map(heating -> translateHeatingZones(heating, selectedHeatingZones, deltaXMillimeters, deltaYMillimeters))
                .map(heating -> selectedHeatingManifolds.contains(heating.id().toString())
                        ? translateHeatingManifold(heating, deltaXMillimeters, deltaYMillimeters)
                        : heating)
                .toList();
        boolean changed = !selectedWalls.isEmpty()
                || !selectedStairs.isEmpty()
                || !selectedRoomObjects.isEmpty()
                || !selectedFloorOpenings.isEmpty()
                || !selectedHeatingExclusionAreas.isEmpty()
                || !selectedHeatingZones.isEmpty()
                || !selectedHeatingManifolds.isEmpty();
        return new TranslationResult(translatedWalls, translatedStairs, translatedRoomObjects,
                translatedFloorOpenings, translatedHeatingExclusionAreas, translatedHydronicHeatings, changed);
    }

    private Set<String> selectedIds(Set<SelectionKey> selections, RenderableKind kind) {
        return selections.stream()
                .filter(selection -> selection.kind() == kind)
                .map(SelectionKey::elementId)
                .collect(Collectors.toSet());
    }

    private List<PlanPoint> selectedWallEndpoints(Level level, Set<String> selectedWalls) {
        return level.walls().stream()
                .filter(wall -> selectedWalls.contains(wall.id().toString()))
                .flatMap(wall -> List.of(wall.axis().start(), wall.axis().end()).stream())
                .toList();
    }

    private Wall translateWall(Wall wall, double deltaXMillimeters, double deltaYMillimeters) {
        return wall.withAxis(
                new PlanSegment(
                        translatePoint(wall.axis().start(), deltaXMillimeters, deltaYMillimeters),
                        translatePoint(wall.axis().end(), deltaXMillimeters, deltaYMillimeters)
                )
        );
    }

    private Wall translateConnectedEndpoints(Wall wall, List<PlanPoint> translatedWallEndpoints, double deltaXMillimeters, double deltaYMillimeters) {
        PlanPoint start = isConnectedToAny(wall.axis().start(), translatedWallEndpoints)
                ? translatePoint(wall.axis().start(), deltaXMillimeters, deltaYMillimeters)
                : wall.axis().start();
        PlanPoint end = isConnectedToAny(wall.axis().end(), translatedWallEndpoints)
                ? translatePoint(wall.axis().end(), deltaXMillimeters, deltaYMillimeters)
                : wall.axis().end();
        if (start == wall.axis().start() && end == wall.axis().end()) {
            return wall;
        }
        return wall.withAxis(new PlanSegment(start, end));
    }

    private boolean isConnectedToAny(PlanPoint point, List<PlanPoint> endpoints) {
        return endpoints.stream().anyMatch(endpoint -> samePoint(point, endpoint));
    }

    private boolean samePoint(PlanPoint first, PlanPoint second) {
        return Math.abs(first.xMillimeters() - second.xMillimeters()) <= EPSILON
                && Math.abs(first.yMillimeters() - second.yMillimeters()) <= EPSILON;
    }

    private Staircase translateStair(Staircase staircase, double deltaXMillimeters, double deltaYMillimeters) {
        return new Staircase(
                staircase.id(),
                staircase.stairType(),
                translatePoint(staircase.firstCorner(), deltaXMillimeters, deltaYMillimeters),
                translatePoint(staircase.oppositeCorner(), deltaXMillimeters, deltaYMillimeters),
                staircase.totalHeight(),
                staircase.stepCount(),
                staircase.rotationQuarterTurns(),
                staircase.startLandingWidth(),
                staircase.endLandingWidth(),
                staircase.leftUnderbuildWidth(),
                staircase.rightUnderbuildWidth(),
                staircase.undersideThickness()
        );
    }

    private RoomObject translateRoomObject(RoomObject roomObject, double deltaXMillimeters, double deltaYMillimeters) {
        return new RoomObject(
                roomObject.id(),
                roomObject.presetId(),
                roomObject.name(),
                roomObject.type(),
                roomObject.shape(),
                translatePoint(roomObject.center(), deltaXMillimeters, deltaYMillimeters),
                roomObject.width(),
                roomObject.depth(),
                roomObject.height(),
                roomObject.rotationDegrees(),
                roomObject.mountingMode(),
                roomObject.visible(),
                roomObject.source(),
                roomObject.baseElevation()
        );
    }

    private PlanPoint translatePoint(PlanPoint point, double deltaXMillimeters, double deltaYMillimeters) {
        return new PlanPoint(point.xMillimeters() + deltaXMillimeters, point.yMillimeters() + deltaYMillimeters);
    }

    private HydronicHeating translateHeatingZones(
            HydronicHeating heating,
            Set<String> selectedHeatingZones,
            double deltaXMillimeters,
            double deltaYMillimeters
    ) {
        if (heating.zones().stream().noneMatch(zone -> selectedHeatingZones.contains(zone.id().toString()))) {
            return heating;
        }
        return heating.withZones(heating.zones().stream()
                .map(zone -> selectedHeatingZones.contains(zone.id().toString())
                        ? translateHeatingZone(zone, deltaXMillimeters, deltaYMillimeters)
                        : zone)
                .toList());
    }

    private HeatingZone translateHeatingZone(HeatingZone zone, double deltaXMillimeters, double deltaYMillimeters) {
        return new HeatingZone(
                zone.id(),
                zone.name(),
                zone.outline().stream()
                        .map(point -> translatePoint(point, deltaXMillimeters, deltaYMillimeters))
                        .toList(),
                zone.layoutPattern(),
                zone.flowInverted(),
                translatePoint(zone.supplyConnectionPoint(), deltaXMillimeters, deltaYMillimeters),
                translatePoint(zone.returnConnectionPoint(), deltaXMillimeters, deltaYMillimeters)
        );
    }

    private HydronicHeating translateHeatingManifold(
            HydronicHeating heating,
            double deltaXMillimeters,
            double deltaYMillimeters
    ) {
        return heating.withManifold(
                translatePoint(heating.supplyPoint(), deltaXMillimeters, deltaYMillimeters),
                translatePoint(heating.returnPoint(), deltaXMillimeters, deltaYMillimeters)
        );
    }

    public record TranslationResult(
            List<Wall> walls,
            List<Staircase> staircases,
            List<RoomObject> roomObjects,
            List<FloorOpening> floorOpenings,
            List<HeatingExclusionArea> heatingExclusionAreas,
            List<HydronicHeating> hydronicHeatings,
            boolean changed
    ) {
    }
}
