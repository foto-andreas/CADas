package de.andreas.cadas.application.reports;

import de.andreas.cadas.application.layers.TileLayoutRequest;
import de.andreas.cadas.application.layers.TileLayoutService;
import de.andreas.cadas.application.layers.TilePlacement;
import de.andreas.cadas.application.layers.WallSurfaceSideService;
import de.andreas.cadas.application.layers.WallSurfaceTargetKey;
import de.andreas.cadas.application.room.OrthogonalPolygonDecompositionService;
import de.andreas.cadas.application.view.WallSurfaceOpeningService;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SurfaceMaterialListService {

    private static final double EPSILON = 0.001;

    private final OrthogonalPolygonDecompositionService decompositionService = new OrthogonalPolygonDecompositionService();
    private final WallSurfaceOpeningService wallSurfaceOpeningService = new WallSurfaceOpeningService();
    private final WallSurfaceSideService wallSurfaceSideService = new WallSurfaceSideService();

    public SurfaceMaterialReport create(ProjectModel project) {
        Map<String, MaterialAccumulator> materials = new LinkedHashMap<>();
        Map<String, RoomAccumulator> rooms = new LinkedHashMap<>();
        for (Level level : project.levels()) {
            collectLevel(level, materials, rooms);
        }
        return new SurfaceMaterialReport(
                materials.values().stream().map(MaterialAccumulator::toSummary).toList(),
                rooms.values().stream().map(RoomAccumulator::toSummary).toList()
        );
    }

    private void collectLevel(Level level, Map<String, MaterialAccumulator> materials, Map<String, RoomAccumulator> rooms) {
        for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
            for (SurfaceLayer layer : stack.layers()) {
                if (!layer.visible()) {
                    continue;
                }
                for (SurfaceCoverage coverage : coverages(level, stack)) {
                    CoverageEstimate estimate = estimateCoverage(layer, coverage.rectangles());
                    if (estimate.pieceCount() == 0) {
                        continue;
                    }
                    String materialKey = materialKey(stack.surfaceType(), layer);
                    MaterialAccumulator material = materials.computeIfAbsent(
                            materialKey,
                            ignored -> new MaterialAccumulator(layer, stack.surfaceType())
                    );
                    MaterialRoomEntry entry = new MaterialRoomEntry(
                            coverage.levelName(),
                            coverage.roomName(),
                            coverage.surfaceDescription(),
                            estimate.coveredAreaSquareMeters(),
                            estimate.pieceCount(),
                            estimate.requiredMaterialAreaSquareMeters(),
                            estimate.fullPieceCount(),
                            estimate.cutPieceCount(),
                            estimate.cutCount(),
                            estimate.complexityScore()
                    );
                    material.add(entry, estimate);
                    String roomKey = coverage.levelName() + "\u0000" + coverage.roomName();
                    rooms.computeIfAbsent(roomKey, ignored -> new RoomAccumulator(coverage.levelName(), coverage.roomName()))
                            .add(estimate);
                }
            }
        }
    }

    private List<SurfaceCoverage> coverages(Level level, SurfaceLayerStack stack) {
        return switch (stack.surfaceType()) {
            case FLOOR, CEILING -> roomCoverages(level, stack);
            case WALL_INTERIOR, WALL_EXTERIOR -> wallCoverages(level, stack);
            case ROOF -> List.of();
        };
    }

    private List<SurfaceCoverage> roomCoverages(Level level, SurfaceLayerStack stack) {
        List<SurfaceCoverage> coverages = new ArrayList<>();
        for (Room room : level.rooms()) {
            if (!matchesRoom(stack, room)) {
                continue;
            }
            List<SurfaceRectangle> rectangles = roomRectangles(room).stream()
                    .map(rectangle -> new SurfaceRectangle(rectangle.width(), rectangle.height()))
                    .toList();
            String surface = stack.surfaceType() == SurfaceType.FLOOR ? "Boden" : "Decke";
            coverages.add(new SurfaceCoverage(level.name(), room.name(), surface, rectangles));
        }
        return coverages;
    }

    private List<SurfaceCoverage> wallCoverages(Level level, SurfaceLayerStack stack) {
        Optional<Wall> wall = level.walls().stream()
                .filter(candidate -> WallSurfaceTargetKey.matchesWall(stack.targetKey(), candidate.id()))
                .findFirst();
        if (wall.isEmpty()) {
            return List.of();
        }
        List<SurfaceCoverage> coverages = new ArrayList<>();
        WallSurfaceSideService.WallLayerSides sides = wallSurfaceSideService.resolve(level, wall.get(), stack.surfaceType(), stack.targetKey());
        WallSurfaceTargetKey.roomId(stack.targetKey())
                .flatMap(roomId -> level.rooms().stream().filter(room -> room.id().equals(roomId)).findFirst())
                .ifPresentOrElse(
                        room -> addWallSideCoverages(level, stack, wall.get(), sides, room.name(), coverages),
                        () -> addWallSideCoverages(level, stack, wall.get(), sides, stack.surfaceType() == SurfaceType.WALL_EXTERIOR ? "Außenflächen" : "Innenflächen", coverages)
                );
        return coverages;
    }

    private void addWallSideCoverages(
            Level level,
            SurfaceLayerStack stack,
            Wall wall,
            WallSurfaceSideService.WallLayerSides sides,
            String roomName,
            List<SurfaceCoverage> coverages
    ) {
        if (sides.positiveSide()) {
            coverages.add(wallSideCoverage(level, stack, wall, roomName, 1.0));
        }
        if (sides.negativeSide()) {
            coverages.add(wallSideCoverage(level, stack, wall, roomName, -1.0));
        }
    }

    private SurfaceCoverage wallSideCoverage(Level level, SurfaceLayerStack stack, Wall wall, String roomName, double sideSign) {
        List<SurfaceRectangle> rectangles = wallSurfaceOpeningService.visibleRectangles(level, wall, sideSign).stream()
                .map(rectangle -> new SurfaceRectangle(rectangle.widthMillimeters(), rectangle.heightMillimeters()))
                .toList();
        String sideLabel = sideSign > 0.0 ? "+" : "-";
        return new SurfaceCoverage(
                level.name(),
                roomName,
                stack.surfaceType() + " Wand " + shortId(wall.id().toString()) + " Seite " + sideLabel,
                rectangles
        );
    }

    private List<OrthogonalPolygonDecompositionService.CellRectangle> roomRectangles(Room room) {
        List<OrthogonalPolygonDecompositionService.CellRectangle> rectangles = decompositionService.decompose(room.outline());
        if (!rectangles.isEmpty()) {
            return rectangles;
        }
        return List.of(new OrthogonalPolygonDecompositionService.CellRectangle(
                room.minXMillimeters(),
                room.maxXMillimeters(),
                room.minYMillimeters(),
                room.maxYMillimeters()
        ));
    }

    private boolean matchesRoom(SurfaceLayerStack stack, Room room) {
        return stack.targetKey().equals(room.id().toString())
                || stack.targetKey().equalsIgnoreCase(room.name())
                || stack.targetKey().contains(room.id().toString())
                || stack.targetKey().contains(room.name());
    }

    private CoverageEstimate estimateCoverage(SurfaceLayer layer, List<SurfaceRectangle> rectangles) {
        CoverageAccumulator accumulator = new CoverageAccumulator(layer);
        for (SurfaceRectangle rectangle : rectangles) {
            accumulator.add(rectangle);
        }
        return accumulator.toEstimate();
    }

    private String materialKey(SurfaceType surfaceType, SurfaceLayer layer) {
        return String.join("|",
                surfaceType.name(),
                layer.name(),
                Double.toString(layer.thickness().toMillimeters()),
                Double.toString(layer.tileWidth().toMillimeters()),
                Double.toString(layer.tileHeight().toMillimeters()),
                layer.layoutMode().name(),
                Double.toString(layer.layoutOffset().toMillimeters()),
                Double.toString(layer.minimumOffset().toMillimeters()),
                Double.toString(layer.minimumEdgeWidth().toMillimeters()),
                Double.toString(layer.minimumStartEndMargin().toMillimeters()),
                Double.toString(layer.jointWidth().toMillimeters()),
                layer.coveringSource()
        );
    }

    private String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private static double squareMeters(double squareMillimeters) {
        return squareMillimeters / 1_000_000.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String decimal(double value, int decimals) {
        return String.format(Locale.GERMAN, "%." + decimals + "f", value);
    }

    private static String length(Length length, LengthUnit unit, int decimals) {
        return length.format(unit, decimals).replace('.', ',');
    }

    private static String markdownCell(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }

    private record SurfaceRectangle(double widthMillimeters, double heightMillimeters) {
    }

    private record SurfaceCoverage(
            String levelName,
            String roomName,
            String surfaceDescription,
            List<SurfaceRectangle> rectangles
    ) {
    }

    private static final class CoverageAccumulator {

        private final SurfaceLayer layer;
        private int pieceCount;
        private int fullPieceCount;
        private int cutPieceCount;
        private int cutCount;
        private double coveredAreaSquareMillimeters;
        private double requiredMaterialAreaSquareMillimeters;
        private double cutPenaltySum;

        private CoverageAccumulator(SurfaceLayer layer) {
            this.layer = layer;
        }

        private void add(SurfaceRectangle rectangle) {
            double tileWidth = layer.tileWidth().toMillimeters();
            double tileHeight = layer.tileHeight().toMillimeters();
            if (rectangle.widthMillimeters() <= EPSILON || rectangle.heightMillimeters() <= EPSILON || tileWidth <= EPSILON || tileHeight <= EPSILON) {
                return;
            }
            List<TilePlacement> placements = new TileLayoutService().fillSurface(new TileLayoutRequest(
                    Length.ofMillimeters(rectangle.widthMillimeters()),
                    Length.ofMillimeters(rectangle.heightMillimeters()),
                    layer.tileWidth(),
                    layer.tileHeight(),
                    layer.layoutMode(),
                    layer.layoutOffset(),
                    layer.minimumOffset(),
                    layer.minimumEdgeWidth(),
                    layer.minimumStartEndMargin()
            ));
            for (TilePlacement placement : placements) {
                addPlacement(placement, tileWidth, tileHeight);
            }
        }

        private void addPlacement(TilePlacement placement, double tileWidth, double tileHeight) {
            double width = placement.width().toMillimeters();
            double height = placement.height().toMillimeters();
            boolean cutsWidth = width < tileWidth - EPSILON;
            boolean cutsHeight = height < tileHeight - EPSILON;
            pieceCount++;
            coveredAreaSquareMillimeters += width * height;
            requiredMaterialAreaSquareMillimeters += tileWidth * tileHeight;
            if (!cutsWidth && !cutsHeight) {
                fullPieceCount++;
                return;
            }
            cutPieceCount++;
            if (cutsWidth) {
                cutCount++;
                cutPenaltySum += 1.0 - clamp(height / tileHeight, 0.0, 1.0);
            }
            if (cutsHeight) {
                cutCount++;
                cutPenaltySum += 1.0 - clamp(width / tileWidth, 0.0, 1.0);
            }
        }

        private CoverageEstimate toEstimate() {
            double complexity = complexity(pieceCount, cutCount, cutPenaltySum);
            return new CoverageEstimate(
                    squareMeters(coveredAreaSquareMillimeters),
                    pieceCount,
                    squareMeters(requiredMaterialAreaSquareMillimeters),
                    fullPieceCount,
                    cutPieceCount,
                    cutCount,
                    cutPenaltySum,
                    complexity
            );
        }
    }

    private static double complexity(int pieceCount, int cutCount, double cutPenaltySum) {
        if (pieceCount <= 0 || cutCount <= 0) {
            return 0.0;
        }
        double cutShare = Math.min(1.0, cutCount / (pieceCount * 2.0));
        double shortEdgePenalty = clamp(cutPenaltySum / cutCount, 0.0, 1.0);
        return clamp((0.65 * cutShare + 0.35 * shortEdgePenalty) * 100.0, 0.0, 100.0);
    }

    private record CoverageEstimate(
            double coveredAreaSquareMeters,
            int pieceCount,
            double requiredMaterialAreaSquareMeters,
            int fullPieceCount,
            int cutPieceCount,
            int cutCount,
            double cutPenaltySum,
            double complexityScore
    ) {
    }

    private static final class MaterialAccumulator {

        private final SurfaceLayer layer;
        private final SurfaceType surfaceType;
        private final List<MaterialRoomEntry> roomEntries = new ArrayList<>();
        private int pieceCount;
        private int fullPieceCount;
        private int cutPieceCount;
        private int cutCount;
        private double coveredAreaSquareMeters;
        private double requiredMaterialAreaSquareMeters;
        private double cutPenaltySum;

        private MaterialAccumulator(SurfaceLayer layer, SurfaceType surfaceType) {
            this.layer = layer;
            this.surfaceType = surfaceType;
        }

        private void add(MaterialRoomEntry entry, CoverageEstimate estimate) {
            roomEntries.add(entry);
            pieceCount += estimate.pieceCount();
            fullPieceCount += estimate.fullPieceCount();
            cutPieceCount += estimate.cutPieceCount();
            cutCount += estimate.cutCount();
            coveredAreaSquareMeters += estimate.coveredAreaSquareMeters();
            requiredMaterialAreaSquareMeters += estimate.requiredMaterialAreaSquareMeters();
            cutPenaltySum += estimate.cutPenaltySum();
        }

        private MaterialSummary toSummary() {
            return new MaterialSummary(
                    layer.name(),
                    surfaceType,
                    layer.coveringSource().isBlank() ? surfaceType.toString() : surfaceType + ", Quelle: " + layer.coveringSource(),
                    values(layer),
                    coveredAreaSquareMeters,
                    pieceCount,
                    requiredMaterialAreaSquareMeters,
                    fullPieceCount,
                    cutPieceCount,
                    cutCount,
                    complexity(pieceCount, cutCount, cutPenaltySum),
                    List.copyOf(roomEntries)
            );
        }

        private String values(SurfaceLayer layer) {
            return "Dicke " + length(layer.thickness(), LengthUnit.MILLIMETER, 1)
                    + ", Format " + length(layer.tileWidth(), LengthUnit.CENTIMETER, 1)
                    + " x " + length(layer.tileHeight(), LengthUnit.CENTIMETER, 1)
                    + ", Verlegung " + layer.layoutMode()
                    + ", Versatz " + length(layer.layoutOffset(), LengthUnit.CENTIMETER, 1)
                    + ", Mindestversatz " + length(layer.minimumOffset(), LengthUnit.CENTIMETER, 1)
                    + ", Mindestrand " + length(layer.minimumEdgeWidth(), LengthUnit.CENTIMETER, 1)
                    + ", Anfang/Ende " + length(layer.minimumStartEndMargin(), LengthUnit.CENTIMETER, 1)
                    + ", Fuge " + length(layer.jointWidth(), LengthUnit.MILLIMETER, 1);
        }
    }

    private static final class RoomAccumulator {

        private final String levelName;
        private final String roomName;
        private int pieceCount;
        private int cutCount;
        private double coveredAreaSquareMeters;
        private double cutPenaltySum;

        private RoomAccumulator(String levelName, String roomName) {
            this.levelName = levelName;
            this.roomName = roomName;
        }

        private void add(CoverageEstimate estimate) {
            pieceCount += estimate.pieceCount();
            cutCount += estimate.cutCount();
            coveredAreaSquareMeters += estimate.coveredAreaSquareMeters();
            cutPenaltySum += estimate.cutPenaltySum();
        }

        private RoomComplexitySummary toSummary() {
            return new RoomComplexitySummary(
                    levelName,
                    roomName,
                    coveredAreaSquareMeters,
                    pieceCount,
                    cutCount,
                    complexity(pieceCount, cutCount, cutPenaltySum)
            );
        }
    }

    public record SurfaceMaterialReport(
            List<MaterialSummary> materials,
            List<RoomComplexitySummary> roomComplexities
    ) {

        public String toMarkdown() {
            StringBuilder markdown = new StringBuilder();
            markdown.append("# Materialliste Beläge\n\n");
            if (materials.isEmpty()) {
                markdown.append("Keine sichtbaren Beläge vorhanden.\n");
                return markdown.toString();
            }
            appendMaterialSummary(markdown);
            appendMaterialDetails(markdown);
            appendRoomComplexities(markdown);
            return markdown.toString();
        }

        private void appendMaterialSummary(StringBuilder markdown) {
            markdown.append("## Zusammenfassung\n\n");
            markdown.append("| Belag | Fläche | Stückzahl | Materialfläche | Schnitte | Komplexität |\n");
            markdown.append("|---|---:|---:|---:|---:|---:|\n");
            for (MaterialSummary material : materials) {
                markdown.append("| ")
                        .append(markdownCell(material.name()))
                        .append(" | ")
                        .append(decimal(material.coveredAreaSquareMeters(), 2)).append(" m²")
                        .append(" | ")
                        .append(material.requiredPieces())
                        .append(" | ")
                        .append(decimal(material.requiredMaterialAreaSquareMeters(), 2)).append(" m²")
                        .append(" | ")
                        .append(material.cutCount())
                        .append(" | ")
                        .append(decimal(material.complexityScore(), 1))
                        .append(" |\n");
            }
            markdown.append('\n');
        }

        private void appendMaterialDetails(StringBuilder markdown) {
            markdown.append("## Beläge\n\n");
            for (MaterialSummary material : materials) {
                markdown.append("### ").append(material.name()).append("\n\n");
                markdown.append("* Beschreibung: ").append(material.description()).append('\n');
                markdown.append("* Werte: ").append(material.values()).append('\n');
                markdown.append("* Belegte Fläche: ").append(decimal(material.coveredAreaSquareMeters(), 2)).append(" m²\n");
                markdown.append("* Benötigte Stückzahl: ").append(material.requiredPieces())
                        .append(" Stück, Materialfläche ")
                        .append(decimal(material.requiredMaterialAreaSquareMeters(), 2)).append(" m²\n");
                markdown.append("* Vollstücke: ").append(material.fullPieces())
                        .append(", Zuschnitte: ").append(material.cutPieces())
                        .append(", notwendige Schnitte: ").append(material.cutCount()).append("\n\n");
                markdown.append("| Raum/Fläche | Fläche | Stückzahl | Materialfläche | Schnitte | Komplexität |\n");
                markdown.append("|---|---:|---:|---:|---:|---:|\n");
                for (MaterialRoomEntry entry : material.roomEntries()) {
                    markdown.append("| ")
                            .append(markdownCell(entry.levelName() + " / " + entry.roomName() + " / " + entry.surfaceDescription()))
                            .append(" | ")
                            .append(decimal(entry.coveredAreaSquareMeters(), 2)).append(" m²")
                            .append(" | ")
                            .append(entry.requiredPieces())
                            .append(" | ")
                            .append(decimal(entry.requiredMaterialAreaSquareMeters(), 2)).append(" m²")
                            .append(" | ")
                            .append(entry.cutCount())
                            .append(" | ")
                            .append(decimal(entry.complexityScore(), 1))
                            .append(" |\n");
                }
                markdown.append('\n');
            }
        }

        private void appendRoomComplexities(StringBuilder markdown) {
            markdown.append("## Komplexität pro Raum\n\n");
            markdown.append("| Raum | Belegte Fläche | Stückzahl | Schnitte | Komplexität |\n");
            markdown.append("|---|---:|---:|---:|---:|\n");
            for (RoomComplexitySummary room : roomComplexities) {
                markdown.append("| ")
                        .append(markdownCell(room.levelName() + " / " + room.roomName()))
                        .append(" | ")
                        .append(decimal(room.coveredAreaSquareMeters(), 2)).append(" m²")
                        .append(" | ")
                        .append(room.requiredPieces())
                        .append(" | ")
                        .append(room.cutCount())
                        .append(" | ")
                        .append(decimal(room.complexityScore(), 1))
                        .append(" |\n");
            }
        }
    }

    public record MaterialSummary(
            String name,
            SurfaceType surfaceType,
            String description,
            String values,
            double coveredAreaSquareMeters,
            int requiredPieces,
            double requiredMaterialAreaSquareMeters,
            int fullPieces,
            int cutPieces,
            int cutCount,
            double complexityScore,
            List<MaterialRoomEntry> roomEntries
    ) {
    }

    public record MaterialRoomEntry(
            String levelName,
            String roomName,
            String surfaceDescription,
            double coveredAreaSquareMeters,
            int requiredPieces,
            double requiredMaterialAreaSquareMeters,
            int fullPieces,
            int cutPieces,
            int cutCount,
            double complexityScore
    ) {
    }

    public record RoomComplexitySummary(
            String levelName,
            String roomName,
            double coveredAreaSquareMeters,
            int requiredPieces,
            int cutCount,
            double complexityScore
    ) {
    }
}
