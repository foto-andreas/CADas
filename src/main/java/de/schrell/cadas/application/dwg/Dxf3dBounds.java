package de.schrell.cadas.application.dwg;

public record Dxf3dBounds(
        double minXMillimeters,
        double minYMillimeters,
        double minZMillimeters,
        double maxXMillimeters,
        double maxYMillimeters,
        double maxZMillimeters
) {

    public Dxf3dBounds {
        if (!Double.isFinite(minXMillimeters)
                || !Double.isFinite(minYMillimeters)
                || !Double.isFinite(minZMillimeters)
                || !Double.isFinite(maxXMillimeters)
                || !Double.isFinite(maxYMillimeters)
                || !Double.isFinite(maxZMillimeters)
                || maxXMillimeters < minXMillimeters
                || maxYMillimeters < minYMillimeters
                || maxZMillimeters < minZMillimeters) {
            throw new IllegalArgumentException("3D-DXF-Grenzen müssen endlich und sortiert sein.");
        }
    }

    public double widthMillimeters() {
        return maxXMillimeters - minXMillimeters;
    }

    public double depthMillimeters() {
        return maxYMillimeters - minYMillimeters;
    }

    public double heightMillimeters() {
        return maxZMillimeters - minZMillimeters;
    }

    public double centerXMillimeters() {
        return (minXMillimeters + maxXMillimeters) / 2.0;
    }

    public double centerYMillimeters() {
        return (minYMillimeters + maxYMillimeters) / 2.0;
    }

    public double centerZMillimeters() {
        return (minZMillimeters + maxZMillimeters) / 2.0;
    }

    public Dxf3dBounds scale(double factor) {
        return new Dxf3dBounds(
                minXMillimeters * factor,
                minYMillimeters * factor,
                minZMillimeters * factor,
                maxXMillimeters * factor,
                maxYMillimeters * factor,
                maxZMillimeters * factor
        );
    }

    public Dxf3dBounds include(Dxf3dBounds other) {
        return new Dxf3dBounds(
                Math.min(minXMillimeters, other.minXMillimeters),
                Math.min(minYMillimeters, other.minYMillimeters),
                Math.min(minZMillimeters, other.minZMillimeters),
                Math.max(maxXMillimeters, other.maxXMillimeters),
                Math.max(maxYMillimeters, other.maxYMillimeters),
                Math.max(maxZMillimeters, other.maxZMillimeters)
        );
    }

    public Dxf3dBounds clampTo(Dxf3dBounds outer) {
        return new Dxf3dBounds(
                Math.max(minXMillimeters, outer.minXMillimeters),
                Math.max(minYMillimeters, outer.minYMillimeters),
                Math.max(minZMillimeters, outer.minZMillimeters),
                Math.min(maxXMillimeters, outer.maxXMillimeters),
                Math.min(maxYMillimeters, outer.maxYMillimeters),
                Math.min(maxZMillimeters, outer.maxZMillimeters)
        );
    }

    public boolean intersects(Dxf3dBounds other) {
        return maxXMillimeters >= other.minXMillimeters
                && minXMillimeters <= other.maxXMillimeters
                && maxYMillimeters >= other.minYMillimeters
                && minYMillimeters <= other.maxYMillimeters
                && maxZMillimeters >= other.minZMillimeters
                && minZMillimeters <= other.maxZMillimeters;
    }
}
