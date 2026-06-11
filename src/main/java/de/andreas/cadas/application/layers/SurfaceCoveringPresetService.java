package de.andreas.cadas.application.layers;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.model.SurfaceLayoutMode;

import java.nio.file.Path;
import java.util.List;

public final class SurfaceCoveringPresetService {

    public List<SurfaceCoveringPreset> defaults() {
        return List.of(
                new SurfaceCoveringPreset(
                        "tile-60x30",
                        "Fliese 60 x 30 cm",
                        Length.of(12, LengthUnit.MILLIMETER),
                        Length.of(60, LengthUnit.CENTIMETER),
                        Length.of(30, LengthUnit.CENTIMETER),
                        SurfaceLayoutMode.AUTOMATIC,
                        Length.zero(),
                        Length.of(10, LengthUnit.CENTIMETER),
                        Length.of(8, LengthUnit.CENTIMETER),
                        "Standard: Fliese"
                ),
                new SurfaceCoveringPreset(
                        "insulation-120x60",
                        "Dämmplatte 120 x 60 cm",
                        Length.of(40, LengthUnit.MILLIMETER),
                        Length.of(120, LengthUnit.CENTIMETER),
                        Length.of(60, LengthUnit.CENTIMETER),
                        SurfaceLayoutMode.FIXED,
                        Length.of(30, LengthUnit.CENTIMETER),
                        Length.of(12, LengthUnit.CENTIMETER),
                        Length.of(10, LengthUnit.CENTIMETER),
                        "Standard: Dämmplatte"
                ),
                new SurfaceCoveringPreset(
                        "rigips-200x125",
                        "Rigipsplatte 200 x 125 cm",
                        Length.of(12.5, LengthUnit.MILLIMETER),
                        Length.of(200, LengthUnit.CENTIMETER),
                        Length.of(125, LengthUnit.CENTIMETER),
                        SurfaceLayoutMode.NONE,
                        Length.zero(),
                        Length.zero(),
                        Length.of(15, LengthUnit.CENTIMETER),
                        "Standard: Rigips"
                ),
                new SurfaceCoveringPreset(
                        "osb-250x67,5",
                        "OSB-Platte 250 x 67,5 cm",
                        Length.of(18, LengthUnit.MILLIMETER),
                        Length.of(250, LengthUnit.CENTIMETER),
                        Length.of(67.5, LengthUnit.CENTIMETER),
                        SurfaceLayoutMode.FIXED,
                        Length.of(33.75, LengthUnit.CENTIMETER),
                        Length.of(10, LengthUnit.CENTIMETER),
                        Length.of(10, LengthUnit.CENTIMETER),
                        "Standard: OSB"
                ),
                new SurfaceCoveringPreset(
                        "tapete",
                        "Tapetenbahn",
                        Length.of(1, LengthUnit.MILLIMETER),
                        Length.of(53, LengthUnit.CENTIMETER),
                        Length.of(10, LengthUnit.METER),
                        SurfaceLayoutMode.NONE,
                        Length.zero(),
                        Length.zero(),
                        Length.of(5, LengthUnit.CENTIMETER),
                        "Standard: Tapete"
                )
        );
    }

    public SurfaceCoveringPreset fromDwg(Path path) {
        String fileName = path.getFileName().toString();
        String baseName = fileName.endsWith(".dwg") ? fileName.substring(0, fileName.length() - 4) : fileName;
        return new SurfaceCoveringPreset(
                "dwg-" + baseName.toLowerCase().replace(' ', '-'),
                "DWG-Referenz: " + baseName,
                Length.of(10, LengthUnit.MILLIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.zero(),
                Length.of(10, LengthUnit.CENTIMETER),
                path.toAbsolutePath().toString()
        );
    }

    public SurfaceCoveringPreset fromDwgBlock(Path path, String blockName) {
        String normalizedBlockName = blockName.trim();
        String fileName = path.getFileName().toString();
        String baseName = fileName.endsWith(".dwg") ? fileName.substring(0, fileName.length() - 4) : fileName;
        return new SurfaceCoveringPreset(
                "dwg-" + baseName.toLowerCase().replace(' ', '-') + "-" + normalizedBlockName.toLowerCase().replace(' ', '-'),
                "DWG-Block: " + normalizedBlockName,
                Length.of(10, LengthUnit.MILLIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                Length.of(100, LengthUnit.CENTIMETER),
                SurfaceLayoutMode.NONE,
                Length.zero(),
                Length.zero(),
                Length.of(10, LengthUnit.CENTIMETER),
                path.toAbsolutePath() + "#" + normalizedBlockName
        );
    }
}
