package de.andreas.cadas.application.reports;

import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ConstructionDrawingPdfService {

    static final String STANDARD = "DIN EN ISO 7519 | 2025-01";
    private static final double POINTS_PER_MILLIMETER = 72.0 / 25.4;
    private static final double PAGE_WIDTH = PDRectangle.A3.getHeight();
    private static final double PAGE_HEIGHT = PDRectangle.A3.getWidth();
    private static final double MARGIN = 34.0;
    private static final double TITLE_HEIGHT = 46.0;
    private static final int[] STANDARD_SCALES = {20, 25, 50, 100, 200, 500, 1_000};
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public void export(ProjectModel project, Path targetFile) throws IOException {
        Objects.requireNonNull(project, "project darf nicht null sein.");
        Objects.requireNonNull(targetFile, "targetFile darf nicht null sein.");
        Path parent = targetFile.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (PDDocument document = new PDDocument()) {
            for (Level level : project.levels()) {
                addPlanPage(document, project, level);
                addElevationsPage(document, project, level);
            }
            addSpatialViewsPage(document, project, true);
            addSpatialViewsPage(document, project, false);
            document.save(targetFile.toFile());
        }
    }

    private void addPlanPage(PDDocument document, ProjectModel project, Level level) throws IOException {
        Bounds bounds = levelBounds(level).expanded(400.0);
        int scale = chooseScale(bounds.width(), bounds.height(), PAGE_WIDTH - 2 * MARGIN, PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT);
        try (PageCanvas canvas = addPage(document, project.name(), "2D-Grundriss – " + level.name(), "M 1:" + scale)) {
            Viewport viewport = new Viewport(MARGIN, MARGIN + TITLE_HEIGHT, PAGE_WIDTH - 2 * MARGIN,
                    PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT, bounds, scale);
            for (Room room : level.rooms()) {
                drawPolygon(canvas, viewport, room.outline(), new Color(225, 231, 221), 0.45f);
                PlanPoint center = room.centerPoint();
                canvas.text(viewport.x(center.xMillimeters()), viewport.y(center.yMillimeters()), 7.5f, room.name());
            }
            for (Wall wall : level.walls()) {
                float width = (float) Math.max(1.1, wall.thickness().toMillimeters() * viewport.factor());
                canvas.line(viewport.x(wall.axis().start().xMillimeters()), viewport.y(wall.axis().start().yMillimeters()),
                        viewport.x(wall.axis().end().xMillimeters()), viewport.y(wall.axis().end().yMillimeters()), width, Color.DARK_GRAY);
                drawIsoDimension(canvas, viewport, wall);
            }
            for (Door door : level.doors()) {
                Wall wall = level.findWall(door.wallId());
                drawOpening(canvas, viewport, wall, door.offsetFromStart().toMillimeters(), door.width().toMillimeters(), new Color(40, 105, 65));
            }
            for (WindowElement window : level.windows()) {
                Wall wall = level.findWall(window.wallId());
                drawOpening(canvas, viewport, wall, window.offsetFromStart().toMillimeters(), window.width().toMillimeters(), new Color(30, 105, 155));
            }
            for (var stair : level.staircases()) {
                canvas.rectangle(
                        viewport.x(stair.minX()), viewport.y(stair.maxY()),
                        stair.widthMillimeters() * viewport.factor(), stair.heightMillimeters() * viewport.factor(),
                        0.7f, new Color(90, 75, 55), null);
            }
        }
    }

    private void addElevationsPage(PDDocument document, ProjectModel project, Level level) throws IOException {
        try (PageCanvas canvas = addPage(document, project.name(), "Seitenaufrisse – " + level.name(), STANDARD)) {
            String[] names = {"Nord", "Ost", "Süd", "West"};
            for (int index = 0; index < 4; index++) {
                double x = MARGIN + (index % 2) * (PAGE_WIDTH - 2 * MARGIN) / 2.0;
                double y = MARGIN + TITLE_HEIGHT + (1 - index / 2) * (PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT) / 2.0;
                double width = (PAGE_WIDTH - 2 * MARGIN) / 2.0 - 12.0;
                double height = (PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT) / 2.0 - 18.0;
                drawElevation(canvas, level, index, x, y, width, height, names[index]);
            }
        }
    }

    private void drawElevation(PageCanvas canvas, Level level, int direction, double x, double y, double width, double height, String name) throws IOException {
        Bounds bounds = levelBounds(level);
        double horizontalExtent = direction % 2 == 0 ? bounds.width() : bounds.height();
        double maximumHeight = maximumHeight(level);
        int scale = chooseScale(horizontalExtent + 800.0, maximumHeight + 800.0, width, height - 18.0);
        double factor = POINTS_PER_MILLIMETER / scale;
        double left = x + (width - horizontalExtent * factor) / 2.0;
        double floor = y + 20.0;
        canvas.text(x + 4, y + height - 12, 8.5f, name + " – M 1:" + scale);
        canvas.line(left, floor, left + horizontalExtent * factor, floor, 0.6f, Color.GRAY);
        for (Wall wall : level.walls()) {
            double first = direction % 2 == 0 ? wall.axis().start().xMillimeters() - bounds.minX() : wall.axis().start().yMillimeters() - bounds.minY();
            double second = direction % 2 == 0 ? wall.axis().end().xMillimeters() - bounds.minX() : wall.axis().end().yMillimeters() - bounds.minY();
            double min = Math.min(first, second);
            double span = Math.max(Math.abs(second - first), wall.thickness().toMillimeters());
            canvas.rectangle(left + min * factor, floor, span * factor, wall.maximumHeightMillimeters() * factor,
                    0.6f, Color.DARK_GRAY, new Color(232, 232, 232));
        }
        drawOverallDimension(canvas, left, floor - 12, left + horizontalExtent * factor, floor - 12,
                formatMeters(horizontalExtent));
        drawVerticalDimension(canvas, left - 14, floor, floor + maximumHeight * factor, formatMeters(maximumHeight));
    }

    private void addSpatialViewsPage(PDDocument document, ProjectModel project, boolean isometric) throws IOException {
        String title = isometric ? "3D-ISO – vier Ansichten" : "3D-Seitenansichten";
        try (PageCanvas canvas = addPage(document, project.name(), title, STANDARD)) {
            double[] angles = isometric ? new double[]{45, 135, 225, 315} : new double[]{0, 90, 180, 270};
            for (int index = 0; index < angles.length; index++) {
                double x = MARGIN + (index % 2) * (PAGE_WIDTH - 2 * MARGIN) / 2.0;
                double y = MARGIN + TITLE_HEIGHT + (1 - index / 2) * (PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT) / 2.0;
                double width = (PAGE_WIDTH - 2 * MARGIN) / 2.0 - 12.0;
                double height = (PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT) / 2.0 - 18.0;
                drawSpatialView(canvas, project, angles[index], isometric, x, y, width, height);
            }
        }
    }

    private void drawSpatialView(PageCanvas canvas, ProjectModel project, double angleDegrees, boolean isometric,
                                 double x, double y, double width, double height) throws IOException {
        List<SpatialLine> lines = spatialLines(project, angleDegrees, isometric);
        Bounds projected = lineBounds(lines).expanded(200.0);
        int scale = chooseScale(projected.width(), projected.height(), width, height - 20.0);
        double factor = POINTS_PER_MILLIMETER / scale;
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        canvas.text(x + 4, y + height - 12, 8.5f, String.format(Locale.GERMAN, "Blick %.0f° – M 1:%d", angleDegrees, scale));
        for (SpatialLine line : lines) {
            canvas.line(centerX + (line.x1() - projected.centerX()) * factor,
                    centerY + (line.y1() - projected.centerY()) * factor,
                    centerX + (line.x2() - projected.centerX()) * factor,
                    centerY + (line.y2() - projected.centerY()) * factor,
                    line.top() ? 0.8f : 0.45f, line.top() ? Color.DARK_GRAY : Color.GRAY);
        }
        drawOverallDimension(canvas, x + 12, y + 8, x + width - 12, y + 8, "maßstabgerechte Ansicht");
    }

    private List<SpatialLine> spatialLines(ProjectModel project, double angleDegrees, boolean isometric) {
        List<SpatialLine> lines = new ArrayList<>();
        double baseHeight = 0.0;
        double angle = Math.toRadians(angleDegrees);
        for (Level level : project.levels()) {
            for (Wall wall : level.walls()) {
                SpatialPoint startBottom = project(wall.axis().start(), baseHeight, angle, isometric);
                SpatialPoint endBottom = project(wall.axis().end(), baseHeight, angle, isometric);
                SpatialPoint startTop = project(wall.axis().start(), baseHeight + wall.startHeight().toMillimeters(), angle, isometric);
                SpatialPoint endTop = project(wall.axis().end(), baseHeight + wall.endHeight().toMillimeters(), angle, isometric);
                lines.add(new SpatialLine(startBottom.x(), startBottom.y(), endBottom.x(), endBottom.y(), false));
                lines.add(new SpatialLine(startTop.x(), startTop.y(), endTop.x(), endTop.y(), true));
                lines.add(new SpatialLine(startBottom.x(), startBottom.y(), startTop.x(), startTop.y(), false));
                lines.add(new SpatialLine(endBottom.x(), endBottom.y(), endTop.x(), endTop.y(), false));
            }
            baseHeight += maximumHeight(level);
        }
        return lines;
    }

    private SpatialPoint project(PlanPoint point, double z, double angle, boolean isometric) {
        double horizontal = point.xMillimeters() * Math.cos(angle) - point.yMillimeters() * Math.sin(angle);
        double depth = point.xMillimeters() * Math.sin(angle) + point.yMillimeters() * Math.cos(angle);
        double vertical = isometric ? z - depth * 0.45 : z;
        return new SpatialPoint(horizontal, vertical);
    }

    private void drawPolygon(PageCanvas canvas, Viewport viewport, List<PlanPoint> points, Color fill, float width) throws IOException {
        if (points.isEmpty()) {
            return;
        }
        float[] coordinates = new float[points.size() * 2];
        for (int index = 0; index < points.size(); index++) {
            coordinates[index * 2] = (float) viewport.x(points.get(index).xMillimeters());
            coordinates[index * 2 + 1] = (float) viewport.y(points.get(index).yMillimeters());
        }
        canvas.polygon(coordinates, width, Color.GRAY, fill);
    }

    private void drawOpening(PageCanvas canvas, Viewport viewport, Wall wall, double offset, double width, Color color) throws IOException {
        double length = wall.axis().length().toMillimeters();
        if (length <= 0) {
            return;
        }
        PlanPoint start = wall.axis().pointAt(de.andreas.cadas.domain.geometry.Length.ofMillimeters(Math.min(offset, length)));
        PlanPoint end = wall.axis().pointAt(de.andreas.cadas.domain.geometry.Length.ofMillimeters(Math.min(offset + width, length)));
        canvas.line(viewport.x(start.xMillimeters()), viewport.y(start.yMillimeters()), viewport.x(end.xMillimeters()), viewport.y(end.yMillimeters()), 2.2f, color);
    }

    private void drawIsoDimension(PageCanvas canvas, Viewport viewport, Wall wall) throws IOException {
        double x1 = viewport.x(wall.axis().start().xMillimeters());
        double y1 = viewport.y(wall.axis().start().yMillimeters());
        double x2 = viewport.x(wall.axis().end().xMillimeters());
        double y2 = viewport.y(wall.axis().end().yMillimeters());
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.max(1, Math.hypot(dx, dy));
        double nx = -dy / length * 13.0;
        double ny = dx / length * 13.0;
        drawOverallDimension(canvas, x1 + nx, y1 + ny, x2 + nx, y2 + ny,
                formatMeters(wall.axis().length().toMillimeters()));
    }

    private void drawOverallDimension(PageCanvas canvas, double x1, double y1, double x2, double y2, String text) throws IOException {
        canvas.line(x1, y1, x2, y2, 0.45f, Color.BLACK);
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.max(1, Math.hypot(dx, dy));
        double tx = (dx / length - dy / length) * 3.2;
        double ty = (dy / length + dx / length) * 3.2;
        canvas.line(x1 - tx, y1 - ty, x1 + tx, y1 + ty, 0.55f, Color.BLACK);
        canvas.line(x2 - tx, y2 - ty, x2 + tx, y2 + ty, 0.55f, Color.BLACK);
        canvas.text((x1 + x2) / 2.0 + 4, (y1 + y2) / 2.0 + 4, 6.5f, text);
    }

    private void drawVerticalDimension(PageCanvas canvas, double x, double y1, double y2, String text) throws IOException {
        drawOverallDimension(canvas, x, y1, x, y2, text);
    }

    private PageCanvas addPage(PDDocument document, String projectName, String title, String subtitle) throws IOException {
        PDPage page = new PDPage(new PDRectangle((float) PAGE_WIDTH, (float) PAGE_HEIGHT));
        document.addPage(page);
        PageCanvas canvas = new PageCanvas(document, page);
        canvas.rectangle(16, 16, PAGE_WIDTH - 32, PAGE_HEIGHT - 32, 0.7f, Color.BLACK, null);
        canvas.line(16, MARGIN + TITLE_HEIGHT - 10, PAGE_WIDTH - 16, MARGIN + TITLE_HEIGHT - 10, 0.55f, Color.BLACK);
        canvas.text(MARGIN, 30, 8, projectName);
        canvas.boldText(MARGIN, PAGE_HEIGHT - 35, 13, title);
        String standardText = STANDARD.equals(subtitle) ? STANDARD : subtitle + " · " + STANDARD;
        canvas.text(PAGE_WIDTH - 280, PAGE_HEIGHT - 35, 8, standardText);
        return canvas;
    }

    private Bounds levelBounds(Level level) {
        List<PlanPoint> points = new ArrayList<>();
        level.walls().forEach(wall -> {
            points.add(wall.axis().start());
            points.add(wall.axis().end());
        });
        level.rooms().forEach(room -> points.addAll(room.outline()));
        level.staircases().forEach(stair -> {
            points.add(stair.firstCorner());
            points.add(stair.oppositeCorner());
        });
        if (points.isEmpty()) {
            return new Bounds(0, 0, 10_000, 7_000);
        }
        return new Bounds(
                points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0),
                points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(10_000),
                points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(7_000)
        );
    }

    private Bounds lineBounds(List<SpatialLine> lines) {
        if (lines.isEmpty()) {
            return new Bounds(0, 0, 10_000, 7_000);
        }
        double minX = lines.stream().mapToDouble(line -> Math.min(line.x1(), line.x2())).min().orElse(0);
        double minY = lines.stream().mapToDouble(line -> Math.min(line.y1(), line.y2())).min().orElse(0);
        double maxX = lines.stream().mapToDouble(line -> Math.max(line.x1(), line.x2())).max().orElse(10_000);
        double maxY = lines.stream().mapToDouble(line -> Math.max(line.y1(), line.y2())).max().orElse(7_000);
        return new Bounds(minX, minY, maxX, maxY);
    }

    private double maximumHeight(Level level) {
        return level.walls().stream().mapToDouble(Wall::maximumHeightMillimeters).max().orElse(2_750.0);
    }

    private int chooseScale(double modelWidth, double modelHeight, double availableWidthPoints, double availableHeightPoints) {
        for (int scale : STANDARD_SCALES) {
            if (modelWidth * POINTS_PER_MILLIMETER / scale <= availableWidthPoints
                    && modelHeight * POINTS_PER_MILLIMETER / scale <= availableHeightPoints) {
                return scale;
            }
        }
        return STANDARD_SCALES[STANDARD_SCALES.length - 1];
    }

    private String formatMeters(double millimeters) {
        return String.format(Locale.GERMAN, "%.2f m", millimeters / 1_000.0);
    }

    private record Bounds(double minX, double minY, double maxX, double maxY) {
        double width() {
            return Math.max(1, maxX - minX);
        }

        double height() {
            return Math.max(1, maxY - minY);
        }

        double centerX() {
            return (minX + maxX) / 2.0;
        }

        double centerY() {
            return (minY + maxY) / 2.0;
        }

        Bounds expanded(double margin) {
            return new Bounds(minX - margin, minY - margin, maxX + margin, maxY + margin);
        }
    }

    private record Viewport(double x, double y, double width, double height, Bounds bounds, int scale) {
        double factor() {
            return POINTS_PER_MILLIMETER / scale;
        }

        double x(double modelX) {
            return x + width / 2.0 + (modelX - bounds.centerX()) * factor();
        }

        double y(double modelY) {
            return y + height / 2.0 - (modelY - bounds.centerY()) * factor();
        }
    }

    private record SpatialPoint(double x, double y) {
    }

    private record SpatialLine(double x1, double y1, double x2, double y2, boolean top) {
    }

    private static final class PageCanvas implements AutoCloseable {
        private final PDPageContentStream stream;

        private PageCanvas(PDDocument document, PDPage page) throws IOException {
            stream = new PDPageContentStream(document, page);
            stream.setStrokingColor(Color.BLACK);
        }

        void line(double x1, double y1, double x2, double y2, float width, Color color) throws IOException {
            stream.setStrokingColor(color);
            stream.setLineWidth(width);
            stream.moveTo((float) x1, (float) y1);
            stream.lineTo((float) x2, (float) y2);
            stream.stroke();
        }

        void rectangle(double x, double y, double width, double height, float lineWidth, Color stroke, Color fill) throws IOException {
            stream.addRect((float) x, (float) y, (float) width, (float) height);
            stream.setLineWidth(lineWidth);
            if (fill != null) {
                stream.setNonStrokingColor(fill);
                if (stroke != null) {
                    stream.setStrokingColor(stroke);
                    stream.fillAndStroke();
                } else {
                    stream.fill();
                }
            } else {
                stream.setStrokingColor(stroke == null ? Color.BLACK : stroke);
                stream.stroke();
            }
        }

        void polygon(float[] coordinates, float lineWidth, Color stroke, Color fill) throws IOException {
            stream.moveTo(coordinates[0], coordinates[1]);
            for (int index = 2; index < coordinates.length; index += 2) {
                stream.lineTo(coordinates[index], coordinates[index + 1]);
            }
            stream.closePath();
            stream.setLineWidth(lineWidth);
            stream.setStrokingColor(stroke);
            stream.setNonStrokingColor(fill);
            stream.fillAndStroke();
        }

        void text(double x, double y, float size, String text) throws IOException {
            drawText(FONT, x, y, size, text);
        }

        void boldText(double x, double y, float size, String text) throws IOException {
            drawText(FONT_BOLD, x, y, size, text);
        }

        private void drawText(PDType1Font font, double x, double y, float size, String text) throws IOException {
            stream.beginText();
            stream.setNonStrokingColor(Color.BLACK);
            stream.setFont(font, size);
            stream.newLineAtOffset((float) x, (float) y);
            stream.showText(sanitize(text));
            stream.endText();
        }

        private String sanitize(String text) {
            return text.replace('–', '-').replace('·', '|');
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }
}
