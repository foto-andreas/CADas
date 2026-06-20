package de.andreas.cadas.domain.model;

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
    private final List<Staircase> staircases = new ArrayList<>();
    private final List<RoomObject> roomObjects = new ArrayList<>();
    private final List<FloorExtension> floorExtensions = new ArrayList<>();
    private final List<SurfaceLayerStack> surfaceLayerStacks = new ArrayList<>();

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
        }
        return removed;
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
        copy.staircases.addAll(staircases);
        copy.roomObjects.addAll(roomObjects);
        copy.floorExtensions.addAll(floorExtensions);
        surfaceLayerStacks.stream()
                .map(SurfaceLayerStack::copy)
                .forEach(copy.surfaceLayerStacks::add);
        return copy;
    }

    @Override
    public String toString() {
        return name;
    }
}
