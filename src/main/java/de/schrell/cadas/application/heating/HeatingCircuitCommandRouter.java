package de.schrell.cadas.application.heating;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class HeatingCircuitCommandRouter {

    private static final String SERPENTINE_MIDDLE_LINE = "rrRRLllLrrRRllLLrrRiRIrR";

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

    public String squareVarioCommands(double sideMillimeters, double spacingMillimeters) {
        if (sideMillimeters <= 0.0) {
            throw new IllegalArgumentException("Die Quadratseite muss größer als null sein.");
        }
        if (spacingMillimeters <= 0.0) {
            throw new IllegalArgumentException("Der Verlegeabstand muss größer als null sein.");
        }
        int sideSteps = (int) Math.floor(sideMillimeters / spacingMillimeters);
        if (sideSteps < 3) {
            throw new IllegalArgumentException("Ein Quadrat-Vario-Heizkreis benötigt mindestens drei Verlegeabstände Seitenlänge.");
        }
        int supplyMaximum = sideSteps - 2;
        int returnMaximum = sideSteps - 1;
        StringBuilder commands = new StringBuilder("rrRR");
        int maximum = Math.max(supplyMaximum, returnMaximum);
        for (int length = 1; length <= maximum; length++) {
            if (length <= supplyMaximum) {
                appendRepeated(commands, 'I', length);
            }
            if (length <= returnMaximum) {
                appendRepeated(commands, 'i', length);
            }
            if (length < supplyMaximum) {
                commands.append('R');
            }
            if (length <= returnMaximum) {
                commands.append('r');
            }
        }
        appendRepeated(commands, 'i', returnMaximum);
        return commands.toString();
    }

    public String rectangularVarioCommands(double widthMillimeters, double heightMillimeters, double spacingMillimeters) {
        return rectangularVarioCommands(widthMillimeters, heightMillimeters, spacingMillimeters, false);
    }

    public String meanderCommands(double widthMillimeters, double heightMillimeters, double spacingMillimeters) {
        return meanderCommands(widthMillimeters, heightMillimeters, spacingMillimeters, false);
    }

    public String meanderCommands(
            double widthMillimeters,
            double heightMillimeters,
            double spacingMillimeters,
            boolean serpentineMiddleLine
    ) {
        if (widthMillimeters <= 0.0 || heightMillimeters <= 0.0) {
            throw new IllegalArgumentException("Die Heizbereichsmaße müssen größer als null sein.");
        }
        if (spacingMillimeters <= 0.0) {
            throw new IllegalArgumentException("Der Verlegeabstand muss größer als null sein.");
        }
        int shortSideSteps = (int) Math.floor(Math.min(widthMillimeters, heightMillimeters) / spacingMillimeters);
        int longSideSteps = (int) Math.floor(Math.max(widthMillimeters, heightMillimeters) / spacingMillimeters);
        int extension = longSideSteps - shortSideSteps;
        int turnPairs = shortSideSteps / 4;
        int lineSteps = longSideSteps - 3;
        int supplyLineSteps = meanderSupplyLineSteps(shortSideSteps, lineSteps, turnPairs, extension);
        if (turnPairs < 1 || lineSteps < 1) {
            throw new IllegalArgumentException("Ein Meander-Heizkreis benötigt mindestens vier Verlegeabstände Breite und vier Verlegeabstände Länge.");
        }

        StringBuilder commands = new StringBuilder();
        appendMeanderReturn(commands, shortSideSteps, lineSteps, turnPairs, extension, serpentineMiddleLine);
        appendMeanderSupply(commands, shortSideSteps, supplyLineSteps, turnPairs, extension, serpentineMiddleLine);
        return commands.toString();
    }

    public String rectangularVarioCommands(
            double widthMillimeters,
            double heightMillimeters,
            double spacingMillimeters,
            boolean serpentineMiddleLine
    ) {
        if (widthMillimeters <= 0.0 || heightMillimeters <= 0.0) {
            throw new IllegalArgumentException("Die Heizbereichsmaße müssen größer als null sein.");
        }
        if (spacingMillimeters <= 0.0) {
            throw new IllegalArgumentException("Der Verlegeabstand muss größer als null sein.");
        }
        double shortSideMillimeters = Math.min(widthMillimeters, heightMillimeters);
        double longSideMillimeters = Math.max(widthMillimeters, heightMillimeters);
        int extension = (int) Math.floor((longSideMillimeters - shortSideMillimeters) / spacingMillimeters);
        int shortSideSteps = (int) Math.floor(shortSideMillimeters / spacingMillimeters);
        if (serpentineMiddleLine && extension > 0) {
            return serpentineMiddleLineVarioCommands(shortSideSteps, extension);
        }
        if (extension <= 0) {
            return squareVarioCommands(shortSideMillimeters, spacingMillimeters);
        }
        if (shortSideSteps < 3) {
            throw new IllegalArgumentException("Ein Rechteck-Vario-Heizkreis benötigt mindestens drei Verlegeabstände auf der kurzen Seite.");
        }
        int supplyBaseMaximum = shortSideSteps - 1;
        if (supplyBaseMaximum % 2 != 0) {
            supplyBaseMaximum--;
        }
        int returnBaseMaximum = supplyBaseMaximum - 1;
        int returnInitialLength = extension / 2;
        int supplyInitialLength = extension - returnInitialLength;
        StringBuilder commands = new StringBuilder();
        appendRepeated(commands, 'i', returnInitialLength);
        appendRepeated(commands, 'I', supplyInitialLength);
        commands.append("RRrr");
        for (int baseLength = 1; baseLength <= supplyBaseMaximum; baseLength++) {
            if (baseLength < returnBaseMaximum) {
                appendRepeated(commands, 'i', rectangularVarioLength(baseLength, extension));
                commands.append('r');
            }
            appendRepeated(commands, 'I', rectangularVarioLength(baseLength, extension));
            commands.append('R');
        }
        return commands.toString();
    }

    private String serpentineMiddleLineVarioCommands(int shortSideSteps, int extension) {
        if (shortSideSteps < 7) {
            throw new IllegalArgumentException("Eine schlangenförmige Vario-Mittellinie benötigt mindestens sieben Verlegeabstände auf der kurzen Seite.");
        }
        int snakeLength = serpentineSnakeLength(extension);
        int supplyMaximum = 5;
        StringBuilder commands = new StringBuilder(SERPENTINE_MIDDLE_LINE);
        appendRepeated(commands, 'i', 1 + snakeLength);
        commands.append('r');
        appendRepeated(commands, 'i', 3);
        appendRepeated(commands, 'I', 1 + snakeLength);
        commands.append('R');
        appendRepeated(commands, 'I', 3);
        commands.append('R');
        appendRepeated(commands, 'I', 3 + snakeLength);
        commands.append("rR");
        appendRepeated(commands, 'I', supplyMaximum);
        appendRepeated(commands, 'i', 3 + snakeLength);
        commands.append('r');
        appendRepeated(commands, 'i', supplyMaximum);
        commands.append('r');
        appendRepeated(commands, 'i', supplyMaximum + snakeLength);
        commands.append('r');
        appendRepeated(commands, 'i', supplyMaximum + 1);
        return commands.toString();
    }

    private void appendMeanderReturn(
            StringBuilder commands,
            int shortSideSteps,
            int lineSteps,
            int turnPairs,
            int extension,
            boolean serpentineMiddleLine
    ) {
        appendRepeated(commands, 'i', shortSideSteps / 2 + 2);
        for (int turnPair = 0; turnPair < turnPairs; turnPair++) {
            commands.append("ll");
            appendRepeated(commands, 'i', lineSteps);
            commands.append("rr");
            if (turnPair < turnPairs - 1) {
                appendRepeated(commands, 'i', lineSteps);
            } else {
                appendRepeated(commands, 'i', lineSteps + 1);
                if (serpentineMiddleLine) {
                    appendMeanderMiddleSnake(commands, 'i', 'r', 'l', serpentineSnakeLength(extension));
                }
                commands.append('r');
                appendRepeated(commands, 'i', shortSideSteps - 1);
            }
        }
    }

    private void appendMeanderSupply(
            StringBuilder commands,
            int shortSideSteps,
            int supplyLineSteps,
            int turnPairs,
            int extension,
            boolean serpentineMiddleLine
    ) {
        appendRepeated(commands, 'I', shortSideSteps / 2 + 4);
        for (int turnPair = 0; turnPair < turnPairs; turnPair++) {
            commands.append("LL");
            appendRepeated(commands, 'I', supplyLineSteps);
            if (turnPair < turnPairs - 1) {
                commands.append("RR");
                appendRepeated(commands, 'I', supplyLineSteps);
            } else {
                if (serpentineMiddleLine) {
                    appendMeanderMiddleSnake(commands, 'I', 'L', 'R', serpentineSnakeLength(extension));
                }
                commands.append('R');
            }
        }
    }

    private void appendMeanderMiddleSnake(
            StringBuilder commands,
            char lineCommand,
            char firstTurnCommand,
            char secondTurnCommand,
            int snakeLength
    ) {
        commands.append(firstTurnCommand);
        commands.append(firstTurnCommand);
        appendRepeated(commands, lineCommand, snakeLength);
        commands.append(secondTurnCommand);
        commands.append(secondTurnCommand);
        appendRepeated(commands, lineCommand, snakeLength);
    }

    private int meanderSupplyLineSteps(int shortSideSteps, int lineSteps, int turnPairs, int extension) {
        int referenceExtension = turnPairs * 2 - 1;
        return lineSteps - Math.max(0, extension - referenceExtension);
    }

    private int serpentineSnakeLength(int extension) {
        return Math.max(1, extension);
    }

    private int rectangularVarioLength(int baseLength, int extension) {
        return baseLength % 2 == 1 ? baseLength + extension : baseLength;
    }

    private void appendRepeated(StringBuilder target, char character, int repetitions) {
        for (int index = 0; index < repetitions; index++) {
            target.append(character);
        }
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
        RoutingPoint endPoint = cursor.position()
                .translate(cursor.direction(), radiusMillimeters)
                .translate(endDirection, radiusMillimeters);
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

        public RoutingResult translatedBy(double xMillimeters, double yMillimeters) {
            return new RoutingResult(
                    widthMillimeters,
                    heightMillimeters,
                    spacingMillimeters,
                    supplyPath.translatedBy(xMillimeters, yMillimeters),
                    returnPath.translatedBy(xMillimeters, yMillimeters)
            );
        }

        public RoutingResult rotatedClockwise() {
            return new RoutingResult(
                    heightMillimeters,
                    widthMillimeters,
                    spacingMillimeters,
                    supplyPath.rotatedClockwise(),
                    returnPath.rotatedClockwise()
            );
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

        PipePath translatedBy(double xMillimeters, double yMillimeters) {
            List<PipePrimitive> translatedPrimitives = primitives.stream()
                    .map(primitive -> primitive.translatedBy(xMillimeters, yMillimeters))
                    .toList();
            return new PipePath(
                    startPoint.translatedBy(xMillimeters, yMillimeters),
                    endPoint.translatedBy(xMillimeters, yMillimeters),
                    endDirection,
                    translatedPrimitives
            );
        }

        PipePath rotatedClockwise() {
            List<PipePrimitive> rotatedPrimitives = primitives.stream()
                    .map(PipePrimitive::rotatedClockwise)
                    .toList();
            return new PipePath(
                    startPoint.rotatedClockwise(),
                    endPoint.rotatedClockwise(),
                    endDirection.rotatedClockwise(),
                    rotatedPrimitives
            );
        }
    }

    public sealed interface PipePrimitive permits LineSegment, QuarterArc {

        RoutingPoint startPoint();

        RoutingPoint endPoint();

        PipePrimitive translatedBy(double xMillimeters, double yMillimeters);

        PipePrimitive rotatedClockwise();
    }

    public record LineSegment(RoutingPoint startPoint, RoutingPoint endPoint) implements PipePrimitive {

        public LineSegment {
            Objects.requireNonNull(startPoint, "startPoint darf nicht null sein.");
            Objects.requireNonNull(endPoint, "endPoint darf nicht null sein.");
        }

        @Override
        public LineSegment translatedBy(double xMillimeters, double yMillimeters) {
            return new LineSegment(
                    startPoint.translatedBy(xMillimeters, yMillimeters),
                    endPoint.translatedBy(xMillimeters, yMillimeters)
            );
        }

        @Override
        public LineSegment rotatedClockwise() {
            return new LineSegment(startPoint.rotatedClockwise(), endPoint.rotatedClockwise());
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

        @Override
        public QuarterArc translatedBy(double xMillimeters, double yMillimeters) {
            return new QuarterArc(
                    startPoint.translatedBy(xMillimeters, yMillimeters),
                    endPoint.translatedBy(xMillimeters, yMillimeters),
                    centerPoint.translatedBy(xMillimeters, yMillimeters),
                    radiusMillimeters,
                    turn,
                    startDirection,
                    endDirection
            );
        }

        @Override
        public QuarterArc rotatedClockwise() {
            return new QuarterArc(
                    startPoint.rotatedClockwise(),
                    endPoint.rotatedClockwise(),
                    centerPoint.rotatedClockwise(),
                    radiusMillimeters,
                    turn,
                    startDirection.rotatedClockwise(),
                    endDirection.rotatedClockwise()
            );
        }
    }

    public record RoutingPoint(double xMillimeters, double yMillimeters) {

        public RoutingPoint translate(CardinalDirection direction, double distanceMillimeters) {
            return new RoutingPoint(
                    xMillimeters + direction.xFactor() * distanceMillimeters,
                    yMillimeters + direction.yFactor() * distanceMillimeters
            );
        }

        public RoutingPoint translatedBy(double xOffsetMillimeters, double yOffsetMillimeters) {
            return new RoutingPoint(xMillimeters + xOffsetMillimeters, yMillimeters + yOffsetMillimeters);
        }

        public RoutingPoint rotatedClockwise() {
            return new RoutingPoint(yMillimeters, -xMillimeters);
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

        CardinalDirection rotatedClockwise() {
            return turn(Turn.RIGHT);
        }
    }
}
