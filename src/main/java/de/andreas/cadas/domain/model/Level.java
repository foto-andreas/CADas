package de.andreas.cadas.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Level {

    private final String name;
    private final List<Wall> walls = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private final List<Door> doors = new ArrayList<>();
    private final List<WindowElement> windows = new ArrayList<>();
    private final List<Staircase> staircases = new ArrayList<>();
    private final List<SurfaceLayerStack> surfaceLayerStacks = new ArrayList<>();

    public Level(String name) {
        this.name = Objects.requireNonNull(name, "name darf nicht null sein.");
    }

    public String name() {
        return name;
    }

    public List<Wall> walls() {
        return List.copyOf(walls);
    }

    public void addWall(Wall wall) {
        walls.add(Objects.requireNonNull(wall, "wall darf nicht null sein."));
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

    public void replaceStaircases(List<Staircase> updatedStaircases) {
        staircases.clear();
        staircases.addAll(Objects.requireNonNull(updatedStaircases, "updatedStaircases darf nicht null sein."));
    }

    public List<SurfaceLayerStack> surfaceLayerStacks() {
        return List.copyOf(surfaceLayerStacks);
    }

    public void addSurfaceLayerStack(SurfaceLayerStack stack) {
        surfaceLayerStacks.add(Objects.requireNonNull(stack, "stack darf nicht null sein."));
    }

    public Wall findWall(UUID wallId) {
        return walls.stream()
                .filter(wall -> wall.id().equals(wallId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Wand nicht gefunden: " + wallId));
    }

    @Override
    public String toString() {
        return name;
    }
}
