package de.schrell.cadas.application.terrain;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPolygonSupport;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Terrain;
import de.schrell.cadas.domain.model.TerrainVertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Projiziert Geländepunkte auf die Gebäudeaußenkontur und interpoliert daraus
 * einen glatten Höhenverlauf entlang des Geländebands.
 */
public final class TerrainProfileService {

    public static final double BAND_WIDTH_MILLIMETERS = 400.0;

    private static final double EPSILON = 0.001;
    private static final double BAND_LABEL_OFFSET = BAND_WIDTH_MILLIMETERS / 2.0;
    private static final double MAX_SAMPLE_STEP_MILLIMETERS = 250.0;

    public Optional<ProjectedTerrainPoint> projectToBand(PlanPoint point, List<PlanPoint> contour) {
        if (contour.size() < 3 || containsPoint(contour, point)) {
            return Optional.empty();
        }
        Optional<ProjectedTerrainPoint> projection = projectToContour(point, contour);
        if (projection.isEmpty()) {
            return Optional.empty();
        }
        ProjectedTerrainPoint projectedPoint = projection.orElseThrow();
        double distanceAlongNormal = (point.xMillimeters() - projectedPoint.contourPoint().xMillimeters()) * projectedPoint.normalX()
                + (point.yMillimeters() - projectedPoint.contourPoint().yMillimeters()) * projectedPoint.normalY();
        if (distanceAlongNormal < -EPSILON || distanceAlongNormal > BAND_WIDTH_MILLIMETERS + EPSILON) {
            return Optional.empty();
        }
        return Optional.of(projectedPoint);
    }

    public Optional<ProjectedTerrainPoint> projectToContour(PlanPoint point, List<PlanPoint> contour) {
        if (contour.size() < 2) {
            return Optional.empty();
        }
        double signedArea = signedArea(contour);
        double bestDistance = Double.POSITIVE_INFINITY;
        ProjectedTerrainPoint bestProjection = null;
        double accumulatedDistance = 0.0;
        for (int index = 0; index < contour.size(); index++) {
            PlanPoint start = contour.get(index);
            PlanPoint end = contour.get((index + 1) % contour.size());
            SegmentProjection projection = projectToSegment(point, start, end, signedArea);
            if (projection.distanceToPoint() < bestDistance) {
                double contourDistance = accumulatedDistance + projection.localDistance();
                bestDistance = projection.distanceToPoint();
                bestProjection = new ProjectedTerrainPoint(
                        projection.projectedPoint(),
                        offsetPoint(projection.projectedPoint(), projection.normalX(), projection.normalY(), BAND_LABEL_OFFSET),
                        offsetPoint(projection.projectedPoint(), projection.normalX(), projection.normalY(), BAND_WIDTH_MILLIMETERS),
                        contourDistance,
                        index,
                        projection.normalX(),
                        projection.normalY()
                );
            }
            accumulatedDistance += start.distanceTo(end).toMillimeters();
        }
        return Optional.ofNullable(bestProjection);
    }

    public List<ProjectedTerrainPoint> projectedSamples(Terrain terrain, List<PlanPoint> contour) {
        Map<Long, ProjectedTerrainPoint> projections = new LinkedHashMap<>();
        for (TerrainVertex vertex : terrain.vertices()) {
            projectToContour(vertex.position(), contour).ifPresent(projectedPoint -> projections.put(
                    roundedDistanceKey(projectedPoint.contourDistance()),
                    projectedPoint.withElevation(vertex.elevationAboveLowestFloor())
            ));
        }
        return projections.values().stream()
                .sorted(Comparator.comparingDouble(ProjectedTerrainPoint::contourDistance))
                .toList();
    }

