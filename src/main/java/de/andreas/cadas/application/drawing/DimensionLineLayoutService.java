package de.andreas.cadas.application.drawing;

public final class DimensionLineLayoutService {

    /**
     * Legt Maß-/Hilfs-/Begrenzungslinien sowie den Textmittelpunkt an.
     *
     * <p>Der Textmittelpunkt liegt auf der Maßlinienmitte. Für "Text von der Linie weg"
     * liefert {@link #textOffsetAwayFromLine(double, double)} den Normalenvektor-basierten
     * Versatz, sodass Aufrufer den Text auf der Außenseite der Linie platzieren können.</p>
     *
     * @param startX       Bildschirm-x des ersten Maßpunkts
     * @param startY       Bildschirm-y des ersten Maßpunkts
     * @param endX         Bildschirm-x des zweiten Maßpunkts
     * @param endY         Bildschirm-y des zweiten Maßpunkts
     * @param normalOffset Normalen-Versatz der Maßlinie gegenüber der Achse
     *                     (positiv in Normalenrichtung, negativ entgegen)
     * @return das Layout mit allen Linien und dem Textmittelpunkt
     */
    public DimensionLineLayout layout(double startX, double startY, double endX, double endY, double normalOffset) {
        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.max(1.0, Math.hypot(dx, dy));
        double normalX = -dy / length;
        double normalY = dx / length;
        double lineStartX = startX + normalX * normalOffset;
        double lineStartY = startY + normalY * normalOffset;
        double lineEndX = endX + normalX * normalOffset;
        double lineEndY = endY + normalY * normalOffset;
        return new DimensionLineLayout(
                startX, startY, lineStartX, lineStartY,
                endX, endY, lineEndX, lineEndY,
                lineStartX, lineStartY, lineEndX, lineEndY,
                (lineStartX + lineEndX) / 2.0,
                (lineStartY + lineEndY) / 2.0,
                normalX, normalY
        );
    }

    /**
     * Liefert den Versatz des Textes von der Maßlinienmitte weg in Normalenrichtung.
     *
     * <p>Der Versatz zeigt immer "von der Linie weg", also in Richtung des
     * {@code placementSideSign}. Für {@code placementSideSign = 1} liegt der Text
     * auf der positiven Normalenseite, für {@code -1} auf der negativen.</p>
     *
     * @param layout              das berechnete Linienlayout
     * @param placementSideSign   Seite, auf der das Maß liegt (-1 oder 1)
     * @param distance            Abstand des Textes von der Maßlinie
     * @return ein Punkt-Versatz (deltaX, deltaY)
     */
    public TextDelta textOffsetAwayFromLine(DimensionLineLayout layout, double placementSideSign, double distance) {
        double sign = placementSideSign >= 0.0 ? 1.0 : -1.0;
        return new TextDelta(layout.normalX() * sign * distance, layout.normalY() * sign * distance);
    }

    public record DimensionLineLayout(
            double firstExtensionStartX,
            double firstExtensionStartY,
            double firstExtensionEndX,
            double firstExtensionEndY,
            double secondExtensionStartX,
            double secondExtensionStartY,
            double secondExtensionEndX,
            double secondExtensionEndY,
            double lineStartX,
            double lineStartY,
            double lineEndX,
            double lineEndY,
            double textX,
            double textY,
            double normalX,
            double normalY
    ) {
    }

    public record TextDelta(double deltaX, double deltaY) {
    }
}
