package de.schrell.cadas.application.heating;

import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.PipePath;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.PipePrimitive;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.QuarterArc;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingPoint;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.RoutingResult;
import de.schrell.cadas.application.heating.HeatingCircuitCommandRouter.Turn;

/**
 * Ermittelt die fachlich relevante Render-Begrenzung eines Heizkreis-Routings.
 * Zulaufsegmente nach dem Feldtrenner werden dabei bewusst ignoriert.
 */
public final class HeatingCircuitFieldBounds {

    private static final double FULL_TURN_RADIANS = Math.PI * 2.0;
    private static final double QUARTER_TURN_RADIANS = Math.PI / 2.0;
    private static final double HALF_TURN_RADIANS = Math.PI;
    private static final double THREE_QUARTER_TURN_RADIANS = QUARTER_TURN_RADIANS * 3.0;
    private static final double ANGLE_EPSILON = 0.000_001;
    private static final double[] CARDINAL_ANGLES = {
            0.0,
            QUARTER_TURN_RADIANS,
            HALF_TURN_RADIANS,
            THREE_QUARTER_TURN_RADIANS
    };

    private HeatingCircuitFieldBounds() {
    }

    /**
     * Liefert die Bounding-Box des Heizfelds ohne Zu- und Ablaufsegmente nach dem `+`.
     */
    public static Bounds fieldBounds(RoutingResult result) {
        return Bounds.from(result.supplyPath().startPoint())
                .include(result.supplyPath(), result.fieldSupplyPrimitiveCount())
                .include(result.returnPath(), result.fieldReturnPrimitiveCount());
    }

    /**
     * Unveränderliche Bounding-Box für Heizkreis-Geometrien.
     */
    public record Bounds(double minX, double minY, double maxX, double maxY) {

        /**
         * Erzeugt eine Bounding-Box aus den übergebenen Rechteckkoordinaten.
         */
        public static Bounds rectangle(double minX, double minY, double maxX, double maxY) {
            return new Bounds(minX, minY, maxX, maxY);
        }

        private static Bounds from(RoutingPoint point) {
            return new Bounds(
                    point.xMillimeters(),
                    point.yMillimeters(),
                    point.xMillimeters(),
                    point.yMillimeters()
            );
        }

        /**
         * Erweitert die Bounding-Box um die ersten fachlich relevanten Primitive eines Pfads.
         */
        public Bounds include(PipePath path, int includedPrimitiveCount) {
            Bounds result = include(path.startPoint());
            int primitiveCount = Math.max(0, Math.min(includedPrimitiveCount, path.primitives().size()));
            for (int index = 0; index < primitiveCount; index++) {
                PipePrimitive primitive = path.primitives().get(index);
                if (primitive instanceof QuarterArc arc) {
                    result = result.include(arc);
                } else {
                    result = result.include(primitive.startPoint()).include(primitive.endPoint());
                }
            }
            return result;
        }

        /**
         * Erweitert die Bounding-Box um einen Außenabstand, etwa den Rohrhalbmesser.
         */
        public Bounds expanded(double marginMillimeters) {
            if (marginMillimeters <= 0.0) {
                return this;
            }
            return new Bounds(
                    minX - marginMillimeters,
                    minY - marginMillimeters,
                    maxX + marginMillimeters,
                    maxY + marginMillimeters
            );
        }

        /**
         * Liefert die Breite der Bounding-Box in Millimetern.
         */
        public double width() {
            return maxX - minX;
        }

        /**
         * Liefert die Höhe der Bounding-Box in Millimetern.
         */
        public double height() {
            return maxY - minY;
        }

        /**
         * Liefert die X-Koordinate des Mittelpunkts.
         */
        public double centerX() {
            return (minX + maxX) / 2.0;
        }

        /**
         * Liefert die Y-Koordinate des Mittelpunkts.
         */
        public double centerY() {
            return (minY + maxY) / 2.0;
        }

        private Bounds include(RoutingPoint point) {
            return new Bounds(
                    Math.min(minX, point.xMillimeters()),
                    Math.min(minY, point.yMillimeters()),
                    Math.max(maxX, point.xMillimeters()),
                    Math.max(maxY, point.yMillimeters())
            );
        }

        private Bounds include(QuarterArc arc) {
            Bounds result = include(arc.startPoint()).include(arc.endPoint());
            double startAngle = normalizedAngle(arc.startPoint(), arc.centerPoint());
            double endAngle = normalizedAngle(arc.endPoint(), arc.centerPoint());
            for (double candidateAngle : CARDINAL_ANGLES) {
                if (angleLiesOnArc(startAngle, endAngle, candidateAngle, arc.turn())) {
                    result = result.include(new RoutingPoint(
                            arc.centerPoint().xMillimeters() + Math.cos(candidateAngle) * arc.radiusMillimeters(),
                            arc.centerPoint().yMillimeters() + Math.sin(candidateAngle) * arc.radiusMillimeters()
                    ));
                }
            }
            return result;
        }

        private double normalizedAngle(RoutingPoint point, RoutingPoint centerPoint) {
            double angle = Math.atan2(
                    point.yMillimeters() - centerPoint.yMillimeters(),
                    point.xMillimeters() - centerPoint.xMillimeters()
            );
            return angle >= 0.0 ? angle : angle + FULL_TURN_RADIANS;
        }

        private boolean angleLiesOnArc(double startAngle, double endAngle, double candidateAngle, Turn turn) {
            double adjustedCandidate = candidateAngle;
            if (turn == Turn.RIGHT) {
                double adjustedEnd = endAngle > startAngle ? endAngle - FULL_TURN_RADIANS : endAngle;
                if (adjustedCandidate > startAngle) {
                    adjustedCandidate -= FULL_TURN_RADIANS;
                }
                return adjustedCandidate <= startAngle + ANGLE_EPSILON
                        && adjustedCandidate >= adjustedEnd - ANGLE_EPSILON;
            }
            double adjustedEnd = endAngle < startAngle ? endAngle + FULL_TURN_RADIANS : endAngle;
            if (adjustedCandidate < startAngle) {
                adjustedCandidate += FULL_TURN_RADIANS;
            }
            return adjustedCandidate >= startAngle - ANGLE_EPSILON
                    && adjustedCandidate <= adjustedEnd + ANGLE_EPSILON;
        }
    }
}
