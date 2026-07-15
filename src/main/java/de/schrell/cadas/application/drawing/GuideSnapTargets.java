package de.schrell.cadas.application.drawing;

import java.util.List;

/**
 * Unveränderliche Menge vertikaler X- und horizontaler Y-Hilfslinien in Weltmillimetern.
 */
public record GuideSnapTargets(List<Double> verticalGuides, List<Double> horizontalGuides) {

    public GuideSnapTargets {
        verticalGuides = List.copyOf(verticalGuides);
        horizontalGuides = List.copyOf(horizontalGuides);
    }

    public static GuideSnapTargets empty() {
        return new GuideSnapTargets(List.of(), List.of());
    }
}
