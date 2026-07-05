package de.schrell.cadas.application.drawing;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.geometry.PlanSegment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionLabelServiceTest {

    @Test
    void liefertVollständigenTextFürRaummaßMitNamen() {
        WallDimensionService.SideDimension dimension = sideDimension("Küche", 900);
        String label = DimensionLabelService.label(dimension, false, DimensionTextStyle.FULL);
        assertEquals("Küche: Raummaß 0,90 m", label);
    }

    @Test
    void liefertVollständigenTextFürAußenmaß() {
        WallDimensionService.SideDimension dimension = sideDimension("Außen", 4_200);
        String label = DimensionLabelService.label(dimension, true, DimensionTextStyle.FULL);
        assertEquals("Außenmaß 4,20 m", label);
    }

    @Test
    void liefertNurLängeWennTextstilNurLänge() {
        WallDimensionService.SideDimension dimension = sideDimension("Küche", 900);
        String label = DimensionLabelService.label(dimension, false, DimensionTextStyle.LENGTH_ONLY);
        assertEquals("0,90 m", label);
    }

    @Test
    void liefertNurLängeAuchFürAußenmaß() {
        WallDimensionService.SideDimension dimension = sideDimension("Außen", 4_200);
        String label = DimensionLabelService.label(dimension, true, DimensionTextStyle.LENGTH_ONLY);
        assertEquals("4,20 m", label);
    }

    @Test
    void liefertNackteLängeFürAchsmaßOhneNamen() {
        String label = DimensionLabelService.label("Achsmaß", Length.ofMillimeters(8_000), false, DimensionTextStyle.LENGTH_ONLY);
        assertEquals("8,00 m", label);
    }

    @Test
    void liefertNackteLängeWennNameLeer() {
        String label = DimensionLabelService.label("", Length.ofMillimeters(8_000), false, DimensionTextStyle.FULL);
        assertEquals("8,00 m", label);
    }

    @Test
    void formatMetersNutztDeutschesFormat() {
        assertTrue(DimensionLabelService.formatMeters(4_200).contains("4,20 m"));
        assertTrue(DimensionLabelService.formatMeters(900).contains("0,90 m"));
    }

    @Test
    void dedupliziertGleicheRaumdimensionAufParallelenWänden() {
        WallDimensionService.SideDimension first = new WallDimensionService.SideDimension(
                "Küche", Length.ofMillimeters(900), -1.0,
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(900, 0)), "Raum:1"
        );
        WallDimensionService.SideDimension opposite = new WallDimensionService.SideDimension(
                "Küche", Length.ofMillimeters(900), 1.0,
                new PlanSegment(new PlanPoint(900, 500), new PlanPoint(0, 500)), "Raum:1"
        );

        assertEquals(DimensionLabelService.deduplicationKey(first, false), DimensionLabelService.deduplicationKey(opposite, false));
    }

    @Test
    void behältGleicheLängeInSenkrechterRichtungAlsEigenesMaß() {
        WallDimensionService.SideDimension horizontal = new WallDimensionService.SideDimension(
                "Quadrat", Length.ofMillimeters(900), -1.0,
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(900, 0)), "Raum:1"
        );
        WallDimensionService.SideDimension vertical = new WallDimensionService.SideDimension(
                "Quadrat", Length.ofMillimeters(900), 1.0,
                new PlanSegment(new PlanPoint(0, 0), new PlanPoint(0, 900)), "Raum:1"
        );

        org.junit.jupiter.api.Assertions.assertNotEquals(
                DimensionLabelService.deduplicationKey(horizontal, false),
                DimensionLabelService.deduplicationKey(vertical, false)
        );
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
