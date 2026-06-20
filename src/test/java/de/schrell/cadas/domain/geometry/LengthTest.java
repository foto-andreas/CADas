package de.schrell.cadas.domain.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class LengthTest {

    @Test
    void rechnetZwischenAllenEinheitenVerlustarmUm() {
        Length length = Length.of(new BigDecimal("1.25"), LengthUnit.METER);

        assertEquals(new BigDecimal("1250.000000"), length.in(LengthUnit.MILLIMETER));
        assertEquals(new BigDecimal("125.000000"), length.in(LengthUnit.CENTIMETER));
        assertEquals(new BigDecimal("1.250000"), length.in(LengthUnit.METER));
    }

    @Test
    void formatiertMitEinheit() {
        Length length = Length.of(175, LengthUnit.CENTIMETER);

        assertEquals("1.75 m", length.format(LengthUnit.METER, 2));
    }
}
