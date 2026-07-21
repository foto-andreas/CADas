package de.schrell.cadas.application.reports;

import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Teilt Deckenflächen in waagerechte und auf die tatsächliche Schräglänge abgerollte Bereiche. */
final class SlopedCeilingSurfaceService {

    private static final double EPSILON = 0.001;

    List<Coverage> coverages(Room room, List<SurfaceRectangle> rectangles) {
        if (room.slopedCeilingProfiles().isEmpty()) {
            return List.of(new Coverage("Decke", rectangles));
        }
        Map<SlopedCeilingProfile, List<SurfaceRectangle>> slopeRectangles = new LinkedHashMap<>();
        List<SurfaceRectangle> horizontalRectangles = new ArrayList<>();
        for (SurfaceRectangle rectangle : rectangles) {
            for (SurfaceRectangle section : splitAtSlopeBoundaries(room, rectangle)) {
                SlopedCeilingProfile profile = activeSlope(room, section);
                if (profile == null) {
                    horizontalRectangles.add(section);
                } else {
                    slopeRectangles.computeIfAbsent(profile, ignored -> new ArrayList<>())
                            .add(toSlopedSurfaceRectangle(room, profile, section));
                }
            }
        }
        List<Coverage> coverages = new ArrayList<>();
        if (!horizontalRectangles.isEmpty()) {
            coverages.add(new Coverage("Decke waagerecht", horizontalRectangles));
        }
        slopeRectangles.forEach((profile, sections) -> coverages.add(new Coverage(
                "Decke Schräge " + profile.lowSide(), sections
        )));
        return List.copyOf(coverages);
    }

    private List<SurfaceRectangle> splitAtSlopeBoundaries(Room room, SurfaceRectangle rectangle) {
        List<Double> xCuts = slopeCuts(room, rectangle.minXMillimeters(), rectangle.maxXMillimeters(), true);
        List<Double> yCuts = slopeCuts(room, rectangle.minYMillimeters(), rectangle.maxYMillimeters(), false);
        List<SurfaceRectangle> sections = new ArrayList<>();
        for (int xIndex = 0; xIndex < xCuts.size() - 1; xIndex++) {
            for (int yIndex = 0; yIndex < yCuts.size() - 1; yIndex++) {
                sections.add(new SurfaceRectangle(
                        xCuts.get(xIndex), yCuts.get(yIndex),
                        xCuts.get(xIndex + 1) - xCuts.get(xIndex),
                        yCuts.get(yIndex + 1) - yCuts.get(yIndex)
                ));
            }
        }
        return List.copyOf(sections);
    }

    private List<Double> slopeCuts(Room room, double minimum, double maximum, boolean xAxis) {
        List<Double> cuts = new ArrayList<>(List.of(minimum, maximum));
        for (SlopedCeilingProfile profile : room.slopedCeilingProfiles()) {
            double boundary = slopeBoundary(room, profile, xAxis, slopeRunMillimeters(room, profile));
            if (Double.isFinite(boundary) && boundary > minimum + EPSILON && boundary < maximum - EPSILON) {
                addDistinctCut(cuts, boundary);
            }
        }
        addOpposingSlopeIntersection(room, cuts, minimum, maximum, xAxis);
        cuts.sort(Double::compareTo);
        return List.copyOf(cuts);
    }

    private double slopeBoundary(Room room, SlopedCeilingProfile profile, boolean xAxis, double run) {
        return switch (profile.lowSide()) {
            case WEST -> xAxis ? room.minXMillimeters() + run : Double.NaN;
            case EAST -> xAxis ? room.maxXMillimeters() - run : Double.NaN;
            case NORTH -> !xAxis ? room.minYMillimeters() + run : Double.NaN;
            case SOUTH -> !xAxis ? room.maxYMillimeters() - run : Double.NaN;
        };
    }

