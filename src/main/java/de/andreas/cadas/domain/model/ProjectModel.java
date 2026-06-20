package de.andreas.cadas.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProjectModel {

    private String name;
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

    public void rename(String newName) {
        String trimmedName = Objects.requireNonNull(newName, "newName darf nicht null sein.").trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Projektname darf nicht leer sein.");
        }
        this.name = trimmedName;
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

    public void renameLevel(Level level, String newName) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        String trimmed = Objects.requireNonNull(newName, "newName darf nicht null sein.").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Etage darf nicht leer benannt werden.");
        }
        boolean duplicate = levels.stream()
                .filter(l -> l != level)
                .anyMatch(l -> l.name().equalsIgnoreCase(trimmed));
        if (duplicate) {
            throw new IllegalArgumentException("Etage mit Name `" + trimmed + "` existiert bereits.");
        }
        level.rename(trimmed);
    }

    public void moveLevel(Level level, int newIndex) {
        Objects.requireNonNull(level, "level darf nicht null sein.");
        int currentIndex = levels.indexOf(level);
        if (currentIndex < 0) {
            throw new IllegalArgumentException("Etage ist nicht Teil des Projekts.");
        }
        if (newIndex < 0 || newIndex >= levels.size()) {
            throw new IndexOutOfBoundsException("Neuer Etage-Index " + newIndex + " außerhalb des gültigen Bereichs.");
        }
        if (newIndex == currentIndex) {
            return;
        }
        levels.remove(currentIndex);
        levels.add(newIndex, level);
    }

    public void moveLevelUp(Level level) {
        // "Hoch" bedeutet im Gebäude nach oben = größerer Index (Index 0 ist die unterste Etage).
        int currentIndex = levels.indexOf(level);
        if (currentIndex >= 0 && currentIndex < levels.size() - 1) {
            moveLevel(level, currentIndex + 1);
        }
    }

    public void moveLevelDown(Level level) {
        // "Runter" bedeutet im Gebäude nach unten = kleinerer Index.
        int currentIndex = levels.indexOf(level);
        if (currentIndex > 0) {
            moveLevel(level, currentIndex - 1);
        }
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
        name = snapshot.name;
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
