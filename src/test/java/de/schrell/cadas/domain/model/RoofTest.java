package de.schrell.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Angle;
import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;

import org.junit.jupiter.api.Test;

class RoofTest {

    @Test
    void projektKannEinSatteldachDefinieren() {
        ProjectModel model = ProjectModel.withDefaultLevel("Haus", "Erdgeschoss");
        model.defineRoof(new Roof(
                RoofType.SADDLE,
                Angle.ofDegrees(38),
                Length.of(45, LengthUnit.CENTIMETER),
                true
        ));

        assertTrue(model.roof().isPresent());
    }
}
