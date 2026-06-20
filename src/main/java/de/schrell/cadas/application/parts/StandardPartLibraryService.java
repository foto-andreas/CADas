package de.schrell.cadas.application.parts;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.model.StairType;

import java.util.List;

public final class StandardPartLibraryService {

    public StandardPartLibrary load() {
        return new StandardPartLibrary(
                List.of(
                        new DoorPreset("door-101x201", "Standardtür 101 x 201 cm", Length.of(1.01, LengthUnit.METER), Length.of(2.01, LengthUnit.METER), Length.zero()),
                        new DoorPreset("door-88x201", "Innentür 88 x 201 cm", Length.of(88, LengthUnit.CENTIMETER), Length.of(2.01, LengthUnit.METER), Length.zero()),
                        new DoorPreset("door-barrier-free", "Barrierefreie Tür 110 x 210 cm", Length.of(1.10, LengthUnit.METER), Length.of(2.10, LengthUnit.METER), Length.zero())
                ),
                List.of(
                        new WindowPreset("window-120x120", "Standardfenster 120 x 120 cm", Length.of(1.20, LengthUnit.METER), Length.of(1.20, LengthUnit.METER), Length.of(90, LengthUnit.CENTIMETER)),
                        new WindowPreset("window-180x120", "Breites Fenster 180 x 120 cm", Length.of(1.80, LengthUnit.METER), Length.of(1.20, LengthUnit.METER), Length.of(90, LengthUnit.CENTIMETER)),
                        new WindowPreset("window-floor", "Bodentiefes Element 120 x 220 cm", Length.of(1.20, LengthUnit.METER), Length.of(2.20, LengthUnit.METER), Length.zero())
                ),
                List.of(
                        new StairPreset("stair-straight", "Gerade Treppe", StairType.STRAIGHT, Length.of(2.80, LengthUnit.METER), 16),
                        new StairPreset("stair-half-turn", "Altbau 180°-Treppe", StairType.HALF_TURN, Length.of(2.90, LengthUnit.METER), 18),
                        new StairPreset("stair-switchback", "Gegenläufige Treppe ohne Podest", StairType.SWITCHBACK, Length.of(2.85, LengthUnit.METER), 18),
                        new StairPreset("stair-spiral", "Wendeltreppe", StairType.SPIRAL, Length.of(2.80, LengthUnit.METER), 15)
                )
        );
    }
}
