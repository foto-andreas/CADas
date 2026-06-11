package de.andreas.cadas.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProjectModel {

    private final String name;
    private final List<Level> levels = new ArrayList<>();
    private Roof roof;

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

    public Level createLevel(String levelName) {
        Level level = new Level(levelName);
        addLevel(level);
        return level;
    }

    public java.util.Optional<Roof> roof() {
        return java.util.Optional.ofNullable(roof);
    }

    public void defineRoof(Roof roof) {
        this.roof = Objects.requireNonNull(roof, "roof darf nicht null sein.");
    }

    public ProjectModel copy() {
        ProjectModel copy = new ProjectModel(name, List.of());
        levels.stream()
                .map(Level::copy)
                .forEach(copy.levels::add);
        copy.roof = roof;
        return copy;
    }

    public void replaceWith(ProjectModel snapshot) {
        Objects.requireNonNull(snapshot, "snapshot darf nicht null sein.");
        if (!name.equals(snapshot.name)) {
            throw new IllegalArgumentException("Projektname darf beim Wiederherstellen nicht geändert werden.");
        }
        levels.clear();
        snapshot.levels.stream()
                .map(Level::copy)
                .forEach(levels::add);
        roof = snapshot.roof;
    }

    public Level resetToSingleLevel(String levelName) {
        levels.clear();
        roof = null;
        Level level = new Level(levelName);
        levels.add(level);
        return level;
    }
}
