package de.schrell.cadas.application.drawing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Kollisionsfreie Platzierung von Maßtexten anhand rechteckiger Sperrflächen.
 *
 * <p>Der Algorithmus sortiert alle Maße nach Länge und danach nach Abstand zur
 * Wandachse. Bei Überdeckung mit einer bereits gesetzten Sperrfläche wird das Maß
 * schrittweise weiter nach außen verschoben, bis alle Sperrflächen frei liegen.</p>
 *
 * <p>Raumtexte in Raummitte werden als vorab gesetzte Sperrflächen übergeben,
 * damit Maße die Raumtexte nicht überdecken.</p>
 */
public final class DimensionLabelPlacementService {

    private static final double MINIMUM_OUTWARD_STEP = 0.001;

    /**
     * Platzier-Algorithmus für eine Menge von Maß-Labels.
     *
     * @param pending       alle noch nicht platzierten Labels
     * @param seedBlockers  bereits existierende Sperrflächen (z. B. Raumtexte in Raummitte)
     * @param layoutForOffset liefert zu einem Label und einem Normalen-Offset die finale
     *                        Platzierung (Text-Position + Sperrfläche)
     * @param <L> Label-Typ
     * @param <R> Platzierungs-Typ
     * @return die platzierten Labels in der Reihenfolge der Sortierung
     */
    public <L extends DimensionLabelPlacementService.PendingLabel, R extends DimensionLabelPlacementService.PlacedLabel>
    List<R> place(List<L> pending, List<TextBlockingBox> seedBlockers, BiFunction<L, Double, R> layoutForOffset) {
        Objects.requireNonNull(pending, "pending darf nicht null sein.");
        Objects.requireNonNull(seedBlockers, "seedBlockers darf nicht null sein.");
        Objects.requireNonNull(layoutForOffset, "layoutForOffset darf nicht null sein.");
        List<L> sorted = new ArrayList<>(deduplicate(pending));
        sorted.sort(Comparator
                .comparingDouble(L::dimensionLengthMillimeters)
                .thenComparingDouble(label -> Math.abs(label.lineDistanceFromAxis()))
                .thenComparing(L::text));
        List<R> placements = new ArrayList<>();
        List<TextBlockingBox> blockers = new ArrayList<>(seedBlockers);
        for (L label : sorted) {
            double normalOffset = label.initialNormalOffset();
            double outwardStep = label.outwardStep();
            if (Math.abs(outwardStep) < MINIMUM_OUTWARD_STEP) {
                throw new IllegalArgumentException("outwardStep muss von null verschieden sein.");
            }
            R placed = layoutForOffset.apply(label, normalOffset);
            while (overlapsAny(placed.blockingBoxes(), blockers)) {
                normalOffset += outwardStep;
                placed = layoutForOffset.apply(label, normalOffset);
            }
            blockers.addAll(placed.blockingBoxes());
            placements.add(placed);
        }
        return List.copyOf(placements);
    }

    public <L extends PendingLabel> List<L> deduplicate(List<L> pending) {
        Objects.requireNonNull(pending, "pending darf nicht null sein.");
        List<L> result = new ArrayList<>();
        for (L candidate : pending) {
            String key = candidate.deduplicationKey();
            if (key == null || key.isBlank()) {
                result.add(candidate);
                continue;
            }
            int existingIndex = indexOfKey(result, key);
            if (existingIndex < 0) {
                result.add(candidate);
            } else if (isPreferred(candidate, result.get(existingIndex))) {
                result.set(existingIndex, candidate);
            }
        }
        return List.copyOf(result);
    }

    private <L extends PendingLabel> int indexOfKey(List<L> labels, String key) {
        for (int index = 0; index < labels.size(); index++) {
            if (key.equals(labels.get(index).deduplicationKey())) {
                return index;
            }
        }
        return -1;
    }

    private boolean isPreferred(PendingLabel candidate, PendingLabel existing) {
        int offsetComparison = Double.compare(
                Math.abs(candidate.initialNormalOffset()),
                Math.abs(existing.initialNormalOffset())
        );
        if (offsetComparison != 0) {
            return offsetComparison < 0;
        }
        return Math.abs(candidate.lineDistanceFromAxis()) < Math.abs(existing.lineDistanceFromAxis());
    }

    private boolean overlapsAny(List<TextBlockingBox> candidates, List<TextBlockingBox> blockers) {
        return candidates.stream().anyMatch(candidate -> TextBlockingBox.overlapsAny(candidate, blockers));
    }

    /**
     * Vertrag für ein noch nicht platziertes Maß-Label.
     */
    public interface PendingLabel {
        /** Anzeigetext des Maßes. */
        String text();

        /** Normalen-Offset des Maßes zur Wandachse im Ursprungszustand. */
        double initialNormalOffset();

        /** Abstand der Maßlinie von der Wandachse (für Sortierung innen-vor-außen). */
        double lineDistanceFromAxis();

        /** Schrittweite, um das Maß bei Überdeckung weiter nach außen zu verschieben. */
        double outwardStep();

        /** Länge des Maßes in Millimetern (für Sortierung klein-vor-groß). */
        double dimensionLengthMillimeters();

        /** Fachlicher Schlüssel; leer bedeutet, dass das Maß nicht dedupliziert wird. */
        default String deduplicationKey() {
            return "";
        }

    }

    /**
     * Vertrag für ein platziertes Maß-Label mit Sperrfläche.
     */
    public interface PlacedLabel {
        /** Sperrfläche des platzierten Textes. */
        TextBlockingBox blockingBox();

        /** Alle Sperrflächen der Bemaßung einschließlich Maßlinie. */
        default List<TextBlockingBox> blockingBoxes() {
            return List.of(blockingBox());
        }
    }
}
