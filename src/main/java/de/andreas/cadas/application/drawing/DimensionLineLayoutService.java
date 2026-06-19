package de.andreas.cadas.application.drawing;

public final class DimensionLineLayoutService {

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
                (lineStartY + lineEndY) / 2.0
        );
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
            double textY
    ) {
    }
}
