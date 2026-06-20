package de.schrell.cadas.application.dwg;

import java.util.List;

public record Dxf3dObjectGeometry(Dxf3dBounds bounds, List<Dxf3dBounds> solidBounds, int sourceSolidCount) {

    public Dxf3dObjectGeometry {
        solidBounds = List.copyOf(solidBounds);
        if (sourceSolidCount < 1) {
            throw new IllegalArgumentException("Eine 3D-DXF-Geometrie braucht mindestens einen 3DSOLID-Körper.");
        }
    }
}
