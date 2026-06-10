package de.andreas.cadas.domain.geometry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

public final class Length implements Comparable<Length> {

    private static final int SCALE = 6;
    private final BigDecimal millimeters;

    private Length(BigDecimal millimeters) {
        this.millimeters = millimeters.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Length zero() {
        return new Length(BigDecimal.ZERO);
    }

    public static Length of(double value, LengthUnit unit) {
        return of(BigDecimal.valueOf(value), unit);
    }

    public static Length of(BigDecimal value, LengthUnit unit) {
        Objects.requireNonNull(value, "value darf nicht null sein.");
        Objects.requireNonNull(unit, "unit darf nicht null sein.");
        return new Length(value.multiply(unit.millimeterFactor()));
    }

    public static Length ofMillimeters(double millimeters) {
        return new Length(BigDecimal.valueOf(millimeters));
    }

    public BigDecimal in(LengthUnit unit) {
        return millimeters.divide(unit.millimeterFactor(), SCALE, RoundingMode.HALF_UP);
    }

    public double toMillimeters() {
        return millimeters.doubleValue();
    }

    public Length add(Length other) {
        return new Length(millimeters.add(other.millimeters));
    }

    public Length subtract(Length other) {
        return new Length(millimeters.subtract(other.millimeters));
    }

    public Length multiply(double factor) {
        return new Length(millimeters.multiply(BigDecimal.valueOf(factor)));
    }

    public String format(LengthUnit unit, int scale) {
        return in(unit).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " " + unit.symbol();
    }

    @Override
    public int compareTo(Length other) {
        return millimeters.compareTo(other.millimeters);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Length other)) {
            return false;
        }
        return millimeters.compareTo(other.millimeters) == 0;
    }

    @Override
    public int hashCode() {
        return millimeters.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return String.format(Locale.GERMAN, "%s mm", millimeters.stripTrailingZeros().toPlainString());
    }
}

