package de.andreas.cadas.application.dwg;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record DwgBlockDefinition(
        Path sourceFile,
        String name,
        DwgUnit unit,
        double originXMillimeters,
        double originYMillimeters,
        DwgBounds bounds,
        List<String> layers,
        List<String> handles,
        List<DwgInsertReference> inserts,
        int entityCount,
        int unsupportedEntityCount,
        List<String> warnings
) {

    public DwgBlockDefinition {
        Objects.requireNonNull(sourceFile, "sourceFile darf nicht null sein.");
        Objects.requireNonNull(name, "name darf nicht null sein.");
        Objects.requireNonNull(unit, "unit darf nicht null sein.");
        Objects.requireNonNull(layers, "layers darf nicht null sein.");
        Objects.requireNonNull(handles, "handles darf nicht null sein.");
        Objects.requireNonNull(inserts, "inserts darf nicht null sein.");
        Objects.requireNonNull(warnings, "warnings darf nicht null sein.");
        layers = List.copyOf(layers);
        handles = List.copyOf(handles);
        inserts = List.copyOf(inserts);
        warnings = List.copyOf(warnings);
    }

    public boolean hasGeometry() {
        return bounds != null && bounds.widthMillimeters() > 0.001 && bounds.heightMillimeters() > 0.001;
    }

    public double widthMillimeters() {
        return hasGeometry() ? bounds.widthMillimeters() : 0.0;
    }

    public double heightMillimeters() {
        return hasGeometry() ? bounds.heightMillimeters() : 0.0;
    }

    public String sourceReference() {
        return sourceFile.toAbsolutePath().normalize() + "#" + name;
    }

    public String displayText() {
        String dimensions = hasGeometry()
                ? String.format(" %.0f x %.0f mm", widthMillimeters(), heightMillimeters())
                : " ohne auswertbare Geometrie";
        return name + " | " + dimensions + " | " + unit;
    }

    @Override
    public String toString() {
        return displayText();
    }
}
