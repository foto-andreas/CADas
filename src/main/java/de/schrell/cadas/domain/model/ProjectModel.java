package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Angle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Wurzelaggregat eines CADas-Projekts mit geordneten Etagen, Dach, Gelände und Projektorientierung.
 * Es garantiert mindestens eine primäre Etage und koordiniert etagenübergreifende Benennung und Reihenfolge.
 */
public final class ProjectModel {

    private String name;
    private final List<Level> levels = new ArrayList<>();
    private final List<SurfaceMaterial> surfaceMaterials = new ArrayList<>();
    private Roof roof;
    private Terrain terrain = Terrain.empty();
    private Angle northAngle = Angle.ofDegrees(0.0);
    private Angle frontAngle = Angle.ofDegrees(0.0);

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

    public List<SurfaceMaterial> surfaceMaterials() {
        return List.copyOf(surfaceMaterials);
    }

    public Optional<SurfaceMaterial> findSurfaceMaterial(UUID materialId) {
        return surfaceMaterials.stream().filter(material -> material.id().equals(materialId)).findFirst();
    }

    public SurfaceMaterial registerSurfaceMaterial(SurfaceMaterial material) {
        SurfaceMaterial requiredMaterial = Objects.requireNonNull(material, "material darf nicht null sein.");
        for (int index = 0; index < surfaceMaterials.size(); index++) {
            if (surfaceMaterials.get(index).id().equals(requiredMaterial.id())) {
                surfaceMaterials.set(index, requiredMaterial);
                return requiredMaterial;
            }
        }
        surfaceMaterials.add(requiredMaterial);
        return requiredMaterial;
    }

    public void replaceSurfaceMaterial(SurfaceMaterial replacement) {
        SurfaceMaterial requiredReplacement = registerSurfaceMaterial(replacement);
        for (Level level : levels) {
            for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
                for (SurfaceLayer layer : stack.layers()) {
                    if (requiredReplacement.id().equals(layer.materialId())) {
                        stack.replaceLayer(layer.id(), requiredReplacement.applyTo(layer));
                    }
                }
            }
        }
    }

    /**
     * Überführt ältere, flächenkopierte Beläge in gemeinsame Materialreferenzen.
     * Bei widersprüchlichen Altwerten wird die häufigste Werteausprägung als zentraler Materialstand gewählt.
     */
    public void normalizeSurfaceMaterials() {
        Map<String, List<LayerUsage>> usagesByReference = new LinkedHashMap<>();
        for (Level level : levels) {
            for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
                for (SurfaceLayer layer : stack.layers()) {
                    SurfaceMaterial material = SurfaceMaterial.fromLayer(layer);
                    String key = layer.materialId() == null
                            ? "legacy:" + material.legacyMigrationKey()
                            : "material:" + layer.materialId();
                    usagesByReference.computeIfAbsent(key, ignored -> new ArrayList<>())
                            .add(new LayerUsage(stack, layer, material));
                }
            }
        }
        for (Map.Entry<String, List<LayerUsage>> entry : usagesByReference.entrySet()) {
            List<LayerUsage> usages = entry.getValue();
            UUID materialId = entry.getKey().startsWith("material:")
                    ? UUID.fromString(entry.getKey().substring("material:".length()))
                    : UUID.randomUUID();
            SurfaceMaterial canonical = findSurfaceMaterial(materialId)
                    .orElseGet(() -> mostFrequentMaterial(usages).withId(materialId));
            registerSurfaceMaterial(canonical);
            for (LayerUsage usage : usages) {
                usage.stack().replaceLayer(usage.layer().id(), canonical.applyTo(usage.layer()));
            }
        }
    }

    private SurfaceMaterial mostFrequentMaterial(List<LayerUsage> usages) {
        return usages.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        usage -> usage.material().valueSignature(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ))
                .values()
                .stream()
                .max(Comparator.comparingInt(List::size))
                .orElseThrow()
                .getFirst()
                .material();
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

    public java.util.Optional<Roof> roof() {
        return java.util.Optional.ofNullable(roof);
    }

    public void defineRoof(Roof roof) {
        this.roof = Objects.requireNonNull(roof, "roof darf nicht null sein.");
    }

    public Terrain terrain() {
        return terrain;
    }

    public void defineTerrain(Terrain terrain) {
        this.terrain = Objects.requireNonNull(terrain, "terrain darf nicht null sein.");
    }

    public Angle northAngle() {
        return northAngle;
    }

    public void defineNorthAngle(Angle northAngle) {
        this.northAngle = Objects.requireNonNull(northAngle, "northAngle darf nicht null sein.");
    }

    public Angle frontAngle() {
        return frontAngle;
    }

    public void defineFrontAngle(Angle frontAngle) {
        this.frontAngle = Objects.requireNonNull(frontAngle, "frontAngle darf nicht null sein.");
    }

    public ProjectModel copy() {
        ProjectModel copy = new ProjectModel(name, List.of());
        levels.stream()
                .map(Level::copy)
                .forEach(copy.levels::add);
        copy.surfaceMaterials.addAll(surfaceMaterials);
        copy.roof = roof;
        copy.terrain = terrain;
        copy.northAngle = northAngle;
        copy.frontAngle = frontAngle;
        return copy;
    }

    public void replaceWith(ProjectModel snapshot) {
        Objects.requireNonNull(snapshot, "snapshot darf nicht null sein.");
        name = snapshot.name;
        levels.clear();
        snapshot.levels.stream()
                .map(Level::copy)
                .forEach(levels::add);
        surfaceMaterials.clear();
        surfaceMaterials.addAll(snapshot.surfaceMaterials);
        roof = snapshot.roof;
        terrain = snapshot.terrain;
        northAngle = snapshot.northAngle;
        frontAngle = snapshot.frontAngle;
    }

    public Level resetToSingleLevel(String levelName) {
        levels.clear();
        surfaceMaterials.clear();
        roof = null;
        terrain = Terrain.empty();
        northAngle = Angle.ofDegrees(0.0);
        frontAngle = Angle.ofDegrees(0.0);
        Level level = new Level(levelName);
        levels.add(level);
        return level;
    }

    private record LayerUsage(SurfaceLayerStack stack, SurfaceLayer layer, SurfaceMaterial material) {
    }
}
