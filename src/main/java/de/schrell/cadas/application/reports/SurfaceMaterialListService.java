package de.schrell.cadas.application.reports;

import de.schrell.cadas.application.heating.HydronicHeatingLayoutService;
import de.schrell.cadas.application.heating.RoomHeatingOutputService;
import de.schrell.cadas.application.layers.SurfaceRectangleTileLayoutService;
import de.schrell.cadas.application.layers.SurfaceRectangleTileLayoutService.PlacedSurfaceTile;
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
import de.schrell.cadas.domain.geometry.PlanPolygonSupport;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectHeatingType;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Wall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SurfaceMaterialListService {

    private static final double EPSILON = 0.001;
    private static final double MINIMUM_REUSABLE_REST_EDGE_MILLIMETERS = 20.0;

    private final OrthogonalPolygonDecompositionService decompositionService = new OrthogonalPolygonDecompositionService();
    private final WallSurfaceOpeningService wallSurfaceOpeningService = new WallSurfaceOpeningService();
    private final WallSurfaceSideService wallSurfaceSideService = new WallSurfaceSideService();
    private final SurfaceLayerEffectService surfaceLayerEffectService = new SurfaceLayerEffectService();
    private final ResidentialAreaService residentialAreaService = new ResidentialAreaService();
    private final FloorOpeningGeometryService floorOpeningGeometryService = new FloorOpeningGeometryService();
    private final HydronicHeatingLayoutService hydronicHeatingLayoutService = new HydronicHeatingLayoutService();
    private final RoomHeatingOutputService roomHeatingOutputService = new RoomHeatingOutputService();

    public static String materialLookupKey(SurfaceType surfaceType, SurfaceLayer layer) {
        return layer.name() + "|" + MaterialProperties.from(surfaceType, layer).key();
    }

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
                .sorted(Comparator.comparing(MaterialSummary::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(material -> material.description(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<String, RoomAccumulator> rooms = new LinkedHashMap<>();
        for (MaterialSummary material : materialSummaries) {
            for (MaterialRoomEntry entry : material.roomEntries()) {
                String roomKey = entry.levelName() + "\u0000" + entry.roomName() + "\u0000" + entry.surfaceDescription();
                rooms.computeIfAbsent(
                                roomKey,
                                ignored -> new RoomAccumulator(entry.levelName(), entry.roomName(), entry.surfaceDescription())
                        )
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
            for (Room room : level.rooms()) {
                RoomHeatingOutputService.RoomHeatTotals roomHeatTotals = roomHeatingOutputService.totals(level, room);
                level.hydronicHeatings().stream()
                        .filter(heating -> heating.roomId().equals(room.id()))
                        .forEach(heating -> appendHydronicHeatingPlans(
                                summaries, level, room, heating, roomHeatTotals
                        ));
                roomHeatingOutputService.heatingElements(level, room).stream()
                        .forEach(element -> summaries.add(new HeatingPlanSummary(
                                level.name(),
                                room.name(),
                                surfacePosition(element.heatingType()),
                                "Raumobjekt",
                                element.objectName(),
                                0.0,
                                0.0,
                                0.0,
                                0.0,
                                element.heatOutputWatts(),
                                roomHeatTotals.floorHeatingWatts(),
                                roomHeatTotals.ceilingHeatingWatts(),
                                roomHeatTotals.additionalSurfaceHeatingWatts(),
                                roomHeatTotals.surfaceHeatingWatts(),
                                roomHeatTotals.heatingElementWatts(),
                                roomHeatTotals.totalHeatOutputWatts(),
                                roomHeatingObjectsSvg(level, room, element.heatingType()),
                                true
                        )));
            }
        }
        return List.copyOf(summaries);
    }

    private void appendHydronicHeatingPlans(
            List<HeatingPlanSummary> summaries,
            Level level,
            Room room,
            HydronicHeating heating,
            RoomHeatingOutputService.RoomHeatTotals roomHeatTotals
    ) {
        List<HydronicHeatingLayoutService.CircuitLayout> circuits = hydronicHeatingLayoutService.layoutBestEffort(heating).circuits();
        String svg = hydronicHeatingLayoutService.toSvg(
                level, room, heating, level.floorOpenings(), level.heatingExclusionAreas()
        );
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
                    roomHeatTotals.floorHeatingWatts(),
                    roomHeatTotals.ceilingHeatingWatts(),
                    roomHeatTotals.additionalSurfaceHeatingWatts(),
                    roomHeatTotals.surfaceHeatingWatts(),
                    roomHeatTotals.heatingElementWatts(),
                    roomHeatTotals.totalHeatOutputWatts(),
                    svg,
                    false
            ));
        }
    }

    private String surfacePosition(RoomObjectHeatingType heatingType) {
        return switch (heatingType) {
            case FLOOR_HEATING -> HeatingSurfacePosition.FLOOR.toString();
            case CEILING_HEATING -> HeatingSurfacePosition.CEILING.toString();
            case SURFACE_HEATING -> "Fläche";
            case HEATING_ELEMENT -> "Heizelement";
            default -> "";
        };
    }

    private String roomHeatingObjectsSvg(Level level, Room room, RoomObjectHeatingType heatingType) {
        List<RoomObject> heatingObjects = level.roomObjects().stream()
                .filter(roomObject -> roomObject.visible())
                .filter(roomObject -> roomObject.heatOutputWatts() > 0.0)
                .filter(roomObject -> roomObject.heatingType() == heatingType)
                .filter(roomObject -> containsPoint(room, roomObject.center()))
                .toList();
        if (heatingObjects.isEmpty()) {
            return "";
        }
        double minX = room.outline().stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0);
        double minY = room.outline().stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0);
        double maxX = room.outline().stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(1_000.0);
        double maxY = room.outline().stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(1_000.0);
        double padding = 120.0;
        double width = Math.max(1.0, maxX - minX + padding * 2.0);
        double height = Math.max(1.0, maxY - minY + padding * 2.0);
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"")
                .append(decimal(minX - padding, 1)).append(' ')
                .append(decimal(minY - padding, 1)).append(' ')
                .append(decimal(width, 1)).append(' ')
                .append(decimal(height, 1))
                .append("\" width=\"100%\" height=\"100%\">");
        svg.append("<rect x=\"").append(decimal(minX - padding, 1))
                .append("\" y=\"").append(decimal(minY - padding, 1))
                .append("\" width=\"").append(decimal(width, 1))
                .append("\" height=\"").append(decimal(height, 1))
                .append("\" fill=\"#fcfcfa\"/>");
        svg.append("<polygon points=\"")
                .append(room.outline().stream()
                        .map(point -> decimal(point.xMillimeters(), 1) + "," + decimal(point.yMillimeters(), 1))
                        .collect(java.util.stream.Collectors.joining(" ")))
                .append("\" fill=\"#f7f7f4\" stroke=\"#6f6559\" stroke-width=\"18\"/>");
        for (RoomObject roomObject : heatingObjects) {
            double objectMinX = roomObject.minXMillimeters();
            double objectMinY = roomObject.minYMillimeters();
            svg.append("<rect x=\"").append(decimal(objectMinX, 1))
                    .append("\" y=\"").append(decimal(objectMinY, 1))
                    .append("\" width=\"").append(decimal(roomObject.footprintWidthMillimeters(), 1))
                    .append("\" height=\"").append(decimal(roomObject.footprintDepthMillimeters(), 1))
                    .append("\" fill=\"#fff0e8\" stroke=\"#b65224\" stroke-width=\"12\" rx=\"10\" ry=\"10\"/>");
            svg.append("<text x=\"").append(decimal(roomObject.center().xMillimeters(), 1))
                    .append("\" y=\"").append(decimal(roomObject.center().yMillimeters(), 1))
                    .append("\" text-anchor=\"middle\" font-family=\"Menlo, monospace\" font-size=\"96\" fill=\"#6b2f18\">")
                    .append(roomObject.name())
                    .append(" ")
                    .append(decimal(roomObject.heatOutputWatts(), 0))
                    .append(" W</text>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private List<HeatingElementSummary> heatingElements(ProjectModel project) {
        List<HeatingElementSummary> summaries = new ArrayList<>();
        for (Level level : project.levels()) {
            for (Room room : level.rooms()) {
                RoomHeatingOutputService.RoomHeatTotals roomHeatTotals = roomHeatingOutputService.totals(level, room);
                roomHeatingOutputService.heatingElements(level, room).forEach(element -> summaries.add(
                        new HeatingElementSummary(
                                level.name(),
                                room.name(),
                                element.objectName(),
                                element.objectType(),
                                element.heatingType().toString(),
                                element.heatOutputWatts(),
                                roomHeatTotals.floorHeatingWatts(),
                                roomHeatTotals.ceilingHeatingWatts(),
                                roomHeatTotals.additionalSurfaceHeatingWatts(),
                                roomHeatTotals.surfaceHeatingWatts(),
                                roomHeatTotals.heatingElementWatts(),
                                roomHeatTotals.totalHeatOutputWatts()
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
                PlanPolygonSupport.perimeterMillimeters(room.outline()),
                surfaceLayerEffectService.effectiveVolumeCubicMeters(level, room),
                residentialAreaService.residentialAreaSquareMeters(level, room),
                roomHeatTotals.floorHeatingWatts(),
                roomHeatTotals.ceilingHeatingWatts(),
                roomHeatTotals.additionalSurfaceHeatingWatts(),
                roomHeatTotals.surfaceHeatingWatts(),
                roomHeatTotals.heatingElementWatts(),
                roomHeatTotals.totalHeatOutputWatts()
        );
    }

    private void collectLevel(Level level, List<FloorOpening> openingsAbove, Map<String, MaterialAccumulator> materials) {
        for (SurfaceLayerStack stack : level.surfaceLayerStacks()) {
            for (SurfaceLayer layer : stack.layers()) {
                for (SurfaceCoverage coverage : coverages(level, stack, openingsAbove)) {
                    CoverageEstimate estimate = estimateCoverage(layer, coverage);
                    if (estimate.placedPieceCount() == 0) {
                        continue;
                    }
                    MaterialProperties materialProperties = MaterialProperties.from(stack.surfaceType(), layer);
                    String materialKey = materialKey(layer.name(), materialProperties);
                    MaterialAccumulator material = materials.computeIfAbsent(
                            materialKey,
                            ignored -> new MaterialAccumulator(layer.name(), materialProperties)
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
            coverages.add(new SurfaceCoverage(level.name(), room.name(), surface, rectangles, true));
        }
        return coverages;
    }

    private SurfaceCoverage floorExtensionCoverage(Level level, FloorExtension extension) {
        return new SurfaceCoverage(
                level.name(),
                extension.type().toString(),
                "Oberseite " + extension.type(),
                List.of(new SurfaceRectangle(extension.minX(), extension.minY(), extension.widthMillimeters(), extension.depthMillimeters())),
                true
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
            coverages.addAll(wallSideCoverages(level, stack, wall, roomName, 1.0));
        }
        if (sides.negativeSide()) {
            coverages.addAll(wallSideCoverages(level, stack, wall, roomName, -1.0));
        }
    }

    private List<SurfaceCoverage> wallSideCoverages(Level level, SurfaceLayerStack stack, Wall wall, String roomName, double sideSign) {
        UUID roomId = stack.surfaceType() == SurfaceType.WALL_INTERIOR
                ? WallSurfaceTargetKey.roomId(stack.targetKey()).orElse(null)
                : null;
        List<de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle> visibleRectangles = roomId == null
                ? wallSurfaceOpeningService.visibleRectangles(level, wall, sideSign)
                : wallSurfaceOpeningService.visibleRectangles(level, wall, sideSign, roomId);
        List<SurfaceRectangle> rectangles = visibleRectangles.stream()
                .map(rectangle -> new SurfaceRectangle(0.0, 0.0, rectangle.widthMillimeters(), rectangle.heightMillimeters()))
                .toList();
        String sideLabel = sideSign > 0.0 ? "+" : "-";
        String baseDescription = stack.surfaceType() + " Wand " + shortId(wall.id().toString()) + " Seite " + sideLabel;
        if (!wall.hasVariableTopHeight()) {
            return List.of(new SurfaceCoverage(level.name(), roomName, baseDescription, rectangles, false));
        }
        List<SurfaceRectangle> sockelRectangles = new ArrayList<>();
        List<SurfaceRectangle> slopeRectangles = new ArrayList<>();
        for (de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle rectangle : visibleRectangles) {
            splitVariableWallRectangle(wall, rectangle, sockelRectangles, slopeRectangles);
        }
        List<SurfaceCoverage> coverages = new ArrayList<>();
        if (!sockelRectangles.isEmpty()) {
            coverages.add(new SurfaceCoverage(level.name(), roomName, baseDescription + " Sockel", List.copyOf(sockelRectangles), false));
        }
        if (!slopeRectangles.isEmpty()) {
            coverages.add(new SurfaceCoverage(level.name(), roomName, baseDescription + " Schräge", List.copyOf(slopeRectangles), false));
        }
        if (coverages.isEmpty()) {
            coverages.add(new SurfaceCoverage(level.name(), roomName, baseDescription, rectangles, false));
        }
        return List.copyOf(coverages);
    }

    private void splitVariableWallRectangle(
            Wall wall,
            de.schrell.cadas.application.view.WallSurfaceOpeningService.WallSurfaceRectangle rectangle,
            List<SurfaceRectangle> sockelRectangles,
            List<SurfaceRectangle> slopeRectangles
    ) {
        List<Double> cuts = wallSegmentCuts(wall, rectangle.startMillimeters(), rectangle.endMillimeters());
        for (int index = 0; index < cuts.size() - 1; index++) {
            double start = cuts.get(index);
            double end = cuts.get(index + 1);
            double width = end - start;
            if (width <= EPSILON) {
                continue;
            }
            double lowerHeight = rectangle.lowerHeightMillimeters();
            double upperHeight = rectangle.upperHeightMillimeters();
            double startTop = clippedWallHeight(wall, start, lowerHeight, upperHeight);
            double endTop = clippedWallHeight(wall, end, lowerHeight, upperHeight);
            double minimumTop = Math.min(startTop, endTop);
            if (minimumTop > lowerHeight + EPSILON) {
                sockelRectangles.add(new SurfaceRectangle(0.0, 0.0, width, minimumTop - lowerHeight));
            }
            double slopeHeight = Math.abs(endTop - startTop) / 2.0;
            if (slopeHeight > EPSILON) {
                slopeRectangles.add(new SurfaceRectangle(0.0, 0.0, width, slopeHeight));
            }
        }
    }

    private List<Double> wallSegmentCuts(Wall wall, double start, double end) {
        List<Double> cuts = new ArrayList<>();
        addDistinctCut(cuts, start);
        wall.resolvedProfile().stream()
                .mapToDouble(point -> point.offset().toMillimeters())
                .filter(offset -> offset > start + EPSILON && offset < end - EPSILON)
                .forEach(offset -> addDistinctCut(cuts, offset));
        addDistinctCut(cuts, end);
        cuts.sort(Double::compareTo);
        return List.copyOf(cuts);
    }

    private void addDistinctCut(List<Double> cuts, double value) {
        if (cuts.stream().noneMatch(existing -> Math.abs(existing - value) <= EPSILON)) {
            cuts.add(value);
        }
    }

    private double clippedWallHeight(Wall wall, double offset, double lowerHeight, double upperHeight) {
        return Math.max(lowerHeight, Math.min(upperHeight, wall.heightAt(offset)));
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

    private CoverageEstimate estimateCoverage(SurfaceLayer layer, SurfaceCoverage coverage) {
        CoverageAccumulator accumulator = new CoverageAccumulator(layer);
        if (coverage.continuousRoomRaster()) {
            accumulator.addContinuousRectangles(coverage.rectangles());
        } else {
            for (SurfaceRectangle rectangle : coverage.rectangles()) {
                accumulator.addRectangle(rectangle);
            }
        }
        return accumulator.toEstimate();
    }

    private String materialKey(String name, MaterialProperties materialProperties) {
        return name + "|" + materialProperties.key();
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
        return PlanPolygonSupport.containsPoint(room.outline(), point);
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
            List<SurfaceRectangle> rectangles,
            boolean continuousRoomRaster
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

        /**
         * Bewertet eine zusammenhängende Boden- oder Deckenfläche mit einem globalen Raster.
         */
        private void addContinuousRectangles(List<SurfaceRectangle> rectangles) {
            double tileWidth = layer.effectiveTileWidth().toMillimeters();
            double tileHeight = layer.effectiveTileHeight().toMillimeters();
            if (rectangles.isEmpty() || tileWidth <= EPSILON || tileHeight <= EPSILON) {
                return;
            }
            List<OrthogonalPolygonDecompositionService.CellRectangle> cellRectangles = rectangles.stream()
                    .map(rectangle -> new OrthogonalPolygonDecompositionService.CellRectangle(
                            rectangle.minXMillimeters(),
                            rectangle.maxXMillimeters(),
                            rectangle.minYMillimeters(),
                            rectangle.maxYMillimeters()
                    ))
                    .toList();
            Map<TileKey, List<SurfaceRectangle>> tilePieces = new LinkedHashMap<>();
            SurfaceRectangleTileLayoutService tileLayoutService = new SurfaceRectangleTileLayoutService();
            for (PlacedSurfaceTile tile : tileLayoutService.tilesForRectangles(cellRectangles, layer)) {
                for (SurfaceRectangle rectangle : rectangles) {
                    double minX = Math.max(tile.x(), rectangle.minXMillimeters());
                    double maxX = Math.min(tile.x() + tile.width(), rectangle.maxXMillimeters());
                    double minY = Math.max(tile.y(), rectangle.minYMillimeters());
                    double maxY = Math.min(tile.y() + tile.height(), rectangle.maxYMillimeters());
                    if (maxX <= minX + EPSILON || maxY <= minY + EPSILON) {
                        continue;
                    }
                    tilePieces.computeIfAbsent(new TileKey(tile.column(), tile.row()), ignored -> new ArrayList<>())
                            .add(new SurfaceRectangle(minX, minY, maxX - minX, maxY - minY));
                }
            }
            for (List<SurfaceRectangle> pieces : tilePieces.values()) {
                for (SurfaceRectangle piece : mergeTouchingPieces(pieces)) {
                    addPlacementDimensions(piece.widthMillimeters(), piece.heightMillimeters(), tileWidth, tileHeight);
                }
            }
        }

        private List<SurfaceRectangle> mergeTouchingPieces(List<SurfaceRectangle> pieces) {
            List<SurfaceRectangle> merged = new ArrayList<>(pieces);
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int firstIndex = 0; firstIndex < merged.size() && !changed; firstIndex++) {
                    for (int secondIndex = firstIndex + 1; secondIndex < merged.size(); secondIndex++) {
                        SurfaceRectangle combined = mergeIfTouching(merged.get(firstIndex), merged.get(secondIndex));
                        if (combined == null) {
                            continue;
                        }
                        merged.remove(secondIndex);
                        merged.remove(firstIndex);
                        merged.add(combined);
                        changed = true;
                        break;
                    }
                }
            }
            return List.copyOf(merged);
        }

        private SurfaceRectangle mergeIfTouching(SurfaceRectangle first, SurfaceRectangle second) {
            if (sameValue(first.minXMillimeters(), second.minXMillimeters())
                    && sameValue(first.widthMillimeters(), second.widthMillimeters())) {
                double firstMaxY = first.maxYMillimeters();
                double secondMaxY = second.maxYMillimeters();
                if (sameValue(firstMaxY, second.minYMillimeters()) || sameValue(secondMaxY, first.minYMillimeters())) {
                    double minY = Math.min(first.minYMillimeters(), second.minYMillimeters());
                    double maxY = Math.max(firstMaxY, secondMaxY);
                    return new SurfaceRectangle(first.minXMillimeters(), minY, first.widthMillimeters(), maxY - minY);
                }
            }
            if (sameValue(first.minYMillimeters(), second.minYMillimeters())
                    && sameValue(first.heightMillimeters(), second.heightMillimeters())) {
                double firstMaxX = first.maxXMillimeters();
                double secondMaxX = second.maxXMillimeters();
                if (sameValue(firstMaxX, second.minXMillimeters()) || sameValue(secondMaxX, first.minXMillimeters())) {
                    double minX = Math.min(first.minXMillimeters(), second.minXMillimeters());
                    double maxX = Math.max(firstMaxX, secondMaxX);
                    return new SurfaceRectangle(minX, first.minYMillimeters(), maxX - minX, first.heightMillimeters());
                }
            }
            return null;
        }

        private boolean sameValue(double first, double second) {
            return Math.abs(first - second) <= EPSILON;
        }

        private void addRectangle(SurfaceRectangle rectangle) {
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
                    layer.minimumStartEndMargin(),
                    layer.freeMargins()
            ));
            for (TilePlacement placement : placements) {
                addPlacementDimensions(placement.width().toMillimeters(), placement.height().toMillimeters(), tileWidth, tileHeight);
            }
        }

        private void addPlacementDimensions(double width, double height, double tileWidth, double tileHeight) {
            boolean cutsWidth = width < tileWidth - EPSILON;
            boolean cutsHeight = height < tileHeight - EPSILON;
            placedPieceCount++;
            coveredAreaSquareMillimeters += width * height;
            if (!cutsWidth && !cutsHeight) {
                fullPieceCount++;
                return;
            }
            cutPieceCount++;
            cutPieces.add(normalizedMaterialCutPiece(width, height));
            if (cutsWidth) {
                cutCount++;
                cutPenaltySum += 1.0 - clamp(height / tileHeight, 0.0, 1.0);
            }
            if (cutsHeight) {
                cutCount++;
                cutPenaltySum += 1.0 - clamp(width / tileWidth, 0.0, 1.0);
            }
        }

        private CutPiece normalizedMaterialCutPiece(double width, double height) {
            return layer.layoutRotatedQuarterTurn()
                    ? new CutPiece(height, width)
                    : new CutPiece(width, height);
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

        private double shortestEdgeMillimeters() {
            return Math.min(widthMillimeters, heightMillimeters);
        }

        private RestPiece normalized() {
            return widthMillimeters >= heightMillimeters
                    ? this
                    : new RestPiece(heightMillimeters, widthMillimeters);
        }
    }

    private record FitCandidate(int restIndex, double widthMillimeters, double heightMillimeters, double wasteSquareMillimeters) {
    }

    private record TileKey(int column, int row) {
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
            List<RestPiece> verticalFirst = usableRestPieces(List.of(
                    new RestPiece(source.widthMillimeters() - usedWidth, source.heightMillimeters()),
                    new RestPiece(usedWidth, source.heightMillimeters() - usedHeight)
            ));
            List<RestPiece> horizontalFirst = usableRestPieces(List.of(
                    new RestPiece(source.widthMillimeters(), source.heightMillimeters() - usedHeight),
                    new RestPiece(source.widthMillimeters() - usedWidth, usedHeight)
            ));
            return compare(score(verticalFirst), score(horizontalFirst)) >= 0 ? verticalFirst : horizontalFirst;
        }

        private static RestPieceScore score(List<RestPiece> restPieces) {
            List<RestPiece> usableRestPieces = usableRestPieces(restPieces);
            return new RestPieceScore(
                    usableRestPieces.size(),
                    usableRestPieces.stream().mapToDouble(RestPiece::shortestEdgeMillimeters).sum(),
                    usableRestPieces.stream().mapToDouble(RestPiece::areaSquareMillimeters).sum(),
                    usableRestPieces.stream().mapToDouble(RestPiece::areaSquareMillimeters).max().orElse(0.0)
            );
        }

        private static int compare(RestPieceScore first, RestPieceScore second) {
            int usableCount = Integer.compare(first.usableCount(), second.usableCount());
            if (usableCount != 0) {
                return usableCount;
            }
            int shortestEdge = Double.compare(first.shortestEdgeSumMillimeters(), second.shortestEdgeSumMillimeters());
            if (shortestEdge != 0) {
                return shortestEdge;
            }
            int totalArea = Double.compare(first.totalAreaSquareMillimeters(), second.totalAreaSquareMillimeters());
            if (totalArea != 0) {
                return totalArea;
            }
            return Double.compare(first.maxAreaSquareMillimeters(), second.maxAreaSquareMillimeters());
        }

        private static List<RestPiece> usableRestPieces(List<RestPiece> candidates) {
            return candidates.stream()
                    .filter(MaterialCuttingOptimizer::isUsable)
                    .toList();
        }

        private static boolean isUsable(RestPiece restPiece) {
            return restPiece.widthMillimeters() + EPSILON >= MINIMUM_REUSABLE_REST_EDGE_MILLIMETERS
                    && restPiece.heightMillimeters() + EPSILON >= MINIMUM_REUSABLE_REST_EDGE_MILLIMETERS;
        }

        private static List<RestPieceSummary> groupedRestPieces(List<RestPiece> restPieces) {
            Map<String, RestPieceAccumulator> groups = new LinkedHashMap<>();
            restPieces.stream()
                    .filter(MaterialCuttingOptimizer::isUsable)
                    .map(RestPiece::normalized)
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

    private record RestPieceScore(
            int usableCount,
            double shortestEdgeSumMillimeters,
            double totalAreaSquareMillimeters,
            double maxAreaSquareMillimeters
    ) {
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

        private final String materialName;
        private final MaterialProperties materialProperties;
        private final List<PendingMaterialRoomEntry> pendingEntries = new ArrayList<>();
        private int placedPieceCount;
        private int fullPieceCount;
        private int cutPieceCount;
        private int cutCount;
        private double coveredAreaSquareMeters;
        private double cutPenaltySum;

        private MaterialAccumulator(String materialName, MaterialProperties materialProperties) {
            this.materialName = materialName;
            this.materialProperties = materialProperties;
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
            double tileAreaSquareMillimeters = materialProperties.tileWidthMillimeters() * materialProperties.tileHeightMillimeters();
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
                    materialName + "|" + materialProperties.key(),
                    materialName,
                    materialProperties.surfaceType(),
                    materialProperties.description(),
                    materialProperties.values(),
                    materialProperties.labeledValues(),
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
                    materialProperties.tileWidthMillimeters(),
                    materialProperties.tileHeightMillimeters(),
                    cutPieces,
                    materialProperties.cutRestriction().allowsMaterialRotation(),
                    pendingEntries.size()
            );
        }
    }

    private record MaterialProperties(
            SurfaceType surfaceType,
            double thicknessMillimeters,
            double tileWidthMillimeters,
            double tileHeightMillimeters,
            SurfaceLayoutMode layoutMode,
            double layoutOffsetMillimeters,
            double minimumOffsetMillimeters,
            double minimumEdgeWidthMillimeters,
            double minimumStartEndMarginMillimeters,
            double freeMarginLeftMillimeters,
            double freeMarginRightMillimeters,
            double freeMarginTopMillimeters,
            double freeMarginBottomMillimeters,
            double jointWidthMillimeters,
            SurfaceCutRestriction cutRestriction,
            String coveringSource
    ) {

        private static MaterialProperties from(SurfaceType surfaceType, SurfaceLayer layer) {
            return new MaterialProperties(
                    surfaceType,
                    layer.thickness().toMillimeters(),
                    layer.tileWidth().toMillimeters(),
                    layer.tileHeight().toMillimeters(),
                    layer.layoutMode(),
                    layer.layoutOffset().toMillimeters(),
                    layer.minimumOffset().toMillimeters(),
                    layer.minimumEdgeWidth().toMillimeters(),
                    layer.minimumStartEndMargin().toMillimeters(),
                    layer.freeMargins().left().toMillimeters(),
                    layer.freeMargins().right().toMillimeters(),
                    layer.freeMargins().top().toMillimeters(),
                    layer.freeMargins().bottom().toMillimeters(),
                    layer.jointWidth().toMillimeters(),
                    layer.cutRestriction(),
                    layer.coveringSource()
            );
        }

        private String key() {
            return String.join("|",
                    surfaceType.name(),
                    Double.toString(thicknessMillimeters),
                    Double.toString(tileWidthMillimeters),
                    Double.toString(tileHeightMillimeters),
                    layoutMode.name(),
                    Double.toString(layoutOffsetMillimeters),
                    Double.toString(minimumOffsetMillimeters),
                    Double.toString(minimumEdgeWidthMillimeters),
                    Double.toString(minimumStartEndMarginMillimeters),
                    Double.toString(freeMarginLeftMillimeters),
                    Double.toString(freeMarginRightMillimeters),
                    Double.toString(freeMarginTopMillimeters),
                    Double.toString(freeMarginBottomMillimeters),
                    Double.toString(jointWidthMillimeters),
                    cutRestriction.name(),
                    coveringSource
            );
        }

        private String description() {
            return coveringSource.isBlank() ? surfaceType.toString() : surfaceType + ", Quelle: " + coveringSource;
        }

        private String values() {
            return labeledValues().entrySet().stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue())
                    .collect(java.util.stream.Collectors.joining(", "));
        }

        private Map<String, String> labeledValues() {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("Dicke", length(Length.ofMillimeters(thicknessMillimeters), LengthUnit.MILLIMETER, 1));
            values.put(
                    "Format",
                    length(Length.ofMillimeters(tileWidthMillimeters), LengthUnit.CENTIMETER, 1)
                            + " x "
                            + length(Length.ofMillimeters(tileHeightMillimeters), LengthUnit.CENTIMETER, 1)
            );
            values.put("Verlegung", layoutMode.toString());
            values.put("Versatz", length(Length.ofMillimeters(layoutOffsetMillimeters), LengthUnit.CENTIMETER, 1));
            values.put("Mindestversatz", length(Length.ofMillimeters(minimumOffsetMillimeters), LengthUnit.CENTIMETER, 1));
            values.put("Mindestrand", length(Length.ofMillimeters(minimumEdgeWidthMillimeters), LengthUnit.CENTIMETER, 1));
            values.put("Anfang/Ende", length(Length.ofMillimeters(minimumStartEndMarginMillimeters), LengthUnit.CENTIMETER, 1));
            values.put(
                    "Freiränder L/R/O/U",
                    length(Length.ofMillimeters(freeMarginLeftMillimeters), LengthUnit.CENTIMETER, 1)
                            + "/"
                            + length(Length.ofMillimeters(freeMarginRightMillimeters), LengthUnit.CENTIMETER, 1)
                            + "/"
                            + length(Length.ofMillimeters(freeMarginTopMillimeters), LengthUnit.CENTIMETER, 1)
                            + "/"
                            + length(Length.ofMillimeters(freeMarginBottomMillimeters), LengthUnit.CENTIMETER, 1)
            );
            values.put("Fuge", length(Length.ofMillimeters(jointWidthMillimeters), LengthUnit.MILLIMETER, 1));
            values.put("Schnittbeschränkung", cutRestriction.label());
            return java.util.Collections.unmodifiableMap(values);
        }
    }

    private static final class RoomAccumulator {

        private final String levelName;
        private final String roomName;
        private final String surfaceDescription;
        private int requiredPieceCount;
        private int placedPieceCount;
        private int cutCount;
        private double coveredAreaSquareMeters;
        private double cutPenaltySum;

        private RoomAccumulator(String levelName, String roomName, String surfaceDescription) {
            this.levelName = levelName;
            this.roomName = roomName;
            this.surfaceDescription = surfaceDescription;
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
                    surfaceDescription,
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
            return toMarkdown(true);
        }

        public String toDisplayMarkdown() {
            return toMarkdown(false);
        }

        private String toMarkdown(boolean includeHeatingPlanSvg) {
            StringBuilder markdown = new StringBuilder();
            markdown.append("# Materialliste Beläge – ").append(projectName).append("\n\n");
            appendRooms(markdown);
            if (materials.isEmpty()) {
                appendHeatingPlans(markdown, includeHeatingPlanSvg);
                appendHeatingElements(markdown);
                markdown.append("## Beläge\n\nKeine Beläge vorhanden.\n");
                return markdown.toString();
            }
            appendMaterialSummary(markdown);
            appendHeatingPlans(markdown, includeHeatingPlanSvg);
            appendHeatingElements(markdown);
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
            rooms.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            RoomSummary::levelName,
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()
                    ))
                    .forEach((levelName, levelRooms) -> {
                        markdown.append("### ").append(levelName).append("\n\n");
                        markdown.append("| Raum | Maße | Lichte Höhe | Grundfläche | Innenumfang | Mietfläche | Volumen | FBH | DH | Flächenheizung | Heizelemente | Gesamtwärme |\n");
                        markdown.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
                        for (RoomSummary room : levelRooms) {
                            markdown.append("| ")
                                    .append(markdownCell(room.roomName()))
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
                                    .append(decimal(room.innerPerimeterMillimeters() / 1000.0, 2)).append(" m")
                                    .append(" | ")
                                    .append(decimal(room.residentialAreaSquareMeters(), 2)).append(" m²")
                                    .append(" | ")
                                    .append(decimal(room.volumeCubicMeters(), 2)).append(" m³")
                                    .append(" | ")
                                    .append(decimal(room.floorHeatingWatts(), 0)).append(" W")
                                    .append(" | ")
                                    .append(decimal(room.ceilingHeatingWatts(), 0)).append(" W")
                                    .append(" | ")
                                    .append(decimal(room.additionalSurfaceHeatingWatts(), 0)).append(" W")
                                    .append(" | ")
                                    .append(decimal(room.heatingElementWatts(), 0)).append(" W")
                                    .append(" | ")
                                    .append(decimal(room.totalHeatOutputWatts(), 0)).append(" W")
                                    .append(" |\n");
                        }
                        markdown.append('\n');
                    });
            markdown.append("Die Mietfläche gewichtet lichte Höhen ab 2 m vollständig, zwischen 1 m und 2 m zur Hälfte und unter 1 m nicht. Sichtbare Boden- und Deckenbeläge reduzieren die lichte Höhe.\n\n");
        }

        private void appendHeatingPlans(StringBuilder markdown, boolean includeSvg) {
            markdown.append("## Flächenheizungen\n\n");
            if (heatingPlans.isEmpty()) {
                markdown.append("Keine Flächenheizungen vorhanden.\n\n");
                return;
            }
            markdown.append("| Raum | Fläche | Verlegung | Heizkreis | Heizfläche | HKL | Maximum | W/m² | Leistung | Raum FBH | Raum DH | Raum Fläche | Heizelemente | Raum gesamt |\n");
            markdown.append("|---|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
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
                        .append(heatingPlanMetric(plan, plan.areaSquareMeters(), 2, " m²"))
                        .append(" | ")
                        .append(heatingPlanMetric(plan, plan.pipeLengthMeters(), 1, " m"))
                        .append(" | ")
                        .append(heatingPlanMetric(plan, plan.maximumPipeLengthMeters(), 1, " m"))
                        .append(" | ")
                        .append(heatingPlanMetric(plan, plan.heatOutputWattsPerSquareMeter(), 1, ""))
                        .append(" | ")
                        .append(decimal(plan.heatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(plan.roomFloorHeatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(plan.roomCeilingHeatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(plan.roomAdditionalSurfaceHeatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(plan.roomHeatingElementWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(plan.roomTotalHeatOutputWatts(), 0)).append(" W")
                        .append(" |\n");
            }
            markdown.append('\n');
            if (!includeSvg) {
                return;
            }
            heatingPlans.stream()
                    .filter(plan -> !plan.objectBased())
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
            markdown.append("| Raum | Objekt | Heizart | Leistung | Raum FBH | Raum DH | Raum Fläche | Raum Heizelemente | Raum gesamt |\n");
            markdown.append("|---|---|---|---:|---:|---:|---:|---:|---:|\n");
            for (HeatingElementSummary element : heatingElements) {
                markdown.append("| ")
                        .append(markdownCell(element.levelName() + " / " + element.roomName()))
                        .append(" | ")
                        .append(markdownCell(element.objectName()))
                        .append(" | ")
                        .append(markdownCell(element.heatingType()))
                        .append(" | ")
                        .append(decimal(element.heatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(element.roomFloorHeatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(element.roomCeilingHeatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(element.roomAdditionalSurfaceHeatOutputWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(element.roomHeatingElementWatts(), 0)).append(" W")
                        .append(" | ")
                        .append(decimal(element.roomTotalHeatOutputWatts(), 0)).append(" W")
                        .append(" |\n");
            }
            markdown.append('\n');
        }

        private String heatingPlanMetric(HeatingPlanSummary plan, double value, int decimals, String unit) {
            if (plan.objectBased()) {
                return "-";
            }
            return decimal(value, decimals) + unit;
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
            appendMaterialSplitReasons(markdown);
        }

        private void appendMaterialSplitReasons(StringBuilder markdown) {
            Map<String, List<MaterialSummary>> materialsByName = materials.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            MaterialSummary::name,
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()
                    ));
            List<Map.Entry<String, List<MaterialSummary>>> splitMaterials = materialsByName.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .toList();
            if (splitMaterials.isEmpty()) {
                return;
            }
            markdown.append("### Trennmerkmale bei mehrfachen Materialnamen\n\n");
            for (Map.Entry<String, List<MaterialSummary>> entry : splitMaterials) {
                markdown.append("* ")
                        .append(markdownCell(entry.getKey()))
                        .append(": ")
                        .append(markdownCell(String.join(", ", differingPropertyDescriptions(entry.getValue()))))
                        .append('\n');
            }
            markdown.append('\n');
        }

        private List<String> differingPropertyDescriptions(List<MaterialSummary> materialsWithSameName) {
            List<String> descriptions = new ArrayList<>();
            LinkedHashMap<String, List<String>> valuesByLabel = new LinkedHashMap<>();
            for (MaterialSummary material : materialsWithSameName) {
                material.labeledValues().forEach((label, value) -> valuesByLabel.computeIfAbsent(label, ignored -> new ArrayList<>()).add(value));
            }
            for (Map.Entry<String, List<String>> entry : valuesByLabel.entrySet()) {
                List<String> distinctValues = entry.getValue().stream().distinct().toList();
                if (distinctValues.size() <= 1) {
                    continue;
                }
                descriptions.add(entry.getKey() + " " + String.join(" | ", distinctValues));
            }
            return descriptions;
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
            markdown.append("## Komplexität pro Raum und Fläche\n\n");
            markdown.append("| Raum/Fläche | Belegte Fläche | Stückzahl | Schnitte | Komplexität |\n");
            markdown.append("|---|---:|---:|---:|---:|\n");
            for (RoomComplexitySummary room : roomComplexities) {
                markdown.append("| ")
                        .append(markdownCell(room.levelName() + " / " + room.roomName() + " / " + room.surfaceDescription()))
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
            String lookupKey,
            String name,
            SurfaceType surfaceType,
            String description,
            String values,
            Map<String, String> labeledValues,
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
            double roomFloorHeatOutputWatts,
            double roomCeilingHeatOutputWatts,
            double roomAdditionalSurfaceHeatOutputWatts,
            double roomSurfaceHeatOutputWatts,
            double roomHeatingElementWatts,
            double roomTotalHeatOutputWatts,
            String svg,
            boolean objectBased
    ) {
    }

    public record HeatingElementSummary(
            String levelName,
            String roomName,
            String objectName,
            String objectType,
            String heatingType,
            double heatOutputWatts,
            double roomFloorHeatOutputWatts,
            double roomCeilingHeatOutputWatts,
            double roomAdditionalSurfaceHeatOutputWatts,
            double roomSurfaceHeatOutputWatts,
            double roomHeatingElementWatts,
            double roomTotalHeatOutputWatts
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
            String surfaceDescription,
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
            double innerPerimeterMillimeters,
            double volumeCubicMeters,
            double residentialAreaSquareMeters,
            double floorHeatingWatts,
            double ceilingHeatingWatts,
            double additionalSurfaceHeatingWatts,
            double surfaceHeatingWatts,
            double heatingElementWatts,
            double totalHeatOutputWatts
    ) {
    }
}
