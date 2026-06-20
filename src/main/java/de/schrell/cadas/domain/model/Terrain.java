package de.schrell.cadas.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Terrain(List<TerrainVertex> vertices) {

    public Terrain {
        Objects.requireNonNull(vertices, "vertices darf nicht null sein.");
        vertices = List.copyOf(vertices);
        if (!vertices.isEmpty() && vertices.size() < 3) {
            throw new IllegalArgumentException("Ein Gelände braucht mindestens drei äußere Ecken.");
        }
        Set<String> coordinates = new HashSet<>();
        for (TerrainVertex vertex : vertices) {
            String key = vertex.position().xMillimeters() + ":" + vertex.position().yMillimeters();
            if (!coordinates.add(key)) {
                throw new IllegalArgumentException("Geländeecken dürfen nicht doppelt vorkommen.");
            }
        }
    }

    public static Terrain empty() {
        return new Terrain(List.of());
    }

    public boolean configured() {
        return !vertices.isEmpty();
    }
}
