package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.Angle;
import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;
import java.util.Optional;

public final class DraftingService {

    public PlanSegment createSegment(PlanPoint start, PlanPoint rawEnd, DraftingConstraints constraints) {
        PlanPoint constrainedEnd = rawEnd;
        if (constraints.orthogonalMode() && constraints.manualAngle().isEmpty()) {
            constrainedEnd = orthogonalize(start, constrainedEnd);
        }

        constrainedEnd = applyManualAngle(start, constrainedEnd, constraints.manualAngle());
        constrainedEnd = applyManualLength(start, constrainedEnd, constraints.manualLength());
        return new PlanSegment(start, constrainedEnd);
    }

    private PlanPoint orthogonalize(PlanPoint start, PlanPoint end) {
        double deltaX = end.xMillimeters() - start.xMillimeters();
        double deltaY = end.yMillimeters() - start.yMillimeters();
        if (Math.abs(deltaX) >= Math.abs(deltaY)) {
            return new PlanPoint(end.xMillimeters(), start.yMillimeters());
        }
        return new PlanPoint(start.xMillimeters(), end.yMillimeters());
    }

    private PlanPoint applyManualAngle(PlanPoint start, PlanPoint end, Optional<Angle> manualAngle) {
        if (manualAngle.isEmpty()) {
            return end;
        }
        Length length = start.distanceTo(end);
        return polarPoint(start, manualAngle.get(), length);
    }

    private PlanPoint applyManualLength(PlanPoint start, PlanPoint end, Optional<Length> manualLength) {
        if (manualLength.isEmpty()) {
            return end;
        }
        Angle angle = start.angleTo(end);
        return polarPoint(start, angle, manualLength.get());
    }

    private PlanPoint polarPoint(PlanPoint start, Angle angle, Length length) {
        double x = start.xMillimeters() + Math.cos(angle.radians()) * length.toMillimeters();
        double y = start.yMillimeters() + Math.sin(angle.radians()) * length.toMillimeters();
        return new PlanPoint(x, y);
    }
}

