package de.andreas.cadas.infrastructure.dxf;

import de.andreas.cadas.application.exchange.ProjectExchangeService;
import de.andreas.cadas.domain.geometry.Angle;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.ProjectModel;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.Roof;
import de.andreas.cadas.domain.model.RoofType;
import de.andreas.cadas.domain.model.SlopedCeilingProfile;
import de.andreas.cadas.domain.model.SlopedCeilingSide;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;
import de.andreas.cadas.domain.model.SurfaceType;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DxfProjectExchangeService implements ProjectExchangeService {

    private final DxfLevelExchangeService levelExchangeService = new DxfLevelExchangeService();

    @Override
    public void exportProject(ProjectModel project, Path targetFile) throws IOException {
        Path parent = targetFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder dxf = new StringBuilder();
        DxfDocumentSupport.DxfWriteContext context = DxfDocumentSupport.startDocument(
                dxf,
                collectProjectLayers(project),
                Set.of(DxfDocumentSupport.BLOCK_DOOR, DxfDocumentSupport.BLOCK_WINDOW, DxfDocumentSupport.BLOCK_STAIR)
        );
        appendMetadataText(dxf, context, new PlanPoint(0, 0), DxfMetadataCodec.CURRENT_MARKER);
        appendMetadataText(dxf, context, new PlanPoint(0, 0), "PROJECT|" + DxfMetadataCodec.encode(project.name()));

        for (Level level : project.levels()) {
            String layerPrefix = sanitizeLayerName(level.name());
            appendMetadataText(dxf, context, new PlanPoint(0, 0), "LEVEL|" + DxfMetadataCodec.encode(level.name()));
            exportLevelGeometry(dxf, context, level, layerPrefix);
            exportLevelMetadata(dxf, context, level);
        }

        project.roof().ifPresent(roof -> appendMetadataText(dxf, context, new PlanPoint(0, 0), String.format(
                Locale.US,
                "ROOF|%s|%.3f|%.3f|%s",
                roof.roofType().name(),
                roof.pitchAngle().degrees(),
                roof.overhang().toMillimeters(),
                roof.gutterEnabled()
        )));

        DxfDocumentSupport.finishDocument(context);
        Files.writeString(targetFile, dxf.toString());
    }

    @Override
    public ProjectModel importProject(Path sourceFile, String projectName) throws IOException {
        List<String> lines = Files.readAllLines(sourceFile);
        List<String> metadata = extractMetadata(lines);
        if (metadata.isEmpty()) {
            Level level = levelExchangeService.importLevel(sourceFile, "Erdgeschoss");
            ProjectModel fallback = ProjectModel.withDefaultLevel(projectName, level.name());
            copyLevelContents(level, fallback.primaryLevel());
            return fallback;
        }

        Map<String, Level> levels = new LinkedHashMap<>();
        Map<String, SurfaceLayerStack> lastStackByLevel = new LinkedHashMap<>();
        String importedProjectName = projectName;
        Roof importedRoof = null;
        boolean encodedFields = DxfMetadataCodec.usesCurrentEncoding(metadata);
        for (String entry : metadata) {
            String[] parts = DxfMetadataCodec.split(entry);
            if (DxfMetadataCodec.isMarker(parts)) {
                continue;
            }
            try {
                switch (parts[0]) {
                    case "PROJECT" -> importedProjectName = stripDxfExtension(DxfMetadataCodec.decode(parts[1], encodedFields));
                    case "LEVEL" -> levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                    case "WALL" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        double startHeight = parts.length >= 11 ? parseDouble(parts[5]) : parseDouble(parts[4]);
                        double endHeight = parts.length >= 11 ? parseDouble(parts[6]) : parseDouble(parts[4]);
                        int pointOffset = parts.length >= 11 ? 2 : 0;
                        level.addWall(new Wall(
                                UUID.fromString(parts[2]),
                                new PlanSegment(
                                        new PlanPoint(parseDouble(parts[5 + pointOffset]), parseDouble(parts[6 + pointOffset])),
                                        new PlanPoint(parseDouble(parts[7 + pointOffset]), parseDouble(parts[8 + pointOffset]))
                                ),
                                Length.ofMillimeters(parseDouble(parts[3])),
                                Length.ofMillimeters(parseDouble(parts[4])),
                                Length.ofMillimeters(startHeight),
                                Length.ofMillimeters(endHeight)
                        ));
                    }
                    case "ROOM" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        if (isUuid(parts[2])) {
                            level.addRoom(new Room(
                                    UUID.fromString(parts[2]),
                                    DxfMetadataCodec.decode(parts[3], encodedFields),
                                    deserializePoints(parts[7]),
                                    Length.ofMillimeters(parseDouble(parts[4])),
                                    Length.ofMillimeters(parseDouble(parts[5])),
                                    Length.ofMillimeters(parseDouble(parts[6])),
                                    parts.length >= 9 ? deserializeSlopedCeiling(parts[8]) : null,
                                    parts.length >= 10 ? deserializeCeilingVertexHeights(parts[9]) : null
                            ));
                        } else {
                            level.addRoom(new Room(
                                    UUID.randomUUID(),
                                    DxfMetadataCodec.decode(parts[2], encodedFields),
                                    deserializePoints(parts[6]),
                                    Length.ofMillimeters(parseDouble(parts[3])),
                                    Length.ofMillimeters(parseDouble(parts[4])),
                                    Length.ofMillimeters(parseDouble(parts[5])),
                                    parts.length >= 8 ? deserializeSlopedCeiling(parts[7]) : null,
                                    parts.length >= 9 ? deserializeCeilingVertexHeights(parts[8]) : null
                            ));
                        }
                    }
                    case "DOOR" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        level.addDoor(deserializeDoor(parts));
                    }
                    case "WINDOW" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        level.addWindow(deserializeWindow(parts));
                    }
                    case "STAIR" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        level.addStaircase(new Staircase(
                                UUID.fromString(parts[2]),
                                StairType.valueOf(parts[3]),
                                new PlanPoint(parseDouble(parts[4]), parseDouble(parts[5])),
                                new PlanPoint(parseDouble(parts[6]), parseDouble(parts[7])),
                                Length.ofMillimeters(parseDouble(parts[8])),
                                Integer.parseInt(parts[9]),
                                Integer.parseInt(parts[10])
                        ));
                    }
                    case "ROOF" -> importedRoof = new Roof(
                            RoofType.valueOf(parts[1]),
                            Angle.ofDegrees(parseDouble(parts[2])),
                            Length.ofMillimeters(parseDouble(parts[3])),
                            Boolean.parseBoolean(parts[4])
                    );
                    case "SLS" -> {
                        String levelName = DxfMetadataCodec.decode(parts[1], encodedFields);
                        Level level = levels.computeIfAbsent(levelName, Level::new);
                        SurfaceLayerStack stack = new SurfaceLayerStack(
                                UUID.fromString(parts[2]),
                                SurfaceType.valueOf(parts[3]),
                                DxfMetadataCodec.decode(parts[4], encodedFields)
                        );
                        level.addSurfaceLayerStack(stack);
                        lastStackByLevel.put(levelName, stack);
                    }
                    case "SLL" -> {
                        SurfaceLayerStack stack = lastStackByLevel.get(DxfMetadataCodec.decode(parts[1], encodedFields));
                        if (stack != null) {
                            SurfaceLayer layer = new SurfaceLayer(
                                    UUID.fromString(parts[2]),
                                    DxfMetadataCodec.decode(parts[3], encodedFields),
                                    Length.ofMillimeters(parseDouble(parts[4])),
                                    Boolean.parseBoolean(parts[5]),
                                    Length.ofMillimeters(parseDouble(parts[6])),
                                    Length.ofMillimeters(parseDouble(parts[7])),
                                    SurfaceLayoutMode.valueOf(parts[8]),
                                    Length.ofMillimeters(parseDouble(parts[9])),
                                    Length.ofMillimeters(parseDouble(parts[10])),
                                    Length.ofMillimeters(parseDouble(parts[11])),
                                    Length.ofMillimeters(parts.length >= 15 ? parseDouble(parts[12]) : parseDouble(parts[11])),
                                    Length.ofMillimeters(parts.length >= 15 ? parseDouble(parts[13]) : parseDouble(parts[12])),
                                    DxfMetadataCodec.decode(parts.length >= 15 ? parts[14] : parts[13], encodedFields)
                            );
                            stack.addLayer(layer);
                        }
                    }
                    default -> {
                    }
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                // Fremde oder beschädigte Metadaten sollen den Geometrieimport nicht blockieren.
            }
        }

        if (levels.isEmpty()) {
            return ProjectModel.withDefaultLevel(importedProjectName, "Erdgeschoss");
        }
        List<Level> importedLevels = new ArrayList<>(levels.values());
        ProjectModel project = ProjectModel.withDefaultLevel(importedProjectName, importedLevels.getFirst().name());
        copyLevelContents(importedLevels.getFirst(), project.primaryLevel());
        for (int index = 1; index < importedLevels.size(); index++) {
            project.addLevel(importedLevels.get(index).copy());
        }
        if (importedRoof != null) {
            project.defineRoof(importedRoof);
        }
        return project;
    }

    private Set<String> collectProjectLayers(ProjectModel project) {
        Set<String> layers = new LinkedHashSet<>();
        layers.add(DxfLayer.CADAS_META.name());
        for (Level level : project.levels()) {
            String layerPrefix = sanitizeLayerName(level.name());
            layers.add(layerPrefix + "_WALLS");
            layers.add(layerPrefix + "_ROOMS");
            layers.add(layerPrefix + "_DOORS");
            layers.add(layerPrefix + "_WINDOWS");
            layers.add(layerPrefix + "_STAIRS");
        }
        return layers;
    }

    private void exportLevelGeometry(StringBuilder dxf, DxfDocumentSupport.DxfWriteContext context, Level level, String layerPrefix) {
        for (Wall wall : level.walls()) {
            appendLineEntity(dxf, context, layerPrefix + "_WALLS", wall.axis().start(), wall.axis().end());
        }
        for (Room room : level.rooms()) {
            appendClosedPolyline(dxf, context, layerPrefix + "_ROOMS", room.outline());
        }
        for (Door door : level.doors()) {
            Wall hostWall = level.findWall(door.wallId());
            PlanPoint start = hostWall.axis().pointAt(door.offsetFromStart());
            PlanPoint end = hostWall.axis().pointAt(door.offsetFromStart().add(door.width()));
            appendLineEntity(dxf, context, layerPrefix + "_DOORS", start, end);
            DxfDocumentSupport.appendInsert(
                    dxf,
                    context,
                    layerPrefix + "_DOORS",
                    DxfDocumentSupport.BLOCK_DOOR,
                    start,
                    Math.max(0.001, door.width().toMillimeters() / 1000.0),
                    1.0,
                    hostWall.axis().angle().degrees()
            );
        }
        for (WindowElement window : level.windows()) {
            Wall hostWall = level.findWall(window.wallId());
            PlanPoint start = hostWall.axis().pointAt(window.offsetFromStart());
            PlanPoint end = hostWall.axis().pointAt(window.offsetFromStart().add(window.width()));
            appendLineEntity(dxf, context, layerPrefix + "_WINDOWS", start, end);
            DxfDocumentSupport.appendInsert(
                    dxf,
                    context,
                    layerPrefix + "_WINDOWS",
                    DxfDocumentSupport.BLOCK_WINDOW,
                    start,
                    Math.max(0.001, window.width().toMillimeters() / 1000.0),
                    1.0,
                    hostWall.axis().angle().degrees()
            );
        }
        for (Staircase staircase : level.staircases()) {
            appendClosedPolyline(dxf, context, layerPrefix + "_STAIRS", List.of(
                    staircase.pointAtLocalPosition(0, 0),
                    staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0),
                    staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()),
                    staircase.pointAtLocalPosition(0, staircase.heightMillimeters())
            ));
            DxfDocumentSupport.appendInsert(
                    dxf,
                    context,
                    layerPrefix + "_STAIRS",
                    DxfDocumentSupport.BLOCK_STAIR,
                    new PlanPoint(staircase.minX(), staircase.minY()),
                    Math.max(0.001, staircase.widthMillimeters() / 1000.0),
                    Math.max(0.001, staircase.heightMillimeters() / 1000.0),
                    staircase.rotationQuarterTurns() * 90.0
            );
        }
    }

    private void exportLevelMetadata(StringBuilder dxf, DxfDocumentSupport.DxfWriteContext context, Level level) {
        for (Wall wall : level.walls()) {
            appendMetadataText(dxf, context, wall.axis().start(), String.format(
                    Locale.US,
                    "WALL|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f",
                    DxfMetadataCodec.encode(level.name()),
                    wall.id(),
                    wall.thickness().toMillimeters(),
                    wall.height().toMillimeters(),
                    wall.startHeight().toMillimeters(),
                    wall.endHeight().toMillimeters(),
                    wall.axis().start().xMillimeters(),
                    wall.axis().start().yMillimeters(),
                    wall.axis().end().xMillimeters(),
                    wall.axis().end().yMillimeters()
            ));
        }
        for (Room room : level.rooms()) {
            appendMetadataText(dxf, context, room.centerPoint(), String.format(
                    Locale.US,
                    "ROOM|%s|%s|%s|%.3f|%.3f|%.3f|%s|%s|%s",
                    DxfMetadataCodec.encode(level.name()),
                    room.id(),
                    DxfMetadataCodec.encode(room.name()),
                    room.roomHeight().toMillimeters(),
                    room.floorThickness().toMillimeters(),
                    room.ceilingThickness().toMillimeters(),
                    serializePoints(room.outline()),
                    serializeSlopedCeiling(room),
                    serializeCeilingVertexHeights(room)
            ));
        }
        for (Door door : level.doors()) {
            appendMetadataText(dxf, context, new PlanPoint(0, 0), String.format(
                    Locale.US,
                    "DOOR|%s|%s|%s|%.3f|%.3f|%.3f|%.3f",
                    DxfMetadataCodec.encode(level.name()),
                    door.id(),
                    door.wallId(),
                    door.offsetFromStart().toMillimeters(),
                    door.width().toMillimeters(),
                    door.height().toMillimeters(),
                    door.thresholdHeight().toMillimeters()
            ));
        }
        for (WindowElement window : level.windows()) {
            appendMetadataText(dxf, context, new PlanPoint(0, 0), String.format(
                    Locale.US,
                    "WINDOW|%s|%s|%s|%.3f|%.3f|%.3f|%.3f",
                    DxfMetadataCodec.encode(level.name()),
                    window.id(),
                    window.wallId(),
                    window.offsetFromStart().toMillimeters(),
                    window.width().toMillimeters(),
                    window.sillHeight().toMillimeters(),
                    window.windowHeight().toMillimeters()
            ));
        }
        for (Staircase staircase : level.staircases()) {
            appendMetadataText(dxf, context, staircase.firstCorner(), String.format(
                    Locale.US,
                    "STAIR|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%d|%d",
                    DxfMetadataCodec.encode(level.name()),
                    staircase.id(),
                    staircase.stairType().name(),
                    staircase.firstCorner().xMillimeters(),
                    staircase.firstCorner().yMillimeters(),
                    staircase.oppositeCorner().xMillimeters(),
                    staircase.oppositeCorner().yMillimeters(),
                    staircase.totalHeight().toMillimeters(),
                    staircase.stepCount(),
                    staircase.rotationQuarterTurns()
            ));
        }
        for (SurfaceLayerStack sls : level.surfaceLayerStacks()) {
            appendMetadataText(dxf, context, new PlanPoint(0, 0), String.format(
                    Locale.US,
                    "SLS|%s|%s|%s|%s",
                    DxfMetadataCodec.encode(level.name()),
                    sls.id(),
                    sls.surfaceType().name(),
                    DxfMetadataCodec.encode(sls.targetKey())
            ));
            for (SurfaceLayer layer : sls.layers()) {
                appendMetadataText(dxf, context, new PlanPoint(0, 0), String.format(
                        Locale.US,
                        "SLL|%s|%s|%s|%.3f|%s|%.3f|%.3f|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%s",
                        DxfMetadataCodec.encode(level.name()),
                        layer.id(),
                        DxfMetadataCodec.encode(layer.name()),
                        layer.thickness().toMillimeters(),
                        layer.visible(),
                        layer.tileWidth().toMillimeters(),
                        layer.tileHeight().toMillimeters(),
                        layer.layoutMode().name(),
                        layer.layoutOffset().toMillimeters(),
                        layer.minimumOffset().toMillimeters(),
                        layer.minimumEdgeWidth().toMillimeters(),
                        layer.minimumStartEndMargin().toMillimeters(),
                        layer.jointWidth().toMillimeters(),
                        DxfMetadataCodec.encode(layer.coveringSource())
                ));
            }
        }
    }

    private List<String> extractMetadata(List<String> lines) {
        List<String> metadata = new ArrayList<>();
        DxfEntityBuilder builder = null;
        for (int index = 0; index < lines.size() - 1; index += 2) {
            String code = lines.get(index).trim();
            String value = lines.get(index + 1).trim();
            if (code.equals("0")) {
                if (builder != null) {
                    DxfEntity entity = builder.build();
                    if (entity.type().equals("TEXT") && entity.layer().equals(DxfLayer.CADAS_META.name())) {
                        entity.firstValue(1).filter(text -> !text.isBlank()).ifPresent(metadata::add);
                    }
                }
                builder = new DxfEntityBuilder(value);
            } else if (builder != null) {
                Optional<Integer> groupCode = parseGroupCode(code);
                if (groupCode.isPresent()) {
                    builder.add(groupCode.get(), value);
                }
            }
        }
        if (builder != null) {
            DxfEntity entity = builder.build();
            if (entity.type().equals("TEXT") && entity.layer().equals(DxfLayer.CADAS_META.name())) {
                entity.firstValue(1).filter(text -> !text.isBlank()).ifPresent(metadata::add);
            }
        }
        return metadata;
    }

    private void copyLevelContents(Level source, Level target) {
        target.replaceWalls(source.walls());
        target.replaceRooms(source.rooms());
        target.replaceDoors(source.doors());
        target.replaceWindows(source.windows());
        target.replaceStaircases(source.staircases());
        target.replaceSurfaceLayerStacks(source.surfaceLayerStacks().stream()
                .map(SurfaceLayerStack::copy)
                .toList());
    }

    private void appendLineEntity(StringBuilder dxf, DxfDocumentSupport.DxfWriteContext context, String layer, PlanPoint start, PlanPoint end) {
        DxfDocumentSupport.appendModelSpaceEntityStart(context, "LINE", layer);
        appendPair(dxf, 100, "AcDbLine");
        appendPair(dxf, 10, start.xMillimeters());
        appendPair(dxf, 20, start.yMillimeters());
        appendPair(dxf, 30, 0.0);
        appendPair(dxf, 11, end.xMillimeters());
        appendPair(dxf, 21, end.yMillimeters());
        appendPair(dxf, 31, 0.0);
    }

    private void appendClosedPolyline(StringBuilder dxf, DxfDocumentSupport.DxfWriteContext context, String layer, List<PlanPoint> points) {
        DxfDocumentSupport.appendModelSpaceEntityStart(context, "LWPOLYLINE", layer);
        appendPair(dxf, 100, "AcDbPolyline");
        appendPair(dxf, 90, points.size());
        appendPair(dxf, 70, 1);
        for (PlanPoint point : points) {
            appendPair(dxf, 10, point.xMillimeters());
            appendPair(dxf, 20, point.yMillimeters());
        }
    }

    private void appendMetadataText(StringBuilder dxf, DxfDocumentSupport.DxfWriteContext context, PlanPoint anchor, String text) {
        DxfDocumentSupport.appendModelSpaceEntityStart(context, "TEXT", DxfLayer.CADAS_META.name());
        appendPair(dxf, 100, "AcDbText");
        appendPair(dxf, 10, anchor.xMillimeters());
        appendPair(dxf, 20, anchor.yMillimeters());
        appendPair(dxf, 30, 0.0);
        appendPair(dxf, 40, 120.0);
        appendPair(dxf, 1, text);
        appendPair(dxf, 7, "Standard");
    }

    private void appendPair(StringBuilder builder, int code, Object value) {
        builder.append(code).append('\n').append(value).append('\n');
    }

    private String serializePoints(List<PlanPoint> points) {
        return points.stream()
                .map(point -> String.format(Locale.US, "%.3f,%.3f", point.xMillimeters(), point.yMillimeters()))
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    private List<PlanPoint> deserializePoints(String text) {
        List<PlanPoint> points = new ArrayList<>();
        for (String pair : text.split(";")) {
            String[] values = pair.split(",");
            points.add(new PlanPoint(parseDouble(values[0]), parseDouble(values[1])));
        }
        return points;
    }

    private double parseDouble(String text) {
        return Double.parseDouble(text);
    }

    private Door deserializeDoor(String[] parts) {
        if (parts.length >= 8) {
            return new Door(
                    UUID.fromString(parts[2]),
                    UUID.fromString(parts[3]),
                    Length.ofMillimeters(parseDouble(parts[4])),
                    Length.ofMillimeters(parseDouble(parts[5])),
                    Length.ofMillimeters(parseDouble(parts[6])),
                    Length.ofMillimeters(parseDouble(parts[7]))
            );
        }
        return new Door(
                UUID.randomUUID(),
                UUID.fromString(parts[2]),
                Length.ofMillimeters(parseDouble(parts[3])),
                Length.ofMillimeters(parseDouble(parts[4])),
                Length.ofMillimeters(parseDouble(parts[5])),
                Length.ofMillimeters(parseDouble(parts[6]))
        );
    }

    private WindowElement deserializeWindow(String[] parts) {
        if (parts.length >= 8) {
            return new WindowElement(
                    UUID.fromString(parts[2]),
                    UUID.fromString(parts[3]),
                    Length.ofMillimeters(parseDouble(parts[4])),
                    Length.ofMillimeters(parseDouble(parts[5])),
                    Length.ofMillimeters(parseDouble(parts[6])),
                    Length.ofMillimeters(parseDouble(parts[7]))
            );
        }
        return new WindowElement(
                UUID.randomUUID(),
                UUID.fromString(parts[2]),
                Length.ofMillimeters(parseDouble(parts[3])),
                Length.ofMillimeters(parseDouble(parts[4])),
                Length.ofMillimeters(parseDouble(parts[5])),
                Length.ofMillimeters(parseDouble(parts[6]))
        );
    }

    private String serializeSlopedCeiling(Room room) {
        return room.slopedCeilingProfile()
                .map(profile -> String.format(
                        Locale.US,
                        "SLOPE,%s,%.3f",
                        profile.lowSide().name(),
                        profile.kneeWallHeight().toMillimeters()
                ))
                .orElse("NONE");
    }

    private String serializeCeilingVertexHeights(Room room) {
        return room.ceilingVertexHeightsProfile()
                .map(heights -> heights.stream()
                        .map(height -> String.format(Locale.US, "%.3f", height.toMillimeters()))
                        .reduce((left, right) -> left + ";" + right)
                        .orElse("NONE"))
                .orElse("NONE");
    }

    private SlopedCeilingProfile deserializeSlopedCeiling(String value) {
        if (value == null || value.isBlank() || value.equals("NONE")) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length != 3 || !parts[0].equals("SLOPE")) {
            return null;
        }
        return new SlopedCeilingProfile(
                SlopedCeilingSide.valueOf(parts[1]),
                Length.ofMillimeters(parseDouble(parts[2]))
        );
    }

    private List<Length> deserializeCeilingVertexHeights(String value) {
        if (value == null || value.isBlank() || value.equals("NONE")) {
            return null;
        }
        List<Length> heights = new ArrayList<>();
        for (String part : value.split(";")) {
            heights.add(Length.ofMillimeters(parseDouble(part)));
        }
        return List.copyOf(heights);
    }

    private static boolean isUuid(String text) {
        if (text == null || text.length() != 36) return false;
        try {
            UUID.fromString(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String stripDxfExtension(String name) {
        String result = name;
        while (result.toLowerCase().endsWith(".dxf")) {
            result = result.substring(0, result.length() - 4);
        }
        return result;
    }

    private String sanitizeLayerName(String value) {
        return value.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private Optional<Integer> parseGroupCode(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private record DxfEntity(String type, Map<Integer, List<String>> values) {

        String layer() {
            return firstValue(8).orElse("");
        }

        java.util.Optional<String> firstValue(int code) {
            return java.util.Optional.ofNullable(values.get(code))
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.getFirst());
        }
    }

    private static final class DxfEntityBuilder {

        private final String type;
        private final Map<Integer, List<String>> values = new LinkedHashMap<>();

        private DxfEntityBuilder(String type) {
            this.type = type;
        }

        private void add(int code, String value) {
            values.computeIfAbsent(code, ignored -> new ArrayList<>()).add(value);
        }

        private DxfEntity build() {
            return new DxfEntity(type, values);
        }
    }
}
