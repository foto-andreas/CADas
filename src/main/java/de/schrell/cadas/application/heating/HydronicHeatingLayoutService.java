package de.schrell.cadas.application.heating;

import de.schrell.cadas.application.layers.SurfaceCoveringPresetService;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.FloorOpening;
import de.schrell.cadas.domain.model.HeatingLayoutPattern;
import de.schrell.cadas.domain.model.HeatingExclusionArea;
import de.schrell.cadas.domain.model.HeatingZone;
import de.schrell.cadas.domain.model.HydronicHeating;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.Staircase;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;

public final class HydronicHeatingLayoutService {

    private static final double EPSILON = 0.001;
    private static final int MAXIMUM_ZONE_COUNT = 64;
    private static final int MAXIMUM_REPAIR_ATTEMPTS = 10;
    private static final double MANIFOLD_PAIR_PITCH_MILLIMETERS = 50.0;
    private static final double MANIFOLD_FREE_AREA_WIDTH_MILLIMETERS = 600.0;
    private static final double MANIFOLD_FREE_AREA_HEIGHT_MILLIMETERS = 1_000.0;
    private static final int COMPUTATION_CACHE_SIZE = 128;

    private final Map<HydronicHeating, LayoutComputation> computationCache = Collections.synchronizedMap(
            new LinkedHashMap<>(COMPUTATION_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<HydronicHeating, LayoutComputation> eldest) {
                    return size() > COMPUTATION_CACHE_SIZE;
                }
            }
    );

    public PlanningResult suggest(Room room, HydronicHeating heating) {
        Objects.requireNonNull(room, "room darf nicht null sein.");
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        return suggest(room, heating, List.of());
    }

    public PlanningResult suggest(Room room, HydronicHeating heating, List<Staircase> staircases) {
        return suggest(room, heating, staircases, List.of(), List.of());
    }

    public PlanningResult suggest(
            Room room,
            HydronicHeating heating,
            List<Staircase> staircases,
            List<FloorOpening> floorOpenings,
            List<HeatingExclusionArea> heatingExclusionAreas
    ) {
        Objects.requireNonNull(room, "room darf nicht null sein.");
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        Objects.requireNonNull(staircases, "staircases darf nicht null sein.");
        Objects.requireNonNull(floorOpenings, "floorOpenings darf nicht null sein.");
        Objects.requireNonNull(heatingExclusionAreas, "heatingExclusionAreas darf nicht null sein.");
        List<HeatingZone> zones = initialZones(room, heating, staircases, floorOpenings, heatingExclusionAreas);
        HydronicHeating planned = heating.withZones(zones);
        List<ValidationIssue> warnings = new ArrayList<>();
        int repairAttempts = 0;
        boolean earlyRelocationAttempted = false;
        while (zones.size() < MAXIMUM_ZONE_COUNT) {
            planned = optimizeFlowOrientation(planned);
            zones = new ArrayList<>(planned.zones());
            LayoutComputation computation = compute(planned);
            if (!computation.unroutableZoneIds().isEmpty()) {
                if (!earlyRelocationAttempted && shouldTryEarlyRelocation(planned)) {
                    earlyRelocationAttempted = true;
                    Optional<PlanningResult> relocated = tryRelocatedManifold(room, planned, warnings);
                    if (relocated.isPresent()) {
                        return relocated.orElseThrow();
                    }
                }
                Optional<HydronicHeating> repaired = repairBySplittingUnroutableZone(planned, computation.unroutableZoneIds());
                if (repaired.isPresent() && repairAttempts < MAXIMUM_REPAIR_ATTEMPTS) {
                    planned = repaired.orElseThrow();
                    zones = new ArrayList<>(planned.zones());
                    repairAttempts++;
                    continue;
                }
                Optional<PlanningResult> relocated = tryRelocatedManifold(room, planned, warnings);
                if (relocated.isPresent()) {
                    return relocated.orElseThrow();
                }
                return partialPlanning(planned, computation, warnings);
            }
            if (!computation.report().valid()) {
                return new PlanningResult(planned, computation.circuits(), computation.report());
            }
            List<CircuitLayout> layouts = computation.circuits();
            HydronicHeating currentPlanned = planned;
            Optional<CircuitLayout> oversized = layouts.stream()
                    .filter(circuit -> circuit.pipeLength().compareTo(heating.maximumPipeLength()) > 0)
                    .max(Comparator.comparingDouble(circuit -> currentPlanned.zones().stream()
                            .filter(zone -> zone.id().equals(circuit.zoneId()))
                            .findFirst()
                            .map(HeatingZone::areaSquareMillimeters)
                            .orElse(0.0)));
            if (oversized.isEmpty()) {
                ValidationReport report = computation.report();
                return new PlanningResult(planned, layouts, withWarnings(report, warnings));
            }
            HeatingZone zone = planned.zones().stream()
                    .filter(candidate -> candidate.id().equals(oversized.orElseThrow().zoneId()))
                    .findFirst()
                    .orElseThrow();
            List<HeatingZone> split = split(zone);
            if (split.size() < 2) {
                ValidationReport report = computation.report();
                return new PlanningResult(planned, layouts, withWarnings(report, warnings));
            }
            int index = zones.indexOf(zone);
            zones.remove(index);
            zones.addAll(index, split);
            zones = renameZones(zones);
            planned = heating.withZones(zones);
        }
        planned = optimizeFlowOrientation(planned);
        LayoutComputation computation = compute(planned);
        if (!computation.unroutableZoneIds().isEmpty()) {
            return partialPlanning(planned, computation, warnings);
        }
        return new PlanningResult(planned, computation.circuits(), withWarnings(computation.report(), warnings));
    }

    private boolean shouldTryEarlyRelocation(HydronicHeating heating) {
        GeometryScope fieldScope = fieldScopeFor(heating);
        return !fieldScope.contains(heating.supplyPoint()) || !fieldScope.contains(heating.returnPoint());
    }

    public List<CircuitLayout> layout(HydronicHeating heating) {
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        if (heating.zones().isEmpty()) {
            return List.of();
        }
        LayoutComputation computation = cachedCompute(heating);
        if (!computation.report().valid()) {
            throw new IllegalArgumentException(computation.report().summary());
        }
        return computation.circuits();
    }

    public ValidationReport validateLayout(HydronicHeating heating) {
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        if (heating.zones().isEmpty()) {
            return ValidationReport.ok();
        }
        return cachedCompute(heating).report();
    }

    public String toSvg(Room room, HydronicHeating heating) {
        return toSvg(room, heating, List.of(), List.of());
    }

    public String toSvg(
            Room room,
            HydronicHeating heating,
            List<FloorOpening> floorOpenings,
            List<HeatingExclusionArea> heatingExclusionAreas
    ) {
        Objects.requireNonNull(room, "room darf nicht null sein.");
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        Objects.requireNonNull(floorOpenings, "floorOpenings darf nicht null sein.");
        Objects.requireNonNull(heatingExclusionAreas, "heatingExclusionAreas darf nicht null sein.");
        List<CircuitLayout> circuits = layout(heating);
        List<PlanPoint> svgPoints = new ArrayList<>(room.outline());
        heating.zones().forEach(zone -> svgPoints.addAll(zone.outline()));
        circuits.forEach(circuit -> svgPoints.addAll(circuit.pipePath()));
        Bounds bounds = bounds(svgPoints);
        double padding = Math.max(heating.pipeSpacing().toMillimeters(), 100.0);
        double minX = bounds.minX() - padding;
        double minY = bounds.minY() - padding;
        double width = bounds.width() + padding * 2.0;
        double height = bounds.height() + padding * 2.0;
        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"%.3f %.3f %.3f %.3f\">\n",
                minX, minY, width, height));
        svg.append("<g id=\"raum\" fill=\"none\" stroke=\"#202020\" stroke-width=\"10\">\n");
        svg.append("<polygon points=\"").append(pointsAttribute(room.outline())).append("\"/>\n</g>\n");
        svg.append("<g id=\"sperrflaechen\" fill=\"#f8dcd8\" stroke=\"#aa2d23\" stroke-width=\"5\">\n");
        for (FloorOpening opening : floorOpenings) {
            if (!opening.roomId().equals(room.id())) {
                continue;
            }
            if (opening.shape() == de.schrell.cadas.domain.model.FloorOpeningShape.CIRCLE) {
                svg.append(String.format(Locale.US,
                        "<circle cx=\"%.3f\" cy=\"%.3f\" r=\"%.3f\"/>\n",
                        opening.center().xMillimeters(), opening.center().yMillimeters(),
                        opening.width().toMillimeters() / 2.0));
            } else {
                svg.append("<polygon points=\"").append(pointsAttribute(rectangle(
                        opening.minXMillimeters(), opening.minYMillimeters(),
                        opening.maxXMillimeters(), opening.maxYMillimeters()
                ))).append("\"/>\n");
            }
        }
        for (HeatingExclusionArea area : heatingExclusionAreas) {
            if (!area.roomId().equals(room.id())) {
                continue;
            }
            svg.append("<polygon points=\"").append(pointsAttribute(rectangle(
                    area.minXMillimeters(), area.minYMillimeters(),
                    area.maxXMillimeters(), area.maxYMillimeters()
            ))).append("\"/>\n");
        }
        svg.append("</g>\n");
        svg.append("<g id=\"variotherm-rinnen\" fill=\"none\" stroke=\"#9aa6ad\" stroke-width=\"2\">\n");
        appendVariothermGrooves(svg, room);
        svg.append("</g>\n");
        svg.append("<g id=\"heizbereiche\" fill=\"rgba(255,255,255,0.1)\" stroke=\"#315f8f\" stroke-width=\"5\" stroke-dasharray=\"35 20\">\n");
        for (HeatingZone zone : heating.zones()) {
            svg.append("<polygon points=\"").append(pointsAttribute(zone.outline())).append("\"/>\n");
        }
        svg.append("</g>\n");
        svg.append(String.format(Locale.US,
                "<g id=\"anschluesse\" fill=\"none\" stroke-width=\"%.3f\" stroke-linecap=\"round\" stroke-linejoin=\"round\">\n",
                heating.pipeDiameter().toMillimeters()));
        for (CircuitLayout circuit : circuits) {
            svg.append(pathElement("connector-vorlauf", circuit.supplyConnectorPath(), "#1f62d0", false));
            svg.append(pathElement("connector-ruecklauf", circuit.returnConnectorPath(), "#d33b32", true));
        }
        svg.append("</g>\n");
        svg.append(String.format(Locale.US,
                "<g id=\"heizrohre\" fill=\"none\" stroke-width=\"%.3f\" stroke-linecap=\"round\" stroke-linejoin=\"round\">\n",
                heating.pipeDiameter().toMillimeters()));
        for (CircuitLayout circuit : circuits) {
            svg.append(pathElement("vorlauf", circuit.fieldSupplyPath(), "#1f62d0", false));
            svg.append(pathElement("ruecklauf", circuit.fieldReturnPath(), "#d33b32", false));
        }
        svg.append("</g>\n");
        svg.append("<g id=\"hkv\" font-family=\"Arial, sans-serif\" font-size=\"90\" text-anchor=\"middle\">\n");
        for (int index = 0; index < circuits.size(); index++) {
            CircuitLayout circuit = circuits.get(index);
            svg.append(portCircle(circuit.supplyPort(), "#1f62d0", "V" + (index + 1)));
            svg.append(portCircle(circuit.returnPort(), "#d33b32", "R" + (index + 1)));
        }
        svg.append("</g>\n</svg>\n");
        return svg.toString();
    }

    public void validateZones(Room room, HydronicHeating heating) {
        if (!room.id().equals(heating.roomId())) {
            throw new IllegalArgumentException("Die Heizung gehört nicht zum ausgewählten Raum.");
        }
        for (HeatingZone zone : heating.zones()) {
            for (int index = 0; index < zone.outline().size(); index++) {
                PlanPoint start = zone.outline().get(index);
                PlanPoint end = zone.outline().get((index + 1) % zone.outline().size());
                if (!segmentInside(start, end, room.outline())) {
                    throw new IllegalArgumentException("Der Heizbereich `" + zone.name() + "` liegt nicht vollständig im Raum.");
                }
            }
        }
        ValidationReport report = validateLayout(heating);
        if (!report.valid()) {
            throw new IllegalArgumentException(report.summary());
        }
    }

    private LayoutComputation compute(HydronicHeating heating) {
        GeometryScope fieldScope = fieldScopeFor(heating);
        GeometryScope scope = scopeFor(heating);
        GridGraph graph = GridGraph.create(scope, heating.pipeSpacing().toMillimeters());
        List<ValidationIssue> errors = new ArrayList<>();
        Set<UUID> unroutableZoneIds = new LinkedHashSet<>();
        List<CircuitLayout> circuits = new ArrayList<>();
        Set<GridEdge> fieldEdges = new LinkedHashSet<>();
        List<FieldPattern> patterns = new ArrayList<>();
        for (HeatingZone zone : heating.zones()) {
            FieldPattern pattern = createPattern(heating, zone);
            patterns.add(pattern);
            fieldEdges.addAll(edgesOf(pattern.fullPath(), heating.pipeSpacing().toMillimeters()));
        }
        Set<GridEdge> blocked = new LinkedHashSet<>(fieldEdges);
        for (int index = 0; index < heating.zones().size(); index++) {
            HeatingZone zone = heating.zones().get(index);
            FieldPattern pattern = patterns.get(index);
            ManifoldPair pair = manifoldPair(heating, index);
            ConnectorPlan connectors;
            try {
                connectors = routeConnectors(graph, scope, fieldScope, pattern, pair, blocked, heating.pipeSpacing().toMillimeters());
            } catch (IllegalArgumentException exception) {
                unroutableZoneIds.add(zone.id());
                errors.add(new ValidationIssue(
                        ValidationErrorType.UNROUTABLE_CONNECTOR,
                        zone.name() + ": " + exception.getMessage()
                ));
                connectors = ConnectorPlan.failed(pair, pattern);
            }
            List<PlanPoint> fullPath = concatenate(connectors.supplyPath(), pattern.fullPath(), connectors.returnPath());
            Length length = Length.ofMillimeters(roundedLength(fullPath, heating.bendRadius().toMillimeters()));
            circuits.add(new CircuitLayout(
                    zone.id(),
                    fullPath,
                    length,
                    heating.bendRadius(),
                    pair.supplyPort(),
                    pair.returnPort(),
                    connectors.supplyPath(),
                    connectors.returnPath(),
                    pattern.supplyPath(),
                    pattern.returnPath(),
                    segments(zone.id(), connectors, pattern),
                    ValidationReport.ok()
            ));
        }
        errors.addAll(validateGeometry(scope, circuits, heating));
        ValidationReport report = new ValidationReport(errors.isEmpty(), errors, List.of());
        return new LayoutComputation(circuits.stream()
                .map(circuit -> circuit.withValidationReport(report))
                .toList(), report, unroutableZoneIds);
    }

    private LayoutComputation cachedCompute(HydronicHeating heating) {
        synchronized (computationCache) {
            LayoutComputation cached = computationCache.get(heating);
            if (cached != null) {
                return cached;
            }
        }
        LayoutComputation computed = compute(heating);
        synchronized (computationCache) {
            computationCache.put(heating, computed);
        }
        return computed;
    }

    private GeometryScope scopeFor(HydronicHeating heating) {
        List<List<PlanPoint>> polygons = new ArrayList<>(heating.zones().stream()
                .map(HeatingZone::outline)
                .toList());
        List<PlanPoint> ports = new ArrayList<>();
        for (int index = 0; index < heating.zones().size(); index++) {
            ManifoldPair pair = manifoldPair(heating, index);
            ports.add(pair.supplyPort());
            ports.add(pair.returnPort());
        }
        boolean needsConnectorCorridor = ports.stream().anyMatch(port -> !strictlyInsideAny(polygons, port));
        if (needsConnectorCorridor) {
            polygons.add(connectorCorridor(polygons, ports, heating.pipeSpacing().toMillimeters()));
        }
        return new GeometryScope(polygons);
    }

    private GeometryScope fieldScopeFor(HydronicHeating heating) {
        return new GeometryScope(heating.zones().stream()
                .map(HeatingZone::outline)
                .toList());
    }

    private List<HeatingZone> initialZones(
            Room room,
            HydronicHeating heating,
            List<Staircase> staircases,
            List<FloorOpening> floorOpenings,
            List<HeatingExclusionArea> heatingExclusionAreas
    ) {
        List<ExclusionRect> exclusions = exclusions(room, staircases, floorOpenings, heatingExclusionAreas);
        if (exclusions.isEmpty()) {
            return List.of(HeatingZone.create("Heizkreis 1", room.outline(), heating.layoutPattern()));
        }
        List<Double> xCoordinates = splitCoordinates(room.outline(), exclusions, true);
        List<Double> yCoordinates = splitCoordinates(room.outline(), exclusions, false);
        List<HeatingZone> zones = new ArrayList<>();
        for (int xIndex = 0; xIndex + 1 < xCoordinates.size(); xIndex++) {
            for (int yIndex = 0; yIndex + 1 < yCoordinates.size(); yIndex++) {
                double minX = xCoordinates.get(xIndex);
                double maxX = xCoordinates.get(xIndex + 1);
                double minY = yCoordinates.get(yIndex);
                double maxY = yCoordinates.get(yIndex + 1);
                if (maxX - minX <= EPSILON || maxY - minY <= EPSILON) {
                    continue;
                }
                PlanPoint center = new PlanPoint((minX + maxX) / 2.0, (minY + maxY) / 2.0);
                if (!containsPoint(room.outline(), center) || exclusions.stream().anyMatch(rect -> rect.contains(center))) {
                    continue;
                }
                zones.add(new HeatingZone(
                        UUID.randomUUID(),
                        "Heizkreis " + (zones.size() + 1),
                        rectangle(minX, minY, maxX, maxY),
                        heating.layoutPattern(),
                        false
                ));
            }
        }
        if (zones.isEmpty()) {
            return List.of(HeatingZone.create("Heizkreis 1", room.outline(), heating.layoutPattern()));
        }
        return renameZones(zones);
    }

    private List<ExclusionRect> exclusions(
            Room room,
            List<Staircase> staircases,
            List<FloorOpening> floorOpenings,
            List<HeatingExclusionArea> heatingExclusionAreas
    ) {
        Bounds roomBounds = bounds(room.outline());
        List<ExclusionRect> exclusions = new ArrayList<>();
        for (Staircase staircase : staircases) {
            ExclusionRect rectangle = new ExclusionRect(
                    Math.max(roomBounds.minX(), staircase.minX()),
                    Math.max(roomBounds.minY(), staircase.minY()),
                    Math.min(roomBounds.maxX(), staircase.maxX()),
                    Math.min(roomBounds.maxY(), staircase.maxY())
            );
            if (rectangle.width() <= EPSILON || rectangle.height() <= EPSILON) {
                continue;
            }
            if (containsPoint(room.outline(), rectangle.center())) {
                exclusions.add(rectangle);
            }
        }
        for (FloorOpening opening : floorOpenings) {
            if (!opening.roomId().equals(room.id())) {
                continue;
            }
            appendExclusion(room, roomBounds, exclusions, opening.minXMillimeters(), opening.minYMillimeters(),
                    opening.maxXMillimeters(), opening.maxYMillimeters());
        }
        for (HeatingExclusionArea area : heatingExclusionAreas) {
            if (!area.roomId().equals(room.id())) {
                continue;
            }
            appendExclusion(room, roomBounds, exclusions, area.minXMillimeters(), area.minYMillimeters(),
                    area.maxXMillimeters(), area.maxYMillimeters());
        }
        return List.copyOf(exclusions);
    }

    private void appendExclusion(
            Room room,
            Bounds roomBounds,
            List<ExclusionRect> exclusions,
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {
        ExclusionRect rectangle = new ExclusionRect(
                Math.max(roomBounds.minX(), minX),
                Math.max(roomBounds.minY(), minY),
                Math.min(roomBounds.maxX(), maxX),
                Math.min(roomBounds.maxY(), maxY)
        );
        if (rectangle.width() <= EPSILON || rectangle.height() <= EPSILON) {
            return;
        }
        if (containsPoint(room.outline(), rectangle.center())) {
            exclusions.add(rectangle);
        }
    }

    private List<Double> splitCoordinates(List<PlanPoint> outline, List<ExclusionRect> exclusions, boolean xAxis) {
        List<Double> coordinates = new ArrayList<>();
        outline.stream()
                .mapToDouble(point -> xAxis ? point.xMillimeters() : point.yMillimeters())
                .forEach(coordinates::add);
        for (ExclusionRect exclusion : exclusions) {
            coordinates.add(xAxis ? exclusion.minX() : exclusion.minY());
            coordinates.add(xAxis ? exclusion.maxX() : exclusion.maxY());
        }
        coordinates.sort(Double::compareTo);
        List<Double> distinct = new ArrayList<>();
        for (double coordinate : coordinates) {
            if (distinct.isEmpty() || Math.abs(distinct.getLast() - coordinate) > EPSILON) {
                distinct.add(coordinate);
            }
        }
        return List.copyOf(distinct);
    }

    private boolean strictlyInsideAny(List<List<PlanPoint>> polygons, PlanPoint point) {
        return polygons.stream().anyMatch(polygon -> containsPoint(polygon, point) && !onBoundary(polygon, point));
    }

    private boolean onBoundary(List<PlanPoint> polygon, PlanPoint point) {
        for (int index = 0; index < polygon.size(); index++) {
            if (pointOnSegment(point, polygon.get(index), polygon.get((index + 1) % polygon.size()))) {
                return true;
            }
        }
        return false;
    }

    private List<PlanPoint> connectorCorridor(List<List<PlanPoint>> polygons, List<PlanPoint> ports, double pitch) {
        List<PlanPoint> zonePoints = polygons.stream()
                .flatMap(List::stream)
                .toList();
        Bounds zoneBounds = bounds(zonePoints);
        Bounds portBounds = bounds(ports);
        double centerX = (portBounds.minX() + portBounds.maxX()) / 2.0;
        double centerY = (portBounds.minY() + portBounds.maxY()) / 2.0;
        double freeMinX = centerX - MANIFOLD_FREE_AREA_WIDTH_MILLIMETERS / 2.0;
        double freeMaxX = centerX + MANIFOLD_FREE_AREA_WIDTH_MILLIMETERS / 2.0;
        double freeMinY = centerY - MANIFOLD_FREE_AREA_HEIGHT_MILLIMETERS / 2.0;
        double freeMaxY = centerY + MANIFOLD_FREE_AREA_HEIGHT_MILLIMETERS / 2.0;
        double padding = Math.max(pitch, MANIFOLD_PAIR_PITCH_MILLIMETERS);
        return rectangle(
                Math.min(zoneBounds.minX(), freeMinX) - padding,
                Math.min(zoneBounds.minY(), freeMinY) - padding,
                Math.max(zoneBounds.maxX(), freeMaxX) + padding,
                Math.max(zoneBounds.maxY(), freeMaxY) + padding
        );
    }

    private Optional<HydronicHeating> repairBySplittingUnroutableZone(HydronicHeating heating, Set<UUID> unroutableZoneIds) {
        Optional<HeatingZone> zoneToSplit = heating.zones().stream()
                .filter(zone -> unroutableZoneIds.contains(zone.id()))
                .max(Comparator.comparingDouble(HeatingZone::areaSquareMillimeters));
        if (zoneToSplit.isEmpty()) {
            return Optional.empty();
        }
        List<HeatingZone> split = split(zoneToSplit.orElseThrow());
        if (split.size() < 2 || heating.zones().size() + split.size() - 1 > MAXIMUM_ZONE_COUNT) {
            return Optional.empty();
        }
        List<HeatingZone> zones = new ArrayList<>(heating.zones());
        int index = zones.indexOf(zoneToSplit.orElseThrow());
        zones.remove(index);
        zones.addAll(index, split);
        return Optional.of(heating.withZones(renameZones(zones)));
    }

    private Optional<PlanningResult> tryRelocatedManifold(
            Room room,
            HydronicHeating heating,
            List<ValidationIssue> existingWarnings
    ) {
        for (ManifoldPair candidate : manifoldCandidates(room, heating)) {
            if (samePoint(candidate.supplyPort(), heating.supplyPoint())
                    && samePoint(candidate.returnPort(), heating.returnPoint())) {
                continue;
            }
            HydronicHeating relocated = optimizeFlowOrientation(withConnectionPoints(
                    heating,
                    candidate.supplyPort(),
                    candidate.returnPort()
            ));
            LayoutComputation computation = compute(relocated);
            if (computation.report().valid() && !maximumPipeLengthExceeded(relocated, computation.circuits())) {
                List<ValidationIssue> warnings = new ArrayList<>(existingWarnings);
                warnings.add(new ValidationIssue(
                        ValidationErrorType.AUTOMATIC_REPAIR,
                        "HKV wurde automatisch von "
                                + format(heating.supplyPoint()) + " / " + format(heating.returnPoint())
                                + " nach "
                                + format(relocated.supplyPoint()) + " / " + format(relocated.returnPoint())
                                + " verschoben, weil die ursprünglichen Anschlüsse nicht kreuzungsfrei erreichbar waren."
                ));
                return Optional.of(new PlanningResult(
                        relocated,
                        computation.circuits(),
                        new ValidationReport(true, List.of(), warnings)
                ));
            }
        }
        return Optional.empty();
    }

    private List<ManifoldPair> manifoldCandidates(Room room, HydronicHeating heating) {
        Bounds bounds = bounds(room.outline());
        double offset = Math.max(heating.pipeSpacing().toMillimeters(), 100.0);
        double pairWidth = Math.max(MANIFOLD_PAIR_PITCH_MILLIMETERS, heating.pipeSpacing().toMillimeters());
        double centerX = (bounds.minX() + bounds.maxX()) / 2.0;
        double centerY = (bounds.minY() + bounds.maxY()) / 2.0;
        return List.of(
                new ManifoldPair(
                        new PlanPoint(centerX - pairWidth / 2.0, bounds.minY() - offset),
                        new PlanPoint(centerX + pairWidth / 2.0, bounds.minY() - offset)
                ),
                new ManifoldPair(
                        new PlanPoint(centerX - pairWidth / 2.0, bounds.maxY() + offset),
                        new PlanPoint(centerX + pairWidth / 2.0, bounds.maxY() + offset)
                ),
                new ManifoldPair(
                        new PlanPoint(bounds.minX() - offset, centerY - pairWidth / 2.0),
                        new PlanPoint(bounds.minX() - offset, centerY + pairWidth / 2.0)
                ),
                new ManifoldPair(
                        new PlanPoint(bounds.maxX() + offset, centerY - pairWidth / 2.0),
                        new PlanPoint(bounds.maxX() + offset, centerY + pairWidth / 2.0)
                )
        );
    }

    private HydronicHeating withConnectionPoints(HydronicHeating heating, PlanPoint supplyPoint, PlanPoint returnPoint) {
        return new HydronicHeating(
                heating.id(),
                heating.roomId(),
                heating.surfacePosition(),
                heating.layoutPattern(),
                heating.pipeSpacing(),
                heating.pipeDiameter(),
                heating.maximumPipeLength(),
                heating.wallClearance(),
                supplyPoint,
                returnPoint,
                heating.zones()
        );
    }

    private PlanningResult partialPlanning(
            HydronicHeating planned,
            LayoutComputation computation,
            List<ValidationIssue> existingWarnings
    ) {
        List<ValidationIssue> warnings = new ArrayList<>(existingWarnings);
        HydronicHeating current = planned;
        LayoutComputation currentComputation = computation;
        while (!current.zones().isEmpty()) {
            if (currentComputation.report().valid()) {
                return new PlanningResult(current, currentComputation.circuits(), new ValidationReport(true, List.of(), warnings));
            }
            Set<UUID> failedZoneIds = new LinkedHashSet<>(currentComputation.unroutableZoneIds());
            if (failedZoneIds.isEmpty()) {
                failedZoneIds.add(current.zones().getLast().id());
            }
            Map<UUID, HeatingZone> zonesById = current.zones().stream()
                    .collect(java.util.stream.Collectors.toMap(HeatingZone::id, zone -> zone));
            for (UUID failedZoneId : failedZoneIds) {
                HeatingZone failedZone = zonesById.get(failedZoneId);
                if (failedZone == null) {
                    continue;
                }
                String reason = currentComputation.unroutableZoneIds().contains(failedZoneId)
                        ? "kein kreuzungsfreier Anschlussweg vom HKV zum Heizfeld gefunden wurde"
                        : "die Restplanung sonst weiterhin Geometriekonflikte enthält: " + currentComputation.report().summary();
                warnings.add(new ValidationIssue(
                        ValidationErrorType.PARTIAL_LAYOUT,
                        "Heizkreis `" + failedZone.name() + "` wurde ausgelassen, weil " + reason + "."
                ));
            }
            Set<UUID> currentFailedZoneIds = Set.copyOf(failedZoneIds);
            List<HeatingZone> remainingZones = current.zones().stream()
                    .filter(zone -> !currentFailedZoneIds.contains(zone.id()))
                    .toList();
            current = current.withZones(remainingZones);
            if (current.zones().isEmpty()) {
                return new PlanningResult(current, List.of(), new ValidationReport(true, List.of(), warnings));
            }
            currentComputation = compute(current);
        }
        return new PlanningResult(current, List.of(), new ValidationReport(true, List.of(), warnings));
    }

    private boolean maximumPipeLengthExceeded(HydronicHeating heating, List<CircuitLayout> circuits) {
        return circuits.stream().anyMatch(circuit -> circuit.pipeLength().compareTo(heating.maximumPipeLength()) > 0);
    }

    private ValidationReport withWarnings(ValidationReport report, List<ValidationIssue> warnings) {
        if (warnings.isEmpty()) {
            return report;
        }
        List<ValidationIssue> combinedWarnings = new ArrayList<>(report.warnings());
        combinedWarnings.addAll(warnings);
        return new ValidationReport(report.valid(), report.errors(), combinedWarnings);
    }

    public HydronicHeating optimizeFlowOrientation(HydronicHeating heating) {
        Objects.requireNonNull(heating, "heating darf nicht null sein.");
        if (heating.zones().size() < 2) {
            return heating;
        }
        List<SharedBoundary> boundaries = sharedBoundaries(heating.zones());
        if (boundaries.isEmpty()) {
            return heating;
        }
        int zoneCount = heating.zones().size();
        boolean[] inverted = zoneCount <= 12
                ? bruteForceFlowOrientation(heating, boundaries)
                : greedyFlowOrientation(heating, boundaries);
        List<HeatingZone> zones = new ArrayList<>();
        for (int index = 0; index < zoneCount; index++) {
            zones.add(heating.zones().get(index).withFlowInverted(inverted[index]));
        }
        return heating.withZones(zones);
    }

    private boolean[] bruteForceFlowOrientation(HydronicHeating heating, List<SharedBoundary> boundaries) {
        int zoneCount = heating.zones().size();
        int maximumMask = 1 << zoneCount;
        double bestScore = Double.POSITIVE_INFINITY;
        int bestMask = currentFlowMask(heating.zones());
        for (int mask = 0; mask < maximumMask; mask++) {
            double score = orientationScore(heating, boundaries, mask);
            if (score < bestScore) {
                bestScore = score;
                bestMask = mask;
            }
        }
        return maskToInversions(zoneCount, bestMask);
    }

    private boolean[] greedyFlowOrientation(HydronicHeating heating, List<SharedBoundary> boundaries) {
        boolean[] inverted = new boolean[heating.zones().size()];
        for (int index = 0; index < heating.zones().size(); index++) {
            inverted[index] = heating.zones().get(index).flowInverted();
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            double current = orientationScore(heating, boundaries, inverted);
            for (int index = 0; index < inverted.length; index++) {
                inverted[index] = !inverted[index];
                double candidate = orientationScore(heating, boundaries, inverted);
                if (candidate + EPSILON < current) {
                    current = candidate;
                    changed = true;
                } else {
                    inverted[index] = !inverted[index];
                }
            }
        }
        return inverted;
    }

    private int currentFlowMask(List<HeatingZone> zones) {
        int mask = 0;
        for (int index = 0; index < zones.size(); index++) {
            if (zones.get(index).flowInverted()) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    private boolean[] maskToInversions(int zoneCount, int mask) {
        boolean[] inverted = new boolean[zoneCount];
        for (int index = 0; index < zoneCount; index++) {
            inverted[index] = (mask & (1 << index)) != 0;
        }
        return inverted;
    }

    private double orientationScore(HydronicHeating heating, List<SharedBoundary> boundaries, int mask) {
        return orientationScore(heating, boundaries, maskToInversions(heating.zones().size(), mask));
    }

    private double orientationScore(HydronicHeating heating, List<SharedBoundary> boundaries, boolean[] inverted) {
        List<FieldPattern> patterns = new ArrayList<>();
        for (int index = 0; index < heating.zones().size(); index++) {
            patterns.add(createPattern(heating, heating.zones().get(index).withFlowInverted(inverted[index])));
        }
        double score = 0.0;
        for (SharedBoundary boundary : boundaries) {
            PipeRole firstRole = roleNearBoundary(patterns.get(boundary.firstIndex()), boundary, heating.pipeSpacing().toMillimeters());
            PipeRole secondRole = roleNearBoundary(patterns.get(boundary.secondIndex()), boundary, heating.pipeSpacing().toMillimeters());
            if (firstRole != null && firstRole == secondRole) {
                score += 1_000.0;
            }
        }
        for (int index = 0; index < heating.zones().size(); index++) {
            if (inverted[index] != heating.zones().get(index).flowInverted()) {
                score += 1.0;
            }
        }
        return score;
    }

    private PipeRole roleNearBoundary(FieldPattern pattern, SharedBoundary boundary, double pitch) {
        double supplyScore = roleScore(pattern.supplyPath(), boundary, pitch);
        double returnScore = roleScore(pattern.returnPath(), boundary, pitch);
        if (supplyScore <= EPSILON && returnScore <= EPSILON) {
            return null;
        }
        return supplyScore >= returnScore ? PipeRole.SUPPLY : PipeRole.RETURN;
    }

    private double roleScore(List<PlanPoint> path, SharedBoundary boundary, double pitch) {
        double score = 0.0;
        for (int index = 1; index < path.size(); index++) {
            score += segmentBoundaryScore(path.get(index - 1), path.get(index), boundary, pitch);
        }
        return score;
    }

    private double segmentBoundaryScore(PlanPoint start, PlanPoint end, SharedBoundary boundary, double pitch) {
        if (boundary.vertical()) {
            if (Math.abs(start.xMillimeters() - end.xMillimeters()) > EPSILON) {
                return 0.0;
            }
            double distance = Math.abs(start.xMillimeters() - boundary.coordinate());
            if (distance - pitch > EPSILON) {
                return 0.0;
            }
            double overlap = overlapLength(start.yMillimeters(), end.yMillimeters(), boundary.start(), boundary.end());
            return overlap / (1.0 + distance);
        }
        if (Math.abs(start.yMillimeters() - end.yMillimeters()) > EPSILON) {
            return 0.0;
        }
        double distance = Math.abs(start.yMillimeters() - boundary.coordinate());
        if (distance - pitch > EPSILON) {
            return 0.0;
        }
        double overlap = overlapLength(start.xMillimeters(), end.xMillimeters(), boundary.start(), boundary.end());
        return overlap / (1.0 + distance);
    }

    private List<SharedBoundary> sharedBoundaries(List<HeatingZone> zones) {
        List<SharedBoundary> boundaries = new ArrayList<>();
        for (int firstIndex = 0; firstIndex < zones.size(); firstIndex++) {
            for (int secondIndex = firstIndex + 1; secondIndex < zones.size(); secondIndex++) {
                boundaries.addAll(sharedBoundaries(firstIndex, zones.get(firstIndex), secondIndex, zones.get(secondIndex)));
            }
        }
        return boundaries;
    }

    private List<SharedBoundary> sharedBoundaries(
            int firstIndex,
            HeatingZone first,
            int secondIndex,
            HeatingZone second
    ) {
        List<SharedBoundary> boundaries = new ArrayList<>();
        for (int firstEdge = 0; firstEdge < first.outline().size(); firstEdge++) {
            PlanPoint firstStart = first.outline().get(firstEdge);
            PlanPoint firstEnd = first.outline().get((firstEdge + 1) % first.outline().size());
            for (int secondEdge = 0; secondEdge < second.outline().size(); secondEdge++) {
                PlanPoint secondStart = second.outline().get(secondEdge);
                PlanPoint secondEnd = second.outline().get((secondEdge + 1) % second.outline().size());
                sharedBoundary(firstIndex, firstStart, firstEnd, secondIndex, secondStart, secondEnd)
                        .ifPresent(boundaries::add);
            }
        }
        return boundaries;
    }

    private Optional<SharedBoundary> sharedBoundary(
            int firstIndex,
            PlanPoint firstStart,
            PlanPoint firstEnd,
            int secondIndex,
            PlanPoint secondStart,
            PlanPoint secondEnd
    ) {
        if (Math.abs(firstStart.xMillimeters() - firstEnd.xMillimeters()) < EPSILON
                && Math.abs(secondStart.xMillimeters() - secondEnd.xMillimeters()) < EPSILON
                && Math.abs(firstStart.xMillimeters() - secondStart.xMillimeters()) < EPSILON) {
            double start = Math.max(Math.min(firstStart.yMillimeters(), firstEnd.yMillimeters()), Math.min(secondStart.yMillimeters(), secondEnd.yMillimeters()));
            double end = Math.min(Math.max(firstStart.yMillimeters(), firstEnd.yMillimeters()), Math.max(secondStart.yMillimeters(), secondEnd.yMillimeters()));
            if (end - start > EPSILON) {
                return Optional.of(new SharedBoundary(firstIndex, secondIndex, true, firstStart.xMillimeters(), start, end));
            }
        }
        if (Math.abs(firstStart.yMillimeters() - firstEnd.yMillimeters()) < EPSILON
                && Math.abs(secondStart.yMillimeters() - secondEnd.yMillimeters()) < EPSILON
                && Math.abs(firstStart.yMillimeters() - secondStart.yMillimeters()) < EPSILON) {
            double start = Math.max(Math.min(firstStart.xMillimeters(), firstEnd.xMillimeters()), Math.min(secondStart.xMillimeters(), secondEnd.xMillimeters()));
            double end = Math.min(Math.max(firstStart.xMillimeters(), firstEnd.xMillimeters()), Math.max(secondStart.xMillimeters(), secondEnd.xMillimeters()));
            if (end - start > EPSILON) {
                return Optional.of(new SharedBoundary(firstIndex, secondIndex, false, firstStart.yMillimeters(), start, end));
            }
        }
        return Optional.empty();
    }

    private double overlapLength(double firstStart, double firstEnd, double secondStart, double secondEnd) {
        double start = Math.max(Math.min(firstStart, firstEnd), Math.min(secondStart, secondEnd));
        double end = Math.min(Math.max(firstStart, firstEnd), Math.max(secondStart, secondEnd));
        return Math.max(0.0, end - start);
    }

    private FieldPattern createPattern(HydronicHeating heating, HeatingZone zone) {
        double clearance = heating.wallClearance().toMillimeters();
        double pitch = heating.pipeSpacing().toMillimeters();
        FieldPattern pattern = zone.layoutPattern() == HeatingLayoutPattern.SPIRAL
                ? spiralPattern(zone.outline(), clearance, pitch)
                : meanderPattern(zone.outline(), clearance, pitch);
        if (pattern.fullPath().isEmpty()) {
            PlanPoint center = centroid(zone.outline());
            return new FieldPattern(List.of(center), List.of(center), List.of(center));
        }
        return zone.flowInverted() ? pattern.inverted() : pattern;
    }

    private FieldPattern meanderPattern(List<PlanPoint> polygon, double clearance, double pitch) {
        Bounds bounds = bounds(polygon);
        boolean horizontal = bounds.height() > bounds.width();
        double minimumLane = snapUp((horizontal ? bounds.minY() : bounds.minX()) + clearance, pitch);
        double maximumLane = snapDown((horizontal ? bounds.maxY() : bounds.maxX()) - clearance, pitch);
        List<PlanPoint> path = new ArrayList<>();
        boolean reverse = false;
        for (double lane = minimumLane; lane <= maximumLane + EPSILON; lane += pitch) {
            List<Interval> intervals = scanlineIntervals(polygon, lane, horizontal);
            if (reverse) {
                intervals = intervals.reversed();
            }
            for (Interval interval : intervals) {
                double start = snapUp(interval.start() + clearance, pitch);
                double end = snapDown(interval.end() - clearance, pitch);
                if (end - start < pitch / 2.0) {
                    continue;
                }
                PlanPoint first = horizontal
                        ? new PlanPoint(reverse ? end : start, lane)
                        : new PlanPoint(lane, reverse ? end : start);
                PlanPoint second = horizontal
                        ? new PlanPoint(reverse ? start : end, lane)
                        : new PlanPoint(lane, reverse ? start : end);
                appendConnected(path, first, polygon, pitch);
                appendDistinct(path, second);
            }
            reverse = !reverse;
        }
        List<PlanPoint> simplified = simplifyPath(path);
        SplitPath split = splitByLength(simplified);
        return new FieldPattern(simplified, split.first(), split.second());
    }

    private FieldPattern spiralPattern(List<PlanPoint> polygon, double clearance, double pitch) {
        if (!isAxisAlignedRectangle(polygon)) {
            return meanderPattern(polygon, clearance, pitch);
        }
        Bounds bounds = bounds(polygon);
        double left = snapUp(bounds.minX() + clearance, pitch);
        double right = snapDown(bounds.maxX() - clearance, pitch);
        double top = snapUp(bounds.minY() + clearance, pitch);
        double bottom = snapDown(bounds.maxY() - clearance, pitch);
        if (right - left < pitch * 3.0 || bottom - top < pitch * 3.0) {
            return meanderPattern(polygon, clearance, pitch);
        }
        List<PlanPoint> supply = rectangularSpiral(left, right, top, bottom, pitch * 2.0);
        List<PlanPoint> ret = rectangularSpiral(left + pitch, right - pitch, top + pitch, bottom - pitch, pitch * 2.0);
        if (supply.size() < 2 || ret.size() < 2) {
            return meanderPattern(polygon, clearance, pitch);
        }
        List<PlanPoint> visibleSupply = new ArrayList<>(supply);
        appendConnected(visibleSupply, ret.getLast(), polygon, pitch);
        List<PlanPoint> full = new ArrayList<>(visibleSupply);
        for (PlanPoint point : ret.reversed()) {
            appendDistinct(full, point);
        }
        if (full.stream().anyMatch(point -> !containsPoint(polygon, point))) {
            return meanderPattern(polygon, clearance, pitch);
        }
        return new FieldPattern(simplifyPath(full), simplifyPath(visibleSupply), simplifyPath(ret.reversed()));
    }

    private List<PlanPoint> rectangularSpiral(double left, double right, double top, double bottom, double step) {
        List<PlanPoint> path = new ArrayList<>();
        double l = left;
        double r = right;
        double t = top;
        double b = bottom;
        appendDistinct(path, new PlanPoint(l, t));
        while (l <= r + EPSILON && t <= b + EPSILON) {
            appendDistinct(path, new PlanPoint(l, b));
            appendDistinct(path, new PlanPoint(r, b));
            appendDistinct(path, new PlanPoint(r, t));
            double nl = l + step;
            double nr = r - step;
            double nt = t + step;
            double nb = b - step;
            if (nl <= nr + EPSILON && nt <= nb + EPSILON) {
                appendDistinct(path, new PlanPoint(nl, t));
                appendDistinct(path, new PlanPoint(nl, nt));
            }
            l = nl;
            r = nr;
            t = nt;
            b = nb;
        }
        return simplifyPath(path);
    }

    private ConnectorPlan routeConnectors(
            GridGraph graph,
            GeometryScope scope,
            GeometryScope fieldScope,
            FieldPattern pattern,
            ManifoldPair pair,
            Set<GridEdge> blocked,
            double pitch
    ) {
        PlanPoint start = pattern.fullPath().getFirst();
        PlanPoint end = pattern.fullPath().getLast();
        List<PlanPoint> supply = routePath(graph, scope, fieldScope, pair.supplyPort(), start, blocked, pitch, false, pair.supplyPort());
        Set<GridEdge> returnBlocked = new LinkedHashSet<>(blocked);
        returnBlocked.addAll(edgesOf(supply, pitch));
        List<PlanPoint> ret = routePath(graph, scope, fieldScope, end, pair.returnPort(), returnBlocked, pitch, false, pair.returnPort());
        return new ConnectorPlan(pair, supply, ret);
    }

    private List<PlanPoint> routePath(
            GridGraph graph,
            GeometryScope scope,
            GeometryScope fieldScope,
            PlanPoint start,
            PlanPoint goal,
            Set<GridEdge> blocked,
            double pitch,
            boolean allowDirect,
            PlanPoint perimeterReference
    ) {
        if (samePoint(start, goal)) {
            return List.of(start);
        }
        if (!allowDirect && !blocked.isEmpty()) {
            List<PlanPoint> perimeter = perimeterConnector(start, goal, scope, fieldScope, blocked, pitch, perimeterReference);
            if (!perimeter.isEmpty()) {
                return perimeter;
            }
        }
        List<PlanPoint> direct = allowDirect ? directConnector(start, goal, scope) : List.of();
        if (!direct.isEmpty() && pathAvoidsBlocked(direct, blocked, pitch) && pathAvoidsInternalVoids(direct, fieldScope)) {
            return direct;
        }
        Set<GridPoint> forbiddenNodes = graph.blockedNodes(blocked, null, null);
        forbiddenNodes.removeIf(candidate -> samePoint(candidate.toPlanPoint(pitch), start)
                || samePoint(candidate.toPlanPoint(pitch), goal));
        Optional<GridPoint> startGrid = graph.nearest(start, scope, forbiddenNodes);
        Optional<GridPoint> goalGrid = graph.nearest(goal, scope, forbiddenNodes);
        if (startGrid.isEmpty() || goalGrid.isEmpty()) {
            throw new IllegalArgumentException("kein Rasterpunkt für Anschluss gefunden");
        }
        List<GridPoint> gridPath = graph.shortestPath(startGrid.orElseThrow(), goalGrid.orElseThrow(), blocked, true);
        if (gridPath.isEmpty()) {
            List<PlanPoint> perimeter = perimeterConnector(start, goal, scope, fieldScope, blocked, pitch, perimeterReference);
            if (!perimeter.isEmpty()) {
                return perimeter;
            }
            gridPath = graph.shortestPath(startGrid.orElseThrow(), goalGrid.orElseThrow(), blocked, false);
        }
        if (gridPath.isEmpty()) {
            throw new IllegalArgumentException("kein kreuzungsfreier Anschlussweg gefunden");
        }
        List<PlanPoint> path = new ArrayList<>();
        appendDistinct(path, start);
        for (GridPoint gridPoint : gridPath) {
            appendDistinct(path, gridPoint.toPlanPoint(pitch));
        }
        appendDistinct(path, goal);
        List<PlanPoint> result = simplifyPath(path);
        if (!pathAvoidsBlocked(result, blocked, pitch) || !pathAvoidsInternalVoids(result, fieldScope)) {
            throw new IllegalArgumentException("kein kreuzungsfreier Anschlussweg gefunden");
        }
        return result;
    }

    private List<PlanPoint> perimeterConnector(
            PlanPoint start,
            PlanPoint goal,
            GeometryScope scope,
            GeometryScope fieldScope,
            Set<GridEdge> blocked,
            double pitch,
            PlanPoint reference
    ) {
        Bounds bounds = scope.bounds();
        double minX = bounds.minX() + pitch / 2.0;
        double maxX = bounds.maxX() - pitch / 2.0;
        double minY = bounds.minY() + pitch / 2.0;
        double maxY = bounds.maxY() - pitch / 2.0;
        PlanPoint sideReference = reference == null ? centroid(List.of(start, goal, new PlanPoint(start.xMillimeters(), goal.yMillimeters()))) : reference;
        record Candidate(List<PlanPoint> path, double sideDistance) {
        }
        List<Candidate> candidates = List.of(
                new Candidate(List.of(start, new PlanPoint(start.xMillimeters(), minY), new PlanPoint(goal.xMillimeters(), minY), goal),
                        Math.abs(sideReference.yMillimeters() - minY)),
                new Candidate(List.of(start, new PlanPoint(start.xMillimeters(), maxY), new PlanPoint(goal.xMillimeters(), maxY), goal),
                        Math.abs(sideReference.yMillimeters() - maxY)),
                new Candidate(List.of(start, new PlanPoint(minX, start.yMillimeters()), new PlanPoint(minX, goal.yMillimeters()), goal),
                        Math.abs(sideReference.xMillimeters() - minX)),
                new Candidate(List.of(start, new PlanPoint(maxX, start.yMillimeters()), new PlanPoint(maxX, goal.yMillimeters()), goal),
                        Math.abs(sideReference.xMillimeters() - maxX))
        );
        return candidates.stream()
                .sorted(Comparator.comparingDouble(Candidate::sideDistance))
                .map(candidate -> simplifyPath(candidate.path()))
                .filter(candidate -> connectorInside(candidate, scope))
                .filter(candidate -> pathAvoidsInternalVoids(candidate, fieldScope))
                .filter(candidate -> pathAvoidsBlocked(candidate, blocked, pitch))
                .findFirst()
                .orElse(List.of());
    }

    private boolean connectorInside(List<PlanPoint> path, GeometryScope scope) {
        for (int index = 1; index < path.size(); index++) {
            if (!scope.containsSegment(path.get(index - 1), path.get(index))) {
                return false;
            }
        }
        return true;
    }

    private List<PlanPoint> directConnector(PlanPoint start, PlanPoint goal, GeometryScope scope) {
        if (axisAligned(start, goal) && scope.containsSegment(start, goal)) {
            return simplifyPath(List.of(start, goal));
        }
        PlanPoint firstCorner = new PlanPoint(start.xMillimeters(), goal.yMillimeters());
        if (scope.containsSegment(start, firstCorner) && scope.containsSegment(firstCorner, goal)) {
            return simplifyPath(List.of(start, firstCorner, goal));
        }
        PlanPoint secondCorner = new PlanPoint(goal.xMillimeters(), start.yMillimeters());
        if (scope.containsSegment(start, secondCorner) && scope.containsSegment(secondCorner, goal)) {
            return simplifyPath(List.of(start, secondCorner, goal));
        }
        return List.of();
    }

    private boolean edgesDisjoint(List<PlanPoint> path, Set<GridEdge> blocked, double pitch) {
        for (GridEdge edge : edgesOf(path, pitch)) {
            if (blocked.contains(edge)) {
                return false;
            }
        }
        return true;
    }

    private boolean pathAvoidsBlocked(List<PlanPoint> path, Set<GridEdge> blocked, double pitch) {
        if (!edgesDisjoint(path, blocked, pitch)) {
            return false;
        }
        for (int index = 1; index < path.size(); index++) {
            PlanPoint start = path.get(index - 1);
            PlanPoint end = path.get(index);
            for (GridEdge blockedEdge : blocked) {
                if (segmentsIntersect(
                        start,
                        end,
                        blockedEdge.a().toPlanPoint(pitch),
                        blockedEdge.b().toPlanPoint(pitch)
                )) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean pathAvoidsInternalVoids(List<PlanPoint> path, GeometryScope fieldScope) {
        Bounds bounds = fieldScope.bounds();
        for (int index = 1; index < path.size(); index++) {
            PlanPoint start = path.get(index - 1);
            PlanPoint end = path.get(index);
            for (int step = 0; step <= 32; step++) {
                double ratio = step / 32.0;
                PlanPoint point = new PlanPoint(
                        start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * ratio,
                        start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * ratio
                );
                if (insideBounds(bounds, point) && !fieldScope.contains(point)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean insideBounds(Bounds bounds, PlanPoint point) {
        return point.xMillimeters() > bounds.minX() + EPSILON
                && point.xMillimeters() < bounds.maxX() - EPSILON
                && point.yMillimeters() > bounds.minY() + EPSILON
                && point.yMillimeters() < bounds.maxY() - EPSILON;
    }

    private ManifoldPair manifoldPair(HydronicHeating heating, int index) {
        PlanPoint supply = heating.supplyPoint();
        PlanPoint ret = heating.returnPoint();
        double dx = ret.xMillimeters() - supply.xMillimeters();
        double dy = ret.yMillimeters() - supply.yMillimeters();
        double length = Math.hypot(dx, dy);
        double axisX;
        double axisY;
        if (length <= EPSILON) {
            axisX = 1.0;
            axisY = 0.0;
        } else {
            axisX = dx / length;
            axisY = dy / length;
        }
        double offset = index * Math.max(MANIFOLD_PAIR_PITCH_MILLIMETERS, heating.pipeSpacing().toMillimeters());
        return new ManifoldPair(
                new PlanPoint(supply.xMillimeters() + axisX * offset, supply.yMillimeters() + axisY * offset),
                new PlanPoint(ret.xMillimeters() + axisX * offset, ret.yMillimeters() + axisY * offset)
        );
    }

    private List<ValidationIssue> validateGeometry(
            GeometryScope scope,
            List<CircuitLayout> circuits,
            HydronicHeating heating
    ) {
        List<ValidationIssue> errors = new ArrayList<>();
        Map<GridEdge, PipeSegment> used = new HashMap<>();
        double pitch = heating.pipeSpacing().toMillimeters();
        for (CircuitLayout circuit : circuits) {
            validateContinuity(circuit, errors);
            for (PipeSegment segment : circuit.segments()) {
                if (!scope.containsSegment(segment.start(), segment.end())
                        && !segmentTouchesManifold(segment, circuit)) {
                    errors.add(new ValidationIssue(
                            ValidationErrorType.PIPE_OUTSIDE_ROOM,
                            "Rohrsegment liegt außerhalb des Raums: " + format(segment.start()) + " -> " + format(segment.end())
                    ));
                }
            }
            for (PipeSegment segment : circuit.segments()) {
                if (segment.role() != PipeRole.SUPPLY && segment.role() != PipeRole.RETURN) {
                    continue;
                }
                for (GridEdge edge : edgesOf(List.of(segment.start(), segment.end()), pitch)) {
                PipeSegment previous = used.putIfAbsent(edge, segment);
                if (previous != null) {
                    errors.add(new ValidationIssue(
                            ValidationErrorType.DUPLICATE_GRID_EDGE,
                            "Rasterrinne mehrfach belegt: " + edge
                                    + " vorher " + previous.role() + " " + format(previous.start()) + " -> " + format(previous.end())
                                    + " aktuell " + segment.role() + " " + format(segment.start()) + " -> " + format(segment.end())
                    ));
                }
                }
            }
        }
        validateIntersections(circuits, errors);
        return errors;
    }

    private boolean segmentTouchesManifold(PipeSegment segment, CircuitLayout circuit) {
        return samePoint(segment.start(), circuit.supplyPort())
                || samePoint(segment.end(), circuit.supplyPort())
                || samePoint(segment.start(), circuit.returnPort())
                || samePoint(segment.end(), circuit.returnPort());
    }

    private void validateContinuity(CircuitLayout circuit, List<ValidationIssue> errors) {
        if (!samePoint(circuit.supplyConnectorPath().getFirst(), circuit.supplyPort())
                || !samePoint(circuit.supplyConnectorPath().getLast(), circuit.fieldSupplyPath().getFirst())) {
            errors.add(new ValidationIssue(ValidationErrorType.CONNECTOR_NOT_CONNECTED, "Vorlauf ist nicht durchgehend angebunden."));
        }
        if (!samePoint(circuit.returnConnectorPath().getFirst(), circuit.fieldReturnPath().getLast())
                || !samePoint(circuit.returnConnectorPath().getLast(), circuit.returnPort())) {
            errors.add(new ValidationIssue(ValidationErrorType.CONNECTOR_NOT_CONNECTED, "Rücklauf ist nicht durchgehend angebunden."));
        }
    }

    private void validateIntersections(List<CircuitLayout> circuits, List<ValidationIssue> errors) {
        List<IndexedSegment> segments = new ArrayList<>();
        for (int circuitIndex = 0; circuitIndex < circuits.size(); circuitIndex++) {
            List<PipeSegment> pipeSegments = circuits.get(circuitIndex).segments();
            for (int segmentIndex = 0; segmentIndex < pipeSegments.size(); segmentIndex++) {
                segments.add(new IndexedSegment(circuitIndex, segmentIndex, pipeSegments.get(segmentIndex)));
            }
        }
        for (int firstIndex = 0; firstIndex < segments.size(); firstIndex++) {
            IndexedSegment first = segments.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < segments.size(); secondIndex++) {
                IndexedSegment second = segments.get(secondIndex);
                if (allowedTouch(first, second)) {
                    continue;
                }
                if (segmentsIntersect(first.segment().start(), first.segment().end(), second.segment().start(), second.segment().end())) {
                    errors.add(new ValidationIssue(
                            ValidationErrorType.GEOMETRIC_INTERSECTION,
                            "Rohrsegmente schneiden sich geometrisch: "
                                    + first.segment().role() + " " + format(first.segment().start()) + " -> " + format(first.segment().end())
                                    + " / "
                                    + second.segment().role() + " " + format(second.segment().start()) + " -> " + format(second.segment().end())
                    ));
                    return;
                }
            }
        }
    }

    private boolean allowedTouch(IndexedSegment first, IndexedSegment second) {
        if (isConnector(first.segment()) && isConnector(second.segment())) {
            return true;
        }
        if (first.circuitIndex() == second.circuitIndex() && Math.abs(first.segmentIndex() - second.segmentIndex()) <= 1) {
            return true;
        }
        if (first.circuitIndex() == second.circuitIndex()
                && touchesAtOwnPathPoint(first.segment(), second.segment())) {
            return true;
        }
        return sharedEndpoint(first.segment(), second.segment())
                && first.circuitIndex() == second.circuitIndex();
    }

    private boolean isConnector(PipeSegment segment) {
        return segment.role() == PipeRole.SUPPLY_CONNECTOR || segment.role() == PipeRole.RETURN_CONNECTOR;
    }

    private boolean touchesAtOwnPathPoint(PipeSegment first, PipeSegment second) {
        return pointOnSegment(first.start(), second.start(), second.end())
                || pointOnSegment(first.end(), second.start(), second.end())
                || pointOnSegment(second.start(), first.start(), first.end())
                || pointOnSegment(second.end(), first.start(), first.end());
    }

    private boolean sharedEndpoint(PipeSegment first, PipeSegment second) {
        return samePoint(first.start(), second.start())
                || samePoint(first.start(), second.end())
                || samePoint(first.end(), second.start())
                || samePoint(first.end(), second.end());
    }

    private boolean segmentsIntersect(PlanPoint a, PlanPoint b, PlanPoint c, PlanPoint d) {
        if (samePoint(a, c) || samePoint(a, d) || samePoint(b, c) || samePoint(b, d)) {
            return false;
        }
        double first = orientation(a, b, c);
        double second = orientation(a, b, d);
        double third = orientation(c, d, a);
        double fourth = orientation(c, d, b);
        if (Math.abs(first) < EPSILON && pointOnSegment(c, a, b)) return true;
        if (Math.abs(second) < EPSILON && pointOnSegment(d, a, b)) return true;
        if (Math.abs(third) < EPSILON && pointOnSegment(a, c, d)) return true;
        if (Math.abs(fourth) < EPSILON && pointOnSegment(b, c, d)) return true;
        return first * second < -EPSILON && third * fourth < -EPSILON;
    }

    private double orientation(PlanPoint first, PlanPoint second, PlanPoint third) {
        return (second.xMillimeters() - first.xMillimeters()) * (third.yMillimeters() - first.yMillimeters())
                - (second.yMillimeters() - first.yMillimeters()) * (third.xMillimeters() - first.xMillimeters());
    }

    private List<HeatingZone> renameZones(List<HeatingZone> zones) {
        List<HeatingZone> namedZones = new ArrayList<>();
        for (int index = 0; index < zones.size(); index++) {
            namedZones.add(zones.get(index).withName("Heizkreis " + (index + 1)));
        }
        return namedZones;
    }

    private List<HeatingZone> split(HeatingZone zone) {
        Bounds bounds = bounds(zone.outline());
        boolean splitVertically = bounds.width() >= 1_200.0 || bounds.width() >= bounds.height();
        double splitCoordinate = splitVertically
                ? snapTo((bounds.minX() + bounds.maxX()) / 2.0, 100.0)
                : snapTo((bounds.minY() + bounds.maxY()) / 2.0, 100.0);
        List<PlanPoint> first = clip(zone.outline(), splitVertically, splitCoordinate, true);
        List<PlanPoint> second = clip(zone.outline(), splitVertically, splitCoordinate, false);
        if (first.size() < 3 || second.size() < 3) {
            return List.of(zone);
        }
        return List.of(
                new HeatingZone(UUID.randomUUID(), zone.name(), first, zone.layoutPattern(), zone.flowInverted()),
                new HeatingZone(UUID.randomUUID(), zone.name(), second, zone.layoutPattern(), zone.flowInverted())
        );
    }

    private List<PlanPoint> clip(List<PlanPoint> polygon, boolean xAxis, double boundary, boolean keepLower) {
        List<PlanPoint> output = new ArrayList<>(polygon);
        List<PlanPoint> input = new ArrayList<>(output);
        output.clear();
        if (input.isEmpty()) {
            return List.of();
        }
        PlanPoint previous = input.getLast();
        for (PlanPoint current : input) {
            boolean currentInside = insideBoundary(current, xAxis, boundary, keepLower);
            boolean previousInside = insideBoundary(previous, xAxis, boundary, keepLower);
            if (currentInside != previousInside) {
                output.add(boundaryIntersection(previous, current, xAxis, boundary));
            }
            if (currentInside) {
                output.add(current);
            }
            previous = current;
        }
        return simplifyPolygon(output);
    }

    private boolean insideBoundary(PlanPoint point, boolean xAxis, double boundary, boolean keepLower) {
        double coordinate = xAxis ? point.xMillimeters() : point.yMillimeters();
        return keepLower ? coordinate <= boundary + EPSILON : coordinate >= boundary - EPSILON;
    }

    private PlanPoint boundaryIntersection(PlanPoint start, PlanPoint end, boolean xAxis, double boundary) {
        double startCoordinate = xAxis ? start.xMillimeters() : start.yMillimeters();
        double endCoordinate = xAxis ? end.xMillimeters() : end.yMillimeters();
        double ratio = Math.abs(endCoordinate - startCoordinate) < EPSILON
                ? 0.0
                : (boundary - startCoordinate) / (endCoordinate - startCoordinate);
        return new PlanPoint(
                xAxis ? boundary : start.xMillimeters() + ratio * (end.xMillimeters() - start.xMillimeters()),
                xAxis ? start.yMillimeters() + ratio * (end.yMillimeters() - start.yMillimeters()) : boundary
        );
    }

    private List<Interval> scanlineIntervals(List<PlanPoint> polygon, double lane, boolean horizontal) {
        List<Double> intersections = new ArrayList<>();
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint first = polygon.get(index);
            PlanPoint second = polygon.get((index + 1) % polygon.size());
            double firstCross = horizontal ? first.yMillimeters() : first.xMillimeters();
            double secondCross = horizontal ? second.yMillimeters() : second.xMillimeters();
            if ((firstCross > lane) == (secondCross > lane) || Math.abs(firstCross - secondCross) < EPSILON) {
                continue;
            }
            double ratio = (lane - firstCross) / (secondCross - firstCross);
            intersections.add(horizontal
                    ? first.xMillimeters() + ratio * (second.xMillimeters() - first.xMillimeters())
                    : first.yMillimeters() + ratio * (second.yMillimeters() - first.yMillimeters()));
        }
        intersections.sort(Double::compareTo);
        List<Interval> intervals = new ArrayList<>();
        for (int index = 0; index + 1 < intersections.size(); index += 2) {
            intervals.add(new Interval(intersections.get(index), intersections.get(index + 1)));
        }
        return intervals;
    }

    private void appendConnected(List<PlanPoint> path, PlanPoint target, List<PlanPoint> polygon, double pitch) {
        if (path.isEmpty()) {
            appendDistinct(path, target);
            return;
        }
        PlanPoint previous = path.getLast();
        if (segmentInside(previous, target, polygon)) {
            appendDistinct(path, target);
            return;
        }
        PlanPoint firstCorner = new PlanPoint(previous.xMillimeters(), target.yMillimeters());
        if (segmentInside(previous, firstCorner, polygon) && segmentInside(firstCorner, target, polygon)) {
            appendDistinct(path, firstCorner);
            appendDistinct(path, target);
            return;
        }
        PlanPoint secondCorner = new PlanPoint(target.xMillimeters(), previous.yMillimeters());
        if (segmentInside(previous, secondCorner, polygon) && segmentInside(secondCorner, target, polygon)) {
            appendDistinct(path, secondCorner);
            appendDistinct(path, target);
            return;
        }
        GeometryScope scope = new GeometryScope(List.of(polygon));
        GridGraph graph = GridGraph.create(scope, pitch);
        List<PlanPoint> connector = routePath(graph, scope, scope, previous, target, edgesOf(path, pitch), pitch, false, null);
        connector.stream().skip(1).forEach(point -> appendDistinct(path, point));
    }

    private List<PlanPoint> simplifyPolygon(List<PlanPoint> points) {
        List<PlanPoint> distinct = simplifyPath(points);
        boolean changed = true;
        while (changed && distinct.size() >= 3) {
            changed = false;
            for (int index = 0; index < distinct.size(); index++) {
                PlanPoint previous = distinct.get((index - 1 + distinct.size()) % distinct.size());
                PlanPoint current = distinct.get(index);
                PlanPoint next = distinct.get((index + 1) % distinct.size());
                double cross = orientation(previous, current, next);
                if (Math.abs(cross) < EPSILON) {
                    distinct.remove(index);
                    changed = true;
                    break;
                }
            }
        }
        return List.copyOf(distinct);
    }

    private List<PlanPoint> simplifyPath(List<PlanPoint> points) {
        List<PlanPoint> distinct = new ArrayList<>();
        for (PlanPoint point : points) {
            appendDistinct(distinct, point);
        }
        if (distinct.size() > 1 && samePoint(distinct.getFirst(), distinct.getLast())) {
            distinct.removeLast();
        }
        boolean changed = true;
        while (changed && distinct.size() >= 3) {
            changed = false;
            for (int index = 1; index + 1 < distinct.size(); index++) {
                PlanPoint previous = distinct.get(index - 1);
                PlanPoint current = distinct.get(index);
                PlanPoint next = distinct.get(index + 1);
                if (Math.abs(orientation(previous, current, next)) < EPSILON) {
                    distinct.remove(index);
                    changed = true;
                    break;
                }
            }
        }
        return List.copyOf(distinct);
    }

    private static List<PlanPoint> reversed(List<PlanPoint> points) {
        List<PlanPoint> result = new ArrayList<>(points);
        Collections.reverse(result);
        return List.copyOf(result);
    }

    private SplitPath splitByLength(List<PlanPoint> path) {
        if (path.size() < 2) {
            return new SplitPath(path, path);
        }
        double total = rawLength(path);
        double half = total / 2.0;
        double length = 0.0;
        int splitIndex = 1;
        for (; splitIndex < path.size(); splitIndex++) {
            double segmentLength = path.get(splitIndex - 1).distanceTo(path.get(splitIndex)).toMillimeters();
            if (length + segmentLength >= half) {
                break;
            }
            length += segmentLength;
        }
        List<PlanPoint> first = new ArrayList<>(path.subList(0, Math.min(path.size(), splitIndex + 1)));
        List<PlanPoint> second = new ArrayList<>(path.subList(Math.max(0, splitIndex), path.size()));
        return new SplitPath(List.copyOf(first), List.copyOf(second));
    }

    private double roundedLength(List<PlanPoint> path, double requestedRadius) {
        double length = rawLength(path);
        for (int index = 1; index + 1 < path.size(); index++) {
            PlanPoint previous = path.get(index - 1);
            PlanPoint current = path.get(index);
            PlanPoint next = path.get(index + 1);
            double firstLength = previous.distanceTo(current).toMillimeters();
            double secondLength = current.distanceTo(next).toMillimeters();
            double radius = Math.min(requestedRadius, Math.min(firstLength, secondLength) / 2.0);
            double firstX = current.xMillimeters() - previous.xMillimeters();
            double firstY = current.yMillimeters() - previous.yMillimeters();
            double secondX = next.xMillimeters() - current.xMillimeters();
            double secondY = next.yMillimeters() - current.yMillimeters();
            double denominator = Math.max(EPSILON, firstLength * secondLength);
            double cosine = Math.max(-1.0, Math.min(1.0, (firstX * secondX + firstY * secondY) / denominator));
            double angle = Math.acos(cosine);
            if (angle > EPSILON && angle < Math.PI - EPSILON) {
                length += radius * angle - 2.0 * radius * Math.tan(angle / 2.0);
            }
        }
        return Math.max(0.0, length);
    }

    private double rawLength(List<PlanPoint> path) {
        double length = 0.0;
        for (int index = 1; index < path.size(); index++) {
            length += path.get(index - 1).distanceTo(path.get(index)).toMillimeters();
        }
        return length;
    }

    private List<PipeSegment> segments(UUID zoneId, ConnectorPlan connectors, FieldPattern pattern) {
        List<PipeSegment> segments = new ArrayList<>();
        addSegments(segments, zoneId, connectors.supplyPath(), PipeRole.SUPPLY_CONNECTOR);
        addSegments(segments, zoneId, pattern.supplyPath(), PipeRole.SUPPLY);
        addSegments(segments, zoneId, pattern.returnPath(), PipeRole.RETURN);
        addSegments(segments, zoneId, connectors.returnPath(), PipeRole.RETURN_CONNECTOR);
        return List.copyOf(segments);
    }

    private void addSegments(List<PipeSegment> segments, UUID zoneId, List<PlanPoint> path, PipeRole role) {
        for (int index = 1; index < path.size(); index++) {
            segments.add(new PipeSegment(zoneId, role, path.get(index - 1), path.get(index)));
        }
    }

    private Set<GridEdge> edgesOf(List<PlanPoint> path, double pitch) {
        Set<GridEdge> edges = new LinkedHashSet<>();
        for (int index = 1; index < path.size(); index++) {
            PlanPoint start = path.get(index - 1);
            PlanPoint end = path.get(index);
            if (!axisAligned(start, end)) {
                continue;
            }
            if (Math.abs(start.xMillimeters() - end.xMillimeters()) < EPSILON) {
                if (!onGrid(start.xMillimeters(), pitch)
                        || !onGrid(start.yMillimeters(), pitch)
                        || !onGrid(end.yMillimeters(), pitch)) {
                    continue;
                }
            } else if (!onGrid(start.yMillimeters(), pitch)
                    || !onGrid(start.xMillimeters(), pitch)
                    || !onGrid(end.xMillimeters(), pitch)) {
                continue;
            }
            int sx = toGrid(start.xMillimeters(), pitch);
            int sy = toGrid(start.yMillimeters(), pitch);
            int ex = toGrid(end.xMillimeters(), pitch);
            int ey = toGrid(end.yMillimeters(), pitch);
            int dx = Integer.compare(ex, sx);
            int dy = Integer.compare(ey, sy);
            int steps = Math.max(Math.abs(ex - sx), Math.abs(ey - sy));
            for (int step = 0; step < steps; step++) {
                GridPoint a = new GridPoint(sx + dx * step, sy + dy * step);
                GridPoint b = new GridPoint(sx + dx * (step + 1), sy + dy * (step + 1));
                edges.add(new GridEdge(a, b));
            }
        }
        return edges;
    }

    private boolean onGrid(double coordinate, double pitch) {
        return Math.abs(coordinate - Math.round(coordinate / pitch) * pitch) <= EPSILON;
    }

    private boolean axisAligned(PlanPoint start, PlanPoint end) {
        return Math.abs(start.xMillimeters() - end.xMillimeters()) < EPSILON
                || Math.abs(start.yMillimeters() - end.yMillimeters()) < EPSILON;
    }

    private int toGrid(double coordinate, double pitch) {
        return (int) Math.round(coordinate / pitch);
    }

    private boolean segmentInside(PlanPoint start, PlanPoint end, List<PlanPoint> polygon) {
        for (int step = 0; step <= 32; step++) {
            double ratio = step / 32.0;
            PlanPoint point = new PlanPoint(
                    start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * ratio,
                    start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * ratio
            );
            if (!containsPoint(polygon, point)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsPoint(List<PlanPoint> polygon, PlanPoint point) {
        boolean inside = false;
        int previousIndex = polygon.size() - 1;
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint current = polygon.get(index);
            PlanPoint previous = polygon.get(previousIndex);
            if (pointOnSegment(point, previous, current)) {
                return true;
            }
            boolean intersects = (current.yMillimeters() > point.yMillimeters()) != (previous.yMillimeters() > point.yMillimeters())
                    && point.xMillimeters() < (previous.xMillimeters() - current.xMillimeters())
                    * (point.yMillimeters() - current.yMillimeters())
                    / (previous.yMillimeters() - current.yMillimeters())
                    + current.xMillimeters();
            if (intersects) {
                inside = !inside;
            }
            previousIndex = index;
        }
        return inside;
    }

    private boolean pointOnSegment(PlanPoint point, PlanPoint start, PlanPoint end) {
        double cross = orientation(start, end, point);
        if (Math.abs(cross) > EPSILON) {
            return false;
        }
        return point.xMillimeters() >= Math.min(start.xMillimeters(), end.xMillimeters()) - EPSILON
                && point.xMillimeters() <= Math.max(start.xMillimeters(), end.xMillimeters()) + EPSILON
                && point.yMillimeters() >= Math.min(start.yMillimeters(), end.yMillimeters()) - EPSILON
                && point.yMillimeters() <= Math.max(start.yMillimeters(), end.yMillimeters()) + EPSILON;
    }

    private boolean isAxisAlignedRectangle(List<PlanPoint> polygon) {
        if (polygon.size() != 4) {
            return false;
        }
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint start = polygon.get(index);
            PlanPoint end = polygon.get((index + 1) % polygon.size());
            if (!axisAligned(start, end)) {
                return false;
            }
        }
        return true;
    }

    private List<PlanPoint> concatenate(List<PlanPoint> first, List<PlanPoint> second, List<PlanPoint> third) {
        List<PlanPoint> result = new ArrayList<>();
        first.forEach(point -> appendDistinct(result, point));
        second.forEach(point -> appendDistinct(result, point));
        third.forEach(point -> appendDistinct(result, point));
        return List.copyOf(result);
    }

    private PlanPoint centroid(List<PlanPoint> polygon) {
        return new PlanPoint(
                polygon.stream().mapToDouble(PlanPoint::xMillimeters).average().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::yMillimeters).average().orElse(0.0)
        );
    }

    private Bounds bounds(List<PlanPoint> polygon) {
        return new Bounds(
                polygon.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                polygon.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
        );
    }

    private List<PlanPoint> rectangle(double minX, double minY, double maxX, double maxY) {
        return List.of(
                new PlanPoint(minX, minY),
                new PlanPoint(maxX, minY),
                new PlanPoint(maxX, maxY),
                new PlanPoint(minX, maxY)
        );
    }

    private void appendDistinct(List<PlanPoint> points, PlanPoint point) {
        if (points.isEmpty() || !samePoint(points.getLast(), point)) {
            points.add(point);
        }
    }

    private boolean samePoint(PlanPoint first, PlanPoint second) {
        return first.distanceTo(second).toMillimeters() <= EPSILON;
    }

    private double snapUp(double coordinate, double pitch) {
        return Math.ceil((coordinate - EPSILON) / pitch) * pitch;
    }

    private double snapDown(double coordinate, double pitch) {
        return Math.floor((coordinate + EPSILON) / pitch) * pitch;
    }

    private double snapTo(double coordinate, double pitch) {
        return Math.round(coordinate / pitch) * pitch;
    }

    private String pointsAttribute(List<PlanPoint> points) {
        StringBuilder attribute = new StringBuilder();
        for (PlanPoint point : points) {
            if (!attribute.isEmpty()) {
                attribute.append(' ');
            }
            attribute.append(String.format(Locale.US, "%.3f,%.3f", point.xMillimeters(), point.yMillimeters()));
        }
        return attribute.toString();
    }

    private void appendVariothermGrooves(StringBuilder svg, Room room) {
        Bounds bounds = bounds(room.outline());
        double pitch = SurfaceCoveringPresetService.VARIOTHERM_GROOVE_PITCH_MILLIMETERS;
        double radius = (pitch - SurfaceCoveringPresetService.VARIOTHERM_PIPE_DIAMETER_MILLIMETERS) / 2.0;
        for (double x = snapUp(bounds.minX(), pitch); x <= bounds.maxX() + EPSILON; x += pitch) {
            for (double y = snapUp(bounds.minY(), pitch); y <= bounds.maxY() + EPSILON; y += pitch) {
                PlanPoint center = new PlanPoint(x, y);
                if (containsPoint(room.outline(), center)) {
                    svg.append(String.format(Locale.US,
                            "<circle cx=\"%.3f\" cy=\"%.3f\" r=\"%.3f\"/>\n",
                            x, y, radius));
                }
            }
        }
    }

    private String pathElement(String cssClass, List<PlanPoint> path, String color, boolean dashed) {
        if (path.size() < 2) {
            return "";
        }
        return String.format(Locale.US,
                "<path class=\"%s\" d=\"%s\" stroke=\"%s\"%s/>\n",
                cssClass,
                svgPath(path),
                color,
                dashed ? " stroke-dasharray=\"60 35\"" : "");
    }

    private String svgPath(List<PlanPoint> path) {
        StringBuilder d = new StringBuilder();
        for (int index = 0; index < path.size(); index++) {
            PlanPoint point = path.get(index);
            d.append(index == 0 ? "M " : " L ");
            d.append(String.format(Locale.US, "%.3f %.3f", point.xMillimeters(), point.yMillimeters()));
        }
        return d.toString();
    }

    private String portCircle(PlanPoint point, String color, String label) {
        return String.format(Locale.US,
                "<circle cx=\"%.3f\" cy=\"%.3f\" r=\"38\" fill=\"#fff\" stroke=\"%s\" stroke-width=\"8\"/>"
                        + "<text x=\"%.3f\" y=\"%.3f\" fill=\"%s\">%s</text>\n",
                point.xMillimeters(), point.yMillimeters(), color,
                point.xMillimeters(), point.yMillimeters() + 30.0, color, label);
    }

    private String format(PlanPoint point) {
        return String.format(Locale.GERMAN, "(%.1f; %.1f)", point.xMillimeters(), point.yMillimeters());
    }

    private record Bounds(double minX, double maxX, double minY, double maxY) {
        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxY - minY;
        }
    }

    private record ExclusionRect(double minX, double minY, double maxX, double maxY) {
        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxY - minY;
        }

        private PlanPoint center() {
            return new PlanPoint((minX + maxX) / 2.0, (minY + maxY) / 2.0);
        }

        private boolean contains(PlanPoint point) {
            return point.xMillimeters() > minX + EPSILON
                    && point.xMillimeters() < maxX - EPSILON
                    && point.yMillimeters() > minY + EPSILON
                    && point.yMillimeters() < maxY - EPSILON;
        }
    }

    private record Interval(double start, double end) {
    }

    private record SplitPath(List<PlanPoint> first, List<PlanPoint> second) {
    }

    private record FieldPattern(List<PlanPoint> fullPath, List<PlanPoint> supplyPath, List<PlanPoint> returnPath) {
        private FieldPattern {
            fullPath = List.copyOf(fullPath);
            supplyPath = List.copyOf(supplyPath);
            returnPath = List.copyOf(returnPath);
        }

        private FieldPattern inverted() {
            return new FieldPattern(reversed(fullPath), reversed(returnPath), reversed(supplyPath));
        }
    }

    private record ManifoldPair(PlanPoint supplyPort, PlanPoint returnPort) {
    }

    private record ConnectorPlan(ManifoldPair pair, List<PlanPoint> supplyPath, List<PlanPoint> returnPath) {
        private static ConnectorPlan failed(ManifoldPair pair, FieldPattern pattern) {
            return new ConnectorPlan(
                    pair,
                    List.of(pair.supplyPort(), pattern.fullPath().getFirst()),
                    List.of(pattern.fullPath().getLast(), pair.returnPort())
            );
        }
    }

    private record LayoutComputation(List<CircuitLayout> circuits, ValidationReport report, Set<UUID> unroutableZoneIds) {
    }

    private record IndexedSegment(int circuitIndex, int segmentIndex, PipeSegment segment) {
    }

    private record SharedBoundary(
            int firstIndex,
            int secondIndex,
            boolean vertical,
            double coordinate,
            double start,
            double end
    ) {
    }

    private record GridPoint(int ix, int iy) {
        private PlanPoint toPlanPoint(double pitch) {
            return new PlanPoint(ix * pitch, iy * pitch);
        }
    }

    private record GridEdge(GridPoint a, GridPoint b) {
        private GridEdge {
            if (compare(b, a) < 0) {
                GridPoint swap = a;
                a = b;
                b = swap;
            }
        }

        private static int compare(GridPoint first, GridPoint second) {
            int x = Integer.compare(first.ix(), second.ix());
            return x != 0 ? x : Integer.compare(first.iy(), second.iy());
        }
    }

    private static final class GridGraph {

        private final double pitch;
        private final Set<GridPoint> nodes;
        private final Map<GridPoint, List<GridPoint>> adjacency;

        private GridGraph(double pitch, Set<GridPoint> nodes, Map<GridPoint, List<GridPoint>> adjacency) {
            this.pitch = pitch;
            this.nodes = nodes;
            this.adjacency = adjacency;
        }

        private static GridGraph create(GeometryScope scope, double pitch) {
            Bounds bounds = scope.bounds();
            int minX = (int) Math.floor(bounds.minX() / pitch) - 1;
            int maxX = (int) Math.ceil(bounds.maxX() / pitch) + 1;
            int minY = (int) Math.floor(bounds.minY() / pitch) - 1;
            int maxY = (int) Math.ceil(bounds.maxY() / pitch) + 1;
            Set<GridPoint> nodes = new LinkedHashSet<>();
            for (int ix = minX; ix <= maxX; ix++) {
                for (int iy = minY; iy <= maxY; iy++) {
                    GridPoint point = new GridPoint(ix, iy);
                    if (scope.contains(point.toPlanPoint(pitch))) {
                        nodes.add(point);
                    }
                }
            }
            Map<GridPoint, List<GridPoint>> adjacency = new HashMap<>();
            for (GridPoint node : nodes) {
                List<GridPoint> neighbors = new ArrayList<>();
                for (int[] delta : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    GridPoint neighbor = new GridPoint(node.ix() + delta[0], node.iy() + delta[1]);
                    if (nodes.contains(neighbor) && scope.containsSegment(node.toPlanPoint(pitch), neighbor.toPlanPoint(pitch))) {
                        neighbors.add(neighbor);
                    }
                }
                adjacency.put(node, neighbors);
            }
            return new GridGraph(pitch, nodes, adjacency);
        }

        private Optional<GridPoint> nearest(PlanPoint point, GeometryScope scope, Set<GridPoint> forbiddenNodes) {
            GridPoint exact = new GridPoint(
                    (int) Math.round(point.xMillimeters() / pitch),
                    (int) Math.round(point.yMillimeters() / pitch)
            );
            if (nodes.contains(exact) && !forbiddenNodes.contains(exact) && scope.containsSegment(point, exact.toPlanPoint(pitch))) {
                return Optional.of(exact);
            }
            return nodes.stream()
                    .filter(candidate -> !forbiddenNodes.contains(candidate))
                    .filter(candidate -> scope.containsSegment(point, candidate.toPlanPoint(pitch)))
                    .min(Comparator.comparingDouble(candidate -> candidate.toPlanPoint(pitch).distanceTo(point).toMillimeters()));
        }

        private List<GridPoint> shortestPath(GridPoint start, GridPoint goal, Set<GridEdge> blockedEdges, boolean blockTouchedNodes) {
            if (!nodes.contains(start) || !nodes.contains(goal)) {
                return List.of();
            }
            Set<GridPoint> blockedNodes = blockTouchedNodes ? blockedNodes(blockedEdges, start, goal) : Set.of();
            PriorityQueue<SearchNode> open = new PriorityQueue<>(Comparator.comparingDouble(SearchNode::score));
            Map<GridPoint, Double> distance = new HashMap<>();
            Map<GridPoint, GridPoint> previous = new HashMap<>();
            open.add(new SearchNode(start, heuristic(start, goal)));
            distance.put(start, 0.0);
            while (!open.isEmpty()) {
                GridPoint current = open.poll().point();
                if (current.equals(goal)) {
                    return compress(reconstruct(previous, current));
                }
                for (GridPoint neighbor : adjacency.getOrDefault(current, List.of())) {
                    GridEdge edge = new GridEdge(current, neighbor);
                    if (blockedEdges.contains(edge) || blockedNodes.contains(neighbor)) {
                        continue;
                    }
                    double turnPenalty = turnPenalty(previous.get(current), current, neighbor);
                    double candidateDistance = distance.get(current) + 1.0 + turnPenalty;
                    if (candidateDistance + EPSILON < distance.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                        distance.put(neighbor, candidateDistance);
                        previous.put(neighbor, current);
                        open.add(new SearchNode(neighbor, candidateDistance + heuristic(neighbor, goal)));
                    }
                }
            }
            return List.of();
        }

        private Set<GridPoint> blockedNodes(Set<GridEdge> blockedEdges, GridPoint start, GridPoint goal) {
            Set<GridPoint> blockedNodes = new HashSet<>();
            for (GridEdge edge : blockedEdges) {
                blockedNodes.add(edge.a());
                blockedNodes.add(edge.b());
            }
            if (start != null) {
                blockedNodes.remove(start);
            }
            if (goal != null) {
                blockedNodes.remove(goal);
            }
            return blockedNodes;
        }

        private static List<GridPoint> reconstruct(Map<GridPoint, GridPoint> previous, GridPoint current) {
            ArrayDeque<GridPoint> points = new ArrayDeque<>();
            points.addFirst(current);
            while (previous.containsKey(current)) {
                current = previous.get(current);
                points.addFirst(current);
            }
            return List.copyOf(points);
        }

        private static List<GridPoint> compress(List<GridPoint> points) {
            if (points.size() < 3) {
                return points;
            }
            List<GridPoint> compressed = new ArrayList<>();
            compressed.add(points.getFirst());
            for (int index = 1; index + 1 < points.size(); index++) {
                GridPoint previous = compressed.getLast();
                GridPoint current = points.get(index);
                GridPoint next = points.get(index + 1);
                int dx1 = Integer.compare(current.ix() - previous.ix(), 0);
                int dy1 = Integer.compare(current.iy() - previous.iy(), 0);
                int dx2 = Integer.compare(next.ix() - current.ix(), 0);
                int dy2 = Integer.compare(next.iy() - current.iy(), 0);
                if (dx1 != dx2 || dy1 != dy2) {
                    compressed.add(current);
                }
            }
            compressed.add(points.getLast());
            return List.copyOf(compressed);
        }

        private double heuristic(GridPoint first, GridPoint second) {
            return Math.abs(first.ix() - second.ix()) + Math.abs(first.iy() - second.iy());
        }

        private double turnPenalty(GridPoint previous, GridPoint current, GridPoint next) {
            if (previous == null) {
                return 0.0;
            }
            int dx1 = Integer.compare(current.ix() - previous.ix(), 0);
            int dy1 = Integer.compare(current.iy() - previous.iy(), 0);
            int dx2 = Integer.compare(next.ix() - current.ix(), 0);
            int dy2 = Integer.compare(next.iy() - current.iy(), 0);
            return dx1 == dx2 && dy1 == dy2 ? 0.0 : 0.18;
        }

        private record SearchNode(GridPoint point, double score) {
        }
    }

    private final class GeometryScope {

        private final List<List<PlanPoint>> polygons;
        private final Bounds bounds;

        private GeometryScope(List<List<PlanPoint>> polygons) {
            this.polygons = polygons.stream().map(List::copyOf).toList();
            this.bounds = new Bounds(
                    polygons.stream().flatMap(List::stream).mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0),
                    polygons.stream().flatMap(List::stream).mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0),
                    polygons.stream().flatMap(List::stream).mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0),
                    polygons.stream().flatMap(List::stream).mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0)
            );
        }

        private Bounds bounds() {
            return bounds;
        }

        private boolean contains(PlanPoint point) {
            return polygons.stream().anyMatch(polygon -> containsPoint(polygon, point));
        }

        private boolean containsSegment(PlanPoint start, PlanPoint end) {
            for (int step = 0; step <= 32; step++) {
                double ratio = step / 32.0;
                PlanPoint point = new PlanPoint(
                        start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * ratio,
                        start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * ratio
                );
                if (!contains(point)) {
                    return false;
                }
            }
            return true;
        }
    }

    public enum PipeRole {
        SUPPLY,
        RETURN,
        SUPPLY_CONNECTOR,
        RETURN_CONNECTOR
    }

    public enum ValidationErrorType {
        PIPE_OUTSIDE_ROOM,
        DUPLICATE_GRID_EDGE,
        GEOMETRIC_INTERSECTION,
        CONNECTOR_NOT_CONNECTED,
        UNROUTABLE_CONNECTOR,
        CIRCUIT_TOO_LONG,
        AUTOMATIC_REPAIR,
        PARTIAL_LAYOUT
    }

    public record PipeSegment(UUID zoneId, PipeRole role, PlanPoint start, PlanPoint end) {
    }

    public record ValidationIssue(ValidationErrorType type, String message) {
    }

    public record ValidationReport(boolean valid, List<ValidationIssue> errors, List<ValidationIssue> warnings) {

        public ValidationReport {
            errors = List.copyOf(errors);
            warnings = List.copyOf(warnings);
        }

        public static ValidationReport ok() {
            return new ValidationReport(true, List.of(), List.of());
        }

        public String summary() {
            if (valid) {
                return "FBH-Layout ist gültig.";
            }
            return errors.stream()
                    .map(issue -> issue.type() + ": " + issue.message())
                    .findFirst()
                    .orElse("FBH-Layout ist ungültig.");
        }
    }

    public record CircuitLayout(
            UUID zoneId,
            List<PlanPoint> pipePath,
            Length pipeLength,
            Length bendRadius,
            PlanPoint supplyPort,
            PlanPoint returnPort,
            List<PlanPoint> supplyConnectorPath,
            List<PlanPoint> returnConnectorPath,
            List<PlanPoint> fieldSupplyPath,
            List<PlanPoint> fieldReturnPath,
            List<PipeSegment> segments,
            ValidationReport validationReport
    ) {
        public CircuitLayout(UUID zoneId, List<PlanPoint> pipePath, Length pipeLength, Length bendRadius) {
            this(zoneId, pipePath, pipeLength, bendRadius,
                    pipePath.isEmpty() ? new PlanPoint(0, 0) : pipePath.getFirst(),
                    pipePath.isEmpty() ? new PlanPoint(0, 0) : pipePath.getLast(),
                    pipePath, List.of(), pipePath, List.of(), List.of(), ValidationReport.ok());
        }

        public CircuitLayout {
            pipePath = List.copyOf(pipePath);
            supplyConnectorPath = List.copyOf(supplyConnectorPath);
            returnConnectorPath = List.copyOf(returnConnectorPath);
            fieldSupplyPath = List.copyOf(fieldSupplyPath);
            fieldReturnPath = List.copyOf(fieldReturnPath);
            segments = List.copyOf(segments);
        }

        private CircuitLayout withValidationReport(ValidationReport report) {
            return new CircuitLayout(
                    zoneId, pipePath, pipeLength, bendRadius, supplyPort, returnPort,
                    supplyConnectorPath, returnConnectorPath, fieldSupplyPath, fieldReturnPath, segments, report
            );
        }
    }

    public record PlanningResult(HydronicHeating heating, List<CircuitLayout> circuits, ValidationReport validationReport) {
        public PlanningResult(HydronicHeating heating, List<CircuitLayout> circuits) {
            this(heating, circuits, ValidationReport.ok());
        }

        public PlanningResult {
            circuits = List.copyOf(circuits);
        }
    }
}
