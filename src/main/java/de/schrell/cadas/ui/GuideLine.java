package de.schrell.cadas.ui;

/** Horizontale oder vertikale Hilfslinie an einer Weltkoordinate in Millimetern. */
public record GuideLine(GuideOrientation orientation, double worldMillimeters) {
}
