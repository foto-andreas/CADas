package de.andreas.cadas.application.dwg;

import java.util.Optional;

public record DwgBounds(
        double minXMillimeters,
        double minYMillimeters,
        double maxXMillimeters,
        double maxYMillimeters
) {

    public DwgBounds {
        if (maxXMillimeters < minXMillimeters || maxYMillimeters < minYMillimeters) {
            throw new IllegalArgumentException("DWG-Grenzen müssen sortiert sein.");
        }
    }

    public double widthMillimeters() {
        return maxXMillimeters - minXMillimeters;
    }

    public double heightMillimeters() {
        return maxYMillimeters - minYMillimeters;
    }

    public DwgBounds include(DwgBounds other) {
        return new DwgBounds(
                Math.min(minXMillimeters, other.minXMillimeters),
                Math.min(minYMillimeters, other.minYMillimeters),
                Math.max(maxXMillimeters, other.maxXMillimeters),
                Math.max(maxYMillimeters, other.maxYMillimeters)
        );
    }

    public static DwgBounds point(double xMillimeters, double yMillimeters) {
        return new DwgBounds(xMillimeters, yMillimeters, xMillimeters, yMillimeters);
    }

    public static Optional<DwgBounds> of(
            double minXDrawingUnits,
            double minYDrawingUnits,
            double maxXDrawingUnits,
            double maxYDrawingUnits,
            DwgUnit unit
    ) {
        if (!Double.isFinite(minXDrawingUnits)
                || !Double.isFinite(minYDrawingUnits)
                || !Double.isFinite(maxXDrawingUnits)
                || !Double.isFinite(maxYDrawingUnits)) {
            return Optional.empty();
        }
        double factor = unit.millimetersPerDrawingUnit();
        return Optional.of(new DwgBounds(
                Math.min(minXDrawingUnits, maxXDrawingUnits) * factor,
                Math.min(minYDrawingUnits, maxYDrawingUnits) * factor,
                Math.max(minXDrawingUnits, maxXDrawingUnits) * factor,
                Math.max(minYDrawingUnits, maxYDrawingUnits) * factor
        ));
    }
}
