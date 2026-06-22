package de.schrell.cadas.application.heating;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HeatingCircuitCommandRouter {

    public RoutingResult route(double widthMillimeters, double heightMillimeters, double spacingMillimeters, String commands) {
        if (widthMillimeters <= 0.0) {
            throw new IllegalArgumentException("Die Heizbereichsbreite muss größer als null sein.");
        }
        if (heightMillimeters <= 0.0) {
            throw new IllegalArgumentException("Die Heizbereichshöhe muss größer als null sein.");
        }
        if (spacingMillimeters <= 0.0) {
            throw new IllegalArgumentException("Der Verlegeabstand muss größer als null sein.");
        }
        Objects.requireNonNull(commands, "commands darf nicht null sein.");

        PipeCursor supplyCursor = new PipeCursor(new RoutingPoint(0.0, 0.0), CardinalDirection.UP);
        PipeCursor returnCursor = new PipeCursor(new RoutingPoint(0.0, 0.0), CardinalDirection.DOWN);
        List<PipePrimitive> supplyPrimitives = new ArrayList<>();
        List<PipePrimitive> returnPrimitives = new ArrayList<>();

        for (int index = 0; index < commands.length(); index++) {
            char command = commands.charAt(index);
            if (isIgnoredCharacter(command)) {
                continue;
            }
            switch (command) {
                case 'I' -> supplyCursor = appendLine(supplyPrimitives, supplyCursor, spacingMillimeters);
                case 'i' -> returnCursor = appendLine(returnPrimitives, returnCursor, spacingMillimeters);
                case 'R' -> supplyCursor = appendArc(supplyPrimitives, supplyCursor, spacingMillimeters, Turn.RIGHT);
                case 'r' -> returnCursor = appendArc(returnPrimitives, returnCursor, spacingMillimeters, Turn.RIGHT);
                case 'L' -> supplyCursor = appendArc(supplyPrimitives, supplyCursor, spacingMillimeters, Turn.LEFT);
                case 'l' -> returnCursor = appendArc(returnPrimitives, returnCursor, spacingMillimeters, Turn.LEFT);
                default -> throw new IllegalArgumentException("Unbekannter Routing-Befehl `" + command + "`.");
            }
        }

        return new RoutingResult(
                widthMillimeters,
                heightMillimeters,
                spacingMillimeters,
                new PipePath(new RoutingPoint(0.0, 0.0), supplyCursor.position(), supplyCursor.direction(), supplyPrimitives),
                new PipePath(new RoutingPoint(0.0, 0.0), returnCursor.position(), returnCursor.direction(), returnPrimitives)
        );
    }

    public boolean isCommandCharacter(char character) {
        return switch (character) {
            case 'I', 'i', 'R', 'r', 'L', 'l' -> true;
            default -> false;
        };
    }

    public boolean isIgnoredCharacter(char character) {
        return Character.isWhitespace(character);
    }

    private PipeCursor appendLine(List<PipePrimitive> primitives, PipeCursor cursor, double spacingMillimeters) {
        RoutingPoint endPoint = cursor.position().translate(cursor.direction(), spacingMillimeters);
        primitives.add(new LineSegment(cursor.position(), endPoint));
        return new PipeCursor(endPoint, cursor.direction());
    }

    private PipeCursor appendArc(
            List<PipePrimitive> primitives,
            PipeCursor cursor,
            double spacingMillimeters,
            Turn turn
    ) {
        double radiusMillimeters = spacingMillimeters / 2.0;
        CardinalDirection endDirection = cursor.direction().turn(turn);
        RoutingPoint centerPoint = cursor.position().translate(cursor.direction().sideDirection(turn), radiusMillimeters);
        RoutingPoint endPoint = centerPoint.translate(endDirection, radiusMillimeters);
        primitives.add(new QuarterArc(
                cursor.position(),
                endPoint,
                centerPoint,
                radiusMillimeters,
                turn,
                cursor.direction(),
                endDirection
        ));
        return new PipeCursor(endPoint, endDirection);
    }

    private record PipeCursor(RoutingPoint position, CardinalDirection direction) {
    }

    public record RoutingResult(
            double widthMillimeters,
            double heightMillimeters,
            double spacingMillimeters,
            PipePath supplyPath,
            PipePath returnPath
    ) {

        public RoutingResult {
            Objects.requireNonNull(supplyPath, "supplyPath darf nicht null sein.");
            Objects.requireNonNull(returnPath, "returnPath darf nicht null sein.");
        }

        public RoutingResult withFlowInverted(boolean flowInverted) {
            if (!flowInverted) {
                return this;
            }
            return new RoutingResult(widthMillimeters, heightMillimeters, spacingMillimeters, returnPath, supplyPath);
        }
    }

    public record PipePath(
            RoutingPoint startPoint,
            RoutingPoint endPoint,
            CardinalDirection endDirection,
            List<PipePrimitive> primitives
    ) {

        public PipePath {
            Objects.requireNonNull(startPoint, "startPoint darf nicht null sein.");
            Objects.requireNonNull(endPoint, "endPoint darf nicht null sein.");
            Objects.requireNonNull(endDirection, "endDirection darf nicht null sein.");
            Objects.requireNonNull(primitives, "primitives darf nicht null sein.");
            primitives = List.copyOf(primitives);
        }
    }

    public sealed interface PipePrimitive permits LineSegment, QuarterArc {

        RoutingPoint startPoint();

        RoutingPoint endPoint();
    }

    public record LineSegment(RoutingPoint startPoint, RoutingPoint endPoint) implements PipePrimitive {

        public LineSegment {
            Objects.requireNonNull(startPoint, "startPoint darf nicht null sein.");
            Objects.requireNonNull(endPoint, "endPoint darf nicht null sein.");
        }
    }

    public record QuarterArc(
            RoutingPoint startPoint,
            RoutingPoint endPoint,
            RoutingPoint centerPoint,
            double radiusMillimeters,
            Turn turn,
            CardinalDirection startDirection,
            CardinalDirection endDirection
    ) implements PipePrimitive {

        public QuarterArc {
            Objects.requireNonNull(startPoint, "startPoint darf nicht null sein.");
            Objects.requireNonNull(endPoint, "endPoint darf nicht null sein.");
            Objects.requireNonNull(centerPoint, "centerPoint darf nicht null sein.");
            Objects.requireNonNull(turn, "turn darf nicht null sein.");
            Objects.requireNonNull(startDirection, "startDirection darf nicht null sein.");
            Objects.requireNonNull(endDirection, "endDirection darf nicht null sein.");
            if (radiusMillimeters <= 0.0) {
                throw new IllegalArgumentException("Der Bogenradius muss größer als null sein.");
            }
        }
    }

    public record RoutingPoint(double xMillimeters, double yMillimeters) {

        public RoutingPoint translate(CardinalDirection direction, double distanceMillimeters) {
            return new RoutingPoint(
                    xMillimeters + direction.xFactor() * distanceMillimeters,
                    yMillimeters + direction.yFactor() * distanceMillimeters
            );
        }
    }

    public enum Turn {
        LEFT,
        RIGHT
    }

    public enum CardinalDirection {
        UP(0.0, 1.0),
        RIGHT(1.0, 0.0),
        DOWN(0.0, -1.0),
        LEFT(-1.0, 0.0);

        private final double xFactor;
        private final double yFactor;

        CardinalDirection(double xFactor, double yFactor) {
            this.xFactor = xFactor;
            this.yFactor = yFactor;
        }

        double xFactor() {
            return xFactor;
        }

        double yFactor() {
            return yFactor;
        }

        CardinalDirection turn(Turn turn) {
            int delta = turn == Turn.RIGHT ? 1 : -1;
            int nextIndex = Math.floorMod(ordinal() + delta, values().length);
            return values()[nextIndex];
        }

        CardinalDirection sideDirection(Turn turn) {
            return turn == Turn.RIGHT ? turn(Turn.RIGHT) : turn(Turn.LEFT);
        }
    }
}