    public double interpolatedElevationMillimeters(Terrain terrain, List<PlanPoint> contour, double contourDistance) {
        List<ProjectedTerrainPoint> samples = projectedSamples(terrain, contour);
        if (samples.isEmpty()) {
            return 0.0;
        }
        if (samples.size() == 1) {
            return samples.getFirst().elevation().toMillimeters();
        }
        double totalLength = contourLength(contour);
        if (totalLength <= EPSILON) {
            return samples.getFirst().elevation().toMillimeters();
        }
        double normalizedDistance = normalizeDistance(contourDistance, totalLength);
        for (int index = 0; index < samples.size(); index++) {
            ProjectedTerrainPoint current = samples.get(index);
            ProjectedTerrainPoint next = samples.get((index + 1) % samples.size());
            double startDistance = current.contourDistance();
            double endDistance = next.contourDistance();
            if (index == samples.size() - 1) {
                endDistance += totalLength;
            }
            double candidateDistance = normalizedDistance;
            if (candidateDistance < startDistance) {
                candidateDistance += totalLength;
            }
            if (candidateDistance + EPSILON < startDistance || candidateDistance - EPSILON > endDistance) {
                continue;
            }
            double span = Math.max(EPSILON, endDistance - startDistance);
            double t = (candidateDistance - startDistance) / span;
            double smooth = 0.5 - Math.cos(Math.PI * t) * 0.5;
            return current.elevation().toMillimeters()
                    + (next.elevation().toMillimeters() - current.elevation().toMillimeters()) * smooth;
        }
        return samples.getFirst().elevation().toMillimeters();
    }

    public List<StripSample> sampledStrip(Terrain terrain, List<PlanPoint> contour) {
        if (contour.size() < 3) {
            return List.of();
        }
        double totalLength = contourLength(contour);
        if (totalLength <= EPSILON) {
            return List.of();
        }
        List<Double> distances = sampleDistances(terrain, contour, totalLength);
        List<StripSample> samples = new ArrayList<>();
        for (double distance : distances) {
            ProjectedTerrainPoint point = pointAtDistance(contour, distance);
            samples.add(new StripSample(
                    point.contourPoint(),
                    point.bandPoint(),
                    point.outerPoint(),
                    interpolatedElevationMillimeters(terrain, contour, distance)
            ));
        }
        return List.copyOf(samples);
    }

    public ProjectedTerrainPoint pointAtDistance(List<PlanPoint> contour, double contourDistance) {
        double totalLength = contourLength(contour);
        double normalizedDistance = normalizeDistance(contourDistance, totalLength);
        double signedArea = signedArea(contour);
        double accumulatedDistance = 0.0;
        for (int index = 0; index < contour.size(); index++) {
            PlanPoint start = contour.get(index);
            PlanPoint end = contour.get((index + 1) % contour.size());
            double segmentLength = start.distanceTo(end).toMillimeters();
            if (segmentLength <= EPSILON) {
                continue;
            }
            if (normalizedDistance <= accumulatedDistance + segmentLength + EPSILON) {
                double localDistance = normalizedDistance - accumulatedDistance;
                double t = localDistance / segmentLength;
                double normalX = outwardNormalX(start, end, signedArea);
                double normalY = outwardNormalY(start, end, signedArea);
                PlanPoint contourPoint = new PlanPoint(
                        start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * t,
                        start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * t
                );
                return new ProjectedTerrainPoint(
                        contourPoint,
                        offsetPoint(contourPoint, normalX, normalY, BAND_LABEL_OFFSET),
                        offsetPoint(contourPoint, normalX, normalY, BAND_WIDTH_MILLIMETERS),
                        normalizedDistance,
                        index,
                        normalX,
                        normalY
                );
            }
            accumulatedDistance += segmentLength;
        }
        return projectToContour(contour.getFirst(), contour).orElseThrow();
    }

    private List<Double> sampleDistances(Terrain terrain, List<PlanPoint> contour, double totalLength) {
        TreeSet<Double> distances = new TreeSet<>();
        distances.add(0.0);
        double accumulatedDistance = 0.0;
        for (int index = 0; index < contour.size(); index++) {
            PlanPoint start = contour.get(index);
            PlanPoint end = contour.get((index + 1) % contour.size());
            double segmentLength = start.distanceTo(end).toMillimeters();
            distances.add(accumulatedDistance);
            for (double step = MAX_SAMPLE_STEP_MILLIMETERS; step < segmentLength - EPSILON; step += MAX_SAMPLE_STEP_MILLIMETERS) {
                distances.add(accumulatedDistance + step);
            }
            accumulatedDistance += segmentLength;
        }
        projectedSamples(terrain, contour).forEach(sample -> distances.add(sample.contourDistance()));
        return distances.stream()
                .filter(distance -> distance < totalLength - EPSILON)
                .toList();
    }

