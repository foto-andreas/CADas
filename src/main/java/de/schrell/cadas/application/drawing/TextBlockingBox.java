package de.schrell.cadas.application.drawing;

import java.util.List;
import java.util.Objects;

/**
 * Rechteckige Sperrfläche für die kollisionsfreie Platzierung von Bemaßungen und Raumtexten.
 *
 * <p>Wird sowohl in der 2D-Ansicht als auch im Bauzeichnung-PDF verwendet, damit
 * beide Renderpfade dieselbe Überdeckungslogik nutzen.</p>
 */
public record TextBlockingBox(double minX, double minY, double width, double height) {

    public TextBlockingBox {
        if (width < 0.0) {
            throw new IllegalArgumentException("width darf nicht negativ sein.");
        }
        if (height < 0.0) {
            throw new IllegalArgumentException("height darf nicht negativ sein.");
        }
    }

    public double maxX() {
        return minX + width;
    }

    public double maxY() {
        return minY + height;
    }

    public double centerX() {
        return minX + width / 2.0;
    }

    public double centerY() {
        return minY + height / 2.0;
    }

    public boolean overlaps(TextBlockingBox other) {
        Objects.requireNonNull(other, "other darf nicht null sein.");
        return minX < other.maxX()
                && maxX() > other.minX
                && minY < other.maxY()
                && maxY() > other.minY;
    }

    public static boolean overlapsAny(TextBlockingBox candidate, List<TextBlockingBox> existing) {
        Objects.requireNonNull(candidate, "candidate darf nicht null sein.");
        Objects.requireNonNull(existing, "existing darf nicht null sein.");
        return existing.stream().anyMatch(candidate::overlaps);
    }

    public static TextBlockingBox aroundLine(double startX, double startY, double endX, double endY, double padding) {
        if (padding < 0.0) {
            throw new IllegalArgumentException("padding darf nicht negativ sein.");
        }
        double minimumX = Math.min(startX, endX) - padding;
        double minimumY = Math.min(startY, endY) - padding;
        return new TextBlockingBox(
                minimumX,
                minimumY,
                Math.abs(endX - startX) + padding * 2.0,
                Math.abs(endY - startY) + padding * 2.0
        );
    }
}
