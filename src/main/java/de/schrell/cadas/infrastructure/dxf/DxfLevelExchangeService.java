package de.schrell.cadas.infrastructure.dxf;

import de.schrell.cadas.application.exchange.LevelExchangeService;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorExtensionPlacement;
import de.schrell.cadas.domain.model.FloorExtensionType;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WindowElement;

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

public final class DxfLevelExchangeService implements LevelExchangeService {

    private static final Length DEFAULT_WALL_THICKNESS = Length.ofMillimeters(175.0);
    private static final Length DEFAULT_WALL_HEIGHT = Length.ofMillimeters(2750.0);
    private static final Length DEFAULT_ROOM_HEIGHT = Length.ofMillimeters(2600.0);
    private static final Length DEFAULT_FLOOR_THICKNESS = Length.ofMillimeters(180.0);
    private static final Length DEFAULT_CEILING_THICKNESS = Length.ofMillimeters(1.0);

    @Override
    public void exportLevel(Level level, Path targetFile) throws IOException {
        Path parent = targetFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder dxf = new StringBuilder();
        DxfDocumentSupport.DxfWriteContext context = DxfDocumentSupport.startDocument(
                dxf,
                collectLevelLayers(),
                Set.of(DxfDocumentSupport.BLOCK_DOOR, DxfDocumentSupport.BLOCK_WINDOW, DxfDocumentSupport.BLOCK_STAIR)
        );
        appendMetadataText(dxf, context, new PlanPoint(0, 0), DxfMetadataCodec.CURRENT_MARKER);

        for (Wall wall : level.walls()) {
            appendLineEntity(dxf, context, DxfLayer.WALLS, wall.axis().start(), wall.axis().end());
            appendMetadataText(dxf, context, wall.axis().start(), String.format(
                    Locale.US,
                    "WALL|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f",
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
            appendClosedPolyline(dxf, context, DxfLayer.ROOMS, room.outline());
            appendMetadataText(dxf, context, room.centerPoint(), String.format(
                    Locale.US,
                    "ROOM|%s|%s|%.3f|%.3f|%.3f|%s|%s|%s",
                    DxfMetadataCodec.encode(room.name()),
                    room.id(),
                    room.roomHeight().toMillimeters(),
                    room.floorThickness().toMillimeters(),
                    room.ceilingThickness().toMillimeters(),
                    serializePoints(room.outline()),
                    serializeSlopedCeiling(room),
                    serializeCeilingVertexHeights(room)
            ));
        }

        for (Door door : level.doors()) {
            Wall hostWall = level.findWall(door.wallId());
            PlanPoint start = hostWall.axis().pointAt(door.offsetFromStart());
            PlanPoint end = hostWall.axis().pointAt(door.offsetFromStart().add(door.width()));
            appendLineEntity(dxf, context, DxfLayer.DOORS, start, end);
            DxfDocumentSupport.appendInsert(
                    dxf,
                    context,
                    DxfLayer.DOORS.name(),
                    DxfDocumentSupport.BLOCK_DOOR,
                    start,
                    Math.max(0.001, door.width().toMillimeters() / 1000.0),
                    1.0,
                    hostWall.axis().angle().degrees()
            );
            appendMetadataText(dxf, context, start, String.format(
                    Locale.US,
                    "DOOR|%s|%s|%.3f|%.3f|%.3f|%.3f",
                    door.id(),
                    door.wallId(),
                    door.offsetFromStart().toMillimeters(),
                    door.width().toMillimeters(),
                    door.height().toMillimeters(),
                    door.thresholdHeight().toMillimeters()
            ));
        }

        for (WindowElement window : level.windows()) {
            Wall hostWall = level.findWall(window.wallId());
            PlanPoint start = hostWall.axis().pointAt(window.offsetFromStart());
            PlanPoint end = hostWall.axis().pointAt(window.offsetFromStart().add(window.width()));
            appendLineEntity(dxf, context, DxfLayer.WINDOWS, start, end);
            DxfDocumentSupport.appendInsert(
                    dxf,
                    context,
                    DxfLayer.WINDOWS.name(),
                    DxfDocumentSupport.BLOCK_WINDOW,
                    start,
                    Math.max(0.001, window.width().toMillimeters() / 1000.0),
                    1.0,
                    hostWall.axis().angle().degrees()
            );
            appendMetadataText(dxf, context, start, String.format(
                    Locale.US,
                    "WINDOW|%s|%s|%.3f|%.3f|%.3f|%.3f",
                    window.id(),
                    window.wallId(),
                    window.offsetFromStart().toMillimeters(),
                    window.width().toMillimeters(),
                    window.sillHeight().toMillimeters(),
                    window.windowHeight().toMillimeters()
            ));
        }

        for (Staircase staircase : level.staircases()) {
            appendClosedPolyline(dxf, context, DxfLayer.STAIRS, List.of(
                    staircase.pointAtLocalPosition(0, 0),
                    staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0),
                    staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()),
                    staircase.pointAtLocalPosition(0, staircase.heightMillimeters())
            ));
            DxfDocumentSupport.appendInsert(
                    dxf,
                    context,
                    DxfLayer.STAIRS.name(),
                    DxfDocumentSupport.BLOCK_STAIR,
                    new PlanPoint(staircase.minX(), staircase.minY()),
                    Math.max(0.001, staircase.widthMillimeters() / 1000.0),
                    Math.max(0.001, staircase.heightMillimeters() / 1000.0),
                    staircase.rotationQuarterTurns() * 90.0
            );
            appendMetadataText(dxf, context, staircase.firstCorner(), String.format(
                    Locale.US,
                    "STAIR|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%d|%d|%.3f|%.3f",
                    staircase.id(),
                    staircase.stairType().name(),
                    staircase.firstCorner().xMillimeters(),
                    staircase.firstCorner().yMillimeters(),
                    staircase.oppositeCorner().xMillimeters(),
                    staircase.oppositeCorner().yMillimeters(),
                    staircase.totalHeight().toMillimeters(),
                    staircase.stepCount(),
                    staircase.rotationQuarterTurns(),
                    staircase.startLandingWidth().toMillimeters(),
                    staircase.endLandingWidth().toMillimeters()
            ));
        }

        for (RoomObject roomObject : level.roomObjects()) {
            appendMetadataText(dxf, context, roomObject.center(), String.format(
                    Locale.US,
                    "OBJ|%s|%s|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%s|%s|%s|%s|%.3f",
                    roomObject.id(),
                    DxfMetadataCodec.encode(roomObject.presetId()),
                    DxfMetadataCodec.encode(roomObject.name()),
                    roomObject.type().name(),
                    roomObject.shape().name(),
                    roomObject.center().xMillimeters(),
                    roomObject.center().yMillimeters(),
                    roomObject.width().toMillimeters(),
                    roomObject.depth().toMillimeters(),
                    roomObject.height().toMillimeters(),
                    roomObject.rotationDegrees(),
                    roomObject.cutsFloorCovering(),
                    roomObject.visible(),
                    DxfMetadataCodec.encode(roomObject.source()),
                    roomObject.mountingMode().name(),
                    roomObject.baseElevation().toMillimeters()
            ));
        }

        for (FloorExtension extension : level.floorExtensions()) {
            appendClosedPolyline(dxf, context, DxfLayer.ROOMS, extension.outline());
            appendMetadataText(dxf, context, extension.firstCorner(), String.format(
                    Locale.US,
                    "FEXT|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f",
                    extension.id(), extension.type().name(), extension.placement().name(),
                    extension.firstCorner().xMillimeters(), extension.firstCorner().yMillimeters(),
                    extension.oppositeCorner().xMillimeters(), extension.oppositeCorner().yMillimeters(),
                    extension.slabThickness().toMillimeters()
            ));
        }

        for (SurfaceLayerStack sls : level.surfaceLayerStacks()) {
            appendMetadataText(dxf, context, new PlanPoint(0, 0), String.format(
                    Locale.US,
                    "SLS|%s|%s|%s",
                    sls.id(),
                    sls.surfaceType().name(),
                    DxfMetadataCodec.encode(sls.targetKey())
            ));
            for (SurfaceLayer layer : sls.layers()) {
                appendMetadataText(dxf, context, new PlanPoint(0, 0), String.format(
                        Locale.US,
                        "SLL|%s|%s|%.3f|%s|%.3f|%.3f|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%s|%s",
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
                        DxfMetadataCodec.encode(layer.coveringSource()),
                        layer.cutRestriction().name()
                ));
            }
        }

        DxfDocumentSupport.finishDocument(context);
        Files.writeString(targetFile, dxf.toString());
    }

