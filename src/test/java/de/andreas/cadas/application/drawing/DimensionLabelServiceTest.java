package de.andreas.cadas.application.drawing;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.geometry.PlanSegment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionLabelServiceTest {

    private final DimensionLabelService service = new DimensionLabelService();

    @Test
    void liefertVollständigenTextFürRaummaßMitNamen() {
        WallDimensionService.SideDimension dimension = sideDimension("Küche", 900);
        String label = service.label(dimension, false, DimensionLabelOptions.full());
        assertEquals("Küche: Raummaß 0,90 m", label);
    }

    @Test
    void liefertVollständigenTextFürAußenmaß() {
        WallDimensionService.SideDimension dimension = sideDimension("Außen", 4_200);
        String label = service.label(dimension, true, DimensionLabelOptions.full());
        assertEquals("Außenmaß 4,20 m", label);
    }

    @Test
    void liefertNurLängeWennTextstilNurLänge() {
        WallDimensionService.SideDimension dimension = sideDimension("Küche", 900);
        String label = service.label(dimension, false, DimensionLabelOptions.lengthOnly());
        assertEquals("0,90 m", label);
    }

    @Test
    void liefertNurLängeAuchFürAußenmaß() {
        WallDimensionService.SideDimension dimension = sideDimension("Außen", 4_200);
        String label = service.label(dimension, true, DimensionLabelOptions.lengthOnly());
        assertEquals("4,20 m", label);
    }

    @Test
    void liefertNackteLängeFürAchsmaßOhneNamen() {
        String label = service.label("Achsmaß", Length.ofMillimeters(8_000), false, DimensionLabelOptions.lengthOnly());
        assertEquals("8,00 m", label);
    }

    @Test
    void liefertNackteLängeWennNameLeer() {
        String label = service.label("", Length.ofMillimeters(8_000), false, DimensionLabelOptions.full());
        assertEquals("8,00 m", label);
    }

    @Test
    void formatMetersNutztDeutschesFormat() {
        assertTrue(service.formatMeters(4_200).contains("4,20 m"));
        assertTrue(service.formatMeters(900).contains("0,90 m"));
    }

    private WallDimensionService.SideDimension sideDimension(String name, double millimeters) {
        return new WallDimensionService.SideDimension(
                name,
                Length.ofMillimeters(millimeters),
                -1.0,
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(millimeters, 0))
        );
    }
}