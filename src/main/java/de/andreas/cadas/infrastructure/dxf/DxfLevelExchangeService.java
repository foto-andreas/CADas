package de.andreas.cadas.infrastructure.dxf;

import de.andreas.cadas.application.exchange.LevelExchangeService;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import de.andreas.cadas.domain.model.Door;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SlopedCeilingProfile;
import de.andreas.cadas.domain.model.SlopedCeilingSide;
import de.andreas.cadas.domain.model.StairType;
import de.andreas.cadas.domain.model.Staircase;
import de.andreas.cadas.domain.model.Wall;
import de.andreas.cadas.domain.model.WindowElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DxfLevelExchangeService implements LevelExchangeService {

    private static final Length DEFAULT_WALL_THICKNESS = Length.ofMillimeters(175.0);
    private static final Length DEFAULT_WALL_HEIGHT = Length.ofMillimeters(2750.0);
    private static final Length DEFAULT_ROOM_HEIGHT = Length.ofMillimeters(2600.0);
    private static final Length DEFAULT_FLOOR_THICKNESS = Length.ofMillimeters(180.0);
    private static final Length DEFAULT_CEILING_THICKNESS = Length.ofMillimeters(200.0);
    private static final Length DEFAULT_DOOR_WIDTH = Length.ofMillimeters(1010.0);
    private static final Length DEFAULT_DOOR_HEIGHT = Length.ofMillimeters(2010.0);
    private static final Length DEFAULT_WINDOW_WIDTH = Length.ofMillimeters(1200.0);
    private static final Length DEFAULT_WINDOW_HEIGHT = Length.ofMillimeters(1200.0);
    private static final Length DEFAULT_SILL_HEIGHT = Length.ofMillimeters(900.0);

    @Override
    public void exportLevel(Level level, Path targetFile) throws IOException {
        Path parent = targetFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder dxf = new StringBuilder();
        DxfDocumentSupport.appendStandardHeader(dxf);

        for (Wall wall : level.walls()) {
            appendLineEntity(dxf, DxfLayer.WALLS, wall.axis().start(), wall.axis().end());
            appendMetadataText(dxf, wall.axis().start(), String.format(
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
            appendClosedPolyline(dxf, DxfLayer.ROOMS, room.outline());
            appendMetadataText(dxf, room.centerPoint(), String.format(
                    Locale.US,
                    "ROOM|%s|%.3f|%.3f|%.3f|%s|%s|%s",
                    sanitize(room.name()),
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
            appendLineEntity(dxf, DxfLayer.DOORS, start, end);
            appendMetadataText(dxf, start, String.format(
                    Locale.US,
                    "DOOR|%s|%.3f|%.3f|%.3f|%.3f",
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
            appendLineEntity(dxf, DxfLayer.WINDOWS, start, end);
            appendMetadataText(dxf, start, String.format(
                    Locale.US,
                    "WINDOW|%s|%.3f|%.3f|%.3f|%.3f",
                    window.wallId(),
                    window.offsetFromStart().toMillimeters(),
                    window.width().toMillimeters(),
                    window.sillHeight().toMillimeters(),
                    window.windowHeight().toMillimeters()
            ));
        }

        for (Staircase staircase : level.staircases()) {
            appendClosedPolyline(dxf, DxfLayer.STAIRS, List.of(
                    staircase.pointAtLocalPosition(0, 0),
                    staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0),
                    staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()),
                    staircase.pointAtLocalPosition(0, staircase.heightMillimeters())
            ));
            appendMetadataText(dxf, staircase.firstCorner(), String.format(
                    Locale.US,
                    "STAIR|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%d|%d",
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

        appendPair(dxf, 0, "ENDSEC");
        appendPair(dxf, 0, "EOF");
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

        for (String metadata : metadataEntries) {
            String[] parts = metadata.split("\\|");
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
                case "ROOM" -> importedRooms.add(new Room(
                        UUID.randomUUID(),
                        desanitize(parts[1]),
                        deserializePoints(parts[5]),
                        Length.ofMillimeters(parseDouble(parts[2])),
                        Length.ofMillimeters(parseDouble(parts[3])),
                        Length.ofMillimeters(parseDouble(parts[4])),
                        parts.length >= 7 ? deserializeSlopedCeiling(parts[6]) : null,
                        parts.length >= 8 ? deserializeCeilingVertexHeights(parts[7]) : null
                ));
                case "DOOR" -> pendingDoors.add(Door.create(
                        UUID.fromString(parts[1]),
                        Length.ofMillimeters(parseDouble(parts[2])),
                        Length.ofMillimeters(parseDouble(parts[3])),
                        Length.ofMillimeters(parseDouble(parts[4])),
                        Length.ofMillimeters(parseDouble(parts[5]))
                ));
                case "WINDOW" -> pendingWindows.add(WindowElement.create(
                        UUID.fromString(parts[1]),
                        Length.ofMillimeters(parseDouble(parts[2])),
                        Length.ofMillimeters(parseDouble(parts[3])),
                        Length.ofMillimeters(parseDouble(parts[4])),
                        Length.ofMillimeters(parseDouble(parts[5]))
                ));
                case "STAIR" -> importedStaircases.add(new Staircase(
                        UUID.fromString(parts[1]),
                        StairType.valueOf(parts[2]),
                        new PlanPoint(parseDouble(parts[3]), parseDouble(parts[4])),
                        new PlanPoint(parseDouble(parts[5]), parseDouble(parts[6])),
                        Length.ofMillimeters(parseDouble(parts[7])),
                        Integer.parseInt(parts[8]),
                        Integer.parseInt(parts[9])
                ));
                default -> {
                }
            }
        }

        level.replaceRooms(importedRooms);
        level.replaceStaircases(importedStaircases);
        pendingDoors.stream()
                .filter(door -> wallsById.containsKey(door.wallId()))
                .forEach(level::addDoor);
        pendingWindows.stream()
                .filter(window -> wallsById.containsKey(window.wallId()))
                .forEach(level::addWindow);
    }

    private void importFallbackGeometry(Level level, List<DxfEntity> entities) {
        for (DxfEntity entity : entities) {
            if (entity.type().equals("LINE") && entity.layer().equals(DxfLayer.WALLS.name())) {
                level.addWall(Wall.create(
                        new PlanSegment(
                                new PlanPoint(entity.doubleValue(10), entity.doubleValue(20)),
                                new PlanPoint(entity.doubleValue(11), entity.doubleValue(21))
                        ),
                        DEFAULT_WALL_THICKNESS,
                        DEFAULT_WALL_HEIGHT
                ));
            } else if (entity.type().equals("LWPOLYLINE") && entity.layer().equals(DxfLayer.ROOMS.name())) {
                List<Double> xValues = entity.values(10).stream().map(DxfLevelExchangeService::parseDouble).toList();
                List<Double> yValues = entity.values(20).stream().map(DxfLevelExchangeService::parseDouble).toList();
                List<PlanPoint> points = new ArrayList<>();
                for (int index = 0; index < Math.min(xValues.size(), yValues.size()); index++) {
                    points.add(new PlanPoint(xValues.get(index), yValues.get(index)));
                }
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
                builder.add(Integer.parseInt(code), value);
            }
        }
        if (builder != null) {
            entities.add(builder.build());
        }
        return entities;
    }

    private void appendLineEntity(StringBuilder dxf, DxfLayer layer, PlanPoint start, PlanPoint end) {
        appendPair(dxf, 0, "LINE");
        DxfDocumentSupport.appendModelSpace(dxf, layer.name());
        appendPair(dxf, 10, start.xMillimeters());
        appendPair(dxf, 20, start.yMillimeters());
        appendPair(dxf, 11, end.xMillimeters());
        appendPair(dxf, 21, end.yMillimeters());
    }

    private void appendClosedPolyline(StringBuilder dxf, DxfLayer layer, List<PlanPoint> points) {
        appendPair(dxf, 0, "LWPOLYLINE");
        DxfDocumentSupport.appendModelSpace(dxf, layer.name());
        appendPair(dxf, 90, points.size());
        appendPair(dxf, 70, 1);
        for (PlanPoint point : points) {
            appendPair(dxf, 10, point.xMillimeters());
            appendPair(dxf, 20, point.yMillimeters());
        }
    }

    private void appendMetadataText(StringBuilder dxf, PlanPoint anchor, String value) {
        appendPair(dxf, 0, "TEXT");
        DxfDocumentSupport.appendModelSpace(dxf, DxfLayer.CADAS_META.name());
        appendPair(dxf, 10, anchor.xMillimeters());
        appendPair(dxf, 20, anchor.yMillimeters());
        appendPair(dxf, 40, 100.0);
        appendPair(dxf, 1, value);
    }

    private static void appendPair(StringBuilder dxf, int code, Object value) {
        dxf.append(code).append('\n').append(value).append('\n');
    }

    private static String sanitize(String value) {
        return value.replace("|", "/");
    }

    private static String desanitize(String value) {
        return value.replace("/", "|");
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

        double doubleValue(int code) {
            return firstValue(code).map(Double::parseDouble).orElse(0.0);
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
