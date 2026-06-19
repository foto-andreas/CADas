package de.andreas.cadas.application.drawing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Kollisionsfreie Platzierung von Maßtexten anhand rechteckiger Sperrflächen.
 *
 * <p>Der Algorithmus sortiert alle Maße nach Abstand zur Wandachse (innen vor außen)
 * und danach nach Länge. Für jedes Maß wird anhand der Ausgangsplatzierung eine
 * {@link TextBlockingBox} berechnet; bei Überdeckung mit einer bereits gesetzten
 * Sperrfläche wird das Maß schrittweise weiter nach außen verschoben, bis es frei liegt.</p>
 *
 * <p>Raumtexte in Raummitte werden als vorab gesetzte Sperrflächen übergeben,
 * damit Maße die Raumtexte nicht überdecken.</p>
 */
public final class DimensionLabelPlacementService {

    private static final int MAX_OUTWARD_STEPS = 128;

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
        List<L> sorted = new ArrayList<>(pending);
        sorted.sort(Comparator
                .comparingDouble((L label) -> Math.abs(label.lineDistanceFromAxis()))
                .thenComparingDouble(L::dimensionLengthMillimeters)
                .thenComparing(L::text));
        List<R> placements = new ArrayList<>();
        List<TextBlockingBox> blockers = new ArrayList<>(seedBlockers);
        for (L label : sorted) {
            double normalOffset = label.initialNormalOffset();
            R placed = layoutForOffset.apply(label, normalOffset);
            int steps = 0;
            while (TextBlockingBox.overlapsAny(placed.blockingBox(), blockers) && steps < MAX_OUTWARD_STEPS) {
                normalOffset += label.outwardStep();
                placed = layoutForOffset.apply(label, normalOffset);
                steps++;
            }
            blockers.add(placed.blockingBox());
            placements.add(placed);
        }
        return List.copyOf(placements);
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
    }

    /**
     * Vertrag für ein platziertes Maß-Label mit Sperrfläche.
     */
    public interface PlacedLabel {
        /** Sperrfläche des platzierten Textes. */
        TextBlockingBox blockingBox();
    }
}