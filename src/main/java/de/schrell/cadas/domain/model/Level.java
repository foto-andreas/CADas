package de.schrell.cadas.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Level {

    private String name;
    private final List<Wall> walls = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Door> doors = new ArrayList<>();
    private final List<WindowElement> windows = new ArrayList<>();
    private final List<RoofWindow> roofWindows = new ArrayList<>();
    private final List<Staircase> staircases = new ArrayList<>();
    private final List<RoomObject> roomObjects = new ArrayList<>();
    private final List<FloorExtension> floorExtensions = new ArrayList<>();
    private final List<FloorOpening> floorOpenings = new ArrayList<>();
    private final List<HeatingExclusionArea> heatingExclusionAreas = new ArrayList<>();
    private final List<SurfaceLayerStack> surfaceLayerStacks = new ArrayList<>();
    private final List<HydronicHeating> hydronicHeatings = new ArrayList<>();

    public Level(String name) {
        this.name = Objects.requireNonNull(name, "name darf nicht null sein.");
    }

    public String name() {
        return name;
    }

    public void rename(String newName) {
        String trimmedName = Objects.requireNonNull(newName, "newName darf nicht null sein.").trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Etage darf nicht leer benannt werden.");
        }
        this.name = trimmedName;
    }

    public List<Wall> walls() {
        return List.copyOf(walls);
    }

    public void addWall(Wall wall) {
        walls.add(Objects.requireNonNull(wall, "wall darf nicht null sein."));
    }

    public boolean removeWall(UUID wallId) {
        Objects.requireNonNull(wallId, "wallId darf nicht null sein.");
        boolean removed = walls.removeIf(wall -> wall.id().equals(wallId));
        if (removed) {
            doors.removeIf(door -> door.wallId().equals(wallId));
            windows.removeIf(window -> window.wallId().equals(wallId));
            surfaceLayerStacks.removeIf(stack -> targetsWall(stack, wallId));
        }
        return removed;
    }

    private boolean targetsWall(SurfaceLayerStack stack, UUID wallId) {
        if (stack.surfaceType() != SurfaceType.WALL_INTERIOR
                && stack.surfaceType() != SurfaceType.WALL_EXTERIOR) {
            return false;
        }
        String wallTargetKey = wallId.toString();
        return stack.targetKey().equals(wallTargetKey)
                || stack.targetKey().startsWith(wallTargetKey + "@");
    }

    public void replaceWalls(List<Wall> updatedWalls) {
        walls.clear();
        walls.addAll(Objects.requireNonNull(updatedWalls, "updatedWalls darf nicht null sein."));
    }

    public List<Room> rooms() {
        return List.copyOf(rooms);
    }

    public void addRoom(Room room) {
        rooms.add(Objects.requireNonNull(room, "room darf nicht null sein."));
    }

    public void replaceRooms(List<Room> updatedRooms) {
        rooms.clear();
        rooms.addAll(Objects.requireNonNull(updatedRooms, "updatedRooms darf nicht null sein."));
        heatingExclusionAreas.removeIf(area -> rooms.stream().noneMatch(room -> room.id().equals(area.roomId())));
        hydronicHeatings.removeIf(heating -> rooms.stream().noneMatch(room -> room.id().equals(heating.roomId())));
    }

    public List<Door> doors() {
        return List.copyOf(doors);
    }

    public void addDoor(Door door) {
        doors.add(Objects.requireNonNull(door, "door darf nicht null sein."));
    }

    public boolean removeDoor(UUID doorId) {
        Objects.requireNonNull(doorId, "doorId darf nicht null sein.");
        return doors.removeIf(door -> door.id().equals(doorId));
    }

    public void replaceDoors(List<Door> updatedDoors) {
        doors.clear();
        doors.addAll(Objects.requireNonNull(updatedDoors, "updatedDoors darf nicht null sein."));
    }

    public List<WindowElement> windows() {
        return List.copyOf(windows);
    }

    public void addWindow(WindowElement window) {
        windows.add(Objects.requireNonNull(window, "window darf nicht null sein."));
    }

    public boolean removeWindow(UUID windowId) {
        Objects.requireNonNull(windowId, "windowId darf nicht null sein.");
        return windows.removeIf(window -> window.id().equals(windowId));
    }

    public void replaceWindows(List<WindowElement> updatedWindows) {
        windows.clear();
        windows.addAll(Objects.requireNonNull(updatedWindows, "updatedWindows darf nicht null sein."));
    }

    public List<RoofWindow> roofWindows() {
        return List.copyOf(roofWindows);
    }

    public void addRoofWindow(RoofWindow roofWindow) {
        roofWindows.add(Objects.requireNonNull(roofWindow, "roofWindow darf nicht null sein."));
    }

    public boolean removeRoofWindow(UUID roofWindowId) {
        Objects.requireNonNull(roofWindowId, "roofWindowId darf nicht null sein.");
        return roofWindows.removeIf(roofWindow -> roofWindow.id().equals(roofWindowId));
    }

    public void replaceRoofWindows(List<RoofWindow> updatedRoofWindows) {
        roofWindows.clear();
        roofWindows.addAll(Objects.requireNonNull(updatedRoofWindows, "updatedRoofWindows darf nicht null sein."));
    }

    public List<Staircase> staircases() {
        return List.copyOf(staircases);
    }

    public void addStaircase(Staircase staircase) {
        staircases.add(Objects.requireNonNull(staircase, "staircase darf nicht null sein."));
    }

    public boolean removeStaircase(UUID staircaseId) {
        Objects.requireNonNull(staircaseId, "staircaseId darf nicht null sein.");
        return staircases.removeIf(staircase -> staircase.id().equals(staircaseId));
    }

    public void replaceStaircases(List<Staircase> updatedStaircases) {
        staircases.clear();
        staircases.addAll(Objects.requireNonNull(updatedStaircases, "updatedStaircases darf nicht null sein."));
    }

    public List<RoomObject> roomObjects() {
        return List.copyOf(roomObjects);
    }

    public void addRoomObject(RoomObject roomObject) {
        roomObjects.add(Objects.requireNonNull(roomObject, "roomObject darf nicht null sein."));
    }

    public boolean removeRoomObject(UUID objectId) {
        Objects.requireNonNull(objectId, "objectId darf nicht null sein.");
        return roomObjects.removeIf(roomObject -> roomObject.id().equals(objectId));
    }

    public void replaceRoomObjects(List<RoomObject> updatedRoomObjects) {
        roomObjects.clear();
        roomObjects.addAll(Objects.requireNonNull(updatedRoomObjects, "updatedRoomObjects darf nicht null sein."));
    }

    public List<FloorExtension> floorExtensions() {
        return List.copyOf(floorExtensions);
    }

    public void addFloorExtension(FloorExtension floorExtension) {
        floorExtensions.add(Objects.requireNonNull(floorExtension, "floorExtension darf nicht null sein."));
    }

    public boolean removeFloorExtension(UUID floorExtensionId) {
        Objects.requireNonNull(floorExtensionId, "floorExtensionId darf nicht null sein.");
        String targetKey = "floor-extension:" + floorExtensionId;
        boolean removed = floorExtensions.removeIf(extension -> extension.id().equals(floorExtensionId));
        if (removed) {
            surfaceLayerStacks.removeIf(stack -> stack.targetKey().equals(targetKey));
        }
        return removed;
    }

    public void replaceFloorExtensions(List<FloorExtension> updatedFloorExtensions) {
        floorExtensions.clear();
        floorExtensions.addAll(Objects.requireNonNull(updatedFloorExtensions, "updatedFloorExtensions darf nicht null sein."));
    }

    public List<FloorOpening> floorOpenings() {
        return List.copyOf(floorOpenings);
    }

    public void addFloorOpening(FloorOpening floorOpening) {
        floorOpenings.add(Objects.requireNonNull(floorOpening, "floorOpening darf nicht null sein."));
    }

    public boolean removeFloorOpening(UUID floorOpeningId) {
        Objects.requireNonNull(floorOpeningId, "floorOpeningId darf nicht null sein.");
        return floorOpenings.removeIf(opening -> opening.id().equals(floorOpeningId));
    }

    public void replaceFloorOpenings(List<FloorOpening> updatedFloorOpenings) {
        floorOpenings.clear();
        floorOpenings.addAll(Objects.requireNonNull(updatedFloorOpenings, "updatedFloorOpenings darf nicht null sein."));
    }

    public List<HeatingExclusionArea> heatingExclusionAreas() {
        return List.copyOf(heatingExclusionAreas);
    }

    public void addHeatingExclusionArea(HeatingExclusionArea area) {
        heatingExclusionAreas.add(Objects.requireNonNull(area, "area darf nicht null sein."));
    }

    public boolean removeHeatingExclusionArea(UUID areaId) {
        Objects.requireNonNull(areaId, "areaId darf nicht null sein.");
        return heatingExclusionAreas.removeIf(area -> area.id().equals(areaId));
    }

    public void replaceHeatingExclusionAreas(List<HeatingExclusionArea> updatedAreas) {
        heatingExclusionAreas.clear();
        heatingExclusionAreas.addAll(Objects.requireNonNull(updatedAreas, "updatedAreas darf nicht null sein."));
    }

    public List<SurfaceLayerStack> surfaceLayerStacks() {
        return List.copyOf(surfaceLayerStacks);
    }

    public void addSurfaceLayerStack(SurfaceLayerStack stack) {
        surfaceLayerStacks.add(Objects.requireNonNull(stack, "stack darf nicht null sein."));
    }

    public void removeSurfaceLayerStack(UUID stackId) {
        surfaceLayerStacks.removeIf(stack -> stack.id().equals(stackId));
    }

    public void replaceSurfaceLayerStacks(List<SurfaceLayerStack> updatedStacks) {
        surfaceLayerStacks.clear();
        surfaceLayerStacks.addAll(Objects.requireNonNull(updatedStacks, "updatedStacks darf nicht null sein."));
    }

    public SurfaceLayerStack findSurfaceLayerStack(SurfaceType surfaceType, String targetKey) {
        return surfaceLayerStacks.stream()
                .filter(stack -> stack.surfaceType() == surfaceType)
                .filter(stack -> stack.targetKey().equals(targetKey))
                .findFirst()
                .orElse(null);
    }

    public List<HydronicHeating> hydronicHeatings() {
        return List.copyOf(hydronicHeatings);
    }

    public void addHydronicHeating(HydronicHeating heating) {
        HydronicHeating requiredHeating = Objects.requireNonNull(heating, "heating darf nicht null sein.");
        if (findHydronicHeating(requiredHeating.roomId(), requiredHeating.surfacePosition()) != null) {
            throw new IllegalArgumentException("Für Raum und Fläche ist bereits eine Heizung vorhanden.");
        }
        hydronicHeatings.add(requiredHeating);
    }

    public boolean removeHydronicHeating(UUID heatingId) {
        Objects.requireNonNull(heatingId, "heatingId darf nicht null sein.");
        return hydronicHeatings.removeIf(heating -> heating.id().equals(heatingId));
    }

    public void replaceHydronicHeating(HydronicHeating updatedHeating) {
        Objects.requireNonNull(updatedHeating, "updatedHeating darf nicht null sein.");
        int index = -1;
        for (int currentIndex = 0; currentIndex < hydronicHeatings.size(); currentIndex++) {
            if (hydronicHeatings.get(currentIndex).id().equals(updatedHeating.id())) {
                index = currentIndex;
                break;
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException("Heizung nicht gefunden: " + updatedHeating.id());
        }
        HydronicHeating conflictingHeating = findHydronicHeating(
                updatedHeating.roomId(), updatedHeating.surfacePosition()
        );
        if (conflictingHeating != null && !conflictingHeating.id().equals(updatedHeating.id())) {
            throw new IllegalArgumentException("Für Raum und Fläche ist bereits eine Heizung vorhanden.");
        }
        hydronicHeatings.set(index, updatedHeating);
    }

    public void replaceHydronicHeatings(List<HydronicHeating> updatedHeatings) {
        hydronicHeatings.clear();
        for (HydronicHeating heating : Objects.requireNonNull(updatedHeatings, "updatedHeatings darf nicht null sein.")) {
            addHydronicHeating(heating);
        }
    }

    public HydronicHeating findHydronicHeating(UUID roomId, HeatingSurfacePosition surfacePosition) {
        Objects.requireNonNull(roomId, "roomId darf nicht null sein.");
        Objects.requireNonNull(surfacePosition, "surfacePosition darf nicht null sein.");
        return hydronicHeatings.stream()
                .filter(heating -> heating.roomId().equals(roomId))
                .filter(heating -> heating.surfacePosition() == surfacePosition)
                .findFirst()
                .orElse(null);
    }

    public Wall findWall(UUID wallId) {
        return walls.stream()
                .filter(wall -> wall.id().equals(wallId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Wand nicht gefunden: " + wallId));
    }

    public Level copy() {
        Level copy = new Level(name);
        copy.walls.addAll(walls);
        copy.rooms.addAll(rooms);
        copy.doors.addAll(doors);
        copy.windows.addAll(windows);
        copy.roofWindows.addAll(roofWindows);
        copy.staircases.addAll(staircases);
        copy.roomObjects.addAll(roomObjects);
        copy.floorExtensions.addAll(floorExtensions);
        copy.floorOpenings.addAll(floorOpenings);
        copy.heatingExclusionAreas.addAll(heatingExclusionAreas);
        surfaceLayerStacks.stream()
                .map(SurfaceLayerStack::copy)
                .forEach(copy.surfaceLayerStacks::add);
        copy.hydronicHeatings.addAll(hydronicHeatings);
        return copy;
    }

    @Override
    public String toString() {
        return name;
    }
}
