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
        DxfDocumentSupport.appendStandardHeader(dxf);
        appendMetadataText(dxf, new PlanPoint(0, 0), "PROJECT|" + sanitize(project.name()));

        for (Level level : project.levels()) {
            String layerPrefix = sanitizeLayerName(level.name());
            appendMetadataText(dxf, new PlanPoint(0, 0), "LEVEL|" + sanitize(level.name()));
            exportLevelGeometry(dxf, level, layerPrefix);
            exportLevelMetadata(dxf, level);
        }

        project.roof().ifPresent(roof -> appendMetadataText(dxf, new PlanPoint(0, 0), String.format(
                Locale.US,
                "ROOF|%s|%.3f|%.3f|%s",
                roof.roofType().name(),
                roof.pitchAngle().degrees(),
                roof.overhang().toMillimeters(),
                roof.gutterEnabled()
        )));

        appendPair(dxf, 0, "ENDSEC");
        appendPair(dxf, 0, "EOF");
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
        String importedProjectName = projectName;
        Roof importedRoof = null;
        for (String entry : metadata) {
            String[] parts = entry.split("\\|");
            switch (parts[0]) {
                case "PROJECT" -> importedProjectName = desanitize(parts[1]);
                case "LEVEL" -> levels.computeIfAbsent(desanitize(parts[1]), Level::new);
                case "WALL" -> {
                    Level level = levels.computeIfAbsent(desanitize(parts[1]), Level::new);
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
                    Level level = levels.computeIfAbsent(desanitize(parts[1]), Level::new);
                    level.addRoom(new Room(
                            UUID.randomUUID(),
                            desanitize(parts[2]),
                            deserializePoints(parts[6]),
                            Length.ofMillimeters(parseDouble(parts[3])),
                            Length.ofMillimeters(parseDouble(parts[4])),
                            Length.ofMillimeters(parseDouble(parts[5])),
                            parts.length >= 8 ? deserializeSlopedCeiling(parts[7]) : null,
                            parts.length >= 9 ? deserializeCeilingVertexHeights(parts[8]) : null
                    ));
                }
                case "DOOR" -> {
                    Level level = levels.computeIfAbsent(desanitize(parts[1]), Level::new);
                    level.addDoor(new Door(
                            UUID.randomUUID(),
                            UUID.fromString(parts[2]),
                            Length.ofMillimeters(parseDouble(parts[3])),
                            Length.ofMillimeters(parseDouble(parts[4])),
                            Length.ofMillimeters(parseDouble(parts[5])),
                            Length.ofMillimeters(parseDouble(parts[6]))
                    ));
                }
                case "WINDOW" -> {
                    Level level = levels.computeIfAbsent(desanitize(parts[1]), Level::new);
                    level.addWindow(new WindowElement(
                            UUID.randomUUID(),
                            UUID.fromString(parts[2]),
                            Length.ofMillimeters(parseDouble(parts[3])),
                            Length.ofMillimeters(parseDouble(parts[4])),
                            Length.ofMillimeters(parseDouble(parts[5])),
                            Length.ofMillimeters(parseDouble(parts[6]))
                    ));
                }
                case "STAIR" -> {
                    Level level = levels.computeIfAbsent(desanitize(parts[1]), Level::new);
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
                default -> {
                }
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

    private void exportLevelGeometry(StringBuilder dxf, Level level, String layerPrefix) {
        for (Wall wall : level.walls()) {
            appendLineEntity(dxf, layerPrefix + "_WALLS", wall.axis().start(), wall.axis().end());
        }
        for (Room room : level.rooms()) {
            appendClosedPolyline(dxf, layerPrefix + "_ROOMS", room.outline());
        }
        for (Door door : level.doors()) {
            Wall hostWall = level.findWall(door.wallId());
            appendLineEntity(dxf, layerPrefix + "_DOORS",
                    hostWall.axis().pointAt(door.offsetFromStart()),
                    hostWall.axis().pointAt(door.offsetFromStart().add(door.width())));
        }
        for (WindowElement window : level.windows()) {
            Wall hostWall = level.findWall(window.wallId());
            appendLineEntity(dxf, layerPrefix + "_WINDOWS",
                    hostWall.axis().pointAt(window.offsetFromStart()),
                    hostWall.axis().pointAt(window.offsetFromStart().add(window.width())));
        }
        for (Staircase staircase : level.staircases()) {
            appendClosedPolyline(dxf, layerPrefix + "_STAIRS", List.of(
                    staircase.pointAtLocalPosition(0, 0),
                    staircase.pointAtLocalPosition(staircase.widthMillimeters(), 0),
                    staircase.pointAtLocalPosition(staircase.widthMillimeters(), staircase.heightMillimeters()),
                    staircase.pointAtLocalPosition(0, staircase.heightMillimeters())
            ));
        }
    }

    private void exportLevelMetadata(StringBuilder dxf, Level level) {
        for (Wall wall : level.walls()) {
            appendMetadataText(dxf, wall.axis().start(), String.format(
                    Locale.US,
                    "WALL|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f",
                    sanitize(level.name()),
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
            appendMetadataText(dxf, room.centerPoint(), String.format(
                    Locale.US,
                    "ROOM|%s|%s|%.3f|%.3f|%.3f|%s|%s|%s",
                    sanitize(level.name()),
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
            appendMetadataText(dxf, new PlanPoint(0, 0), String.format(
                    Locale.US,
                    "DOOR|%s|%s|%.3f|%.3f|%.3f|%.3f",
                    sanitize(level.name()),
                    door.wallId(),
                    door.offsetFromStart().toMillimeters(),
                    door.width().toMillimeters(),
                    door.height().toMillimeters(),
                    door.thresholdHeight().toMillimeters()
            ));
        }
        for (WindowElement window : level.windows()) {
            appendMetadataText(dxf, new PlanPoint(0, 0), String.format(
                    Locale.US,
                    "WINDOW|%s|%s|%.3f|%.3f|%.3f|%.3f",
                    sanitize(level.name()),
                    window.wallId(),
                    window.offsetFromStart().toMillimeters(),
                    window.width().toMillimeters(),
                    window.sillHeight().toMillimeters(),
                    window.windowHeight().toMillimeters()
            ));
        }
        for (Staircase staircase : level.staircases()) {
            appendMetadataText(dxf, staircase.firstCorner(), String.format(
                    Locale.US,
                    "STAIR|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%d|%d",
                    sanitize(level.name()),
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
                builder.add(Integer.parseInt(code), value);
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
    }

    private void appendLineEntity(StringBuilder dxf, String layer, PlanPoint start, PlanPoint end) {
        appendPair(dxf, 0, "LINE");
        DxfDocumentSupport.appendModelSpace(dxf, layer);
        appendPair(dxf, 10, start.xMillimeters());
        appendPair(dxf, 20, start.yMillimeters());
        appendPair(dxf, 11, end.xMillimeters());
        appendPair(dxf, 21, end.yMillimeters());
    }

    private void appendClosedPolyline(StringBuilder dxf, String layer, List<PlanPoint> points) {
        appendPair(dxf, 0, "LWPOLYLINE");
        DxfDocumentSupport.appendModelSpace(dxf, layer);
        appendPair(dxf, 90, points.size());
        appendPair(dxf, 70, 1);
        for (PlanPoint point : points) {
            appendPair(dxf, 10, point.xMillimeters());
            appendPair(dxf, 20, point.yMillimeters());
        }
    }

    private void appendMetadataText(StringBuilder dxf, PlanPoint anchor, String text) {
        appendPair(dxf, 0, "TEXT");
        DxfDocumentSupport.appendModelSpace(dxf, DxfLayer.CADAS_META.name());
        appendPair(dxf, 10, anchor.xMillimeters());
        appendPair(dxf, 20, anchor.yMillimeters());
        appendPair(dxf, 40, 120.0);
        appendPair(dxf, 1, text);
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

    private String sanitize(String value) {
        return value.replace("|", "/").replace("\n", " ").trim();
    }

    private String desanitize(String value) {
        return value.replace('/', '|');
    }

    private String sanitizeLayerName(String value) {
        return value.replaceAll("[^A-Za-z0-9_\\-]", "_");
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
