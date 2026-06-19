package de.andreas.cadas.application.drawing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionLabelPlacementServiceTest {

    private final DimensionLabelPlacementService service = new DimensionLabelPlacementService();

    @Test
    void platziertMaßeSortiertVonInnenNachAußen() {
        FakeLabel nah = new FakeLabel("nah", 100, 1_000);
        FakeLabel fern = new FakeLabel("fern", 200, 2_000);

        List<FakePlacement> placements = service.place(List.of(fern, nah), List.of(), FakeLabel::layout);

        assertEquals(2, placements.size());
        assertEquals("nah", placements.get(0).pending().text);
        assertEquals("fern", placements.get(1).pending().text);
    }

    @Test
    void weichtAufRaumtextSeedBlockerNachAußenAus() {
        // Seed-Blocker überdeckt die Ausgangsplatzierung des Maßes (x=150, y=50, 10x10).
        TextBlockingBox roomBlocker = new TextBlockingBox(145, 45, 20, 20);
        FakeLabel label = new FakeLabel("Maß", 100, 1_000);

        List<FakePlacement> placements = service.place(List.of(label), List.of(roomBlocker), FakeLabel::layout);

        assertEquals(1, placements.size());
        // Das Maß muss weiter nach außen verschoben worden sein (größerer Offset als Startwert).
        assertTrue(placements.get(0).usedNormalOffset > 100.0);
    }

    @Test
    void platziertMaßeOhneÜberdeckungNebeneinander() {
        FakeLabel a = new FakeLabel("a", 100, 1_000);
        FakeLabel b = new FakeLabel("b", 100, 2_000);

        List<FakePlacement> placements = service.place(List.of(a, b), List.of(), FakeLabel::layout);

        assertEquals(2, placements.size());
        // Beide Sperrflächen dürfen sich nicht überdecken.
        assertFalse(placements.get(0).blockingBox.overlaps(placements.get(1).blockingBox));
    }

    private static void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }

    private record FakeLabel(String text, double startOffset, double length) implements DimensionLabelPlacementService.PendingLabel {
        @Override
        public double initialNormalOffset() {
            return startOffset;
        }

        @Override
        public double lineDistanceFromAxis() {
            return startOffset;
        }

        @Override
        public double outwardStep() {
            return 10.0;
        }

        @Override
        public double dimensionLengthMillimeters() {
            return length;
        }

        FakePlacement layout(double normalOffset) {
            double x = 50 + normalOffset;
            TextBlockingBox box = new TextBlockingBox(x, 50, 10, 10);
            return new FakePlacement(this, normalOffset, box);
        }
    }

    private record FakePlacement(FakeLabel pending, double usedNormalOffset, TextBlockingBox blockingBox) implements DimensionLabelPlacementService.PlacedLabel {
    }
}