package de.schrell.cadas.ui;

import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.application.layers.SurfaceRectangleTileLayoutService;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;

import java.util.function.DoubleUnaryOperator;

/** Zeichnet die aus Heizkreisdaten abgeleiteten Variotherm-Nuten als maßstäbliche 2D-Überlagerung. */
final class CadWorkbenchVariothermGrooveRenderer {

    private Image patternImage;
    private double patternPitchPixels = -1.0;

    void drawPanelGrooves(
            GraphicsContext graphics,
            SurfaceRectangleTileLayoutService.PlacedSurfaceTile tile,
            double scale,
            DoubleUnaryOperator toScreenX,
            DoubleUnaryOperator toScreenY
    ) {
        double pitch = SurfaceCoveringPresetService.VARIOTHERM_GROOVE_PITCH_MILLIMETERS;
        double radius = Math.max(1.0, (pitch - SurfaceCoveringPresetService.VARIOTHERM_PIPE_DIAMETER_MILLIMETERS) / 2.0);
        double pitchPixels = pitch * scale;
        double radiusPixels = radius * scale;
        double screenX = toScreenX.applyAsDouble(tile.x());
        double screenY = toScreenY.applyAsDouble(tile.y());
        graphics.save();
        graphics.setFill(pattern(toScreenX.applyAsDouble(tile.fullX()), toScreenY.applyAsDouble(tile.fullY()), pitchPixels, radiusPixels, scale));
        graphics.fillRect(screenX, screenY, tile.width() * scale, tile.height() * scale);
        graphics.restore();
    }

    private ImagePattern pattern(double tileScreenX, double tileScreenY, double pitchPixels, double radiusPixels, double scale) {
        return new ImagePattern(patternImage(pitchPixels, radiusPixels, scale), tileScreenX, tileScreenY, pitchPixels, pitchPixels, false);
    }

    private Image patternImage(double pitchPixels, double radiusPixels, double scale) {
        if (patternImage != null && Math.abs(patternPitchPixels - pitchPixels) <= 0.01) {
            return patternImage;
        }
        double canvasSize = Math.max(2.0, pitchPixels);
        Canvas patternCanvas = new Canvas(canvasSize, canvasSize);
        GraphicsContext patternGraphics = patternCanvas.getGraphicsContext2D();
        patternGraphics.setStroke(Color.color(0.18, 0.36, 0.44, 0.48));
        patternGraphics.setLineWidth(Math.max(0.45, 1.2 * scale));
        patternGraphics.strokeOval(
                (canvasSize - radiusPixels * 2.0) / 2.0,
                (canvasSize - radiusPixels * 2.0) / 2.0,
                radiusPixels * 2.0,
                radiusPixels * 2.0
        );
        patternImage = patternCanvas.snapshot(null, null);
        patternPitchPixels = pitchPixels;
        return patternImage;
    }
}
