package de.andreas.cadas.domain.geometry;

public record PlanSegment(PlanPoint start, PlanPoint end) {

    public Length length() {
        return start.distanceTo(end);
    }

    public Angle angle() {
        return start.angleTo(end);
    }

    public PlanPoint pointAt(Length distanceFromStart) {
        double totalLength = length().toMillimeters();
        if (totalLength == 0.0) {
            return start;
        }
        double ratio = distanceFromStart.toMillimeters() / totalLength;
        return new PlanPoint(
                start.xMillimeters() + (end.xMillimeters() - start.xMillimeters()) * ratio,
                start.yMillimeters() + (end.yMillimeters() - start.yMillimeters()) * ratio
        );
    }

    public Length projectedLength(PlanPoint point) {
        double dx = end.xMillimeters() - start.xMillimeters();
        double dy = end.yMillimeters() - start.yMillimeters();
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0.0) {
            return Length.zero();
        }
        double projection = ((point.xMillimeters() - start.xMillimeters()) * dx + (point.yMillimeters() - start.yMillimeters()) * dy) / lengthSquared;
        double clampedProjection = Math.max(0.0, Math.min(1.0, projection));
        return length().multiply(clampedProjection);
    }

    public Length distanceTo(PlanPoint point) {
        PlanPoint nearestPoint = nearestPoint(point);
        return nearestPoint.distanceTo(point);
    }

    public PlanPoint nearestPoint(PlanPoint point) {
        double dx = end.xMillimeters() - start.xMillimeters();
        double dy = end.yMillimeters() - start.yMillimeters();
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0.0) {
            return start;
        }
        double projection = ((point.xMillimeters() - start.xMillimeters()) * dx + (point.yMillimeters() - start.yMillimeters()) * dy) / lengthSquared;
        double clampedProjection = Math.max(0.0, Math.min(1.0, projection));
        return new PlanPoint(
                start.xMillimeters() + dx * clampedProjection,
                start.yMillimeters() + dy * clampedProjection
        );
    }
}
