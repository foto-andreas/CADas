package de.schrell.cadas.application.dwg;

import java.util.Arrays;

public record Dxf3dMesh(int sourceSolidIndex, Dxf3dBounds bounds, double[] triangleCoordinates) {

    public Dxf3dMesh {
        triangleCoordinates = Arrays.copyOf(triangleCoordinates, triangleCoordinates.length);
        if (sourceSolidIndex < 0) {
            throw new IllegalArgumentException("Der Quellkörper-Index darf nicht negativ sein.");
        }
        if (triangleCoordinates.length == 0 || triangleCoordinates.length % 9 != 0) {
            throw new IllegalArgumentException("Ein 3D-DXF-Netz braucht vollständige Dreiecke.");
        }
        for (double coordinate : triangleCoordinates) {
            if (!Double.isFinite(coordinate)) {
                throw new IllegalArgumentException("3D-DXF-Netzkoordinaten müssen endlich sein.");
            }
        }
    }

    @Override
    public double[] triangleCoordinates() {
        return Arrays.copyOf(triangleCoordinates, triangleCoordinates.length);
    }

    public int triangleCount() {
        return triangleCoordinates.length / 9;
    }

    static Dxf3dMesh box(int sourceSolidIndex, Dxf3dBounds bounds) {
        double x0 = bounds.minXMillimeters();
        double x1 = bounds.maxXMillimeters();
        double y0 = bounds.minYMillimeters();
        double y1 = bounds.maxYMillimeters();
        double z0 = bounds.minZMillimeters();
        double z1 = bounds.maxZMillimeters();
        return new Dxf3dMesh(sourceSolidIndex, bounds, new double[]{
                x0, y0, z0, x1, y0, z0, x1, y1, z0,
                x0, y0, z0, x1, y1, z0, x0, y1, z0,
                x0, y0, z1, x1, y1, z1, x1, y0, z1,
                x0, y0, z1, x0, y1, z1, x1, y1, z1,
                x0, y0, z0, x0, y1, z0, x0, y1, z1,
                x0, y0, z0, x0, y1, z1, x0, y0, z1,
                x1, y0, z0, x1, y0, z1, x1, y1, z1,
                x1, y0, z0, x1, y1, z1, x1, y1, z0,
                x0, y0, z0, x0, y0, z1, x1, y0, z1,
                x0, y0, z0, x1, y0, z1, x1, y0, z0,
                x0, y1, z0, x1, y1, z0, x1, y1, z1,
                x0, y1, z0, x1, y1, z1, x0, y1, z1
        });
    }

    public Dxf3dMesh scale(double factor) {
        double[] scaled = triangleCoordinates();
        for (int index = 0; index < scaled.length; index++) {
            scaled[index] *= factor;
        }
        return new Dxf3dMesh(sourceSolidIndex, bounds.scale(factor), scaled);
    }
}
