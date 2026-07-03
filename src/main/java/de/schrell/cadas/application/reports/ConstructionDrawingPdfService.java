package de.schrell.cadas.application.reports;

import de.schrell.cadas.application.drawing.DimensionLabelOptions;
import de.schrell.cadas.application.drawing.DimensionLabelPlacementService;
import de.schrell.cadas.application.drawing.DimensionLabelService;
import de.schrell.cadas.application.drawing.DimensionLineLayoutService;
import de.schrell.cadas.application.drawing.TextBlockingBox;
import de.schrell.cadas.application.drawing.WallDimensionPlacementService;
import de.schrell.cadas.application.drawing.WallDimensionService;
import de.schrell.cadas.application.heating.RoomHeatingOutputService;
import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.view.ProjectionMode;
import de.schrell.cadas.application.terrain.TerrainContourService;
import de.schrell.cadas.application.terrain.TerrainProfileService;
import de.schrell.cadas.application.view.WallPlanOutlineService;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import de.schrell.cadas.domain.model.WindowElement;
import de.schrell.cadas.ui.ThreeDViewPreset;
import de.schrell.cadas.ui.ThreeDViewport;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;

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
    private static final float PDF_FONT_SIZE = 6.5f;
    private static final double PDF_TEXT_AWAY_DISTANCE = 4.0;
    private static final double PDF_PARALLEL_TEXT_AWAY_DISTANCE = 8.0;
    private static final double PDF_DIMENSION_LINE_BLOCKING_PADDING = 2.0;
    private static final double SPATIAL_VIEW_RENDER_FACTOR = 2.8;
    private static final double SPATIAL_VIEW_MINIMUM_PIXELS = 560.0;
    private static final double STANDARD_SPATIAL_DEPTH_FACTOR = 0.45;
    private static final double MAXIMUM_SAME_LEVEL_DEPTH_SHIFT_RATIO = 0.55;
    private static final String RASTER_SUBTITLE = "Grafische Rasteransicht";
    private static final AtomicBoolean JAVA_FX_STARTED = new AtomicBoolean();
    private final WallDimensionService wallDimensionService = new WallDimensionService();
    private final WallDimensionPlacementService wallDimensionPlacementService = new WallDimensionPlacementService();
    private final DimensionLineLayoutService dimensionLineLayoutService = new DimensionLineLayoutService();
    private final DimensionLabelService dimensionLabelService = new DimensionLabelService();
    private final DimensionLabelPlacementService dimensionLabelPlacementService = new DimensionLabelPlacementService();
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();
    private final HydronicHeatingLayoutService hydronicHeatingLayoutService = new HydronicHeatingLayoutService();
    private final RoomHeatingOutputService roomHeatingOutputService = new RoomHeatingOutputService();
    private final WallPlanOutlineService wallPlanOutlineService = new WallPlanOutlineService();
    private final TerrainContourService terrainContourService = new TerrainContourService();
    private final TerrainProfileService terrainProfileService = new TerrainProfileService();

    public enum GraphicVariant {
        VEKTOR,
        RASTERGRAFIK
    }

    public record ExportAssets(
            GraphicVariant graphicVariant,
            Map<String, BufferedImage> levelPlanImages,
            Map<String, BufferedImage> heatingPlanImages
    ) {
        public ExportAssets {
            Objects.requireNonNull(graphicVariant, "graphicVariant darf nicht null sein.");
            levelPlanImages = Map.copyOf(levelPlanImages);
            heatingPlanImages = Map.copyOf(heatingPlanImages);
        }

        public static ExportAssets empty() {
            return new ExportAssets(GraphicVariant.VEKTOR, Map.of(), Map.of());
        }
    }

    @FunctionalInterface
    public interface ProgressListener {
        void update(double progress, String section);

        static ProgressListener noOp() {
            return (progress, section) -> {
            };
        }
    }

    public void export(ProjectModel project, Path targetFile) throws IOException {
        export(project, targetFile, ConstructionDrawingOptions.defaults());
    }

    public void export(ProjectModel project, Path targetFile, ConstructionDrawingOptions options) throws IOException {
        export(project, targetFile, options, ExportAssets.empty(), ProgressListener.noOp());
    }

    public void export(
            ProjectModel project,
            Path targetFile,
            ConstructionDrawingOptions options,
            ExportAssets exportAssets
    ) throws IOException {
        export(project, targetFile, options, exportAssets, ProgressListener.noOp());
    }

    public void export(
            ProjectModel project,
            Path targetFile,
            ConstructionDrawingOptions options,
            ProgressListener progressListener
    ) throws IOException {
        export(project, targetFile, options, ExportAssets.empty(), progressListener);
    }

    public void export(
            ProjectModel project,
            Path targetFile,
            ConstructionDrawingOptions options,
            ExportAssets exportAssets,
            ProgressListener progressListener
    ) throws IOException {
        Objects.requireNonNull(project, "project darf nicht null sein.");
        Objects.requireNonNull(targetFile, "targetFile darf nicht null sein.");
        Objects.requireNonNull(options, "options darf nicht null sein.");
        Objects.requireNonNull(exportAssets, "exportAssets darf nicht null sein.");
        Objects.requireNonNull(progressListener, "progressListener darf nicht null sein.");
        Path parent = targetFile.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        int totalSteps = exportStepCount(project);
        int completedSteps = 0;
        try (PDDocument document = new PDDocument()) {
            for (Level level : project.levels()) {
                reportProgress(progressListener, completedSteps, totalSteps, planPageTitle(level));
                BufferedImage levelPlanImage = exportAssets.graphicVariant() == GraphicVariant.RASTERGRAFIK
                        ? exportAssets.levelPlanImages().get(level.name())
                        : null;
                if (levelPlanImage != null) {
                    addRasterPage(document, project.name(), planPageTitle(level), levelPlanImage);
                } else {
                    addPlanPage(document, project, level, options);
                }
                completedSteps++;
                for (HeatingSurfacePosition surfacePosition : HeatingSurfacePosition.values()) {
                    if (hasHeatingSurfacePage(level, surfacePosition)) {
                        reportProgress(progressListener, completedSteps, totalSteps, heatingPageTitle(level, surfacePosition));
                        BufferedImage heatingPlanImage = exportAssets.graphicVariant() == GraphicVariant.RASTERGRAFIK
                                ? exportAssets.heatingPlanImages().get(heatingPageKey(level.name(), surfacePosition))
                                : null;
                        if (heatingPlanImage != null) {
                            addRasterPage(document, project.name(), heatingPageTitle(level, surfacePosition), heatingPlanImage);
                        } else {
                            addHeatingPage(document, project, level, surfacePosition);
                        }
                        completedSteps++;
                    }
                }
                if (!heatingElementEntries(level).isEmpty()) {
                    reportProgress(progressListener, completedSteps, totalSteps, heatingElementPageTitle(level));
                    addHeatingElementsPage(document, project, level);
                    completedSteps++;
                }
            }
            reportProgress(progressListener, completedSteps, totalSteps, "3D-ISO – gesamtes Gebäude");
            addSpatialViewsPage(document, project, true);
            completedSteps++;
            reportProgress(progressListener, completedSteps, totalSteps, "Seitenansichten – gesamtes Gebäude");
            addSpatialViewsPage(document, project, false);
            completedSteps++;
            reportProgress(progressListener, completedSteps, totalSteps, "PDF-Datei wird geschrieben");
            saveAtomically(document, targetFile.toAbsolutePath().normalize());
            reportProgress(progressListener, totalSteps, totalSteps, "Bauzeichnung abgeschlossen");
        }
    }

    private int exportStepCount(ProjectModel project) {
        int totalSteps = 3;
        for (Level level : project.levels()) {
            totalSteps++;
            for (HeatingSurfacePosition surfacePosition : HeatingSurfacePosition.values()) {
                if (hasHeatingSurfacePage(level, surfacePosition)) {
                    totalSteps++;
                }
            }
            if (!heatingElementEntries(level).isEmpty()) {
                totalSteps++;
            }
        }
        return totalSteps;
    }

    private boolean hasHeatingSurfacePage(Level level, HeatingSurfacePosition surfacePosition) {
        return level.hydronicHeatings().stream().anyMatch(heating -> heating.surfacePosition() == surfacePosition)
                || !heatingSurfaceObjectEntries(level, surfacePosition).isEmpty();
    }

    private List<HeatingElementEntry> heatingSurfaceObjectEntries(Level level, HeatingSurfacePosition surfacePosition) {
        RoomObjectHeatingType heatingType = surfacePosition == HeatingSurfacePosition.CEILING
                ? RoomObjectHeatingType.CEILING_HEATING
                : RoomObjectHeatingType.FLOOR_HEATING;
        return heatingObjectEntries(level, heatingType);
    }

    private List<HeatingElementEntry> heatingObjectEntries(Level level, RoomObjectHeatingType heatingType) {
        List<HeatingElementEntry> entries = new ArrayList<>();
        for (Room room : level.rooms()) {
            for (RoomHeatingOutputService.HeatingElementSummary summary : roomHeatingOutputService.heatingElements(level, room)) {
                if (summary.heatingType() != heatingType) {
                    continue;
                }
                level.roomObjects().stream()
                        .filter(RoomObject::visible)
                        .filter(roomObject -> roomObject.id().toString().equals(summary.objectId()))
                        .findFirst()
                        .ifPresent(roomObject -> entries.add(new HeatingElementEntry(room, roomObject)));
            }
        }
        return List.copyOf(entries);
    }

    private void reportProgress(ProgressListener progressListener, int completedSteps, int totalSteps, String section) {
        double boundedProgress = totalSteps <= 0
                ? 1.0
                : Math.max(0.0, Math.min(1.0, completedSteps / (double) totalSteps));
        progressListener.update(boundedProgress, section);
    }

    private void saveAtomically(PDDocument document, Path targetFile) throws IOException {
        Path parent = targetFile.getParent();
        Path tempFile = Files.createTempFile(parent, tempFilePrefix(targetFile), ".pdf.tmp");
        try {
            document.save(tempFile.toFile());
            try {
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String tempFilePrefix(Path targetFile) {
        String fileName = targetFile.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        String prefix = fileName.length() < 3 ? "pdf_" + fileName : fileName;
        return prefix.length() < 3 ? "pdf" : prefix;
    }

    private String planPageTitle(Level level) {
        return "2D-Grundriss – " + level.name();
    }

    private String heatingPageTitle(Level level, HeatingSurfacePosition surfacePosition) {
        return "Heizflächen " + surfacePosition + " – " + level.name();
    }

    private String heatingElementPageTitle(Level level) {
        return "Heizelemente – " + level.name();
    }

    private String heatingPageKey(String levelName, HeatingSurfacePosition surfacePosition) {
        return levelName + "\u0000" + surfacePosition.name();
    }

    private void addRasterPage(
            PDDocument document,
            String projectName,
            String title,
            BufferedImage image
    ) throws IOException {
        try (PageCanvas canvas = addPage(document, projectName, title, RASTER_SUBTITLE)) {
            drawFittedImage(
                    canvas,
                    image,
                    MARGIN + 2.0,
                    MARGIN + 6.0,
                    PAGE_WIDTH - 2.0 * MARGIN - 4.0,
                    PAGE_HEIGHT - 2.0 * MARGIN - TITLE_HEIGHT - 12.0
            );
        }
    }

    private void addPlanPage(PDDocument document, ProjectModel project, Level level, ConstructionDrawingOptions options) throws IOException {
        Viewport viewport = planViewport(level, options);
        try (PageCanvas canvas = addPage(document, project.name(), planPageTitle(level), "M 1:" + viewport.scale())) {
            for (Room room : level.rooms()) {
                drawPolygon(canvas, viewport, room.outline(), new Color(225, 231, 221), 0.45f);
            }
            for (FloorOpening opening : level.floorOpenings()) {
                drawFloorOpening(canvas, viewport, opening);
            }
            for (HeatingExclusionArea area : level.heatingExclusionAreas()) {
                drawHeatingExclusionArea(canvas, viewport, area);
            }
            for (var extension : level.floorExtensions()) {
                drawPolygon(canvas, viewport, extension.outline(), new Color(222, 210, 190), 0.7f);
                canvas.text(viewport.x(extension.minX()) + 5, viewport.y(extension.minY()) - 12, 7.5f, extension.type().toString());
            }
            for (Wall wall : level.walls()) {
                drawWall(canvas, viewport, wall, new Color(145, 145, 140), Color.DARK_GRAY, 0.85f);
            }
            for (Door door : level.doors()) {
                Wall wall = level.findWall(door.wallId());
                drawOpening(canvas, viewport, wall, door.offsetFromStart().toMillimeters(), door.width().toMillimeters(), new Color(40, 105, 65));
            }
            for (WindowElement window : level.windows()) {
                Wall wall = level.findWall(window.wallId());
                drawOpening(canvas, viewport, wall, window.offsetFromStart().toMillimeters(), window.width().toMillimeters(), new Color(30, 105, 155));
            }
            for (RoomObject roomObject : level.roomObjects()) {
                if (roomObject.visible() && !isHeatingRoomObject(roomObject)) {
                    drawRoomObject(canvas, viewport, roomObject);
                }
            }
            for (var stair : level.staircases()) {
                canvas.rectangle(
                        viewport.x(stair.minX()), viewport.y(stair.maxY()),
                        stair.widthMillimeters() * viewport.factor(), stair.heightMillimeters() * viewport.factor(),
                        0.7f, new Color(90, 75, 55), null);
            }
            // Raumtexte zuerst zeichnen, damit ihre Sperrflächen als Seed-Blocker für die Bemaßung dienen.
            List<TextBlockingBox> roomBlockers = drawRoomLabels(canvas, viewport, level, options);
            if (options.showDimensions()) {
                drawWallDimensions(canvas, viewport, level, options, roomBlockers);
            }
        }
    }

    private void addHeatingPage(
            PDDocument document,
            ProjectModel project,
            Level level,
            HeatingSurfacePosition surfacePosition
    ) throws IOException {
        List<HydronicHeating> heatings = level.hydronicHeatings().stream()
                .filter(heating -> heating.surfacePosition() == surfacePosition)
                .toList();
        List<HeatingElementEntry> heatingObjects = heatingSurfaceObjectEntries(level, surfacePosition);
        Viewport viewport = heatingViewport(level, heatings);
        String title = heatingPageTitle(level, surfacePosition);
        try (PageCanvas canvas = addPage(document, project.name(), title, "M 1:" + viewport.scale())) {
            for (Room room : level.rooms()) {
                drawPolygon(canvas, viewport, room.outline(), new Color(247, 247, 244), 0.35f);
            }
            for (FloorOpening opening : level.floorOpenings()) {
                drawFloorOpening(canvas, viewport, opening);
            }
            for (HeatingExclusionArea area : level.heatingExclusionAreas()) {
                drawHeatingExclusionArea(canvas, viewport, area);
            }
            for (Wall wall : level.walls()) {
                drawWall(canvas, viewport, wall, new Color(222, 222, 217), new Color(120, 120, 120), 0.7f);
            }
            Color zoneColor = surfacePosition == HeatingSurfacePosition.FLOOR
                    ? new Color(180, 45, 38)
                    : new Color(35, 105, 160);
            Color supplyColor = new Color(211, 59, 50);
            Color returnColor = new Color(31, 98, 208);
            for (HydronicHeating heating : heatings) {
                List<HydronicHeatingLayoutService.CircuitLayout> circuits = hydronicHeatingLayoutService.layoutBestEffort(heating).circuits();
                for (HeatingZone zone : heating.zones()) {
                    drawHeatingZone(canvas, viewport, zone, circuits, zoneColor);
                }
                for (HydronicHeatingLayoutService.CircuitLayout circuit : circuits) {
                    drawHeatingPath(canvas, viewport, heating, circuit.fieldSupplyPath().reversed(), circuit, supplyColor);
                    drawHeatingPath(canvas, viewport, heating, circuit.fieldReturnPath(), circuit, returnColor);
                }
            }
            for (HeatingElementEntry entry : heatingObjects) {
                drawHeatingElement(canvas, viewport, entry);
            }
        }
    }

    private void drawHeatingPath(
            PageCanvas canvas,
            Viewport viewport,
            HydronicHeating heating,
            List<PlanPoint> path,
            HydronicHeatingLayoutService.CircuitLayout circuit,
            Color color
    ) throws IOException {
        if (path.size() < 2) {
            return;
        }
        List<ScreenPoint> screenPath = path.stream()
                .map(point -> new ScreenPoint(viewport.x(point.xMillimeters()), viewport.y(point.yMillimeters())))
                .toList();
        canvas.roundedPolyline(
                screenPath,
                (float) Math.max(0.8, heating.pipeDiameter().toMillimeters() * viewport.factor()),
                color,
                circuit.bendRadius().toMillimeters() * viewport.factor()
        );
    }

    private void drawHeatingZone(
            PageCanvas canvas,
            Viewport viewport,
            HeatingZone zone,
            List<HydronicHeatingLayoutService.CircuitLayout> circuits,
            Color color
    ) throws IOException {
        float[] coordinates = new float[zone.outline().size() * 2];
        for (int index = 0; index < zone.outline().size(); index++) {
            coordinates[index * 2] = (float) viewport.x(zone.outline().get(index).xMillimeters());
            coordinates[index * 2 + 1] = (float) viewport.y(zone.outline().get(index).yMillimeters());
        }
        canvas.polygon(coordinates, 0.45f, color, new Color(255, 255, 255));
        PlanPoint center = polygonCenter(zone.outline());
        double pipeLength = circuits.stream()
                .filter(circuit -> circuit.zoneId().equals(zone.id()))
                .findFirst()
                .map(circuit -> circuit.pipeLength().toMillimeters())
                .orElse(0.0);
        canvas.text(
                viewport.x(center.xMillimeters()) + 4.0,
                viewport.y(center.yMillimeters()) + 4.0,
                6.5f,
                String.format(
                        Locale.GERMAN,
                        "%s | %s | HKL %.1f m | %.2f m² | %.0f W",
                        zone.name(), zone.layoutPattern(), pipeLength / 1_000.0,
                        zone.areaSquareMeters(), zone.heatOutputWatts()
                )
        );
    }

    private void drawHeatingConnectorLabel(
            PageCanvas canvas,
            Viewport viewport,
            PlanPoint point,
            String label,
            Color color
    ) throws IOException {
        double x = viewport.x(point.xMillimeters());
        double y = viewport.y(point.yMillimeters());
        canvas.rectangle(x - 5.0, y - 5.0, 10.0, 10.0, 0.8f, color, Color.WHITE);
        canvas.text(x - 2.2, y - 2.3, 6.0f, label);
    }

    private boolean isHeatingRoomObject(RoomObject roomObject) {
        return roomObject.heatOutputWatts() > 0.0 && roomObject.heatingType().isHeated();
    }

    private void addHeatingElementsPage(
            PDDocument document,
            ProjectModel project,
            Level level
    ) throws IOException {
        List<HeatingElementEntry> entries = heatingElementEntries(level);
        Viewport viewport = heatingElementViewport(level, entries);
        try (PageCanvas canvas = addPage(document, project.name(), heatingElementPageTitle(level), "M 1:" + viewport.scale())) {
            for (Room room : level.rooms()) {
                drawPolygon(canvas, viewport, room.outline(), new Color(247, 247, 244), 0.35f);
            }
            for (Wall wall : level.walls()) {
                drawWall(canvas, viewport, wall, new Color(222, 222, 217), new Color(120, 120, 120), 0.7f);
            }
            for (HeatingElementEntry entry : entries) {
                drawHeatingElement(canvas, viewport, entry);
            }
        }
    }

    private Viewport heatingElementViewport(Level level, List<HeatingElementEntry> entries) {
        List<PlanPoint> points = new ArrayList<>();
        entries.forEach(entry -> {
            points.addAll(entry.room().outline());
            RoomObject roomObject = entry.roomObject();
            points.add(new PlanPoint(roomObject.minXMillimeters(), roomObject.minYMillimeters()));
            points.add(new PlanPoint(roomObject.maxXMillimeters(), roomObject.maxYMillimeters()));
        });
        Bounds bounds = points.isEmpty() ? levelBounds(level) : new Bounds(
                points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(10_000.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(7_000.0)
        );
        Bounds expandedBounds = bounds.expanded(400.0);
        int scale = chooseScale(
                expandedBounds.width(), expandedBounds.height(),
                PAGE_WIDTH - 2 * MARGIN, PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT
        );
        return centeredViewport(expandedBounds, scale);
    }

    private void drawHeatingElement(PageCanvas canvas, Viewport viewport, HeatingElementEntry entry) throws IOException {
        RoomObject roomObject = entry.roomObject();
        canvas.rectangle(
                viewport.x(roomObject.minXMillimeters()),
                viewport.y(roomObject.maxYMillimeters()),
                roomObject.footprintWidthMillimeters() * viewport.factor(),
                roomObject.footprintDepthMillimeters() * viewport.factor(),
                0.8f,
                new Color(182, 82, 36),
                new Color(255, 245, 236)
        );
        canvas.text(
                viewport.x(roomObject.center().xMillimeters()) + 5.0,
                viewport.y(roomObject.center().yMillimeters()) - 5.0,
                6.5f,
                heatingElementLabel(entry)
        );
    }

    private String heatingElementLabel(HeatingElementEntry entry) {
        RoomObject roomObject = entry.roomObject();
        if (roomObject.heatingType().countsAsCeilingHeating()) {
            return String.format(
                    Locale.GERMAN,
                    "%s | %s | %.0f W",
                    roomObject.name(),
                    roomObject.heatingType(),
                    roomObject.heatOutputWatts()
            );
        }
        return String.format(
                Locale.GERMAN,
                "%s | %s | %s | %.0f W",
                entry.room().name(),
                roomObject.name(),
                roomObject.heatingType(),
                roomObject.heatOutputWatts()
        );
    }

    private List<HeatingElementEntry> heatingElementEntries(Level level) {
        List<HeatingElementEntry> entries = new ArrayList<>();
        for (Room room : level.rooms()) {
            for (RoomHeatingOutputService.HeatingElementSummary summary : roomHeatingOutputService.heatingElements(level, room)) {
                if (summary.heatingType().countsAsFloorHeating() || summary.heatingType().countsAsCeilingHeating()) {
                    continue;
                }
                level.roomObjects().stream()
                        .filter(RoomObject::visible)
                        .filter(roomObject -> roomObject.id().toString().equals(summary.objectId()))
                        .findFirst()
                        .ifPresent(roomObject -> entries.add(new HeatingElementEntry(room, roomObject)));
            }
        }
        return List.copyOf(entries);
    }

    private PlanPoint polygonCenter(List<PlanPoint> points) {
        return new PlanPoint(
                points.stream().mapToDouble(PlanPoint::xMillimeters).average().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).average().orElse(0.0)
        );
    }

    private List<TextBlockingBox> drawRoomLabels(PageCanvas canvas, Viewport viewport, Level level, ConstructionDrawingOptions options) throws IOException {
        List<TextBlockingBox> blockers = new ArrayList<>();
        if (!options.showAreaVolume()) {
            for (Room room : level.rooms()) {
                PlanPoint center = room.centerPoint();
                double cx = viewport.x(center.xMillimeters());
                double cy = viewport.y(center.yMillimeters());
                canvas.text(cx - 26, cy, 7.5f, room.name());
                blockers.add(textBoundsApproximate(room.name(), cx - 26, cy, 7.5f));
            }
            return blockers;
        }
        for (Room room : level.rooms()) {
            PlanPoint center = room.centerPoint();
            double cx = viewport.x(center.xMillimeters());
            double cy = viewport.y(center.yMillimeters());
            String name = room.name();
            canvas.text(cx - 26, cy - 6, 7.5f, name);
            blockers.add(textBoundsApproximate(name, cx - 26, cy - 6, 7.5f));
            String area = String.format(Locale.GERMAN, "%.2f m²", effectiveAreaSquareMeters(level, room));
            canvas.text(cx - 18, cy + 10, 7.0f, area);
            blockers.add(textBoundsApproximate(area, cx - 18, cy + 10, 7.0f));
        }
        return blockers;
    }

    private double effectiveAreaSquareMeters(Level level, Room room) {
        return surfaceLayerEffectService.effectiveAreaSquareMeters(level, room);
    }

    private TextBlockingBox textBoundsApproximate(String text, double x, double y, float fontSize) {
        double approxWidth = text.length() * fontSize * 0.55;
        double approxHeight = fontSize * 1.2;
        double padding = 4.0;
        return new TextBlockingBox(x - padding, y - approxHeight - padding, approxWidth + padding * 2.0, approxHeight + padding * 2.0);
    }

    private void addSpatialViewsPage(PDDocument document, ProjectModel project, boolean isometric) throws IOException {
        String title = isometric ? "3D-ISO – gesamtes Gebäude" : "Seitenansichten – gesamtes Gebäude";
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
        BufferedImage renderedImage = renderSpatialViewImage(project, angleDegrees, isometric, width, height).orElse(null);
        if (renderedImage != null) {
            canvas.text(x + 4, y + height - 12, 8.5f, spatialViewGraphicLabel(angleDegrees, isometric));
            drawFittedImage(canvas, renderedImage, x + 2, y + 8, width - 4, height - 24);
            drawOverallDimension(canvas, x + 12, y + 8, x + width - 12, y + 8, "grafische 3D-Ansicht");
            return;
        }
        List<SpatialLine> lines = spatialLines(project, angleDegrees, isometric);
        Bounds projected = lineBounds(lines).expanded(200.0);
        int scale = chooseScale(projected.width(), projected.height(), width, height - 20.0);
        double factor = POINTS_PER_MILLIMETER / scale;
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        canvas.text(x + 4, y + height - 12, 8.5f, spatialViewFallbackLabel(angleDegrees, isometric, scale));
        for (SpatialLine line : lines) {
            canvas.line(centerX + (line.x1() - projected.centerX()) * factor,
                    centerY + (line.y1() - projected.centerY()) * factor,
                    centerX + (line.x2() - projected.centerX()) * factor,
                    centerY + (line.y2() - projected.centerY()) * factor,
                    line.terrain() || line.top() ? 0.8f : 0.45f,
                    line.terrain() ? new Color(166, 130, 78) : line.top() ? Color.DARK_GRAY : Color.GRAY);
        }
        drawOverallDimension(canvas, x + 12, y + 8, x + width - 12, y + 8, "maßstabgerechte Ansicht");
    }

    private String spatialViewGraphicLabel(double angleDegrees, boolean isometric) {
        if (isometric) {
            return String.format(Locale.GERMAN, "3D-Grafik %.0f°", angleDegrees);
        }
        String direction = switch ((int) Math.round(angleDegrees)) {
            case 0 -> "Nord";
            case 90 -> "Ost";
            case 180 -> "Süd";
            case 270 -> "West";
            default -> String.format(Locale.GERMAN, "Blick %.0f°", angleDegrees);
        };
        return direction + " – Grafik";
    }

    private String spatialViewFallbackLabel(double angleDegrees, boolean isometric, int scale) {
        if (isometric) {
            return String.format(Locale.GERMAN, "Blick %.0f° – M 1:%d", angleDegrees, scale);
        }
        String direction = switch ((int) Math.round(angleDegrees)) {
            case 0 -> "Nord";
            case 90 -> "Ost";
            case 180 -> "Süd";
            case 270 -> "West";
            default -> String.format(Locale.GERMAN, "Blick %.0f°", angleDegrees);
        };
        return direction + " – M 1:" + scale;
    }

    private java.util.Optional<BufferedImage> renderSpatialViewImage(
            ProjectModel project,
            double angleDegrees,
            boolean isometric,
            double widthPoints,
            double heightPoints
    ) {
        try {
            return java.util.Optional.of(runOnFxThread(() -> createSpatialViewImage(project, angleDegrees, isometric, widthPoints, heightPoints)));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private BufferedImage createSpatialViewImage(
            ProjectModel project,
            double angleDegrees,
            boolean isometric,
            double widthPoints,
            double heightPoints
    ) {
        double widthPixels = Math.max(SPATIAL_VIEW_MINIMUM_PIXELS, widthPoints * SPATIAL_VIEW_RENDER_FACTOR);
        double heightPixels = Math.max(SPATIAL_VIEW_MINIMUM_PIXELS, heightPoints * SPATIAL_VIEW_RENDER_FACTOR);
        ThreeDViewport viewport = new ThreeDViewport(ignored -> { }, () -> { });
        viewport.setPrefSize(widthPixels, heightPixels);
        viewport.resize(widthPixels, heightPixels);
        viewport.setRoomObjectsVisible(true);
        viewport.setSurfaceLayersVisible(false);
        viewport.setSurfaceRenderingEnabled(true);
        viewport.syncLevels(project.levels(), project.primaryLevel().name());
        viewport.setVisibleLevels(project.levels().stream()
                .map(Level::name)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        configureSpatialViewport(viewport, angleDegrees, isometric);
        viewport.refresh(project);
        Scene scene = new Scene(viewport, widthPixels, heightPixels);
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        viewport.centerCurrentView();
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        return trimWhiteMargins(SwingFXUtils.fromFXImage(viewport.snapshotSceneOnly(javafx.scene.paint.Color.WHITE), null));
    }

    private void configureSpatialViewport(ThreeDViewport viewport, double angleDegrees, boolean isometric) {
        if (isometric) {
            viewport.resetToDefaultView();
            viewport.automationOrbit(angleDegrees - 45.0, 0.0);
            return;
        }
        viewport.applyViewPreset(sideViewPreset(angleDegrees));
        viewport.setProjectionMode(ProjectionMode.ORTHOGRAPHIC);
    }

    private void drawFittedImage(
            PageCanvas canvas,
            BufferedImage image,
            double left,
            double bottom,
            double width,
            double height
    ) throws IOException {
        double scale = Math.min(width / Math.max(1.0, image.getWidth()), height / Math.max(1.0, image.getHeight()));
        double drawWidth = image.getWidth() * scale;
        double drawHeight = image.getHeight() * scale;
        double drawX = left + (width - drawWidth) / 2.0;
        double drawY = bottom + (height - drawHeight) / 2.0;
        canvas.image(image, drawX, drawY, drawWidth, drawHeight);
    }

    private BufferedImage trimWhiteMargins(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (red >= 248 && green >= 248 && blue >= 248) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) {
            return image;
        }
        int padding = 4;
        int left = Math.max(0, minX - padding);
        int top = Math.max(0, minY - padding);
        int right = Math.min(image.getWidth() - 1, maxX + padding);
        int bottom = Math.min(image.getHeight() - 1, maxY + padding);
        BufferedImage trimmed = new BufferedImage(right - left + 1, bottom - top + 1, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = trimmed.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, trimmed.getWidth(), trimmed.getHeight(), left, top, right + 1, bottom + 1, null);
        } finally {
            graphics.dispose();
        }
        return trimmed;
    }

    private ThreeDViewPreset sideViewPreset(double angleDegrees) {
        return switch ((int) Math.round(angleDegrees)) {
            case 90 -> ThreeDViewPreset.RIGHT;
            case 180 -> ThreeDViewPreset.BACK;
            case 270 -> ThreeDViewPreset.LEFT;
            default -> ThreeDViewPreset.FRONT;
        };
    }

    private static void ensureJavaFxStarted() {
        if (Platform.isFxApplicationThread()) {
            return;
        }
        if (!JAVA_FX_STARTED.compareAndSet(false, true)) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            latch.await();
        } catch (IllegalStateException ignored) {
            latch.countDown();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("JavaFX-Toolkit konnte nicht initialisiert werden.", exception);
        } catch (RuntimeException exception) {
            JAVA_FX_STARTED.set(false);
            throw exception;
        }
    }

    private <T> T runOnFxThread(FxSupplier<T> supplier) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
        ensureJavaFxStarted();
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Exception> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(supplier.get());
            } catch (Exception exception) {
                failure.set(exception);
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }

    private List<SpatialLine> spatialLines(ProjectModel project, double angleDegrees, boolean isometric) {
        List<SpatialLine> lines = new ArrayList<>();
        double baseHeight = 0.0;
        double angle = Math.toRadians(angleDegrees);
        double depthFactor = isometric ? spatialDepthFactor(project, angle) : 0.0;
        List<TerrainProfileService.StripSample> terrain = terrainProfileService.sampledStrip(
                project.terrain(),
                terrainContourService.contour(project)
        );
        for (int index = 0; index < terrain.size(); index++) {
            TerrainProfileService.StripSample firstSample = terrain.get(index);
            TerrainProfileService.StripSample secondSample = terrain.get((index + 1) % terrain.size());
            SpatialPoint first = project(firstSample.outerPoint(), firstSample.elevationMillimeters(), angle, depthFactor);
            SpatialPoint second = project(secondSample.outerPoint(), secondSample.elevationMillimeters(), angle, depthFactor);
            lines.add(new SpatialLine(first.x(), first.y(), second.x(), second.y(), true, true));
        }
        for (Level level : project.levels()) {
            for (var extension : level.floorExtensions()) {
                List<PlanPoint> outline = extension.outline();
                for (int index = 0; index < outline.size(); index++) {
                    SpatialPoint first = project(outline.get(index), baseHeight, angle, depthFactor);
                    SpatialPoint second = project(outline.get((index + 1) % outline.size()), baseHeight, angle, depthFactor);
                    lines.add(new SpatialLine(first.x(), first.y(), second.x(), second.y(), true));
                }
            }
            for (Wall wall : level.walls()) {
                SpatialPoint startBottom = project(wall.axis().start(), baseHeight, angle, depthFactor);
                SpatialPoint endBottom = project(wall.axis().end(), baseHeight, angle, depthFactor);
                List<WallProfilePoint> profile = wall.resolvedProfile();
                SpatialPoint startTop = project(wall.axis().start(), baseHeight + profile.getFirst().height().toMillimeters(), angle, depthFactor);
                SpatialPoint endTop = project(wall.axis().end(), baseHeight + profile.getLast().height().toMillimeters(), angle, depthFactor);
                lines.add(new SpatialLine(startBottom.x(), startBottom.y(), endBottom.x(), endBottom.y(), false));
                for (int profileIndex = 1; profileIndex < profile.size(); profileIndex++) {
                    var previousProfilePoint = profile.get(profileIndex - 1);
                    var profilePoint = profile.get(profileIndex);
                    SpatialPoint previousTop = project(
                            wall.axis().pointAt(previousProfilePoint.offset()),
                            baseHeight + previousProfilePoint.height().toMillimeters(),
                            angle,
                            depthFactor
                    );
                    SpatialPoint top = project(
                            wall.axis().pointAt(profilePoint.offset()),
                            baseHeight + profilePoint.height().toMillimeters(),
                            angle,
                            depthFactor
                    );
                    lines.add(new SpatialLine(previousTop.x(), previousTop.y(), top.x(), top.y(), true));
                }
                lines.add(new SpatialLine(startBottom.x(), startBottom.y(), startTop.x(), startTop.y(), false));
                lines.add(new SpatialLine(endBottom.x(), endBottom.y(), endTop.x(), endTop.y(), false));
            }
            baseHeight += estimateLevelHeight(level);
        }
        return lines;
    }

    double spatialDepthFactor(ProjectModel project, double angleRadians) {
        List<PlanPoint> points = new ArrayList<>();
        for (Level level : project.levels()) {
            level.walls().forEach(wall -> {
                points.add(wall.axis().start());
                points.add(wall.axis().end());
            });
            level.rooms().forEach(room -> points.addAll(room.outline()));
            level.floorExtensions().forEach(extension -> points.addAll(extension.outline()));
        }
        if (points.isEmpty()) {
            return STANDARD_SPATIAL_DEPTH_FACTOR;
        }
        double minimumDepth = points.stream()
                .mapToDouble(point -> spatialDepth(point, angleRadians))
                .min()
                .orElse(0.0);
        double maximumDepth = points.stream()
                .mapToDouble(point -> spatialDepth(point, angleRadians))
                .max()
                .orElse(minimumDepth);
        double depthSpan = maximumDepth - minimumDepth;
        if (depthSpan <= 0.0) {
            return STANDARD_SPATIAL_DEPTH_FACTOR;
        }
        double minimumLevelHeight = project.levels().stream()
                .mapToDouble(this::estimateLevelHeight)
                .filter(height -> height > 0.0)
                .min()
                .orElse(2_750.0);
        return Math.min(
                STANDARD_SPATIAL_DEPTH_FACTOR,
                minimumLevelHeight * MAXIMUM_SAME_LEVEL_DEPTH_SHIFT_RATIO / depthSpan
        );
    }

    private double spatialDepth(PlanPoint point, double angleRadians) {
        return point.xMillimeters() * Math.sin(angleRadians) + point.yMillimeters() * Math.cos(angleRadians);
    }

    private double estimateLevelHeight(Level level) {
        double wallHeight = level.walls().stream().mapToDouble(Wall::maximumHeightMillimeters).max().orElse(2_750.0);
        double objectHeight = level.roomObjects().stream().mapToDouble(roomObject -> roomObject.height().toMillimeters()).max().orElse(0.0);
        double roomHeight = level.rooms().stream()
                .mapToDouble(room -> room.maximumCeilingHeightMillimeters()
                        + room.floorThickness().toMillimeters()
                        + room.ceilingThickness().toMillimeters())
                .max()
                .orElse(0.0);
        double stairHeight = level.staircases().stream().mapToDouble(staircase -> staircase.totalHeight().toMillimeters()).max().orElse(0.0);
        return Math.max(Math.max(wallHeight, objectHeight), Math.max(roomHeight, stairHeight));
    }

    private SpatialPoint project(PlanPoint point, double z, double angle, double depthFactor) {
        double horizontal = point.xMillimeters() * Math.cos(angle) - point.yMillimeters() * Math.sin(angle);
        double depth = spatialDepth(point, angle);
        double vertical = z - depth * depthFactor;
        return new SpatialPoint(horizontal, vertical);
    }

    private void drawPolygon(PageCanvas canvas, Viewport viewport, List<PlanPoint> points, Color fill, float width) throws IOException {
        drawPolygon(canvas, viewport, points, fill, Color.GRAY, width);
    }

    private void drawPolygon(
            PageCanvas canvas,
            Viewport viewport,
            List<PlanPoint> points,
            Color fill,
            Color stroke,
            float width
    ) throws IOException {
        if (points.isEmpty()) {
            return;
        }
        float[] coordinates = new float[points.size() * 2];
        for (int index = 0; index < points.size(); index++) {
            coordinates[index * 2] = (float) viewport.x(points.get(index).xMillimeters());
            coordinates[index * 2 + 1] = (float) viewport.y(points.get(index).yMillimeters());
        }
        canvas.polygon(coordinates, width, stroke, fill);
    }

    private void drawWall(PageCanvas canvas, Viewport viewport, Wall wall, Color fill, Color stroke, float width) throws IOException {
        drawPolygon(canvas, viewport, wallPlanOutlineService.outline(wall), fill, stroke, width);
    }

    private void drawRoomObject(PageCanvas canvas, Viewport viewport, RoomObject roomObject) throws IOException {
        canvas.polygon(rotatedRoomObjectOutline(viewport, roomObject), 0.75f, new Color(63, 96, 90), new Color(229, 239, 235));
        if (!roomObject.name().isBlank()) {
            canvas.text(
                    viewport.x(roomObject.center().xMillimeters()) + 5.0,
                    viewport.y(roomObject.center().yMillimeters()) - 5.0,
                    6.5f,
                    roomObject.name()
            );
        }
    }

    private float[] rotatedRoomObjectOutline(Viewport viewport, RoomObject roomObject) {
        double halfWidth = roomObject.width().toMillimeters() / 2.0;
        double halfDepth = roomObject.depth().toMillimeters() / 2.0;
        double radians = Math.toRadians(roomObject.rotationDegrees());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double centerX = roomObject.center().xMillimeters();
        double centerY = roomObject.center().yMillimeters();
        double[][] corners = {
                {-halfWidth, -halfDepth},
                {halfWidth, -halfDepth},
                {halfWidth, halfDepth},
                {-halfWidth, halfDepth}
        };
        float[] coordinates = new float[corners.length * 2];
        for (int index = 0; index < corners.length; index++) {
            double localX = corners[index][0];
            double localY = corners[index][1];
            double rotatedX = centerX + localX * cos - localY * sin;
            double rotatedY = centerY + localX * sin + localY * cos;
            coordinates[index * 2] = (float) viewport.x(rotatedX);
            coordinates[index * 2 + 1] = (float) viewport.y(rotatedY);
        }
        return coordinates;
    }

    private void drawOpening(PageCanvas canvas, Viewport viewport, Wall wall, double offset, double width, Color color) throws IOException {
        double length = wall.axis().length().toMillimeters();
        if (length <= 0) {
            return;
        }
        PlanPoint start = wall.axis().pointAt(de.schrell.cadas.domain.geometry.Length.ofMillimeters(Math.min(offset, length)));
        PlanPoint end = wall.axis().pointAt(de.schrell.cadas.domain.geometry.Length.ofMillimeters(Math.min(offset + width, length)));
        canvas.line(viewport.x(start.xMillimeters()), viewport.y(start.yMillimeters()), viewport.x(end.xMillimeters()), viewport.y(end.yMillimeters()), 2.2f, color);
    }

    private void drawFloorOpening(PageCanvas canvas, Viewport viewport, FloorOpening opening) throws IOException {
        List<PlanPoint> outline = new ArrayList<>();
        if (opening.shape() == FloorOpeningShape.CIRCLE) {
            double radius = opening.width().toMillimeters() / 2.0;
            for (int index = 0; index < 48; index++) {
                double angle = 2.0 * Math.PI * index / 48.0;
                outline.add(new PlanPoint(
                        opening.center().xMillimeters() + Math.cos(angle) * radius,
                        opening.center().yMillimeters() + Math.sin(angle) * radius
                ));
            }
        } else {
            outline.add(new PlanPoint(opening.minXMillimeters(), opening.minYMillimeters()));
            outline.add(new PlanPoint(opening.maxXMillimeters(), opening.minYMillimeters()));
            outline.add(new PlanPoint(opening.maxXMillimeters(), opening.maxYMillimeters()));
            outline.add(new PlanPoint(opening.minXMillimeters(), opening.maxYMillimeters()));
        }
        drawPolygon(canvas, viewport, outline, Color.WHITE, 0.9f);
    }

    private void drawHeatingExclusionArea(PageCanvas canvas, Viewport viewport, HeatingExclusionArea area) throws IOException {
        canvas.rectangle(
                viewport.x(area.minXMillimeters()),
                viewport.y(area.maxYMillimeters()),
                area.widthMillimeters() * viewport.factor(),
                area.depthMillimeters() * viewport.factor(),
                0.9f,
                new Color(170, 45, 35),
                new Color(248, 220, 216)
        );
        canvas.text(
                viewport.x(area.minXMillimeters()) + 4,
                viewport.y(area.minYMillimeters()) + 10,
                6.0f,
                area.name()
        );
    }

    private void drawWallDimensions(PageCanvas canvas, Viewport viewport, Level level, ConstructionDrawingOptions options, List<TextBlockingBox> seedBlockers) throws IOException {
        DimensionLabelOptions labelOptions = options.dimensionLabelOptions();
        List<PdfPendingDimension> pending = new ArrayList<>();
        for (Wall wall : level.walls()) {
            appendPdfWallDimensions(pending, level, wall, viewport, labelOptions);
        }
        List<PdfPlacedDimension> placed = dimensionLabelPlacementService.place(
                pending,
                seedBlockers,
                (label, offset) -> layoutPdfDimension(viewport, label, offset)
        );
        for (PdfPlacedDimension dim : placed) {
            drawPdfDimensionLine(canvas, dim);
        }
    }

    private void appendPdfWallDimensions(List<PdfPendingDimension> pending, Level level, Wall wall, Viewport viewport, DimensionLabelOptions options) {
        WallDimensionService.WallDimensions dimensions = wallDimensionService.dimensions(level, wall);
        double baseOffset = pdfDimensionBaseOffset(wall, viewport.factor());
        double stepOffset = 16.0;
        for (WallDimensionPlacementService.PlacedDimension placement : wallDimensionPlacementService.place(
                level, wall, dimensions, viewport.factor(), baseOffset, stepOffset
        )) {
            WallDimensionService.SideDimension dimension = placement.dimension();
            pending.add(new PdfPendingDimension(
                    dimension.dimensionSegment(),
                    dimensionLabelService.label(dimension, placement.exterior(), options),
                    placement.normalOffset(),
                    placement.lineDistanceFromAxis(),
                    Math.copySign(stepOffset, placement.normalOffset()),
                    dimension.length().toMillimeters(),
                    dimensionLabelService.deduplicationKey(dimension, placement.exterior())
            ));
        }
        if (dimensions.roomDimensions().isEmpty() && dimensions.exteriorDimension().isEmpty()) {
            WallDimensionPlacementService.PlacedDimension axis = wallDimensionPlacementService.placeAxisDimension(
                    level, wall, viewport.factor(), baseOffset
            );
            pending.add(new PdfPendingDimension(
                    wall.axis(),
                    dimensionLabelService.label("Achsmaß", wall.axis().length(), false, options),
                    axis.normalOffset(),
                    axis.lineDistanceFromAxis(),
                    Math.copySign(stepOffset, axis.normalOffset()),
                    wall.axis().length().toMillimeters(),
                    ""
            ));
        }
    }

    private Viewport planViewport(Level level, ConstructionDrawingOptions options) {
        Bounds buildingBounds = levelBounds(level).expanded(400.0);
        double availableWidth = PAGE_WIDTH - 2 * MARGIN;
        double availableHeight = PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT;
        int initialScale = chooseScale(buildingBounds.width(), buildingBounds.height(), availableWidth, availableHeight);
        if (!options.showDimensions()) {
            return centeredViewport(buildingBounds, initialScale);
        }
        Viewport lastViewport = centeredViewport(buildingBounds, initialScale);
        for (int scale : STANDARD_SCALES) {
            if (scale < initialScale) {
                continue;
            }
            Viewport viewport = centeredViewport(buildingBounds, scale);
            List<PdfPendingDimension> pending = new ArrayList<>();
            for (Wall wall : level.walls()) {
                appendPdfWallDimensions(pending, level, wall, viewport, options.dimensionLabelOptions());
            }
            List<PdfPlacedDimension> placed = dimensionLabelPlacementService.place(
                    pending,
                    List.of(),
                    (label, offset) -> layoutPdfDimension(viewport, label, offset)
            );
            ScreenBounds screenBounds = screenBounds(level, viewport, placed);
            lastViewport = translatedToPage(viewport, screenBounds);
            if (screenBounds.width() <= availableWidth && screenBounds.height() <= availableHeight) {
                return lastViewport;
            }
        }
        return lastViewport;
    }

    private Viewport heatingViewport(Level level, List<HydronicHeating> heatings) {
        List<PlanPoint> points = new ArrayList<>(levelPlanPoints(level));
        heatings.forEach(heating -> {
            points.add(heating.supplyPoint());
            points.add(heating.returnPoint());
            heating.zones().forEach(zone -> points.addAll(zone.outline()));
        });
        Bounds bounds = points.isEmpty() ? levelBounds(level) : new Bounds(
                points.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                points.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(10_000.0),
                points.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(7_000.0)
        );
        Bounds expandedBounds = bounds.expanded(400.0);
        int scale = chooseScale(
                expandedBounds.width(), expandedBounds.height(),
                PAGE_WIDTH - 2 * MARGIN, PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT
        );
        return centeredViewport(expandedBounds, scale);
    }

    private Viewport centeredViewport(Bounds bounds, int scale) {
        return new Viewport(
                MARGIN,
                MARGIN + TITLE_HEIGHT,
                PAGE_WIDTH - 2 * MARGIN,
                PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT,
                bounds,
                scale
        );
    }

    private ScreenBounds screenBounds(Level level, Viewport viewport, List<PdfPlacedDimension> dimensions) {
        Bounds modelBounds = levelBounds(level);
        double minimumX = viewport.x(modelBounds.minX());
        double maximumX = viewport.x(modelBounds.maxX());
        double minimumY = viewport.y(modelBounds.maxY());
        double maximumY = viewport.y(modelBounds.minY());
        for (PdfPlacedDimension dimension : dimensions) {
            for (TextBlockingBox box : dimension.blockingBoxes()) {
                minimumX = Math.min(minimumX, box.minX());
                minimumY = Math.min(minimumY, box.minY());
                maximumX = Math.max(maximumX, box.maxX());
                maximumY = Math.max(maximumY, box.maxY());
            }
        }
        return new ScreenBounds(minimumX, minimumY, maximumX, maximumY);
    }

    private Viewport translatedToPage(Viewport viewport, ScreenBounds screenBounds) {
        double targetCenterX = PAGE_WIDTH / 2.0;
        double targetCenterY = MARGIN + TITLE_HEIGHT + (PAGE_HEIGHT - 2 * MARGIN - TITLE_HEIGHT) / 2.0;
        return new Viewport(
                viewport.x() + targetCenterX - screenBounds.centerX(),
                viewport.y() + targetCenterY - screenBounds.centerY(),
                viewport.width(),
                viewport.height(),
                viewport.bounds(),
                viewport.scale()
        );
    }

    private double pdfDimensionBaseOffset(Wall wall, double factor) {
        return Math.max(wall.thickness().toMillimeters() * factor / 2.0 + 10.0, 18.0);
    }

    private PdfPlacedDimension layoutPdfDimension(Viewport viewport, PdfPendingDimension pending, double normalOffset) {
        PlanSegment segment = pending.segment();
        double x1 = viewport.x(segment.start().xMillimeters());
        double y1 = viewport.y(segment.start().yMillimeters());
        double x2 = viewport.x(segment.end().xMillimeters());
        double y2 = viewport.y(segment.end().yMillimeters());
        double effectiveOffset = dimensionLineLayoutService.projectedNormalOffset(normalOffset, true, 24.0);
        double screenPlacementSign = Math.copySign(1.0, effectiveOffset);
        DimensionLineLayoutService.DimensionLineLayout layout = dimensionLineLayoutService.layout(x1, y1, x2, y2, effectiveOffset);
        double textAwayDistance = dimensionLineLayoutService.isParallelToHorizontalText(x2 - x1, y2 - y1)
                ? PDF_PARALLEL_TEXT_AWAY_DISTANCE
                : PDF_TEXT_AWAY_DISTANCE;
        DimensionLineLayoutService.TextDelta away = dimensionLineLayoutService.textOffsetAwayFromLine(
                layout, screenPlacementSign, textAwayDistance
        );
        double textX = layout.textX() + away.deltaX();
        double textY = layout.textY() + away.deltaY();
        double directionX = x2 - x1;
        double directionY = y2 - y1;
        double directionLength = Math.max(1.0, Math.hypot(directionX, directionY));
        TextBlockingBox box = textBoundsApproximate(pending.text(), textX - PDF_FONT_SIZE * 0.5, textY - PDF_FONT_SIZE * 0.7, PDF_FONT_SIZE);
        return new PdfPlacedDimension(
                pending,
                layout,
                directionX / directionLength,
                directionY / directionLength,
                textX,
                textY,
                box
        );
    }

    private void drawPdfDimensionLine(PageCanvas canvas, PdfPlacedDimension dim) throws IOException {
        DimensionLineLayoutService.DimensionLineLayout layout = dim.layout();
        canvas.line(layout.lineStartX(), layout.lineStartY(), layout.lineEndX(), layout.lineEndY(), 0.45f, Color.BLACK);
        canvas.line(layout.firstExtensionStartX(), layout.firstExtensionStartY(), layout.firstExtensionEndX(), layout.firstExtensionEndY(), 0.45f, Color.BLACK);
        canvas.line(layout.secondExtensionStartX(), layout.secondExtensionStartY(), layout.secondExtensionEndX(), layout.secondExtensionEndY(), 0.45f, Color.BLACK);
        double tickX = (dim.directionX() - dim.directionY()) * 3.2;
        double tickY = (dim.directionY() + dim.directionX()) * 3.2;
        canvas.line(layout.lineStartX() - tickX, layout.lineStartY() - tickY, layout.lineStartX() + tickX, layout.lineStartY() + tickY, 0.55f, Color.BLACK);
        canvas.line(layout.lineEndX() - tickX, layout.lineEndY() - tickY, layout.lineEndX() + tickX, layout.lineEndY() + tickY, 0.55f, Color.BLACK);
        canvas.text(dim.textX(), dim.textY(), PDF_FONT_SIZE, dim.pending().text());
    }

    private record PdfPendingDimension(
            PlanSegment segment,
            String text,
            double normalOffset,
            double lineDistanceFromAxis,
            double outwardStep,
            double dimensionLengthMillimeters,
            String deduplicationKey
    ) implements DimensionLabelPlacementService.PendingLabel {
        @Override
        public double initialNormalOffset() {
            return normalOffset;
        }
    }

    private record PdfPlacedDimension(
            PdfPendingDimension pending,
            DimensionLineLayoutService.DimensionLineLayout layout,
            double directionX,
            double directionY,
            double textX,
            double textY,
            TextBlockingBox blockingBox
    ) implements DimensionLabelPlacementService.PlacedLabel {

        @Override
        public List<TextBlockingBox> blockingBoxes() {
            return List.of(
                    blockingBox,
                    TextBlockingBox.aroundLine(
                            layout.lineStartX(),
                            layout.lineStartY(),
                            layout.lineEndX(),
                            layout.lineEndY(),
                            PDF_DIMENSION_LINE_BLOCKING_PADDING
                    )
            );
        }
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
        canvas.text((x1 + x2) / 2.0 + 4, (y1 + y2) / 2.0 + 4, PDF_FONT_SIZE, text);
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
        List<PlanPoint> points = new ArrayList<>(levelPlanPoints(level));
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

    private List<PlanPoint> levelPlanPoints(Level level) {
        List<PlanPoint> points = new ArrayList<>();
        level.walls().forEach(wall -> points.addAll(wallPlanOutlineService.outline(wall)));
        level.rooms().forEach(room -> points.addAll(room.outline()));
        level.roomObjects().forEach(roomObject -> {
            points.add(new PlanPoint(roomObject.minXMillimeters(), roomObject.minYMillimeters()));
            points.add(new PlanPoint(roomObject.maxXMillimeters(), roomObject.maxYMillimeters()));
        });
        level.staircases().forEach(stair -> {
            points.add(stair.firstCorner());
            points.add(stair.oppositeCorner());
        });
        level.floorExtensions().forEach(extension -> points.addAll(extension.outline()));
        return points;
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

    private int chooseScale(double modelWidth, double modelHeight, double availableWidthPoints, double availableHeightPoints) {
        for (int scale : STANDARD_SCALES) {
            if (modelWidth * POINTS_PER_MILLIMETER / scale <= availableWidthPoints
                    && modelHeight * POINTS_PER_MILLIMETER / scale <= availableHeightPoints) {
                return scale;
            }
        }
        return STANDARD_SCALES[STANDARD_SCALES.length - 1];
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

    private record ScreenBounds(double minX, double minY, double maxX, double maxY) {
        double width() {
            return maxX - minX;
        }

        double height() {
            return maxY - minY;
        }

        double centerX() {
            return (minX + maxX) / 2.0;
        }

        double centerY() {
            return (minY + maxY) / 2.0;
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

    private record ScreenPoint(double x, double y) {
    }

    private record HeatingElementEntry(Room room, RoomObject roomObject) {
    }

    private record SpatialLine(double x1, double y1, double x2, double y2, boolean top, boolean terrain) {

        private SpatialLine(double x1, double y1, double x2, double y2, boolean top) {
            this(x1, y1, x2, y2, top, false);
        }
    }

    @FunctionalInterface
    private interface FxSupplier<T> {
        T get() throws Exception;
    }

    private static final class PageCanvas implements AutoCloseable {
        private final PDDocument document;
        private final PDPageContentStream stream;

        private PageCanvas(PDDocument document, PDPage page) throws IOException {
            this.document = document;
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

        void roundedPolyline(List<ScreenPoint> points, float lineWidth, Color color, double radius) throws IOException {
            if (points.size() < 2) {
                return;
            }
            stream.setStrokingColor(color);
            stream.setLineWidth(lineWidth);
            stream.moveTo((float) points.getFirst().x(), (float) points.getFirst().y());
            for (int index = 1; index + 1 < points.size(); index++) {
                ScreenPoint previous = points.get(index - 1);
                ScreenPoint current = points.get(index);
                ScreenPoint next = points.get(index + 1);
                double firstLength = Math.hypot(current.x() - previous.x(), current.y() - previous.y());
                double secondLength = Math.hypot(next.x() - current.x(), next.y() - current.y());
                double trim = Math.min(radius, Math.min(firstLength, secondLength) / 2.0);
                if (trim <= 0.001) {
                    stream.lineTo((float) current.x(), (float) current.y());
                    continue;
                }
                ScreenPoint before = interpolate(current, previous, trim / firstLength);
                ScreenPoint after = interpolate(current, next, trim / secondLength);
                stream.lineTo((float) before.x(), (float) before.y());
                double firstControlX = before.x() + (current.x() - before.x()) * 2.0 / 3.0;
                double firstControlY = before.y() + (current.y() - before.y()) * 2.0 / 3.0;
                double secondControlX = after.x() + (current.x() - after.x()) * 2.0 / 3.0;
                double secondControlY = after.y() + (current.y() - after.y()) * 2.0 / 3.0;
                stream.curveTo(
                        (float) firstControlX, (float) firstControlY,
                        (float) secondControlX, (float) secondControlY,
                        (float) after.x(), (float) after.y()
                );
            }
            stream.lineTo((float) points.getLast().x(), (float) points.getLast().y());
            stream.stroke();
        }

        private ScreenPoint interpolate(ScreenPoint start, ScreenPoint target, double ratio) {
            return new ScreenPoint(
                    start.x() + (target.x() - start.x()) * ratio,
                    start.y() + (target.y() - start.y()) * ratio
            );
        }

        void text(double x, double y, float size, String text) throws IOException {
            drawText(FONT, x, y, size, text);
        }

        void boldText(double x, double y, float size, String text) throws IOException {
            drawText(FONT_BOLD, x, y, size, text);
        }

        void image(BufferedImage image, double x, double y, double width, double height) throws IOException {
            PDImageXObject imageObject = LosslessFactory.createFromImage(document, image);
            stream.drawImage(imageObject, (float) x, (float) y, (float) width, (float) height);
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
