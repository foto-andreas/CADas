package de.schrell.cadas.application.reports;

import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.heating.RoomHeatingOutputService;
import de.schrell.cadas.application.layers.TileLayoutRequest;
import de.schrell.cadas.application.layers.TileLayoutService;
import de.schrell.cadas.application.layers.TilePlacement;
import de.schrell.cadas.application.layers.SurfaceLayerEffectService;
import de.schrell.cadas.application.layers.WallSurfaceSideService;
import de.schrell.cadas.application.floor.FloorOpeningGeometryService;
import de.schrell.cadas.application.layers.WallSurfaceTargetKey;
import de.schrell.cadas.application.room.OrthogonalPolygonDecompositionService;
import de.schrell.cadas.application.view.WallSurfaceOpeningService;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();
    private final ResidentialAreaService residentialAreaService = new ResidentialAreaService();
    private final FloorOpeningGeometryService floorOpeningGeometryService = new FloorOpeningGeometryService();
    private final HydronicHeatingLayoutService hydronicHeatingLayoutService = new HydronicHeatingLayoutService();
    private final RoomHeatingOutputService roomHeatingOutputService = new RoomHeatingOutputService();

    public SurfaceMaterialReport create(ProjectModel project) {
        Map<String, MaterialAccumulator> materials = new LinkedHashMap<>();
        for (int levelIndex = 0; levelIndex < project.levels().size(); levelIndex++) {
            Level level = project.levels().get(levelIndex);
            List<FloorOpening> openingsAbove = levelIndex + 1 < project.levels().size()
                    ? project.levels().get(levelIndex + 1).floorOpenings()
                    : List.of();
            collectLevel(level, openingsAbove, materials);
        }
        List<MaterialSummary> materialSummaries = materials.values().stream()
                .map(MaterialAccumulator::toSummary)
                .toList();
        Map<String, RoomAccumulator> rooms = new LinkedHashMap<>();
        for (MaterialSummary material : materialSummaries) {
            for (MaterialRoomEntry entry : material.roomEntries()) {
                String roomKey = entry.levelName() + "\u0000" + entry.roomName();
                rooms.computeIfAbsent(roomKey, ignored -> new RoomAccumulator(entry.levelName(), entry.roomName()))
                        .add(entry);
            }
        }
        return new SurfaceMaterialReport(
                project.name(),
                materialSummaries,
                rooms.values().stream().map(RoomAccumulator::toSummary).toList(),
                project.levels().stream()
                        .flatMap(level -> level.rooms().stream().map(room -> roomSummary(level, room)))
                        .toList(),
                heatingPlans(project),
                heatingElements(project)
        );
    }

    private List<HeatingPlanSummary> heatingPlans(ProjectModel project) {
        List<HeatingPlanSummary> summaries = new ArrayList<>();
        for (Level level : project.levels()) {
            for (HydronicHeating heating : level.hydronicHeatings()) {
                Room room = level.rooms().stream()
                        .filter(candidate -> candidate.id().equals(heating.roomId()))
                        .findFirst()
                        .orElse(null);
                if (room == null) {
                    continue;
                }
                List<HydronicHeatingLayoutService.CircuitLayout> circuits = hydronicHeatingLayoutService.layoutBestEffort(heating).circuits();
                String svg = hydronicHeatingLayoutService.toSvg(
                        level, room, heating, level.floorOpenings(), level.heatingExclusionAreas()
                );
                RoomHeatingOutputService.RoomHeatTotals roomHeatTotals = roomHeatingOutputService.totals(level, room);
                for (HeatingZone zone : heating.zones()) {
                    double pipeLength = circuits.stream()
                            .filter(circuit -> circuit.zoneId().equals(zone.id()))
                            .findFirst()
                            .map(circuit -> circuit.pipeLength().toMillimeters())
                            .orElse(0.0);
                    summaries.add(new HeatingPlanSummary(
                            level.name(),
                            room.name(),
                            heating.surfacePosition().toString(),
                            zone.layoutPattern().toString(),
                            zone.name(),
                            zone.areaSquareMeters(),
                            pipeLength / 1_000.0,
                            heating.maximumPipeLength().toMillimeters() / 1_000.0,
                            zone.heatOutputWattsPerSquareMeter(),
                            zone.heatOutputWatts(),
                            roomHeatTotals.surfaceHeatingWatts(),
                            roomHeatTotals.heatingElementWatts(),
                            roomHeatTotals.totalHeatOutputWatts(),
                            svg
                    ));
                }
            }
        }
        return List.copyOf(summaries);
    }

    private List<HeatingElementSummary> heatingElements(ProjectModel project) {
        List<HeatingElementSummary> summaries = new ArrayList<>();
        for (Level level : project.levels()) {
            for (Room room : level.rooms()) {
                roomHeatingOutputService.heatingElements(level, room).forEach(element -> summaries.add(
                        new HeatingElementSummary(
                                level.name(),
                                room.name(),
                                element.objectName(),
                                element.objectType(),
                                element.heatOutputWatts()
                        )
                ));
            }
        }
        return List.copyOf(summaries);
    }

    private RoomSummary roomSummary(Level level, Room room) {
        RoomHeatingOutputService.RoomHeatTotals roomHeatTotals = roomHeatingOutputService.totals(level, room);
        return new RoomSummary(
                level.name(),
                room.name(),
                room.widthMillimeters(),
                room.depthMillimeters(),
                surfaceLayerEffectService.effectiveMinimumCeilingHeightMillimeters(level, room),
                surfaceLayerEffectService.effectiveMaximumCeilingHeightMillimeters(level, room),
                surfaceLayerEffectService.effectiveAreaSquareMeters(level, room),
                surfaceLayerEffectService.effectiveVolumeCubicMeters(level, room),
                residentialAreaService.residentialAreaSquareMeters(level, room),
                roomHeatTotals.surfaceHeatingWatts(),
                roomHeatTotals.heatingElementWatts(),
                roomHeatTotals.totalHeatOutputWatts()
        );
    }

    private void collectLevel(Level level, List<FloorOpening> openingsAbove, Map<String, MaterialAccumulator> materials) {
        for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
            for (SurfaceLayer layer : stack.layers()) {
                if (!layer.visible()) {
                    continue;
                }
                for (SurfaceCoverage coverage : coverages(level, stack, openingsAbove)) {
                    CoverageEstimate estimate = estimateCoverage(layer, coverage.rectangles());
                    if (estimate.placedPieceCount() == 0) {
                        continue;
                    }
                    String materialKey = materialKey(stack.surfaceType(), layer);
                    MaterialAccumulator material = materials.computeIfAbsent(
                            materialKey,
                            ignored -> new MaterialAccumulator(layer, stack.surfaceType())
                    );
                    PendingMaterialRoomEntry entry = new PendingMaterialRoomEntry(
                            coverage.levelName(),
                            coverage.roomName(),
                            coverage.surfaceDescription(),
                            estimate
                    );
                    material.add(entry);
                }
            }
        }
    }

    private List<SurfaceCoverage> coverages(Level level, SurfaceLayerStack stack, List<FloorOpening> openingsAbove) {
        return switch (stack.surfaceType()) {
            case FLOOR, CEILING -> roomCoverages(level, stack, openingsAbove);
            case WALL_INTERIOR, WALL_EXTERIOR -> wallCoverages(level, stack);
            case ROOF -> List.of();
        };
    }

    private List<SurfaceCoverage> roomCoverages(Level level, SurfaceLayerStack stack, List<FloorOpening> openingsAbove) {
        List<SurfaceCoverage> coverages = new ArrayList<>();
        if (stack.surfaceType() == SurfaceType.FLOOR) {
            level.floorExtensions().stream()
                    .filter(extension -> stack.targetKey().equals(extension.surfaceTargetKey()))
                    .map(extension -> floorExtensionCoverage(level, extension))
                    .forEach(coverages::add);
        }
        for (Room room : level.rooms()) {
            if (!matchesRoom(stack, room)) {
                continue;
            }
            List<SurfaceRectangle> rectangles = roomSurfaceRectangles(level, room, stack.surfaceType(), openingsAbove);
            String surface = stack.surfaceType() == SurfaceType.FLOOR ? "Boden" : "Decke";
            coverages.add(new SurfaceCoverage(level.name(), room.name(), surface, rectangles));
        }
        return coverages;
    }

    private SurfaceCoverage floorExtensionCoverage(Level level, FloorExtension extension) {
        return new SurfaceCoverage(
                level.name(),
                extension.type().toString(),
                "Oberseite " + extension.type(),
                List.of(new SurfaceRectangle(extension.minX(), extension.minY(), extension.widthMillimeters(), extension.depthMillimeters()))
        );
    }

    private List<SurfaceRectangle> roomSurfaceRectangles(
            Level level,
            Room room,
            SurfaceType surfaceType,
            List<FloorOpening> openingsAbove
    ) {
        List<OrthogonalPolygonDecompositionService.CellRectangle> availableRectangles = surfaceType == SurfaceType.FLOOR
                ? floorOpeningGeometryService.floorRectangles(level, room)
                : floorOpeningGeometryService.ceilingRectangles(room, openingsAbove);
        List<SurfaceRectangle> rectangles = availableRectangles.stream()
                .map(rectangle -> new SurfaceRectangle(rectangle.minX(), rectangle.minY(), rectangle.width(), rectangle.height()))
                .toList();
        if (surfaceType != SurfaceType.FLOOR) {
            return rectangles;
        }
        for (RoomObject roomObject : level.roomObjects()) {
            if (!roomObject.visible() || !roomObject.cutsFloorCovering() || !objectCenterInsideRoom(room, roomObject)) {
                continue;
            }
            rectangles = subtractCutout(rectangles, new SurfaceRectangle(
                    roomObject.minXMillimeters(),
                    roomObject.minYMillimeters(),
                    roomObject.footprintWidthMillimeters(),
                    roomObject.footprintDepthMillimeters()
            ));
        }
        return rectangles;
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
                .map(rectangle -> new SurfaceRectangle(0.0, 0.0, rectangle.widthMillimeters(), rectangle.heightMillimeters()))
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
                layer.cutRestriction().name(),
                Boolean.toString(layer.layoutRotatedQuarterTurn()),
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

    private boolean objectCenterInsideRoom(Room room, RoomObject roomObject) {
        return containsPoint(room, roomObject.center());
    }

    private List<SurfaceRectangle> subtractCutout(List<SurfaceRectangle> rectangles, SurfaceRectangle cutout) {
        List<SurfaceRectangle> result = new ArrayList<>();
        for (SurfaceRectangle rectangle : rectangles) {
            result.addAll(subtractCutout(rectangle, cutout));
        }
        return result;
    }

    private List<SurfaceRectangle> subtractCutout(SurfaceRectangle rectangle, SurfaceRectangle cutout) {
        double minX = Math.max(rectangle.minXMillimeters(), cutout.minXMillimeters());
        double maxX = Math.min(rectangle.maxXMillimeters(), cutout.maxXMillimeters());
        double minY = Math.max(rectangle.minYMillimeters(), cutout.minYMillimeters());
        double maxY = Math.min(rectangle.maxYMillimeters(), cutout.maxYMillimeters());
        if (maxX <= minX + EPSILON || maxY <= minY + EPSILON) {
            return List.of(rectangle);
        }
        List<SurfaceRectangle> pieces = new ArrayList<>();
        addRectangleIfUsable(pieces, rectangle.minXMillimeters(), rectangle.minYMillimeters(), rectangle.widthMillimeters(), minY - rectangle.minYMillimeters());
        addRectangleIfUsable(pieces, rectangle.minXMillimeters(), maxY, rectangle.widthMillimeters(), rectangle.maxYMillimeters() - maxY);
        addRectangleIfUsable(pieces, rectangle.minXMillimeters(), minY, minX - rectangle.minXMillimeters(), maxY - minY);
        addRectangleIfUsable(pieces, maxX, minY, rectangle.maxXMillimeters() - maxX, maxY - minY);
        return pieces;
    }

    private void addRectangleIfUsable(List<SurfaceRectangle> rectangles, double minX, double minY, double width, double height) {
        if (width > EPSILON && height > EPSILON) {
            rectangles.add(new SurfaceRectangle(minX, minY, width, height));
        }
    }

    private boolean containsPoint(Room room, PlanPoint point) {
        boolean inside = false;
        int lastIndex = room.outline().size() - 1;
        for (int currentIndex = 0; currentIndex < room.outline().size(); currentIndex++) {
            PlanPoint current = room.outline().get(currentIndex);
            PlanPoint previous = room.outline().get(lastIndex);
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            lastIndex = currentIndex;
        }
        return inside;
    }

    private record SurfaceRectangle(double minXMillimeters, double minYMillimeters, double widthMillimeters, double heightMillimeters) {

        private double maxXMillimeters() {
            return minXMillimeters + widthMillimeters;
        }

        private double maxYMillimeters() {
            return minYMillimeters + heightMillimeters;
        }
    }

    private record SurfaceCoverage(
            String levelName,
            String roomName,
            String surfaceDescription,
            List<SurfaceRectangle> rectangles
    ) {
    }

    private record PendingMaterialRoomEntry(
            String levelName,
            String roomName,
            String surfaceDescription,
            CoverageEstimate estimate
    ) {
    }

    private static final class CoverageAccumulator {

        private final SurfaceLayer layer;
        private final List<CutPiece> cutPieces = new ArrayList<>();
        private int placedPieceCount;
        private int fullPieceCount;
        private int cutPieceCount;
        private int cutCount;
        private double coveredAreaSquareMillimeters;
        private double cutPenaltySum;

        private CoverageAccumulator(SurfaceLayer layer) {
            this.layer = layer;
        }

        private void add(SurfaceRectangle rectangle) {
            double tileWidth = layer.effectiveTileWidth().toMillimeters();
            double tileHeight = layer.effectiveTileHeight().toMillimeters();
            if (rectangle.widthMillimeters() <= EPSILON || rectangle.heightMillimeters() <= EPSILON || tileWidth <= EPSILON || tileHeight <= EPSILON) {
                return;
            }
            List<TilePlacement> placements = new TileLayoutService().fillSurface(new TileLayoutRequest(
                    Length.ofMillimeters(rectangle.widthMillimeters()),
                    Length.ofMillimeters(rectangle.heightMillimeters()),
                    layer.effectiveTileWidth(),
                    layer.effectiveTileHeight(),
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
            placedPieceCount++;
            coveredAreaSquareMillimeters += width * height;
            if (!cutsWidth && !cutsHeight) {
                fullPieceCount++;
                return;
            }
            cutPieceCount++;
            cutPieces.add(new CutPiece(width, height));
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
            double complexity = complexity(placedPieceCount, cutCount, cutPenaltySum);
            return new CoverageEstimate(
                    squareMeters(coveredAreaSquareMillimeters),
                    placedPieceCount,
                    fullPieceCount,
                    cutPieceCount,
                    cutCount,
                    cutPenaltySum,
                    List.copyOf(cutPieces),
                    complexity
            );
        }
    }

    private record CutPiece(double widthMillimeters, double heightMillimeters) {

        private double areaSquareMillimeters() {
            return widthMillimeters * heightMillimeters;
        }

        private double longestSideMillimeters() {
            return Math.max(widthMillimeters, heightMillimeters);
        }
    }

    private record OwnedCutPiece(int ownerIndex, double widthMillimeters, double heightMillimeters) {

        private double areaSquareMillimeters() {
            return widthMillimeters * heightMillimeters;
        }

        private double longestSideMillimeters() {
            return Math.max(widthMillimeters, heightMillimeters);
        }
    }

    private record RestPiece(double widthMillimeters, double heightMillimeters) {

        private double areaSquareMillimeters() {
            return widthMillimeters * heightMillimeters;
        }
    }

    private record FitCandidate(int restIndex, double widthMillimeters, double heightMillimeters, double wasteSquareMillimeters) {
    }

    private record MaterialCutOptimization(int requiredCutSheets, int[] requiredCutSheetsByOwner, List<RestPieceSummary> restPieces) {
    }

    private static final class MaterialCuttingOptimizer {

        private MaterialCuttingOptimizer() {
        }

        private static MaterialCutOptimization optimize(
                double sheetWidth,
                double sheetHeight,
                List<OwnedCutPiece> cutPieces,
                boolean allowRotation,
                int ownerCount
        ) {
            int[] requiredCutSheetsByOwner = new int[ownerCount];
            if (cutPieces.isEmpty()) {
                return new MaterialCutOptimization(0, requiredCutSheetsByOwner, List.of());
            }
            List<RestPiece> restPieces = new ArrayList<>();
            int requiredCutSheets = 0;
            List<OwnedCutPiece> orderedPieces = cutPieces.stream()
                    .sorted(Comparator.<OwnedCutPiece>comparingDouble(OwnedCutPiece::areaSquareMillimeters).reversed()
                            .thenComparing(Comparator.comparingDouble(OwnedCutPiece::longestSideMillimeters).reversed()))
                    .toList();
            for (OwnedCutPiece cutPiece : orderedPieces) {
                FitCandidate fit = bestFit(restPieces, cutPiece, allowRotation);
                if (fit == null) {
                    restPieces.add(new RestPiece(sheetWidth, sheetHeight));
                    requiredCutSheets++;
                    requiredCutSheetsByOwner[cutPiece.ownerIndex()]++;
                    fit = bestFit(restPieces, cutPiece, allowRotation);
                }
                if (fit == null) {
                    throw new IllegalStateException("Zuschnitt passt nicht in das Materialformat.");
                }
                RestPiece source = restPieces.remove(fit.restIndex());
                restPieces.addAll(splitRestPiece(source, fit.widthMillimeters(), fit.heightMillimeters()));
            }
            return new MaterialCutOptimization(requiredCutSheets, requiredCutSheetsByOwner, groupedRestPieces(restPieces));
        }

        private static FitCandidate bestFit(List<RestPiece> restPieces, OwnedCutPiece cutPiece, boolean allowRotation) {
            FitCandidate best = null;
            for (int index = 0; index < restPieces.size(); index++) {
                RestPiece restPiece = restPieces.get(index);
                best = betterFit(best, candidate(index, restPiece, cutPiece.widthMillimeters(), cutPiece.heightMillimeters()));
                if (allowRotation && Math.abs(cutPiece.widthMillimeters() - cutPiece.heightMillimeters()) > EPSILON) {
                    best = betterFit(best, candidate(index, restPiece, cutPiece.heightMillimeters(), cutPiece.widthMillimeters()));
                }
            }
            return best;
        }

        private static FitCandidate candidate(int restIndex, RestPiece restPiece, double width, double height) {
            if (width > restPiece.widthMillimeters() + EPSILON || height > restPiece.heightMillimeters() + EPSILON) {
                return null;
            }
            return new FitCandidate(restIndex, width, height, restPiece.areaSquareMillimeters() - width * height);
        }

        private static FitCandidate betterFit(FitCandidate current, FitCandidate candidate) {
            if (candidate == null) {
                return current;
            }
            if (current == null) {
                return candidate;
            }
            if (candidate.wasteSquareMillimeters() < current.wasteSquareMillimeters() - EPSILON) {
                return candidate;
            }
            if (Math.abs(candidate.wasteSquareMillimeters() - current.wasteSquareMillimeters()) <= EPSILON
                    && candidate.restIndex() < current.restIndex()) {
                return candidate;
            }
            return current;
        }

        private static List<RestPiece> splitRestPiece(RestPiece source, double usedWidth, double usedHeight) {
            List<RestPiece> verticalFirst = List.of(
                    new RestPiece(source.widthMillimeters() - usedWidth, source.heightMillimeters()),
                    new RestPiece(usedWidth, source.heightMillimeters() - usedHeight)
            );
            List<RestPiece> horizontalFirst = List.of(
                    new RestPiece(source.widthMillimeters(), source.heightMillimeters() - usedHeight),
                    new RestPiece(source.widthMillimeters() - usedWidth, usedHeight)
            );
            return usableRestPieces(score(verticalFirst) >= score(horizontalFirst) ? verticalFirst : horizontalFirst);
        }

        private static double score(List<RestPiece> restPieces) {
            return restPieces.stream()
                    .filter(MaterialCuttingOptimizer::isUsable)
                    .mapToDouble(RestPiece::areaSquareMillimeters)
                    .max()
                    .orElse(0.0);
        }

        private static List<RestPiece> usableRestPieces(List<RestPiece> candidates) {
            return candidates.stream()
                    .filter(MaterialCuttingOptimizer::isUsable)
                    .toList();
        }

        private static boolean isUsable(RestPiece restPiece) {
            return restPiece.widthMillimeters() > EPSILON && restPiece.heightMillimeters() > EPSILON;
        }

        private static List<RestPieceSummary> groupedRestPieces(List<RestPiece> restPieces) {
            Map<String, RestPieceAccumulator> groups = new LinkedHashMap<>();
            restPieces.stream()
                    .filter(MaterialCuttingOptimizer::isUsable)
                    .sorted(Comparator.comparingDouble(RestPiece::areaSquareMillimeters).reversed())
                    .forEach(restPiece -> groups.computeIfAbsent(restKey(restPiece), ignored -> new RestPieceAccumulator(restPiece))
                            .add());
            return groups.values().stream()
                    .map(RestPieceAccumulator::toSummary)
                    .toList();
        }

        private static String restKey(RestPiece restPiece) {
            return Math.round(restPiece.widthMillimeters() * 1000.0)
                    + "|"
                    + Math.round(restPiece.heightMillimeters() * 1000.0);
        }
    }

    private static final class RestPieceAccumulator {

        private final RestPiece restPiece;
        private int count;

        private RestPieceAccumulator(RestPiece restPiece) {
            this.restPiece = restPiece;
        }

        private void add() {
            count++;
        }

        private RestPieceSummary toSummary() {
            return new RestPieceSummary(
                    count,
                    restPiece.widthMillimeters(),
                    restPiece.heightMillimeters(),
                    squareMeters(count * restPiece.areaSquareMillimeters())
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
            int placedPieceCount,
            int fullPieceCount,
            int cutPieceCount,
            int cutCount,
            double cutPenaltySum,
            List<CutPiece> cutPieces,
            double complexityScore
    ) {
    }

    private static final class MaterialAccumulator {

        private final SurfaceLayer layer;
        private final SurfaceType surfaceType;
        private final List<PendingMaterialRoomEntry> pendingEntries = new ArrayList<>();
        private int placedPieceCount;
        private int fullPieceCount;
        private int cutPieceCount;
        private int cutCount;
        private double coveredAreaSquareMeters;
        private double cutPenaltySum;

        private MaterialAccumulator(SurfaceLayer layer, SurfaceType surfaceType) {
            this.layer = layer;
            this.surfaceType = surfaceType;
        }

        private void add(PendingMaterialRoomEntry entry) {
            pendingEntries.add(entry);
            CoverageEstimate estimate = entry.estimate();
            placedPieceCount += estimate.placedPieceCount();
            fullPieceCount += estimate.fullPieceCount();
            cutPieceCount += estimate.cutPieceCount();
            cutCount += estimate.cutCount();
            coveredAreaSquareMeters += estimate.coveredAreaSquareMeters();
            cutPenaltySum += estimate.cutPenaltySum();
        }

        private MaterialSummary toSummary() {
            MaterialCutOptimization optimization = optimizeCutPieces();
            double tileAreaSquareMillimeters = layer.tileWidth().toMillimeters() * layer.tileHeight().toMillimeters();
            List<MaterialRoomEntry> roomEntries = new ArrayList<>();
            for (int index = 0; index < pendingEntries.size(); index++) {
                PendingMaterialRoomEntry pendingEntry = pendingEntries.get(index);
                CoverageEstimate estimate = pendingEntry.estimate();
                int requiredPieces = estimate.fullPieceCount() + optimization.requiredCutSheetsByOwner()[index];
                roomEntries.add(new MaterialRoomEntry(
                        pendingEntry.levelName(),
                        pendingEntry.roomName(),
                        pendingEntry.surfaceDescription(),
                        estimate.coveredAreaSquareMeters(),
                        requiredPieces,
                        squareMeters(requiredPieces * tileAreaSquareMillimeters),
                        estimate.fullPieceCount(),
                        estimate.cutPieceCount(),
                        estimate.cutCount(),
                        estimate.complexityScore(),
                        estimate.cutPenaltySum()
                ));
            }
            int requiredPieces = fullPieceCount + optimization.requiredCutSheets();
            return new MaterialSummary(
                    layer.name(),
                    surfaceType,
                    layer.coveringSource().isBlank() ? surfaceType.toString() : surfaceType + ", Quelle: " + layer.coveringSource(),
                    values(layer),
                    coveredAreaSquareMeters,
                    requiredPieces,
                    squareMeters(requiredPieces * tileAreaSquareMillimeters),
                    fullPieceCount,
                    cutPieceCount,
                    cutCount,
                    complexity(placedPieceCount, cutCount, cutPenaltySum),
                    List.copyOf(roomEntries),
                    optimization.restPieces()
            );
        }

        private MaterialCutOptimization optimizeCutPieces() {
            List<OwnedCutPiece> cutPieces = new ArrayList<>();
            for (int index = 0; index < pendingEntries.size(); index++) {
                for (CutPiece cutPiece : pendingEntries.get(index).estimate().cutPieces()) {
                    cutPieces.add(new OwnedCutPiece(index, cutPiece.widthMillimeters(), cutPiece.heightMillimeters()));
                }
            }
            return MaterialCuttingOptimizer.optimize(
                    layer.effectiveTileWidth().toMillimeters(),
                    layer.effectiveTileHeight().toMillimeters(),
                    cutPieces,
                    layer.cutRestriction().allowsMaterialRotation(),
                    pendingEntries.size()
            );
        }

        private String values(SurfaceLayer layer) {
            return "Dicke " + length(layer.thickness(), LengthUnit.MILLIMETER, 1)
                    + ", Format " + length(layer.tileWidth(), LengthUnit.CENTIMETER, 1)
                    + " x " + length(layer.tileHeight(), LengthUnit.CENTIMETER, 1)
                    + ", Verlegung " + layer.layoutMode()
                    + (layer.layoutRotatedQuarterTurn() ? " um 90° gedreht" : "")
                    + ", Versatz " + length(layer.layoutOffset(), LengthUnit.CENTIMETER, 1)
                    + ", Mindestversatz " + length(layer.minimumOffset(), LengthUnit.CENTIMETER, 1)
                    + ", Mindestrand " + length(layer.minimumEdgeWidth(), LengthUnit.CENTIMETER, 1)
                    + ", Anfang/Ende " + length(layer.minimumStartEndMargin(), LengthUnit.CENTIMETER, 1)
                    + ", Fuge " + length(layer.jointWidth(), LengthUnit.MILLIMETER, 1)
                    + ", Schnittbeschränkung " + layer.cutRestriction().label();
        }
    }

    private static final class RoomAccumulator {

        private final String levelName;
        private final String roomName;
        private int requiredPieceCount;
        private int placedPieceCount;
        private int cutCount;
        private double coveredAreaSquareMeters;
        private double cutPenaltySum;

        private RoomAccumulator(String levelName, String roomName) {
            this.levelName = levelName;
            this.roomName = roomName;
        }

        private void add(MaterialRoomEntry entry) {
            requiredPieceCount += entry.requiredPieces();
            placedPieceCount += entry.fullPieces() + entry.cutPieces();
            cutCount += entry.cutCount();
            coveredAreaSquareMeters += entry.coveredAreaSquareMeters();
            cutPenaltySum += entry.cutPenaltySum();
        }

        private RoomComplexitySummary toSummary() {
            return new RoomComplexitySummary(
                    levelName,
                    roomName,
                    coveredAreaSquareMeters,
                    requiredPieceCount,
                    cutCount,
                    complexity(placedPieceCount, cutCount, cutPenaltySum)
            );
        }
    }

    public record SurfaceMaterialReport(
            String projectName,
            List<MaterialSummary> materials,
            List<RoomComplexitySummary> roomComplexities,
            List<RoomSummary> rooms,
            List<HeatingPlanSummary> heatingPlans,
            List<HeatingElementSummary> heatingElements
    ) {

        public String toMarkdown() {
            StringBuilder markdown = new StringBuilder();
            markdown.append("# Materialliste Beläge – ").append(projectName).append("\n\n");
            appendRooms(markdown);
            appendHeatingPlans(markdown);
            appendHeatingElements(markdown);
            if (materials.isEmpty()) {
                markdown.append("## Beläge\n\nKeine sichtbaren Beläge vorhanden.\n");
                return markdown.toString();
            }
            appendMaterialSummary(markdown);
            appendMaterialDetails(markdown);
            appendRoomComplexities(markdown);
            return markdown.toString();
        }

        private void appendRooms(StringBuilder markdown) {
            markdown.append("## Räume und Mietflächen nach WoFlV\n\n");
            if (rooms.isEmpty()) {
                markdown.append("Keine Räume vorhanden.\n\n");
                return;
            }
            markdown.append("| Raum | Maße | Lichte Höhe | Grundfläche | Mietfläche | Volumen | FBH/DH | Heizelemente | Gesamtwärme |\n");
            markdown.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
            for (RoomSummary room : rooms) {
                markdown.append("| ")
                        .append(markdownCell(room.levelName() + " / " + room.roomName()))
                        .append(" | ")
                        .append(decimal(room.widthMillimeters() / 1000.0, 2)).append(" × ")
                        .append(decimal(room.depthMillimeters() / 1000.0, 2)).append(" m")
                        .append(" | ")
                        .append(decimal(room.minimumHeightMillimeters() / 1000.0, 2));
                if (Math.abs(room.maximumHeightMillimeters() - room.minimumHeightMillimeters()) > EPSILON) {
                    markdown.append("–").append(decimal(room.maximumHeightMillimeters() / 1000.0, 2));
                }
                markdown.append(" m | ")
                        .append(decimal(room.areaSquareMeters(), 2)).append(" m²")
                        .append(" | ")
                        .append(decimal(room.residentialAreaSquareMeters(), 2)).append(" m²")
                        .append(" | ")
                        .append(decimal(room.volumeCubicMeters(), 2)).append(" m³")
                        .append(" | ")
                        .append(decimal(room.surfaceHeatingWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(room.heatingElementWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(room.totalHeatOutputWatts(), 0)).append(" W")
                        .append(" |\n");
            }
            markdown.append("\nDie Mietfläche gewichtet lichte Höhen ab 2 m vollständig, zwischen 1 m und 2 m zur Hälfte und unter 1 m nicht. Sichtbare Boden- und Deckenbeläge reduzieren die lichte Höhe.\n\n");
        }

        private void appendHeatingPlans(StringBuilder markdown) {
            markdown.append("## Flächenheizungen\n\n");
            if (heatingPlans.isEmpty()) {
                markdown.append("Keine Flächenheizungen vorhanden.\n\n");
                return;
            }
            markdown.append("| Raum | Fläche | Verlegung | Heizkreis | Heizfläche | HKL | Maximum | W/m² | Leistung | Raum FBH/DH | Heizelemente | Raum gesamt |\n");
            markdown.append("|---|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
            for (HeatingPlanSummary plan : heatingPlans) {
                markdown.append("| ")
                        .append(markdownCell(plan.levelName() + " / " + plan.roomName()))
                        .append(" | ")
                        .append(markdownCell(plan.surfacePosition()))
                        .append(" | ")
                        .append(markdownCell(plan.layoutPattern()))
                        .append(" | ")
                        .append(markdownCell(plan.zoneName()))
                        .append(" | ")
                        .append(decimal(plan.areaSquareMeters(), 2)).append(" m²")
                        .append(" | ")
                        .append(decimal(plan.pipeLengthMeters(), 1)).append(" m")
                        .append(" | ")
                        .append(decimal(plan.maximumPipeLengthMeters(), 1)).append(" m")
                        .append(" | ")
                        .append(decimal(plan.heatOutputWattsPerSquareMeter(), 1))
                        .append(" | ")
                        .append(decimal(plan.heatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(plan.roomSurfaceHeatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(plan.roomHeatingElementWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(plan.roomTotalHeatOutputWatts(), 0)).append(" W")
                        .append(" |\n");
            }
            markdown.append('\n');
            heatingPlans.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            plan -> plan.levelName() + "\u0000" + plan.roomName() + "\u0000" + plan.surfacePosition(),
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()
                    ))
                    .values()
                    .forEach(plans -> {
                        HeatingPlanSummary first = plans.getFirst();
                        markdown.append("### Heizplan ")
                                .append(first.levelName()).append(" / ")
                                .append(first.roomName()).append(" / ")
                                .append(first.surfacePosition()).append("\n\n")
                                .append(first.svg()).append("\n\n");
                    });
        }

        private void appendHeatingElements(StringBuilder markdown) {
            markdown.append("## Heizelemente\n\n");
            if (heatingElements.isEmpty()) {
                markdown.append("Keine Heizelemente vorhanden.\n\n");
                return;
            }
            markdown.append("| Raum | Objekt | Typ | Leistung |\n");
            markdown.append("|---|---|---|---:|\n");
            for (HeatingElementSummary element : heatingElements) {
                markdown.append("| ")
                        .append(markdownCell(element.levelName() + " / " + element.roomName()))
                        .append(" | ")
                        .append(markdownCell(element.objectName()))
                        .append(" | ")
                        .append(markdownCell(element.objectType()))
                        .append(" | ")
                        .append(decimal(element.heatOutputWatts(), 0)).append(" W")
                        .append(" |\n");
            }
            markdown.append('\n');
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
                appendRestPieces(markdown, material.restPieces());
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

        private void appendRestPieces(StringBuilder markdown, List<RestPieceSummary> restPieces) {
            if (restPieces.isEmpty()) {
                markdown.append("* Reststücke: keine\n\n");
                return;
            }
            markdown.append("* Reststücke: ").append(restPieces.stream().mapToInt(RestPieceSummary::count).sum()).append(" Stück\n\n");
            markdown.append("| Anzahl | Breite | Höhe | Gesamtfläche |\n");
            markdown.append("|---:|---:|---:|---:|\n");
            for (RestPieceSummary restPiece : restPieces) {
                markdown.append("| ")
                        .append(restPiece.count())
                        .append(" | ")
                        .append(decimal(restPiece.widthMillimeters() / 10.0, 1)).append(" cm")
                        .append(" | ")
                        .append(decimal(restPiece.heightMillimeters() / 10.0, 1)).append(" cm")
                        .append(" | ")
                        .append(decimal(restPiece.totalAreaSquareMeters(), 2)).append(" m²")
                        .append(" |\n");
            }
            markdown.append('\n');
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
            List<MaterialRoomEntry> roomEntries,
            List<RestPieceSummary> restPieces
    ) {
    }

    public record HeatingPlanSummary(
            String levelName,
            String roomName,
            String surfacePosition,
            String layoutPattern,
            String zoneName,
            double areaSquareMeters,
            double pipeLengthMeters,
            double maximumPipeLengthMeters,
            double heatOutputWattsPerSquareMeter,
            double heatOutputWatts,
            double roomSurfaceHeatOutputWatts,
            double roomHeatingElementWatts,
            double roomTotalHeatOutputWatts,
            String svg
    ) {
    }

    public record HeatingElementSummary(
            String levelName,
            String roomName,
            String objectName,
            String objectType,
            double heatOutputWatts
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
            double complexityScore,
            double cutPenaltySum
    ) {
    }

    public record RestPieceSummary(
            int count,
            double widthMillimeters,
            double heightMillimeters,
            double totalAreaSquareMeters
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

    public record RoomSummary(
            String levelName,
            String roomName,
            double widthMillimeters,
            double depthMillimeters,
            double minimumHeightMillimeters,
            double maximumHeightMillimeters,
            double areaSquareMeters,
            double volumeCubicMeters,
            double residentialAreaSquareMeters,
            double surfaceHeatingWatts,
            double heatingElementWatts,
            double totalHeatOutputWatts
    ) {
    }
}
