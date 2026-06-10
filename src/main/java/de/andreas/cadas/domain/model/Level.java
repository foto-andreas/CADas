package de.andreas.cadas.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Level {

    private final String name;
    private final List<Wall> walls = new ArrayList<>();

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

    @Override
    public String toString() {
        return name;
    }
}

