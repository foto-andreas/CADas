package de.andreas.cadas.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProjectModel {

    private final String name;
    private final List<Level> levels = new ArrayList<>();

    private ProjectModel(String name, List<Level> initialLevels) {
        this.name = Objects.requireNonNull(name, "name darf nicht null sein.");
        this.levels.addAll(initialLevels);
    }

    public static ProjectModel withDefaultLevel(String projectName, String levelName) {
        return new ProjectModel(projectName, List.of(new Level(levelName)));
    }

    public String name() {
        return name;
    }

    public List<Level> levels() {
        return List.copyOf(levels);
    }

    public Level primaryLevel() {
        return levels.getFirst();
    }

    public void addLevel(Level level) {
        levels.add(Objects.requireNonNull(level, "level darf nicht null sein."));
    }
}

