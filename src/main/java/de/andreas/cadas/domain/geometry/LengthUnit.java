package de.andreas.cadas.domain.geometry;

import java.math.BigDecimal;

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
}