    private void addOpposingSlopeIntersection(Room room, List<Double> cuts, double minimum, double maximum, boolean xAxis) {
        SlopedCeilingSide lowSide = xAxis ? SlopedCeilingSide.WEST : SlopedCeilingSide.NORTH;
        SlopedCeilingSide opposingSide = xAxis ? SlopedCeilingSide.EAST : SlopedCeilingSide.SOUTH;
        SlopedCeilingProfile first = profileAt(room, lowSide);
        SlopedCeilingProfile second = profileAt(room, opposingSide);
        if (first == null || second == null) {
            return;
        }
        double firstRun = slopeRunMillimeters(room, first);
        double secondRun = slopeRunMillimeters(room, second);
        double firstRise = room.roomHeight().toMillimeters() - first.kneeWallHeight().toMillimeters();
        double secondRise = room.roomHeight().toMillimeters() - second.kneeWallHeight().toMillimeters();
        if (firstRun <= EPSILON || secondRun <= EPSILON || firstRise <= EPSILON || secondRise <= EPSILON) {
            return;
        }
        double firstGradient = firstRise / firstRun;
        double secondGradient = secondRise / secondRun;
        double lowCoordinate = xAxis ? room.minXMillimeters() : room.minYMillimeters();
        double highCoordinate = xAxis ? room.maxXMillimeters() : room.maxYMillimeters();
        double intersection = (second.kneeWallHeight().toMillimeters() + secondGradient * highCoordinate
                - first.kneeWallHeight().toMillimeters() + firstGradient * lowCoordinate)
                / (firstGradient + secondGradient);
        if (intersection > lowCoordinate + EPSILON && intersection < lowCoordinate + firstRun - EPSILON
                && intersection > highCoordinate - secondRun + EPSILON && intersection < highCoordinate - EPSILON
                && intersection > minimum + EPSILON && intersection < maximum - EPSILON) {
            addDistinctCut(cuts, intersection);
        }
    }

    private SlopedCeilingProfile profileAt(Room room, SlopedCeilingSide side) {
        return room.slopedCeilingProfiles().stream().filter(profile -> profile.lowSide() == side).findFirst().orElse(null);
    }

    private SlopedCeilingProfile activeSlope(Room room, SurfaceRectangle section) {
        PlanPoint center = new PlanPoint(
                section.minXMillimeters() + section.widthMillimeters() / 2.0,
                section.minYMillimeters() + section.heightMillimeters() / 2.0
        );
        double ceilingHeight = room.ceilingHeightAt(center);
        if (ceilingHeight >= room.roomHeight().toMillimeters() - EPSILON) {
            return null;
        }
        return room.slopedCeilingProfiles().stream()
                .filter(profile -> Math.abs(room.ceilingHeightAt(center, profile) - ceilingHeight) <= EPSILON)
                .min(Comparator.comparingDouble(profile -> room.ceilingHeightAt(center, profile)))
                .orElse(null);
    }

    private SurfaceRectangle toSlopedSurfaceRectangle(Room room, SlopedCeilingProfile profile, SurfaceRectangle rectangle) {
        double run = slopeRunMillimeters(room, profile);
        double rise = room.roomHeight().toMillimeters() - profile.kneeWallHeight().toMillimeters();
        double scale = run <= EPSILON || rise <= EPSILON ? 1.0 : Math.hypot(run, rise) / run;
        return switch (profile.lowSide()) {
            case NORTH -> new SurfaceRectangle(rectangle.minXMillimeters(), rectangle.minYMillimeters() * scale,
                    rectangle.widthMillimeters(), rectangle.heightMillimeters() * scale);
            case SOUTH -> new SurfaceRectangle(rectangle.minXMillimeters(), -rectangle.maxYMillimeters() * scale,
                    rectangle.widthMillimeters(), rectangle.heightMillimeters() * scale);
            case WEST -> new SurfaceRectangle(rectangle.minXMillimeters() * scale, rectangle.minYMillimeters(),
                    rectangle.widthMillimeters() * scale, rectangle.heightMillimeters());
            case EAST -> new SurfaceRectangle(-rectangle.maxXMillimeters() * scale, rectangle.minYMillimeters(),
                    rectangle.widthMillimeters() * scale, rectangle.heightMillimeters());
        };
    }

    private double slopeRunMillimeters(Room room, SlopedCeilingProfile profile) {
        double roomRun = switch (profile.lowSide()) {
            case NORTH, SOUTH -> room.depthMillimeters();
            case EAST, WEST -> room.widthMillimeters();
        };
        double configuredRun = profile.horizontalRun().toMillimeters();
        return configuredRun <= 0.0 ? roomRun : Math.min(roomRun, configuredRun);
    }

    private void addDistinctCut(List<Double> cuts, double value) {
        if (cuts.stream().noneMatch(existing -> Math.abs(existing - value) <= EPSILON)) {
            cuts.add(value);
        }
    }

    record Coverage(String description, List<SurfaceRectangle> rectangles) {
    }

    record SurfaceRectangle(double minXMillimeters, double minYMillimeters, double widthMillimeters, double heightMillimeters) {

        double maxXMillimeters() {
            return minXMillimeters + widthMillimeters;
        }

        double maxYMillimeters() {
            return minYMillimeters + heightMillimeters;
        }
    }
}
