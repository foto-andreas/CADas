package de.schrell.cadas.domain.model;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanSegment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Wall(
        UUID id,
        PlanSegment axis,
        Length thickness,
        Length height,
        Length startHeight,
        Length endHeight,
        List<WallProfilePoint> profile
) {

    private static final double EPSILON = 0.001;

    public Wall {
        Objects.requireNonNull(id, "id darf nicht null sein.");
        Objects.requireNonNull(axis, "axis darf nicht null sein.");
        Objects.requireNonNull(thickness, "thickness darf nicht null sein.");
        Objects.requireNonNull(height, "height darf nicht null sein.");
        Objects.requireNonNull(startHeight, "startHeight darf nicht null sein.");
        Objects.requireNonNull(endHeight, "endHeight darf nicht null sein.");
        Objects.requireNonNull(profile, "profile darf nicht null sein.");
        profile = List.copyOf(profile);
        validateProfile(axis, profile);
        if (!profile.isEmpty()) {
            startHeight = profile.getFirst().height();
            endHeight = profile.getLast().height();
            height = Length.ofMillimeters(profile.stream()
                    .mapToDouble(point -> point.height().toMillimeters())
                    .max()
                    .orElse(height.toMillimeters()));
        }
    }

    public Wall(UUID id, PlanSegment axis, Length thickness, Length height, Length startHeight, Length endHeight) {
        this(id, axis, thickness, height, startHeight, endHeight, List.of());
    }

    public Wall(UUID id, PlanSegment axis, Length thickness, Length height) {
        this(id, axis, thickness, height, height, height);
    }

    public static Wall create(PlanSegment axis, Length thickness, Length height) {
        return new Wall(UUID.randomUUID(), axis, thickness, height);
    }

    public double minimumHeightMillimeters() {
        return resolvedProfile().stream().mapToDouble(point -> point.height().toMillimeters()).min().orElse(0.0);
    }

    public double maximumHeightMillimeters() {
        return resolvedProfile().stream().mapToDouble(point -> point.height().toMillimeters()).max().orElse(0.0);
    }

    public double heightAt(double offsetMillimeters) {
        double totalLength = axis.length().toMillimeters();
        if (totalLength <= EPSILON) {
            return startHeight.toMillimeters();
        }
        double clampedOffset = Math.max(0.0, Math.min(totalLength, offsetMillimeters));
        List<WallProfilePoint> points = resolvedProfile();
        for (int index = 1; index < points.size(); index++) {
            WallProfilePoint previous = points.get(index - 1);
            WallProfilePoint next = points.get(index);
            if (clampedOffset <= next.offset().toMillimeters() + EPSILON) {
                double interval = next.offset().toMillimeters() - previous.offset().toMillimeters();
                if (interval <= EPSILON) {
                    return next.height().toMillimeters();
                }
                double ratio = (clampedOffset - previous.offset().toMillimeters()) / interval;
                return previous.height().toMillimeters()
                        + (next.height().toMillimeters() - previous.height().toMillimeters()) * ratio;
            }
        }
        return points.getLast().height().toMillimeters();
    }

    public double heightAtStart() {
        return startHeight.toMillimeters();
    }

    public double heightAtEnd() {
        return endHeight.toMillimeters();
    }

    public boolean hasVariableTopHeight() {
        double reference = resolvedProfile().getFirst().height().toMillimeters();
        return resolvedProfile().stream().anyMatch(point -> Math.abs(point.height().toMillimeters() - reference) > EPSILON);
    }

    public boolean hasPolygonalProfile() {
        return profile.size() > 2;
    }

    public List<WallProfilePoint> resolvedProfile() {
        if (!profile.isEmpty()) {
            return profile;
        }
        return List.of(
                new WallProfilePoint(Length.zero(), startHeight),
                new WallProfilePoint(axis.length(), endHeight)
        );
    }

    public Wall withEndpointHeights(Length newStartHeight, Length newEndHeight) {
        if (!profile.isEmpty()) {
            List<WallProfilePoint> updatedProfile = new ArrayList<>(profile);
            updatedProfile.set(0, new WallProfilePoint(Length.zero(), newStartHeight));
            updatedProfile.set(updatedProfile.size() - 1, new WallProfilePoint(axis.length(), newEndHeight));
            return withProfile(updatedProfile);
        }
        double maximumHeight = Math.max(newStartHeight.toMillimeters(), newEndHeight.toMillimeters());
        return new Wall(id, axis, thickness, Length.ofMillimeters(maximumHeight), newStartHeight, newEndHeight);
    }

    public Wall withProfile(List<WallProfilePoint> newProfile) {
        return new Wall(id, axis, thickness, height, startHeight, endHeight, newProfile);
    }

    public Wall withAxis(PlanSegment newAxis) {
        if (profile.isEmpty()) {
            return new Wall(id, newAxis, thickness, height, startHeight, endHeight);
        }
        double oldLength = Math.max(EPSILON, axis.length().toMillimeters());
        double newLength = newAxis.length().toMillimeters();
        List<WallProfilePoint> scaledProfile = profile.stream()
                .map(point -> new WallProfilePoint(
                        Length.ofMillimeters(point.offset().toMillimeters() / oldLength * newLength),
                        point.height()
                ))
                .toList();
        return new Wall(id, newAxis, thickness, height, startHeight, endHeight, scaledProfile);
    }

    private static void validateProfile(PlanSegment axis, List<WallProfilePoint> profile) {
        if (profile.isEmpty()) {
            return;
        }
        if (profile.size() < 2) {
            throw new IllegalArgumentException("Ein Wandprofil braucht mindestens zwei Punkte.");
        }
        List<WallProfilePoint> sorted = profile.stream().sorted(Comparator.comparing(WallProfilePoint::offset)).toList();
        if (!sorted.equals(profile)) {
            throw new IllegalArgumentException("Wandprofilpunkte müssen nach ihrem Abstand sortiert sein.");
        }
        double wallLength = axis.length().toMillimeters();
        if (Math.abs(profile.getFirst().offset().toMillimeters()) > EPSILON
                || Math.abs(profile.getLast().offset().toMillimeters() - wallLength) > EPSILON) {
            throw new IllegalArgumentException("Das Wandprofil muss an beiden Wandenden abschließen.");
        }
        for (int index = 1; index < profile.size(); index++) {
            if (profile.get(index).offset().toMillimeters() - profile.get(index - 1).offset().toMillimeters() <= EPSILON) {
                throw new IllegalArgumentException("Wandprofilpunkte müssen unterschiedliche Abstände besitzen.");
            }
        }
    }
}