    @Override
    public Level importLevel(Path sourceFile, String levelName) throws IOException {
        List<String> lines = Files.readAllLines(sourceFile);
        List<DxfEntity> entities = parseEntities(lines);
        Level level = new Level(levelName);
        List<String> metadata = entities.stream()
                .filter(entity -> entity.type().equals("TEXT"))
                .filter(entity -> entity.layer().equals(DxfLayer.CADAS_META.name()))
                .map(entity -> entity.firstValue(1).orElse(""))
                .filter(value -> !value.isBlank())
                .toList();

        if (!metadata.isEmpty()) {
            importFromMetadata(level, metadata);
            return level;
        }

        importFallbackGeometry(level, entities);
        return level;
    }

    private void importFromMetadata(Level level, List<String> metadataEntries) {
        Map<UUID, Wall> wallsById = new LinkedHashMap<>();
        List<Room> importedRooms = new ArrayList<>();
        List<Door> pendingDoors = new ArrayList<>();
        List<WindowElement> pendingWindows = new ArrayList<>();
        List<Staircase> importedStaircases = new ArrayList<>();
        List<SurfaceLayerStack> importedStacks = new ArrayList<>();
        boolean encodedFields = DxfMetadataCodec.usesCurrentEncoding(metadataEntries);
        boolean objectRotationDegrees = DxfMetadataCodec.usesObjectRotationDegrees(metadataEntries);

        for (String metadata : metadataEntries) {
            String[] parts = DxfMetadataCodec.split(metadata);
            if (DxfMetadataCodec.isMarker(parts)) {
                continue;
            }
            try {
                switch (parts[0]) {
                    case "WALL" -> {
                        double startHeight = parts.length >= 10 ? parseDouble(parts[4]) : parseDouble(parts[3]);
                        double endHeight = parts.length >= 10 ? parseDouble(parts[5]) : parseDouble(parts[3]);
                        int pointOffset = parts.length >= 10 ? 2 : 0;
                        Wall wall = new Wall(
                                UUID.fromString(parts[1]),
                                new PlanSegment(
                                        new PlanPoint(parseDouble(parts[4 + pointOffset]), parseDouble(parts[5 + pointOffset])),
                                        new PlanPoint(parseDouble(parts[6 + pointOffset]), parseDouble(parts[7 + pointOffset]))
                                ),
                                Length.ofMillimeters(parseDouble(parts[2])),
                                Length.ofMillimeters(parseDouble(parts[3])),
                                Length.ofMillimeters(startHeight),
                                Length.ofMillimeters(endHeight)
                        );
                        level.addWall(wall);
                        wallsById.put(wall.id(), wall);
                    }
                    case "ROOM" -> {
                        if (isUuid(parts[2])) {
                            importedRooms.add(new Room(
                                    UUID.fromString(parts[2]),
                                    DxfMetadataCodec.decode(parts[1], encodedFields),
                                    deserializePoints(parts[6]),
                                    Length.ofMillimeters(parseDouble(parts[3])),
                                    Length.ofMillimeters(parseDouble(parts[4])),
                                    Length.ofMillimeters(parseDouble(parts[5])),
                                    parts.length >= 8 ? deserializeSlopedCeiling(parts[7]) : null,
                                    parts.length >= 9 ? deserializeCeilingVertexHeights(parts[8]) : null
                            ));
                        } else {
                            importedRooms.add(new Room(
                                    UUID.randomUUID(),
                                    DxfMetadataCodec.decode(parts[1], encodedFields),
                                    deserializePoints(parts[5]),
                                    Length.ofMillimeters(parseDouble(parts[2])),
                                    Length.ofMillimeters(parseDouble(parts[3])),
                                    Length.ofMillimeters(parseDouble(parts[4])),
                                    parts.length >= 7 ? deserializeSlopedCeiling(parts[6]) : null,
                                    parts.length >= 8 ? deserializeCeilingVertexHeights(parts[7]) : null
                            ));
                        }
                    }
                    case "DOOR" -> pendingDoors.add(deserializeDoor(parts));
                    case "WINDOW" -> pendingWindows.add(deserializeWindow(parts));
                    case "STAIR" -> importedStaircases.add(new Staircase(
                            UUID.fromString(parts[1]),
                            StairType.valueOf(parts[2]),
                            new PlanPoint(parseDouble(parts[3]), parseDouble(parts[4])),
                            new PlanPoint(parseDouble(parts[5]), parseDouble(parts[6])),
                            Length.ofMillimeters(parseDouble(parts[7])),
                            Integer.parseInt(parts[8]),
                            Integer.parseInt(parts[9]),
                            Length.ofMillimeters(parts.length >= 11 ? parseDouble(parts[10]) : 0),
                            Length.ofMillimeters(parts.length >= 12 ? parseDouble(parts[11]) : 0)
                    ));
                    case "FEXT" -> level.addFloorExtension(new FloorExtension(
                            UUID.fromString(parts[1]), FloorExtensionType.valueOf(parts[2]),
                            FloorExtensionPlacement.valueOf(parts[3]),
                            new PlanPoint(parseDouble(parts[4]), parseDouble(parts[5])),
                            new PlanPoint(parseDouble(parts[6]), parseDouble(parts[7])),
                            Length.ofMillimeters(parseDouble(parts[8]))
                    ));
                    case "SLS" -> {
                        SurfaceLayerStack stack = new SurfaceLayerStack(
                                UUID.fromString(parts[1]),
                                SurfaceType.valueOf(parts[2]),
                                DxfMetadataCodec.decode(parts[3], encodedFields)
                        );
                        importedStacks.add(stack);
                    }
                    case "SLL" -> {
                        if (!importedStacks.isEmpty()) {
                            SurfaceLayerStack stack = importedStacks.getLast();
                            SurfaceLayer layer = new SurfaceLayer(
                                    UUID.fromString(parts[1]),
                                    DxfMetadataCodec.decode(parts[2], encodedFields),
                                    Length.ofMillimeters(parseDouble(parts[3])),
                                    Boolean.parseBoolean(parts[4]),
                                    Length.ofMillimeters(parseDouble(parts[5])),
                                    Length.ofMillimeters(parseDouble(parts[6])),
                                    SurfaceLayoutMode.valueOf(parts[7]),
                                    Length.ofMillimeters(parseDouble(parts[8])),
                                    Length.ofMillimeters(parseDouble(parts[9])),
                                    Length.ofMillimeters(parseDouble(parts[10])),
                                    Length.ofMillimeters(parts.length >= 14 ? parseDouble(parts[11]) : parseDouble(parts[10])),
                                    Length.ofMillimeters(parts.length >= 14 ? parseDouble(parts[12]) : parseDouble(parts[11])),
                                    SurfaceCutRestriction.fromStoredValue(parts.length >= 15 ? parts[14] : null),
                                    DxfMetadataCodec.decode(parts.length >= 14 ? parts[13] : parts[12], encodedFields)
                            );
                            stack.addLayer(layer);
                        }
                    }
                    case "OBJ" -> level.addRoomObject(new RoomObject(
                            UUID.fromString(parts[1]),
                            DxfMetadataCodec.decode(parts[2], encodedFields),
                            DxfMetadataCodec.decode(parts[3], encodedFields),
                            RoomObjectType.valueOf(parts[4]),
                            RoomObjectShape.valueOf(parts[5]),
                            new PlanPoint(parseDouble(parts[6]), parseDouble(parts[7])),
                            Length.ofMillimeters(parseDouble(parts[8])),
                            Length.ofMillimeters(parseDouble(parts[9])),
                            Length.ofMillimeters(parseDouble(parts[10])),
                            parseObjectRotation(parts[11], objectRotationDegrees),
                            RoomObjectMountingMode.fromStoredValue(parts.length >= 16 ? parts[15] : null, Boolean.parseBoolean(parts[12])),
                            Boolean.parseBoolean(parts[13]),
                            DxfMetadataCodec.decode(parts[14], encodedFields),
                            Length.ofMillimeters(parts.length >= 17 ? parseDouble(parts[16]) : 0.0)
                    ));
                    default -> {
                    }
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                // Fremde oder beschädigte Metadaten sollen den Geometrieimport nicht blockieren.
            }
        }

        level.replaceRooms(importedRooms);
        level.replaceStaircases(importedStaircases);
        level.replaceSurfaceLayerStacks(importedStacks);
        pendingDoors.stream()
                .filter(door -> wallsById.containsKey(door.wallId()))
                .forEach(level::addDoor);
        pendingWindows.stream()
                .filter(window -> wallsById.containsKey(window.wallId()))
                .forEach(level::addWindow);
    }

    private double parseObjectRotation(String storedRotation, boolean storedInDegrees) {
        double value = parseDouble(storedRotation);
        return storedInDegrees ? value : value * 90.0;
    }

    private void importFallbackGeometry(Level level, List<DxfEntity> entities) {
        for (DxfEntity entity : entities) {
            if (entity.type().equals("LINE") && entity.layer().equals(DxfLayer.WALLS.name())) {
                fallbackLineSegment(entity).ifPresent(segment -> level.addWall(Wall.create(
                        segment,
                        DEFAULT_WALL_THICKNESS,
                        DEFAULT_WALL_HEIGHT
                )));
            } else if (entity.type().equals("LWPOLYLINE") && entity.layer().equals(DxfLayer.ROOMS.name())) {
                List<PlanPoint> points = fallbackPolylinePoints(entity);
                if (points.size() >= 3) {
                    level.addRoom(new Room(
                            UUID.randomUUID(),
                            "Importierter Raum",
                            points,
                            DEFAULT_ROOM_HEIGHT,
                            DEFAULT_FLOOR_THICKNESS,
                            DEFAULT_CEILING_THICKNESS,
                            null
                    ));
                }
            }
        }
    }

    private Optional<PlanSegment> fallbackLineSegment(DxfEntity entity) {
        Optional<Double> startX = entity.doubleValue(10);
        Optional<Double> startY = entity.doubleValue(20);
        Optional<Double> endX = entity.doubleValue(11);
        Optional<Double> endY = entity.doubleValue(21);
        if (startX.isEmpty() || startY.isEmpty() || endX.isEmpty() || endY.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PlanSegment(
                new PlanPoint(startX.get(), startY.get()),
                new PlanPoint(endX.get(), endY.get())
        ));
    }

    private List<PlanPoint> fallbackPolylinePoints(DxfEntity entity) {
        List<String> xValues = entity.values(10);
        List<String> yValues = entity.values(20);
        List<PlanPoint> points = new ArrayList<>();
        for (int index = 0; index < Math.min(xValues.size(), yValues.size()); index++) {
            Optional<Double> x = parseOptionalDouble(xValues.get(index));
            Optional<Double> y = parseOptionalDouble(yValues.get(index));
            if (x.isPresent() && y.isPresent()) {
                points.add(new PlanPoint(x.get(), y.get()));
            }
        }
        return points;
    }

    private List<DxfEntity> parseEntities(List<String> lines) {
        List<DxfEntity> entities = new ArrayList<>();
        DxfEntityBuilder builder = null;
        for (int index = 0; index < lines.size() - 1; index += 2) {
            String code = lines.get(index).trim();
            String value = lines.get(index + 1).trim();
            if (code.equals("0")) {
                if (builder != null) {
                    entities.add(builder.build());
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
            entities.add(builder.build());
        }
        return entities;
    }

    private void appendLineEntity(StringBuilder dxf, DxfDocumentSupport.DxfWriteContext context, DxfLayer layer, PlanPoint start, PlanPoint end) {
        DxfDocumentSupport.appendModelSpaceEntityStart(context, "LINE", layer.name());
        appendPair(dxf, 100, "AcDbLine");
        appendPair(dxf, 10, start.xMillimeters());
        appendPair(dxf, 20, start.yMillimeters());
        appendPair(dxf, 30, 0.0);
        appendPair(dxf, 11, end.xMillimeters());
        appendPair(dxf, 21, end.yMillimeters());
        appendPair(dxf, 31, 0.0);
    }

    private void appendClosedPolyline(StringBuilder dxf, DxfDocumentSupport.DxfWriteContext context, DxfLayer layer, List<PlanPoint> points) {
        DxfDocumentSupport.appendModelSpaceEntityStart(context, "LWPOLYLINE", layer.name());
        appendPair(dxf, 100, "AcDbPolyline");
        appendPair(dxf, 90, points.size());
        appendPair(dxf, 70, 1);
        for (PlanPoint point : points) {
            appendPair(dxf, 10, point.xMillimeters());
            appendPair(dxf, 20, point.yMillimeters());
        }
    }

    private void appendMetadataText(StringBuilder dxf, DxfDocumentSupport.DxfWriteContext context, PlanPoint anchor, String value) {
        DxfDocumentSupport.appendModelSpaceEntityStart(context, "TEXT", DxfLayer.CADAS_META.name());
        appendPair(dxf, 100, "AcDbText");
        appendPair(dxf, 10, anchor.xMillimeters());
        appendPair(dxf, 20, anchor.yMillimeters());
        appendPair(dxf, 30, 0.0);
        appendPair(dxf, 40, 100.0);
        appendPair(dxf, 1, value);
        appendPair(dxf, 7, "Standard");
    }

    private static void appendPair(StringBuilder dxf, int code, Object value) {
        dxf.append(code).append('\n').append(value).append('\n');
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

    private static String serializePoints(List<PlanPoint> points) {
        List<String> values = new ArrayList<>();
        for (PlanPoint point : points) {
            values.add(String.format(Locale.US, "%.3f,%.3f", point.xMillimeters(), point.yMillimeters()));
        }
        return String.join(";", values);
    }

    private static List<PlanPoint> deserializePoints(String value) {
        List<PlanPoint> points = new ArrayList<>();
        for (String entry : value.split(";")) {
            String[] coordinates = entry.split(",");
            points.add(new PlanPoint(parseDouble(coordinates[0]), parseDouble(coordinates[1])));
        }
        return points;
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value);
    }

    private static Door deserializeDoor(String[] parts) {
        if (parts.length >= 7) {
            return new Door(
                    UUID.fromString(parts[1]),
                    UUID.fromString(parts[2]),
                    Length.ofMillimeters(parseDouble(parts[3])),
                    Length.ofMillimeters(parseDouble(parts[4])),
                    Length.ofMillimeters(parseDouble(parts[5])),
                    Length.ofMillimeters(parseDouble(parts[6]))
            );
        }
        return Door.create(
                UUID.fromString(parts[1]),
                Length.ofMillimeters(parseDouble(parts[2])),
                Length.ofMillimeters(parseDouble(parts[3])),
                Length.ofMillimeters(parseDouble(parts[4])),
                Length.ofMillimeters(parseDouble(parts[5]))
        );
    }

    private static WindowElement deserializeWindow(String[] parts) {
        if (parts.length >= 7) {
            return new WindowElement(
                    UUID.fromString(parts[1]),
                    UUID.fromString(parts[2]),
                    Length.ofMillimeters(parseDouble(parts[3])),
                    Length.ofMillimeters(parseDouble(parts[4])),
                    Length.ofMillimeters(parseDouble(parts[5])),
                    Length.ofMillimeters(parseDouble(parts[6]))
            );
        }
        return WindowElement.create(
                UUID.fromString(parts[1]),
                Length.ofMillimeters(parseDouble(parts[2])),
                Length.ofMillimeters(parseDouble(parts[3])),
                Length.ofMillimeters(parseDouble(parts[4])),
                Length.ofMillimeters(parseDouble(parts[5]))
        );
    }

    private static Optional<Double> parseOptionalDouble(String value) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseGroupCode(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String serializeSlopedCeiling(Room room) {
        return room.slopedCeilingProfile()
                .map(profile -> String.format(
                        Locale.US,
                        "SLOPE,%s,%.3f",
                        profile.lowSide().name(),
                        profile.kneeWallHeight().toMillimeters()
                ))
                .orElse("NONE");
    }

    private static String serializeCeilingVertexHeights(Room room) {
        return room.ceilingVertexHeightsProfile()
                .map(heights -> heights.stream()
                        .map(height -> String.format(Locale.US, "%.3f", height.toMillimeters()))
                        .reduce((left, right) -> left + ";" + right)
                        .orElse("NONE"))
                .orElse("NONE");
    }

    private static SlopedCeilingProfile deserializeSlopedCeiling(String value) {
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

    private static List<Length> deserializeCeilingVertexHeights(String value) {
        if (value == null || value.isBlank() || value.equals("NONE")) {
            return null;
        }
        List<Length> heights = new ArrayList<>();
        for (String part : value.split(";")) {
            heights.add(Length.ofMillimeters(parseDouble(part)));
        }
        return List.copyOf(heights);
    }

    private Set<String> collectLevelLayers() {
        Set<String> layers = new LinkedHashSet<>();
        for (DxfLayer layer : DxfLayer.values()) {
            layers.add(layer.name());
        }
        return layers;
    }

    private record DxfEntity(String type, Map<Integer, List<String>> values) {

        String layer() {
            return firstValue(8).orElse("");
        }

        Optional<String> firstValue(int code) {
            return values.getOrDefault(code, List.of()).stream().findFirst();
        }

        List<String> values(int code) {
            return values.getOrDefault(code, List.of());
        }

        Optional<Double> doubleValue(int code) {
            return firstValue(code).flatMap(DxfLevelExchangeService::parseOptionalDouble);
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
