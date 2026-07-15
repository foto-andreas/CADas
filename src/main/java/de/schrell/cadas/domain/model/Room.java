package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Room {

    private final UUID id;
    private final String name;
    private final List<PlanPoint> outline;
    private final Length roomHeight;
    private final Length floorThickness;
    private final Length ceilingThickness;
    private final List<SlopedCeilingProfile> slopedCeilings;
    private final List<Length> ceilingVertexHeights;
    private final double heatLoadWatts;

    public Room(
            UUID id,
            String name,
            List<PlanPoint> outline,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness,
            SlopedCeilingProfile slopedCeiling,
            List<Length> ceilingVertexHeights
    ) {
        this(id, name, outline, roomHeight, floorThickness, ceilingThickness,
                slopedCeiling == null ? List.of() : List.of(slopedCeiling), ceilingVertexHeights, 0.0);
    }

    private Room(
            UUID id,
            String name,
            List<PlanPoint> outline,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness,
            List<SlopedCeilingProfile> slopedCeilings,
            List<Length> ceilingVertexHeights,
            double heatLoadWatts
    ) {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(name, "name darf nicht null sein.");
        Objects.requireNonNull(outline, "outline darf nicht null sein.");
        Objects.requireNonNull(roomHeight, "roomHeight darf nicht null sein.");
        Objects.requireNonNull(floorThickness, "floorThickness darf nicht null sein.");
        Objects.requireNonNull(ceilingThickness, "ceilingThickness darf nicht null sein.");
        Objects.requireNonNull(slopedCeilings, "slopedCeilings darf nicht null sein.");
        if (ceilingVertexHeights != null && ceilingVertexHeights.size() != outline.size()) {
            throw new IllegalArgumentException("Deckenhöhen müssen zu allen Raum-Eckpunkten passen.");
        }
        if (slopedCeilings.stream().anyMatch(profile -> profile.kneeWallHeight().toMillimeters() > roomHeight.toMillimeters())) {
            throw new IllegalArgumentException("Die Sockelhöhe der Dachschräge darf die lichte Raumhöhe nicht überschreiten.");
        }
        if (outline.size() < 3) {
            throw new IllegalArgumentException("Ein Raum benötigt mindestens drei Eckpunkte.");
        }
        if (heatLoadWatts < 0.0 || !Double.isFinite(heatLoadWatts)) {
            throw new IllegalArgumentException("Die Heizlast darf nicht negativ oder unendlich sein.");
        }
        this.id = id;
        this.name = name;
        this.outline = List.copyOf(outline);
        this.roomHeight = roomHeight;
        this.floorThickness = floorThickness;
        this.ceilingThickness = ceilingThickness;
        this.slopedCeilings = List.copyOf(slopedCeilings);
        this.ceilingVertexHeights = ceilingVertexHeights == null ? null : List.copyOf(ceilingVertexHeights);
        this.heatLoadWatts = heatLoadWatts;
    }

    public static Room withSlopedCeilings(
            UUID id,
            String name,
            List<PlanPoint> outline,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness,
            List<SlopedCeilingProfile> slopedCeilings,
            List<Length> ceilingVertexHeights
    ) {
        return new Room(id, name, outline, roomHeight, floorThickness, ceilingThickness, slopedCeilings, ceilingVertexHeights, 0.0);
    }

    public Room(
            UUID id,
            String name,
            List<PlanPoint> outline,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness,
            SlopedCeilingProfile slopedCeiling
    ) {
        this(id, name, outline, roomHeight, floorThickness, ceilingThickness, slopedCeiling, null);
    }

    public static Room rectangular(
            String name,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness
    ) {
        return rectangular(name, firstCorner, oppositeCorner, roomHeight, floorThickness, ceilingThickness, null);
    }

    public static Room rectangular(
            String name,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length roomHeight,
            Length floorThickness
    ) {
        return rectangular(name, firstCorner, oppositeCorner, roomHeight, floorThickness, Length.ofMillimeters(0.1), null);
    }

    public static Room rectangular(
            String name,
            PlanPoint firstCorner,
            PlanPoint oppositeCorner,
            Length roomHeight,
            Length floorThickness,
            Length ceilingThickness,
            SlopedCeilingProfile slopedCeiling
    ) {
        double minX = Math.min(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
        double maxX = Math.max(firstCorner.xMillimeters(), oppositeCorner.xMillimeters());
        double minY = Math.min(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
        double maxY = Math.max(firstCorner.yMillimeters(), oppositeCorner.yMillimeters());
        return new Room(
                UUID.randomUUID(),
                name,
                List.of(
                        new PlanPoint(minX, minY),
                        new PlanPoint(maxX, minY),
                        new PlanPoint(maxX, maxY),
                        new PlanPoint(minX, maxY)
                ),
                roomHeight,
                floorThickness,
                ceilingThickness,
                slopedCeiling,
                null
        );
    }

    public Optional<SlopedCeilingProfile> slopedCeilingProfile() {
        return slopedCeilings.stream().findFirst();
    }

    public List<SlopedCeilingProfile> slopedCeilingProfiles() {
        return slopedCeilings;
    }

    public SlopedCeilingProfile slopedCeiling() {
        return slopedCeilings.stream().findFirst().orElse(null);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<PlanPoint> outline() {
        return outline;
    }

    public Length roomHeight() {
        return roomHeight;
    }

    public Length floorThickness() {
        return floorThickness;
    }

    public Length ceilingThickness() {
        return ceilingThickness;
    }

    public double heatLoadWatts() {
        return heatLoadWatts;
    }

    public List<Length> ceilingVertexHeights() {
        return ceilingVertexHeights;
    }

    public Room withName(String newName) {
        String trimmedName = Objects.requireNonNull(newName, "newName darf nicht null sein.").trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Der Raumname darf nicht leer sein.");
        }
        return new Room(
                id, trimmedName, outline, roomHeight, floorThickness, ceilingThickness,
                slopedCeilings, ceilingVertexHeights, heatLoadWatts
        );
    }

    public Room withSlopedCeilingProfiles(List<SlopedCeilingProfile> profiles) {
        return new Room(id, name, outline, roomHeight, floorThickness, ceilingThickness, profiles, null, heatLoadWatts);
    }

    public Room withHeatLoadWatts(double newHeatLoadWatts) {
        return new Room(id, name, outline, roomHeight, floorThickness, ceilingThickness, slopedCeilings, ceilingVertexHeights, newHeatLoadWatts);
    }

    public Optional<List<Length>> ceilingVertexHeightsProfile() {
        return Optional.ofNullable(ceilingVertexHeights);
    }

    public Length area() {
        double areaDouble = 0.0;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint next = outline.get((index + 1) % outline.size());
            areaDouble += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return Length.ofMillimeters(Math.abs(areaDouble) / 2.0);
    }

    public double areaSquareMeters() {
        return area().toMillimeters() / 1_000_000.0;
    }

    public double volumeCubicMeters() {
        return volumeCubicMeters(outline, 0.0);
    }

    /**
     * Integriert die lichte Raumhöhe über dem angegebenen Teil des Grundrisses.
     * Dachschrägen werden dabei als untere Hülle ihrer Ebenen behandelt; eine pauschale Mittelhöhe wäre bei
     * begrenzten oder aufeinandertreffenden Schrägen fachlich falsch. Die Höhenreduktion bildet sichtbare Boden-
     * und Deckenlagen ab. Außerhalb des Raumumrisses liegende Teile der übergebenen Fläche bleiben unberücksichtigt.
     */
    public double volumeCubicMeters(List<PlanPoint> footprint, double heightReductionMillimeters) {
        Objects.requireNonNull(footprint, "footprint darf nicht null sein.");
        if (footprint.size() < 3) {
            return 0.0;
        }
        double reduction = Math.max(0.0, heightReductionMillimeters);
        List<HeightRegion> heightRegions = ceilingVertexHeights == null || ceilingVertexHeights.isEmpty()
                ? slopeHeightRegions()
                : vertexHeightRegions();
        double volumeCubicMillimeters = 0.0;
        List<List<PlanPoint>> roomTriangles = triangulate(outline);
        for (List<PlanPoint> footprintTriangle : triangulate(footprint)) {
            for (List<PlanPoint> roomTriangle : roomTriangles) {
                // Das zusätzliche Schneiden am Raum verhindert Volumen außerhalb konkaver Grundrisse.
                List<PlanPoint> footprintInsideRoom = intersectConvexPolygons(footprintTriangle, roomTriangle);
                if (footprintInsideRoom.size() < 3) {
                    continue;
                }
                for (HeightRegion region : heightRegions) {
                    List<PlanPoint> intersection = region.bounds() == null
                            ? footprintInsideRoom
                            : intersectConvexPolygons(footprintInsideRoom, region.bounds());
                    if (intersection.size() < 3) {
                        continue;
                    }
                    List<PlanPoint> activeRegion = clipByPlane(intersection, region.height(), reduction, true);
                    for (Plane competingPlane : region.lowerThan()) {
                        activeRegion = clipByPlaneDifference(activeRegion, region.height(), competingPlane);
                        if (activeRegion.size() < 3) {
                            break;
                        }
                    }
                    volumeCubicMillimeters += integrate(activeRegion, region.height(), reduction);
                }
            }
        }
        return volumeCubicMillimeters / 1_000_000_000.0;
    }

    public PlanPoint areaCentroid() {
        double crossSum = 0.0;
        double weightedX = 0.0;
        double weightedY = 0.0;
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint current = outline.get(index);
            PlanPoint next = outline.get((index + 1) % outline.size());
            double cross = current.xMillimeters() * next.yMillimeters()
                    - next.xMillimeters() * current.yMillimeters();
            crossSum += cross;
            weightedX += (current.xMillimeters() + next.xMillimeters()) * cross;
            weightedY += (current.yMillimeters() + next.yMillimeters()) * cross;
        }
        if (Math.abs(crossSum) < 0.001) {
            return centerPoint();
        }
        return new PlanPoint(weightedX / (3.0 * crossSum), weightedY / (3.0 * crossSum));
    }

    public PlanPoint centerPoint() {
        double sumX = 0.0;
        double sumY = 0.0;
        for (PlanPoint point : outline) {
            sumX += point.xMillimeters();
            sumY += point.yMillimeters();
        }
        return new PlanPoint(sumX / outline.size(), sumY / outline.size());
    }

    public double minXMillimeters() {
        return outline.stream().mapToDouble(PlanPoint::xMillimeters).min().orElse(0.0);
    }

    public double maxXMillimeters() {
        return outline.stream().mapToDouble(PlanPoint::xMillimeters).max().orElse(0.0);
    }

    public double minYMillimeters() {
        return outline.stream().mapToDouble(PlanPoint::yMillimeters).min().orElse(0.0);
    }

    public double maxYMillimeters() {
        return outline.stream().mapToDouble(PlanPoint::yMillimeters).max().orElse(0.0);
    }

    public double widthMillimeters() {
        return maxXMillimeters() - minXMillimeters();
    }

    public double depthMillimeters() {
        return maxYMillimeters() - minYMillimeters();
    }

    public double ceilingHeightAt(PlanPoint point) {
        if (ceilingVertexHeights != null && ceilingVertexHeights.size() == outline.size()) {
            return interpolatedVertexHeight(point);
        }
        if (slopedCeilings.isEmpty()) {
            return roomHeight.toMillimeters();
        }
        return slopedCeilings.stream()
                .mapToDouble(profile -> ceilingHeightAt(point, profile))
                .min()
                .orElse(roomHeight.toMillimeters());
    }

    public double ceilingHeightAt(PlanPoint point, SlopedCeilingProfile profile) {
        double lowHeight = profile.kneeWallHeight().toMillimeters();
        double highHeight = roomHeight.toMillimeters();
        double run = runMillimeters(profile);
        if (run <= 1.0 || highHeight <= lowHeight) {
            return highHeight;
        }
        double distance = distanceFromLowSide(point, profile.lowSide());
        double ratio = Math.clamp(distance / run, 0.0, 1.0);
        return lowHeight + (highHeight - lowHeight) * ratio;
    }

    public double minimumCeilingHeightMillimeters() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return ceilingVertexHeights.stream().mapToDouble(Length::toMillimeters).min().orElse(roomHeight.toMillimeters());
        }
        return slopedCeilings.stream()
                .mapToDouble(profile -> profile.kneeWallHeight().toMillimeters())
                .min()
                .orElse(roomHeight.toMillimeters());
    }

    public double maximumCeilingHeightMillimeters() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return ceilingVertexHeights.stream().mapToDouble(Length::toMillimeters).max().orElse(roomHeight.toMillimeters());
        }
        return roomHeight.toMillimeters();
    }

    public double slopeAngleDegrees() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return 0.0;
        }
        if (slopedCeilings.isEmpty()) {
            return 0.0;
        }
        return slopeAngleDegrees(slopedCeilings.getFirst());
    }

    public double slopeAngleDegrees(SlopedCeilingProfile profile) {
        double rise = roomHeight.toMillimeters() - profile.kneeWallHeight().toMillimeters();
        double run = runMillimeters(profile);
        if (rise <= 0.0 || run <= 1.0) {
            return 0.0;
        }
        return Math.toDegrees(Math.atan(rise / run));
    }

    public boolean slopeVisibleInEastWestView() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return hasVariableCeilingHeights();
        }
        return slopedCeilings.stream().anyMatch(profile ->
                profile.lowSide() == SlopedCeilingSide.NORTH || profile.lowSide() == SlopedCeilingSide.SOUTH);
    }

    public boolean slopeVisibleInNorthSouthView() {
        if (ceilingVertexHeights != null && !ceilingVertexHeights.isEmpty()) {
            return hasVariableCeilingHeights();
        }
        return slopedCeilings.stream().anyMatch(profile ->
                profile.lowSide() == SlopedCeilingSide.EAST || profile.lowSide() == SlopedCeilingSide.WEST);
    }

    public boolean hasVariableCeilingHeights() {
        if (ceilingVertexHeights != null && ceilingVertexHeights.size() > 1) {
            double reference = ceilingVertexHeights.getFirst().toMillimeters();
            return ceilingVertexHeights.stream().anyMatch(length -> Math.abs(length.toMillimeters() - reference) > 0.001);
        }
        return !slopedCeilings.isEmpty();
    }

    private double runMillimeters(SlopedCeilingProfile profile) {
        SlopedCeilingSide side = profile.lowSide();
        double roomRun = switch (side) {
            case NORTH, SOUTH -> depthMillimeters();
            case EAST, WEST -> widthMillimeters();
        };
        if (profile.horizontalRun().toMillimeters() <= 0.0) {
            return roomRun;
        }
        return Math.min(roomRun, profile.horizontalRun().toMillimeters());
    }

    private double distanceFromLowSide(PlanPoint point, SlopedCeilingSide side) {
        return switch (side) {
            case NORTH -> point.yMillimeters() - minYMillimeters();
            case SOUTH -> maxYMillimeters() - point.yMillimeters();
            case EAST -> maxXMillimeters() - point.xMillimeters();
            case WEST -> point.xMillimeters() - minXMillimeters();
        };
    }

    private double interpolatedVertexHeight(PlanPoint point) {
        PlanPoint center = centerPoint();
        double centerHeight = ceilingVertexHeights.stream().mapToDouble(Length::toMillimeters).average().orElse(roomHeight.toMillimeters());
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint first = outline.get(index);
            PlanPoint second = outline.get((index + 1) % outline.size());
            if (pointInsideTriangle(point, center, first, second)) {
                return barycentricHeight(
                        point,
                        center,
                        first,
                        second,
                        centerHeight,
                        ceilingVertexHeights.get(index).toMillimeters(),
                        ceilingVertexHeights.get((index + 1) % ceilingVertexHeights.size()).toMillimeters()
                );
            }
        }
        return centerHeight;
    }

    private boolean pointInsideTriangle(PlanPoint point, PlanPoint a, PlanPoint b, PlanPoint c) {
        double denominator = ((b.yMillimeters() - c.yMillimeters()) * (a.xMillimeters() - c.xMillimeters())
                + (c.xMillimeters() - b.xMillimeters()) * (a.yMillimeters() - c.yMillimeters()));
        if (Math.abs(denominator) < 0.001) {
            return false;
        }
        double alpha = ((b.yMillimeters() - c.yMillimeters()) * (point.xMillimeters() - c.xMillimeters())
                + (c.xMillimeters() - b.xMillimeters()) * (point.yMillimeters() - c.yMillimeters())) / denominator;
        double beta = ((c.yMillimeters() - a.yMillimeters()) * (point.xMillimeters() - c.xMillimeters())
                + (a.xMillimeters() - c.xMillimeters()) * (point.yMillimeters() - c.yMillimeters())) / denominator;
        double gamma = 1.0 - alpha - beta;
        return alpha >= -0.0001 && beta >= -0.0001 && gamma >= -0.0001;
    }

    private double barycentricHeight(PlanPoint point, PlanPoint a, PlanPoint b, PlanPoint c, double heightA, double heightB, double heightC) {
        double denominator = ((b.yMillimeters() - c.yMillimeters()) * (a.xMillimeters() - c.xMillimeters())
                + (c.xMillimeters() - b.xMillimeters()) * (a.yMillimeters() - c.yMillimeters()));
        if (Math.abs(denominator) < 0.001) {
            return heightA;
        }
        double alpha = ((b.yMillimeters() - c.yMillimeters()) * (point.xMillimeters() - c.xMillimeters())
                + (c.xMillimeters() - b.xMillimeters()) * (point.yMillimeters() - c.yMillimeters())) / denominator;
        double beta = ((c.yMillimeters() - a.yMillimeters()) * (point.xMillimeters() - c.xMillimeters())
                + (a.xMillimeters() - c.xMillimeters()) * (point.yMillimeters() - c.yMillimeters())) / denominator;
        double gamma = 1.0 - alpha - beta;
        return alpha * heightA + beta * heightB + gamma * heightC;
    }

    private double triangleArea(PlanPoint a, PlanPoint b, PlanPoint c) {
        return Math.abs(
                a.xMillimeters() * (b.yMillimeters() - c.yMillimeters())
                        + b.xMillimeters() * (c.yMillimeters() - a.yMillimeters())
                        + c.xMillimeters() * (a.yMillimeters() - b.yMillimeters())
        ) / 2.0;
    }

    private List<HeightRegion> slopeHeightRegions() {
        List<Plane> planes = new ArrayList<>();
        planes.add(new Plane(0.0, 0.0, roomHeight.toMillimeters()));
        for (SlopedCeilingProfile profile : slopedCeilings) {
            double run = runMillimeters(profile);
            double lowHeight = profile.kneeWallHeight().toMillimeters();
            double rise = roomHeight.toMillimeters() - lowHeight;
            if (run <= 1.0 || rise <= 0.0) {
                continue;
            }
            double gradient = rise / run;
            planes.add(switch (profile.lowSide()) {
                case NORTH -> new Plane(0.0, gradient, lowHeight - gradient * minYMillimeters());
                case SOUTH -> new Plane(0.0, -gradient, lowHeight + gradient * maxYMillimeters());
                case WEST -> new Plane(gradient, 0.0, lowHeight - gradient * minXMillimeters());
                case EAST -> new Plane(-gradient, 0.0, lowHeight + gradient * maxXMillimeters());
            });
        }
        // Doppelte Profile dürfen keine deckungsgleichen Integrationsgebiete und damit doppeltes Volumen erzeugen.
        List<Plane> distinctPlanes = planes.stream().distinct().toList();
        List<HeightRegion> regions = new ArrayList<>();
        for (Plane plane : distinctPlanes) {
            regions.add(new HeightRegion(null, plane, distinctPlanes.stream().filter(candidate -> candidate != plane).toList()));
        }
        return regions;
    }

    private List<HeightRegion> vertexHeightRegions() {
        PlanPoint center = centerPoint();
        double centerHeight = ceilingVertexHeights.stream()
                .mapToDouble(Length::toMillimeters)
                .average()
                .orElse(roomHeight.toMillimeters());
        List<HeightRegion> regions = new ArrayList<>();
        for (int index = 0; index < outline.size(); index++) {
            PlanPoint first = outline.get(index);
            PlanPoint second = outline.get((index + 1) % outline.size());
            Plane plane = Plane.through(
                    center, centerHeight,
                    first, ceilingVertexHeights.get(index).toMillimeters(),
                    second, ceilingVertexHeights.get((index + 1) % outline.size()).toMillimeters()
            );
            regions.add(new HeightRegion(List.of(center, first, second), plane, List.of()));
        }
        return regions;
    }

    private List<List<PlanPoint>> triangulate(List<PlanPoint> polygon) {
        // Ear-Clipping zerlegt auch konkave, einfache Grundrisse ohne Flächen außerhalb des Polygons.
        List<PlanPoint> remaining = new ArrayList<>(polygon);
        List<List<PlanPoint>> triangles = new ArrayList<>();
        double orientation = Math.signum(signedArea(remaining));
        if (orientation == 0.0) {
            return List.of();
        }
        while (remaining.size() > 3) {
            boolean earFound = false;
            for (int index = 0; index < remaining.size(); index++) {
                PlanPoint previous = remaining.get((index - 1 + remaining.size()) % remaining.size());
                PlanPoint current = remaining.get(index);
                PlanPoint next = remaining.get((index + 1) % remaining.size());
                if (cross(previous, current, next) * orientation <= 0.001) {
                    continue;
                }
                boolean containsVertex = remaining.stream()
                        .filter(point -> point != previous && point != current && point != next)
                        .anyMatch(point -> pointInsideTriangle(point, previous, current, next));
                if (containsVertex) {
                    continue;
                }
                triangles.add(List.of(previous, current, next));
                remaining.remove(index);
                earFound = true;
                break;
            }
            if (!earFound) {
                return List.of();
            }
        }
        triangles.add(List.copyOf(remaining));
        return triangles;
    }

    private List<PlanPoint> intersectConvexPolygons(List<PlanPoint> subject, List<PlanPoint> clipPolygon) {
        List<PlanPoint> result = List.copyOf(subject);
        double orientation = Math.signum(signedArea(clipPolygon));
        for (int index = 0; index < clipPolygon.size() && !result.isEmpty(); index++) {
            PlanPoint edgeStart = clipPolygon.get(index);
            PlanPoint edgeEnd = clipPolygon.get((index + 1) % clipPolygon.size());
            result = clip(result, point -> orientation * cross(edgeStart, edgeEnd, point));
        }
        return result;
    }

    private List<PlanPoint> clipByPlaneDifference(List<PlanPoint> polygon, Plane ownPlane, Plane competingPlane) {
        return clip(polygon, point -> competingPlane.valueAt(point) - ownPlane.valueAt(point));
    }

    private List<PlanPoint> clipByPlane(List<PlanPoint> polygon, Plane plane, double threshold, boolean keepAbove) {
        return clip(polygon, point -> keepAbove ? plane.valueAt(point) - threshold : threshold - plane.valueAt(point));
    }

    private List<PlanPoint> clip(List<PlanPoint> polygon, SignedDistance signedDistance) {
        // Sutherland-Hodgman: Nichtnegative Distanz bezeichnet stets die beizubehaltende Halbebene.
        if (polygon.isEmpty()) {
            return List.of();
        }
        List<PlanPoint> result = new ArrayList<>();
        PlanPoint previous = polygon.getLast();
        double previousDistance = signedDistance.at(previous);
        for (PlanPoint current : polygon) {
            double currentDistance = signedDistance.at(current);
            boolean previousInside = previousDistance >= -0.000001;
            boolean currentInside = currentDistance >= -0.000001;
            if (previousInside != currentInside) {
                double ratio = previousDistance / (previousDistance - currentDistance);
                result.add(new PlanPoint(
                        previous.xMillimeters() + ratio * (current.xMillimeters() - previous.xMillimeters()),
                        previous.yMillimeters() + ratio * (current.yMillimeters() - previous.yMillimeters())
                ));
            }
            if (currentInside) {
                result.add(current);
            }
            previous = current;
            previousDistance = currentDistance;
        }
        return List.copyOf(result);
    }

    private double integrate(List<PlanPoint> polygon, Plane plane, double reduction) {
        if (polygon.size() < 3) {
            return 0.0;
        }
        double volume = 0.0;
        PlanPoint first = polygon.getFirst();
        for (int index = 1; index < polygon.size() - 1; index++) {
            PlanPoint second = polygon.get(index);
            PlanPoint third = polygon.get(index + 1);
            // Für eine affine Ebene ist der Mittelwert der drei Eckhöhen das exakte Dreiecksmittel.
            double averageHeight = (plane.valueAt(first) + plane.valueAt(second) + plane.valueAt(third)) / 3.0 - reduction;
            volume += triangleArea(first, second, third) * Math.max(0.0, averageHeight);
        }
        return volume;
    }

    private double signedArea(List<PlanPoint> polygon) {
        double sum = 0.0;
        for (int index = 0; index < polygon.size(); index++) {
            PlanPoint current = polygon.get(index);
            PlanPoint next = polygon.get((index + 1) % polygon.size());
            sum += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return sum / 2.0;
    }

    private double cross(PlanPoint first, PlanPoint second, PlanPoint third) {
        return (second.xMillimeters() - first.xMillimeters()) * (third.yMillimeters() - first.yMillimeters())
                - (second.yMillimeters() - first.yMillimeters()) * (third.xMillimeters() - first.xMillimeters());
    }

    @FunctionalInterface
    private interface SignedDistance {

        double at(PlanPoint point);
    }

    private record HeightRegion(List<PlanPoint> bounds, Plane height, List<Plane> lowerThan) {
    }

    private record Plane(double xFactor, double yFactor, double offset) {

        private static Plane through(
                PlanPoint first, double firstHeight,
                PlanPoint second, double secondHeight,
                PlanPoint third, double thirdHeight
        ) {
            double determinant = crossDeterminant(first, second, third);
            if (Math.abs(determinant) < 0.001) {
                return new Plane(0.0, 0.0, (firstHeight + secondHeight + thirdHeight) / 3.0);
            }
            double xFactor = (firstHeight * (second.yMillimeters() - third.yMillimeters())
                    + secondHeight * (third.yMillimeters() - first.yMillimeters())
                    + thirdHeight * (first.yMillimeters() - second.yMillimeters())) / determinant;
            double yFactor = (firstHeight * (third.xMillimeters() - second.xMillimeters())
                    + secondHeight * (first.xMillimeters() - third.xMillimeters())
                    + thirdHeight * (second.xMillimeters() - first.xMillimeters())) / determinant;
            return new Plane(xFactor, yFactor, firstHeight - xFactor * first.xMillimeters() - yFactor * first.yMillimeters());
        }

        private static double crossDeterminant(PlanPoint first, PlanPoint second, PlanPoint third) {
            return first.xMillimeters() * (second.yMillimeters() - third.yMillimeters())
                    + second.xMillimeters() * (third.yMillimeters() - first.yMillimeters())
                    + third.xMillimeters() * (first.yMillimeters() - second.yMillimeters());
        }

        private double valueAt(PlanPoint point) {
            return xFactor * point.xMillimeters() + yFactor * point.yMillimeters() + offset;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Room room)) {
            return false;
        }
        return id.equals(room.id)
                && name.equals(room.name)
                && outline.equals(room.outline)
                && roomHeight.equals(room.roomHeight)
                && floorThickness.equals(room.floorThickness)
                && ceilingThickness.equals(room.ceilingThickness)
                && slopedCeilings.equals(room.slopedCeilings)
                && Objects.equals(ceilingVertexHeights, room.ceilingVertexHeights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, outline, roomHeight, floorThickness, ceilingThickness, slopedCeilings, ceilingVertexHeights);
    }

    @Override
    public String toString() {
        return "Room[id=" + id + ", name=" + name + ", outline=" + outline + "]";
    }
}
