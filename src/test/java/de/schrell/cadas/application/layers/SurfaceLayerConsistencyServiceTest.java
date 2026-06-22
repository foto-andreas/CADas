package de.schrell.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.LengthUnit;
import de.schrell.cadas.domain.model.SurfaceLayer;
import de.schrell.cadas.domain.model.SurfaceLayerStack;
import de.schrell.cadas.domain.model.SurfaceType;

import org.junit.jupiter.api.Test;

class SurfaceLayerConsistencyServiceTest {

    private final SurfaceLayerConsistencyService consistencyService = new SurfaceLayerConsistencyService();

    @Test
    void erkenntGleicheEbenenfolgen() {
        SurfaceLayer estrichA = SurfaceLayer.create("Estrich", Length.of(6, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.zero());
        SurfaceLayer flieseA = SurfaceLayer.create("Fliese", Length.of(1.2, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(30, LengthUnit.CENTIMETER), Length.of(10, LengthUnit.CENTIMETER));
        SurfaceLayer estrichB = SurfaceLayer.create("Estrich", Length.of(6, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.zero());
        SurfaceLayer flieseB = SurfaceLayer.create("Fliese", Length.of(1.2, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(30, LengthUnit.CENTIMETER), Length.of(10, LengthUnit.CENTIMETER));

        SurfaceLayerStack first = new SurfaceLayerStack(SurfaceType.FLOOR, "raum-a");
        first.addLayer(estrichA);
        first.addLayer(flieseA);
        SurfaceLayerStack second = new SurfaceLayerStack(SurfaceType.FLOOR, "raum-b");
        second.addLayer(estrichB);
        second.addLayer(flieseB);

        assertTrue(consistencyService.haveEqualSequence(first, second));
    }

    @Test
    void erkenntUnterschiedlicheEbenenfolgen() {
        SurfaceLayerStack first = new SurfaceLayerStack(SurfaceType.FLOOR, "raum-a");
        first.addLayer(SurfaceLayer.create("Estrich", Length.of(6, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.zero()));
        SurfaceLayerStack second = new SurfaceLayerStack(SurfaceType.FLOOR, "raum-b");
        second.addLayer(SurfaceLayer.create("Holz", Length.of(2, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.zero()));

        assertFalse(consistencyService.haveEqualSequence(first, second));
    }

    @Test
    void erkenntGedrehteVerlegerichtungAlsUnterschiedlicheEbenenfolge() {
        SurfaceLayerStack first = new SurfaceLayerStack(SurfaceType.FLOOR, "raum-a");
        first.addLayer(SurfaceLayer.create("Dielen", Length.of(2, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero()));
        SurfaceLayerStack second = new SurfaceLayerStack(SurfaceType.FLOOR, "raum-b");
        second.addLayer(SurfaceLayer.create("Dielen", Length.of(2, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero())
                .withLayoutRotatedQuarterTurn(true));

        assertFalse(consistencyService.haveEqualSequence(first, second));
    }
}
