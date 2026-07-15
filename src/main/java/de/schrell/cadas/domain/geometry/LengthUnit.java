package de.schrell.cadas.domain.geometry;

import java.math.BigDecimal;

/**
 * Unterstützte Anzeige- und Eingabeeinheiten mit ihrem exakten Faktor zur internen Einheit Millimeter.
 */
public enum LengthUnit {
    MILLIMETER("mm", BigDecimal.ONE),
    CENTIMETER("cm", BigDecimal.TEN),
    METER("m", BigDecimal.valueOf(1000));

    private final String symbol;
    private final BigDecimal millimeterFactor;

    LengthUnit(String symbol, BigDecimal millimeterFactor) {
        this.symbol = symbol;
        this.millimeterFactor = millimeterFactor;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal millimeterFactor() {
        return millimeterFactor;
    }

    @Override
    public String toString() {
        return symbol;
    }
}
