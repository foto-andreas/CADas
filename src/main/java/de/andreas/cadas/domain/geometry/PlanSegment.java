package de.andreas.cadas.domain.geometry;

public record PlanSegment(PlanPoint start, PlanPoint end) {

    public Length length() {
        return start.distanceTo(end);
    }

    public Angle angle() {
        return start.angleTo(end);
    }
}

