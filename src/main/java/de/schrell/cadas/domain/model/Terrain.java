package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Terrain(List<TerrainVertex> vertices, Length displayWidth) {

    private static final Length DEFAULT_DISPLAY_WIDTH = Length.ofMillimeters(2_000.0);

    public Terrain {
        Objects.requireNonNull(vertices, "vertices darf nicht null sein.");
        Objects.requireNonNull(displayWidth, "displayWidth darf nicht null sein.");
        vertices = List.copyOf(vertices);
        if (!vertices.isEmpty() && vertices.size() < 3) {
            throw new IllegalArgumentException("Ein Gelände braucht mindestens drei äußere Ecken.");
        }
        if (displayWidth.toMillimeters() <= 0.0) {
            throw new IllegalArgumentException("Die Geländebreite muss größer als 0 sein.");
        }
        Set<String> coordinates = new HashSet<>();
        for (TerrainVertex vertex : vertices) {
            String key = vertex.position().xMillimeters() + ":" + vertex.position().yMillimeters();
            if (!coordinates.add(key)) {
                throw new IllegalArgumentException("Geländeecken dürfen nicht doppelt vorkommen.");
            }
        }
    }

    public Terrain(List<TerrainVertex> vertices) {
        this(vertices, DEFAULT_DISPLAY_WIDTH);
    }

    public static Terrain empty() {
        return new Terrain(List.of(), DEFAULT_DISPLAY_WIDTH);
    }

    public static Length defaultDisplayWidth() {
        return DEFAULT_DISPLAY_WIDTH;
    }

    public boolean configured() {
        return !vertices.isEmpty();
    }
}
