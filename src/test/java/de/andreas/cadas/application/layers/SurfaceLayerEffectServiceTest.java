package de.andreas.cadas.application.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.andreas.cadas.domain.geometry.Length;
import de.andreas.cadas.domain.geometry.LengthUnit;
import de.andreas.cadas.domain.geometry.PlanPoint;
import de.andreas.cadas.domain.model.Level;
import de.andreas.cadas.domain.model.Room;
import de.andreas.cadas.domain.model.SurfaceLayer;
import de.andreas.cadas.domain.model.SurfaceLayerStack;
import de.andreas.cadas.domain.model.SurfaceType;

import org.junit.jupiter.api.Test;

class SurfaceLayerEffectServiceTest {

    private final SurfaceLayerEffectService effectService = new SurfaceLayerEffectService();

    @Test
    void reduziertLichteHoeheUndVolumenDurchSichtbareBodenUndDeckenlagen() {
        Level level = new Level("Dachgeschoss");
        Room room = Room.rectangular(
                "Studio",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3000),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);

        SurfaceLayerStack floor = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        floor.addLayer(SurfaceLayer.create("Fliese", Length.of(1.2, LengthUnit.CENTIMETER), Length.of(60, LengthUnit.CENTIMETER), Length.of(30, LengthUnit.CENTIMETER), Length.of(10, LengthUnit.CENTIMETER)));
        SurfaceLayerStack ceiling = new SurfaceLayerStack(SurfaceType.CEILING, room.id().toString());
        ceiling.addLayer(SurfaceLayer.create("Rigips", Length.of(1.25, LengthUnit.CENTIMETER), Length.of(200, LengthUnit.CENTIMETER), Length.of(125, LengthUnit.CENTIMETER), Length.zero()));
        level.addSurfaceLayerStack(floor);
        level.addSurfaceLayerStack(ceiling);

        assertEquals(2775.5, effectService.effectiveMaximumCeilingHeightMillimeters(level, room), 0.001);
        assertTrue(effectService.effectiveVolumeCubicMeters(level, room) < room.volumeCubicMeters());
    }

    @Test
    void summiertGestapelteSichtbareLagenIterativUndIgnoriertVersteckte() {
        Level level = new Level("Erdgeschoss");
        Room room = Room.rectangular(
                "Wohnen",
                new PlanPoint(0, 0),
                new PlanPoint(4000, 3000),
                Length.of(2.8, LengthUnit.METER),
                Length.of(18, LengthUnit.CENTIMETER),
                Length.of(20, LengthUnit.CENTIMETER)
        );
        level.addRoom(room);

        SurfaceLayerStack floor = new SurfaceLayerStack(SurfaceType.FLOOR, room.id().toString());
        floor.addLayer(SurfaceLayer.create("Parkett", Length.of(1.2, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero()));
        floor.addLayer(SurfaceLayer.create("Trittschall", Length.of(0.8, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero()).withVisibility(false));
        floor.addLayer(SurfaceLayer.create("Ausgleich", Length.of(1.8, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero()));
        SurfaceLayerStack ceiling = new SurfaceLayerStack(SurfaceType.CEILING, room.id().toString());
        ceiling.addLayer(SurfaceLayer.create("Spachtel", Length.of(0.5, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero()).withVisibility(false));
        ceiling.addLayer(SurfaceLayer.create("Akustikplatte", Length.of(1.25, LengthUnit.CENTIMETER), Length.of(120, LengthUnit.CENTIMETER), Length.of(20, LengthUnit.CENTIMETER), Length.zero()));
        level.addSurfaceLayerStack(floor);
        level.addSurfaceLayerStack(ceiling);

        assertEquals(30.0, effectService.floorLayerThicknessMillimeters(level, room), 0.001);
        assertEquals(12.5, effectService.ceilingLayerThicknessMillimeters(level, room), 0.001);
        assertEquals(2757.5, effectService.effectiveMaximumCeilingHeightMillimeters(level, room), 0.001);
        assertEquals(33.09, effectService.effectiveVolumeCubicMeters(level, room), 0.001);
        assertEquals(room.areaSquareMeters(), effectService.effectiveAreaSquareMeters(level, room), 0.001);
    }
}