    private SegmentProjection projectToSegment(PlanPoint point, PlanPoint start, PlanPoint end, double signedArea) {
        double dx = end.xMillimeters() - start.xMillimeters();
        double dy = end.yMillimeters() - start.yMillimeters();
        double length = Math.max(EPSILON, Math.hypot(dx, dy));
        double projection = ((point.xMillimeters() - start.xMillimeters()) * dx
                + (point.yMillimeters() - start.yMillimeters()) * dy) / (length * length);
        double clamped = Math.max(0.0, Math.min(1.0, projection));
        PlanPoint projectedPoint = new PlanPoint(
                start.xMillimeters() + dx * clamped,
                start.yMillimeters() + dy * clamped
        );
        return new SegmentProjection(
                projectedPoint,
                clamped * length,
                projectedPoint.distanceTo(point).toMillimeters(),
                outwardNormalX(start, end, signedArea),
                outwardNormalY(start, end, signedArea)
        );
    }

    private double contourLength(List<PlanPoint> contour) {
        double length = 0.0;
        for (int index = 0; index < contour.size(); index++) {
            length += contour.get(index).distanceTo(contour.get((index + 1) % contour.size())).toMillimeters();
        }
        return length;
    }

    private double normalizeDistance(double distance, double totalLength) {
        if (totalLength <= EPSILON) {
            return 0.0;
        }
        double normalized = distance % totalLength;
        return normalized < 0.0 ? normalized + totalLength : normalized;
    }

    private long roundedDistanceKey(double distance) {
        return Math.round(distance * 1000.0);
    }

    private double outwardNormalX(PlanPoint start, PlanPoint end, double signedArea) {
        double dx = end.xMillimeters() - start.xMillimeters();
        double dy = end.yMillimeters() - start.yMillimeters();
        double length = Math.max(EPSILON, Math.hypot(dx, dy));
        return signedArea > 0.0 ? dy / length : -dy / length;
    }

    private double outwardNormalY(PlanPoint start, PlanPoint end, double signedArea) {
        double dx = end.xMillimeters() - start.xMillimeters();
        double dy = end.yMillimeters() - start.yMillimeters();
        double length = Math.max(EPSILON, Math.hypot(dx, dy));
        return signedArea > 0.0 ? -dx / length : dx / length;
    }

    private double signedArea(List<PlanPoint> points) {
        double area = 0.0;
        for (int index = 0; index < points.size(); index++) {
            PlanPoint current = points.get(index);
            PlanPoint next = points.get((index + 1) % points.size());
            area += current.xMillimeters() * next.yMillimeters() - next.xMillimeters() * current.yMillimeters();
        }
        return area / 2.0;
    }

    private PlanPoint offsetPoint(PlanPoint point, double normalX, double normalY, double distance) {
        return new PlanPoint(
                point.xMillimeters() + normalX * distance,
                point.yMillimeters() + normalY * distance
        );
    }

    private boolean containsPoint(List<PlanPoint> outline, PlanPoint point) {
        return PlanPolygonSupport.containsPoint(outline, point);
    }

    public record ProjectedTerrainPoint(
            PlanPoint contourPoint,
            PlanPoint bandPoint,
            PlanPoint outerPoint,
            double contourDistance,
            int segmentIndex,
            double normalX,
            double normalY,
            Length elevation
    ) {
        private ProjectedTerrainPoint(
                PlanPoint contourPoint,
                PlanPoint bandPoint,
                PlanPoint outerPoint,
                double contourDistance,
                int segmentIndex,
                double normalX,
                double normalY
        ) {
            this(contourPoint, bandPoint, outerPoint, contourDistance, segmentIndex, normalX, normalY, Length.zero());
        }

        private ProjectedTerrainPoint withElevation(Length newElevation) {
            return new ProjectedTerrainPoint(
                    contourPoint,
                    bandPoint,
                    outerPoint,
                    contourDistance,
                    segmentIndex,
                    normalX,
                    normalY,
                    newElevation
            );
        }
    }

    public record StripSample(
            PlanPoint contourPoint,
            PlanPoint bandPoint,
            PlanPoint outerPoint,
            double elevationMillimeters
    ) {
    }

    private record SegmentProjection(
            PlanPoint projectedPoint,
            double localDistance,
            double distanceToPoint,
            double normalX,
            double normalY
    ) {
    }
}
