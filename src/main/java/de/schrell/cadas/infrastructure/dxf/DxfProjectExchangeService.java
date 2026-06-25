package de.schrell.cadas.infrastructure.dxf;

import de.schrell.cadas.application.exchange.ProjectExchangeService;
import de.schrell.cadas.domain.geometry.Angle;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;
import de.schrell.cadas.domain.model.Door;
import de.schrell.cadas.domain.model.FloorExtension;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.FloorOpeningShape;
import de.schrell.cadas.domain.model.FloorExtensionPlacement;
import de.schrell.cadas.domain.model.FloorExtensionType;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingSurfacePosition;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Level;
import de.schrell.cadas.domain.model.ProjectModel;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.RoofWindow;
import de.schrell.cadas.domain.model.RoomObject;
import de.schrell.cadas.domain.model.RoomObjectMountingMode;
import de.schrell.cadas.domain.model.RoomObjectShape;
import de.schrell.cadas.domain.model.RoomObjectType;
import de.schrell.cadas.domain.model.Roof;
import de.schrell.cadas.domain.model.RoofType;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.StairType;
import de.schrell.cadas.domain.model.Staircase;
import de.schrell.cadas.domain.model.SurfaceCutRestriction;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceLayoutMode;
import de.schrell.cadas.domain.model.SurfaceType;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
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
import java.util.stream.Collectors;

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
        project.terrain().vertices().forEach(vertex -> appendMetadataText(dxf, context, vertex.position(), String.format(
                Locale.US,
                "TERRAIN|%.3f|%.3f|%.3f",
                vertex.position().xMillimeters(),
                vertex.position().yMillimeters(),
                vertex.elevationAboveLowestFloor().toMillimeters()
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
        Map<String, List<Door>> pendingDoorsByLevel = new LinkedHashMap<>();
        Map<String, List<WindowElement>> pendingWindowsByLevel = new LinkedHashMap<>();
        Map<String, Map<UUID, HydronicHeating>> pendingHeatingsByLevel = new LinkedHashMap<>();
        Map<UUID, List<HeatingZone>> pendingHeatingZones = new LinkedHashMap<>();
        String importedProjectName = projectName;
        Roof importedRoof = null;
        List<TerrainVertex> importedTerrainVertices = new ArrayList<>();
        boolean encodedFields = DxfMetadataCodec.usesCurrentEncoding(metadata);
        boolean objectRotationDegrees = DxfMetadataCodec.usesObjectRotationDegrees(metadata);
        for (String entry : metadata) {
            String[] parts = DxfMetadataCodec.split(entry);
            if (DxfMetadataCodec.isMarker(parts)) {
                continue;
            }
            try {
                switch (parts[0]) {
                    case "PROJECT" -> importedProjectName = stripDxfExtension(DxfMetadataCodec.decode(parts[1], encodedFields));
                    case "LEVEL" -> levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                    case "TERRAIN" -> importedTerrainVertices.add(new TerrainVertex(
                            new PlanPoint(parseDouble(parts[1]), parseDouble(parts[2])),
                            Length.ofMillimeters(parseDouble(parts[3]))
                    ));
                    case "WALL" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        double startHeight = parts.length >= 11 ? parseDouble(parts[5]) : parseDouble(parts[4]);
                        double endHeight = parts.length >= 11 ? parseDouble(parts[6]) : parseDouble(parts[4]);
                        int pointOffset = parts.length >= 11 ? 2 : 0;
                        List<WallProfilePoint> profile = parts.length >= 12 ? deserializeWallProfile(parts[11]) : List.of();
                        level.addWall(new Wall(
                                UUID.fromString(parts[2]),
                                new PlanSegment(
                                        new PlanPoint(parseDouble(parts[5 + pointOffset]), parseDouble(parts[6 + pointOffset])),
                                        new PlanPoint(parseDouble(parts[7 + pointOffset]), parseDouble(parts[8 + pointOffset]))
                                ),
                                Length.ofMillimeters(parseDouble(parts[3])),
                                Length.ofMillimeters(parseDouble(parts[4])),
                                Length.ofMillimeters(startHeight),
                                Length.ofMillimeters(endHeight),
                                profile
                        ));
                    }
                    case "ROOM" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        if (isUuid(parts[2])) {
                            level.addRoom(Room.withSlopedCeilings(
                                    UUID.fromString(parts[2]),
                                    DxfMetadataCodec.decode(parts[3], encodedFields),
                                    deserializePoints(parts[7]),
                                    Length.ofMillimeters(parseDouble(parts[4])),
                                    Length.ofMillimeters(parseDouble(parts[5])),
                                    Length.ofMillimeters(parseDouble(parts[6])),
                                    parts.length >= 9 ? deserializeSlopedCeilings(parts[8]) : List.of(),
                                    parts.length >= 10 ? deserializeCeilingVertexHeights(parts[9]) : null
                            ));
                        } else {
                            level.addRoom(Room.withSlopedCeilings(
                                    UUID.randomUUID(),
                                    DxfMetadataCodec.decode(parts[2], encodedFields),
                                    deserializePoints(parts[6]),
                                    Length.ofMillimeters(parseDouble(parts[3])),
                                    Length.ofMillimeters(parseDouble(parts[4])),
                                    Length.ofMillimeters(parseDouble(parts[5])),
                                    parts.length >= 8 ? deserializeSlopedCeilings(parts[7]) : List.of(),
                                    parts.length >= 9 ? deserializeCeilingVertexHeights(parts[8]) : null
                            ));
                        }
                    }
                    case "DOOR" -> {
                        String levelName = DxfMetadataCodec.decode(parts[1], encodedFields);
                        levels.computeIfAbsent(levelName, Level::new);
                        pendingDoorsByLevel.computeIfAbsent(levelName, ignored -> new ArrayList<>()).add(deserializeDoor(parts));
                    }
                    case "WINDOW" -> {
                        String levelName = DxfMetadataCodec.decode(parts[1], encodedFields);
                        levels.computeIfAbsent(levelName, Level::new);
                        pendingWindowsByLevel.computeIfAbsent(levelName, ignored -> new ArrayList<>()).add(deserializeWindow(parts));
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
                                Integer.parseInt(parts[10]),
                                Length.ofMillimeters(parts.length >= 12 ? parseDouble(parts[11]) : 0),
                                Length.ofMillimeters(parts.length >= 13 ? parseDouble(parts[12]) : 0),
                                Length.ofMillimeters(parts.length >= 14 ? parseDouble(parts[13]) : 0),
                                Length.ofMillimeters(parts.length >= 15 ? parseDouble(parts[14]) : 0),
                                Length.ofMillimeters(parts.length >= 16 ? parseDouble(parts[15]) : 0)
                        ));
                    }
                    case "FEXT" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        level.addFloorExtension(new FloorExtension(
                                UUID.fromString(parts[2]), FloorExtensionType.valueOf(parts[3]),
                                FloorExtensionPlacement.valueOf(parts[4]),
                                new PlanPoint(parseDouble(parts[5]), parseDouble(parts[6])),
                                new PlanPoint(parseDouble(parts[7]), parseDouble(parts[8])),
                                Length.ofMillimeters(parseDouble(parts[9]))
                        ));
                    }
                    case "FOPEN" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        level.addFloorOpening(new FloorOpening(
                                UUID.fromString(parts[2]), UUID.fromString(parts[3]), FloorOpeningShape.valueOf(parts[4]),
                                new PlanPoint(parseDouble(parts[5]), parseDouble(parts[6])),
                                Length.ofMillimeters(parseDouble(parts[7])), Length.ofMillimeters(parseDouble(parts[8]))
                        ));
                    }
                    case "HEXCL" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        level.addHeatingExclusionArea(new HeatingExclusionArea(
                                UUID.fromString(parts[2]), UUID.fromString(parts[3]),
                                DxfMetadataCodec.decode(parts[4], encodedFields),
                                new PlanPoint(parseDouble(parts[5]), parseDouble(parts[6])),
                                new PlanPoint(parseDouble(parts[7]), parseDouble(parts[8]))
                        ));
                    }
                    case "ROOF_WINDOW" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        level.addRoofWindow(new RoofWindow(
                                UUID.fromString(parts[2]), UUID.fromString(parts[3]),
                                new PlanPoint(parseDouble(parts[4]), parseDouble(parts[5])),
                                Length.ofMillimeters(parseDouble(parts[6])), Length.ofMillimeters(parseDouble(parts[7])),
                                SlopedCeilingSide.valueOf(parts[8])
                        ));
                    }
                    case "HEAT" -> {
                        String levelName = DxfMetadataCodec.decode(parts[1], encodedFields);
                        levels.computeIfAbsent(levelName, Level::new);
                        HydronicHeating heating = deserializeProjectHeating(parts);
                        pendingHeatingsByLevel.computeIfAbsent(levelName, ignored -> new LinkedHashMap<>())
                                .put(heating.id(), heating);
                    }
                    case "HZONE" -> pendingHeatingZones
                            .computeIfAbsent(UUID.fromString(parts[2]), ignored -> new ArrayList<>())
                            .add(deserializeProjectHeatingZone(parts, encodedFields));
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
                                    SurfaceCutRestriction.fromStoredValue(parts.length >= 16 ? parts[15] : null),
                                    DxfMetadataCodec.decode(parts.length >= 15 ? parts[14] : parts[13], encodedFields),
                                    parts.length >= 17 && Boolean.parseBoolean(parts[16])
                            );
                            stack.addLayer(layer);
                        }
                    }
                    case "OBJ" -> {
                        Level level = levels.computeIfAbsent(DxfMetadataCodec.decode(parts[1], encodedFields), Level::new);
                        level.addRoomObject(new RoomObject(
                                UUID.fromString(parts[2]),
                                DxfMetadataCodec.decode(parts[3], encodedFields),
                                DxfMetadataCodec.decode(parts[4], encodedFields),
                                RoomObjectType.valueOf(parts[5]),
                                RoomObjectShape.valueOf(parts[6]),
                                new PlanPoint(parseDouble(parts[7]), parseDouble(parts[8])),
                                Length.ofMillimeters(parseDouble(parts[9])),
                                Length.ofMillimeters(parseDouble(parts[10])),
                                Length.ofMillimeters(parseDouble(parts[11])),
                                parseObjectRotation(parts[12], objectRotationDegrees),
                                RoomObjectMountingMode.fromStoredValue(parts.length >= 17 ? parts[16] : null, Boolean.parseBoolean(parts[13])),
                                Boolean.parseBoolean(parts[14]),
                                DxfMetadataCodec.decode(parts[15], encodedFields),
                                Length.ofMillimeters(parts.length >= 18 ? parseDouble(parts[17]) : 0.0)
                        ));
                    }
                    default -> {
                    }
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                // Fremde oder beschädigte Metadaten sollen den Geometrieimport nicht blockieren.
            }
        }

        addValidOpenings(levels, pendingDoorsByLevel, pendingWindowsByLevel);
        addHydronicHeatings(levels, pendingHeatingsByLevel, pendingHeatingZones);

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
        if (importedTerrainVertices.size() >= 3) {
            project.defineTerrain(new Terrain(importedTerrainVertices));
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
        for (FloorExtension extension : level.floorExtensions()) {
            appendClosedPolyline(dxf, context, layerPrefix + "_ROOMS", extension.outline());
        }
        for (HeatingExclusionArea area : level.heatingExclusionAreas()) {
            appendClosedPolyline(dxf, context, layerPrefix + "_ROOMS", rectangle(area));
        }
    }

    private void exportLevelMetadata(StringBuilder dxf, DxfDocumentSupport.DxfWriteContext context, Level level) {
        for (Wall wall : level.walls()) {
            appendMetadataText(dxf, context, wall.axis().start(), String.format(
                    Locale.US,
                    "WALL|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%s",
                    DxfMetadataCodec.encode(level.name()),
                    wall.id(),
                    wall.thickness().toMillimeters(),
                    wall.height().toMillimeters(),
                    wall.startHeight().toMillimeters(),
                    wall.endHeight().toMillimeters(),
                    wall.axis().start().xMillimeters(),
                    wall.axis().start().yMillimeters(),
                    wall.axis().end().xMillimeters(),
                    wall.axis().end().yMillimeters(),
                    serializeWallProfile(wall)
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
                    "STAIR|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%d|%d|%.3f|%.3f|%.3f|%.3f|%.3f",
                    DxfMetadataCodec.encode(level.name()),
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
                    staircase.endLandingWidth().toMillimeters(),
                    staircase.leftUnderbuildWidth().toMillimeters(),
                    staircase.rightUnderbuildWidth().toMillimeters(),
                    staircase.undersideThickness().toMillimeters()
            ));
        }
        for (FloorExtension extension : level.floorExtensions()) {
            appendMetadataText(dxf, context, extension.firstCorner(), String.format(
                    Locale.US,
                    "FEXT|%s|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f",
                    DxfMetadataCodec.encode(level.name()), extension.id(), extension.type().name(), extension.placement().name(),
                    extension.firstCorner().xMillimeters(), extension.firstCorner().yMillimeters(),
                    extension.oppositeCorner().xMillimeters(), extension.oppositeCorner().yMillimeters(),
                    extension.slabThickness().toMillimeters()
            ));
        }
        for (FloorOpening opening : level.floorOpenings()) {
            appendMetadataText(dxf, context, opening.center(), String.format(
                    Locale.US,
                    "FOPEN|%s|%s|%s|%s|%.3f|%.3f|%.3f|%.3f",
                    DxfMetadataCodec.encode(level.name()), opening.id(), opening.roomId(), opening.shape().name(),
                    opening.center().xMillimeters(), opening.center().yMillimeters(),
                    opening.width().toMillimeters(), opening.depth().toMillimeters()
            ));
        }
        for (HeatingExclusionArea area : level.heatingExclusionAreas()) {
            appendMetadataText(dxf, context, area.center(), String.format(
                    Locale.US,
                    "HEXCL|%s|%s|%s|%s|%.3f|%.3f|%.3f|%.3f",
                    DxfMetadataCodec.encode(level.name()), area.id(), area.roomId(),
                    DxfMetadataCodec.encode(area.name()),
                    area.firstCorner().xMillimeters(), area.firstCorner().yMillimeters(),
                    area.oppositeCorner().xMillimeters(), area.oppositeCorner().yMillimeters()
            ));
        }
        for (RoofWindow roofWindow : level.roofWindows()) {
            appendMetadataText(dxf, context, roofWindow.center(), String.format(
                    Locale.US,
                    "ROOF_WINDOW|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%s",
                    DxfMetadataCodec.encode(level.name()), roofWindow.id(), roofWindow.roomId(),
                    roofWindow.center().xMillimeters(), roofWindow.center().yMillimeters(),
                    roofWindow.width().toMillimeters(), roofWindow.depth().toMillimeters(), roofWindow.slopeSide().name()
            ));
        }
        for (HydronicHeating heating : level.hydronicHeatings()) {
            appendMetadataText(dxf, context, heating.supplyPoint(), String.format(
                    Locale.US,
                    "HEAT|%s|%s|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f",
                    DxfMetadataCodec.encode(level.name()), heating.id(), heating.roomId(),
                    heating.surfacePosition().name(), heating.layoutPattern().name(),
                    heating.pipeSpacing().toMillimeters(), heating.pipeDiameter().toMillimeters(),
                    heating.maximumPipeLength().toMillimeters(), heating.wallClearance().toMillimeters(),
                    heating.supplyPoint().xMillimeters(), heating.supplyPoint().yMillimeters(),
                    heating.returnPoint().xMillimeters(), heating.returnPoint().yMillimeters(),
                    heating.manifoldFreeAreaWidth().toMillimeters(), heating.manifoldFreeAreaDepth().toMillimeters()
            ));
            for (HeatingZone zone : heating.zones()) {
                appendMetadataText(dxf, context, zone.outline().getFirst(), String.format(
                        Locale.US,
                        "HZONE|%s|%s|%s|%s|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%s|%s|%.3f|%d|%s|%s",
                        DxfMetadataCodec.encode(level.name()), heating.id(), zone.id(),
                        DxfMetadataCodec.encode(zone.name()), zone.layoutPattern().name(),
                        zone.flowInverted(), serializePoints(zone.outline()),
                        zone.supplyConnectionPoint().xMillimeters(), zone.supplyConnectionPoint().yMillimeters(),
                        zone.returnConnectionPoint().xMillimeters(), zone.returnConnectionPoint().yMillimeters(),
                        DxfMetadataCodec.encode(zone.routingCommands()), zone.serpentineMiddleLine(),
                        zone.heatOutputWattsPerSquareMeter(), zone.routingQuarterTurns(),
                        zone.routingMirroredHorizontally(), zone.routingMirroredVertically()
                ));
            }
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
                        "SLL|%s|%s|%s|%.3f|%s|%.3f|%.3f|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%s|%s|%s",
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
                        DxfMetadataCodec.encode(layer.coveringSource()),
                        layer.cutRestriction().name(),
                        layer.layoutRotatedQuarterTurn()
                ));
            }
        }
        for (RoomObject roomObject : level.roomObjects()) {
            appendMetadataText(dxf, context, roomObject.center(), String.format(
                    Locale.US,
                    "OBJ|%s|%s|%s|%s|%s|%s|%.3f|%.3f|%.3f|%.3f|%.3f|%.3f|%s|%s|%s|%s|%.3f",
                    DxfMetadataCodec.encode(level.name()),
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
    }

    private double parseObjectRotation(String storedRotation, boolean storedInDegrees) {
        double value = parseDouble(storedRotation);
        return storedInDegrees ? value : value * 90.0;
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
        target.replaceRoomObjects(source.roomObjects());
        target.replaceFloorExtensions(source.floorExtensions());
        target.replaceFloorOpenings(source.floorOpenings());
        target.replaceHeatingExclusionAreas(source.heatingExclusionAreas());
        target.replaceSurfaceLayerStacks(source.surfaceLayerStacks().stream()
                .map(SurfaceLayerStack::copy)
                .toList());
        target.replaceHydronicHeatings(source.hydronicHeatings());
    }

    private void addHydronicHeatings(
            Map<String, Level> levels,
            Map<String, Map<UUID, HydronicHeating>> pendingHeatingsByLevel,
            Map<UUID, List<HeatingZone>> pendingHeatingZones
    ) {
        for (Map.Entry<String, Map<UUID, HydronicHeating>> levelEntry : pendingHeatingsByLevel.entrySet()) {
            Level level = levels.get(levelEntry.getKey());
            if (level == null) {
                continue;
            }
            levelEntry.getValue().values().stream()
                    .map(heating -> heating.withZones(pendingHeatingZones.getOrDefault(heating.id(), List.of())))
                    .filter(heating -> level.rooms().stream().anyMatch(room -> room.id().equals(heating.roomId())))
                    .forEach(level::addHydronicHeating);
        }
    }

    private void addValidOpenings(
            Map<String, Level> levels,
            Map<String, List<Door>> pendingDoorsByLevel,
            Map<String, List<WindowElement>> pendingWindowsByLevel
    ) {
        for (Map.Entry<String, Level> entry : levels.entrySet()) {
            Set<UUID> wallIds = entry.getValue().walls().stream()
                    .map(Wall::id)
                    .collect(Collectors.toSet());
            pendingDoorsByLevel.getOrDefault(entry.getKey(), List.of()).stream()
                    .filter(door -> wallIds.contains(door.wallId()))
                    .forEach(entry.getValue()::addDoor);
            pendingWindowsByLevel.getOrDefault(entry.getKey(), List.of()).stream()
                    .filter(window -> wallIds.contains(window.wallId()))
                    .forEach(entry.getValue()::addWindow);
        }
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

    private List<PlanPoint> rectangle(HeatingExclusionArea area) {
        return List.of(
                new PlanPoint(area.minXMillimeters(), area.minYMillimeters()),
                new PlanPoint(area.maxXMillimeters(), area.minYMillimeters()),
                new PlanPoint(area.maxXMillimeters(), area.maxYMillimeters()),
                new PlanPoint(area.minXMillimeters(), area.maxYMillimeters())
        );
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

    private HydronicHeating deserializeProjectHeating(String[] parts) {
        return new HydronicHeating(
                UUID.fromString(parts[2]), UUID.fromString(parts[3]),
                HeatingSurfacePosition.valueOf(parts[4]), HeatingLayoutPattern.valueOf(parts[5]),
                Length.ofMillimeters(parseDouble(parts[6])), Length.ofMillimeters(parseDouble(parts[7])),
                Length.ofMillimeters(parseDouble(parts[8])), Length.ofMillimeters(parseDouble(parts[9])),
                new PlanPoint(parseDouble(parts[10]), parseDouble(parts[11])),
                new PlanPoint(parseDouble(parts[12]), parseDouble(parts[13])),
                parts.length >= 16 ? Length.ofMillimeters(parseDouble(parts[14])) : HydronicHeating.DEFAULT_MANIFOLD_FREE_AREA_WIDTH,
                parts.length >= 16 ? Length.ofMillimeters(parseDouble(parts[15])) : HydronicHeating.DEFAULT_MANIFOLD_FREE_AREA_DEPTH,
                List.of()
        );
    }

    private HeatingZone deserializeProjectHeatingZone(String[] parts, boolean encodedFields) {
        if (parts.length >= 15) {
            return new HeatingZone(
                    UUID.fromString(parts[3]),
                    DxfMetadataCodec.decode(parts[4], encodedFields),
                    deserializePoints(parts[7]),
                    HeatingLayoutPattern.valueOf(parts[5]),
                    Boolean.parseBoolean(parts[6]),
                    new PlanPoint(parseDouble(parts[8]), parseDouble(parts[9])),
                    new PlanPoint(parseDouble(parts[10]), parseDouble(parts[11])),
                    DxfMetadataCodec.decode(parts[12], encodedFields),
                    Boolean.parseBoolean(parts[13]),
                    parseDouble(parts[14]),
                    parts.length >= 18 ? Integer.parseInt(parts[15]) : 0,
                    parts.length >= 18 && Boolean.parseBoolean(parts[16]),
                    parts.length >= 18 && Boolean.parseBoolean(parts[17])
            );
        }
        if (parts.length >= 12) {
            return new HeatingZone(
                    UUID.fromString(parts[3]),
                    DxfMetadataCodec.decode(parts[4], encodedFields),
                    deserializePoints(parts[7]),
                    HeatingLayoutPattern.valueOf(parts[5]),
                    Boolean.parseBoolean(parts[6]),
                    new PlanPoint(parseDouble(parts[8]), parseDouble(parts[9])),
                    new PlanPoint(parseDouble(parts[10]), parseDouble(parts[11]))
            );
        }
        if (parts.length >= 8) {
            return new HeatingZone(
                    UUID.fromString(parts[3]),
                    DxfMetadataCodec.decode(parts[4], encodedFields),
                    deserializePoints(parts[7]),
                    HeatingLayoutPattern.valueOf(parts[5]),
                    Boolean.parseBoolean(parts[6])
            );
        }
        return new HeatingZone(
                UUID.fromString(parts[3]),
                DxfMetadataCodec.decode(parts[4], encodedFields),
                deserializePoints(parts[5])
        );
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
        if (room.slopedCeilingProfiles().isEmpty()) {
            return "NONE";
        }
        return "SLOPES;" + room.slopedCeilingProfiles().stream()
                .map(profile -> String.format(Locale.US, "%s,%.3f,%.3f",
                        profile.lowSide().name(),
                        profile.kneeWallHeight().toMillimeters(),
                        profile.horizontalRun().toMillimeters()))
                .reduce((left, right) -> left + ";" + right)
                .orElseThrow();
    }

    private String serializeCeilingVertexHeights(Room room) {
        return room.ceilingVertexHeightsProfile()
                .map(heights -> heights.stream()
                        .map(height -> String.format(Locale.US, "%.3f", height.toMillimeters()))
                        .reduce((left, right) -> left + ";" + right)
                        .orElse("NONE"))
                .orElse("NONE");
    }

    private List<SlopedCeilingProfile> deserializeSlopedCeilings(String value) {
        if (value == null || value.isBlank() || value.equals("NONE")) {
            return List.of();
        }
        if (value.startsWith("SLOPES;")) {
            return java.util.Arrays.stream(value.substring("SLOPES;".length()).split(";"))
                    .map(this::deserializeSlopeValues)
                    .toList();
        }
        String[] parts = value.split(",");
        if ((parts.length != 3 && parts.length != 4) || !parts[0].equals("SLOPE")) {
            return List.of();
        }
        return List.of(new SlopedCeilingProfile(
                SlopedCeilingSide.valueOf(parts[1]),
                Length.ofMillimeters(parseDouble(parts[2])),
                parts.length == 4 ? Length.ofMillimeters(parseDouble(parts[3])) : Length.zero()
        ));
    }

    private SlopedCeilingProfile deserializeSlopeValues(String value) {
        String[] parts = value.split(",");
        return new SlopedCeilingProfile(
                SlopedCeilingSide.valueOf(parts[0]),
                Length.ofMillimeters(parseDouble(parts[1])),
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

    private String serializeWallProfile(Wall wall) {
        if (!wall.hasPolygonalProfile()) {
            return "NONE";
        }
        return wall.profile().stream()
                .map(point -> String.format(Locale.US, "%.3f,%.3f", point.offset().toMillimeters(), point.height().toMillimeters()))
                .reduce((left, right) -> left + ";" + right)
                .orElse("NONE");
    }

    private List<WallProfilePoint> deserializeWallProfile(String value) {
        if (value == null || value.isBlank() || value.equals("NONE")) {
            return List.of();
        }
        List<WallProfilePoint> profile = new ArrayList<>();
        for (String serializedPoint : value.split(";")) {
            String[] coordinates = serializedPoint.split(",");
            if (coordinates.length != 2) {
                throw new IllegalArgumentException("Ungültiger Wandprofilpunkt: " + serializedPoint);
            }
            profile.add(new WallProfilePoint(
                    Length.ofMillimeters(parseDouble(coordinates[0])),
                    Length.ofMillimeters(parseDouble(coordinates[1]))
            ));
        }
        return List.copyOf(profile);
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
