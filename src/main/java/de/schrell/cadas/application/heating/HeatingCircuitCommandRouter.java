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
                case 'X' -> supplyCursor = removeLastPrimitive(supplyPrimitives, supplyCursor);
                case 'x' -> returnCursor = removeLastPrimitive(returnPrimitives, returnCursor);
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
            case 'I', 'i', 'R', 'r', 'L', 'l', 'X', 'x' -> true;
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
        int turnCount = shortSideSteps / 2;
        int lineSteps = longSideSteps - 1;
        int firstLineSteps = extension > 0 ? lineSteps : longSideSteps % 2 == 1 ? longSideSteps - 2 : lineSteps;
        int supplyInitialSteps = extension > 0 && longSideSteps % 2 == 1 ? longSideSteps / 2 + 1 : longSideSteps / 2;
        int returnInitialSteps = Math.max(1, longSideSteps / 2 - 1);
        if (turnCount < 1 || firstLineSteps < 1) {
            throw new IllegalArgumentException("Ein Meander-Heizkreis benötigt mindestens vier Verlegeabstände Breite und vier Verlegeabstände Länge.");
        }

        StringBuilder commands = new StringBuilder();
        boolean startWithLeftTurns = shortSideSteps % 4 == 0;
        if (serpentineMiddleLine && extension > 0) {
            boolean oddLongSide = longSideSteps % 2 == 1;
            appendMeanderReturnSnake(commands, extension, lineSteps, oddLongSide);
            appendMeanderRows(
                    commands,
                    'i',
                    startWithLeftTurns ? 'r' : 'l',
                    startWithLeftTurns ? 'l' : 'r',
                    turnCount - 1,
                    lineSteps,
                    lineSteps,
                    lineSteps,
                    oddLongSide ? 1 : 2
            );
            appendMeanderSupplySnake(commands, lineSteps, oddLongSide);
            appendMeanderRows(
                    commands,
                    'I',
                    startWithLeftTurns ? 'R' : 'L',
                    startWithLeftTurns ? 'L' : 'R',
                    turnCount,
                    lineSteps,
                    lineSteps,
                    lineSteps,
                    oddLongSide ? 1 : 2
            );
            return commands.toString();
        }
        appendMeanderPipe(
                commands,
                'i',
                startWithLeftTurns ? 'l' : 'r',
                startWithLeftTurns ? 'r' : 'l',
                returnInitialSteps,
                firstLineSteps,
                lineSteps,
                turnCount
        );
        appendMeanderPipe(
                commands,
                'I',
                startWithLeftTurns ? 'L' : 'R',
                startWithLeftTurns ? 'R' : 'L',
                supplyInitialSteps,
                firstLineSteps,
                lineSteps,
                turnCount
        );
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
        appendRepeated(commands, 'I', supplyBaseMaximum + extension);
        appendRepeated(commands, 'i', returnBaseMaximum + extension);
        return commands.toString();
    }

    private String serpentineMiddleLineVarioCommands(int shortSideSteps, int extension) {
        if (shortSideSteps < 7) {
            throw new IllegalArgumentException("Eine schlangenförmige Vario-Mittellinie benötigt mindestens sieben Verlegeabstände auf der kurzen Seite.");
        }
        int snakeLength = serpentineVarioSnakeLength(extension);
        StringBuilder commands = new StringBuilder("rLRR");
        boolean lowerSnakeGroup = true;
        for (int length = 2; length < snakeLength; length += 2) {
            commands.append(lowerSnakeGroup ? "llrr" : "LLRR");
            lowerSnakeGroup = !lowerSnakeGroup;
        }
        commands.append("iIRr");
        int supplyBaseMaximum = largestEvenGridSteps(shortSideSteps) - 2;
        int returnBaseMaximum = supplyBaseMaximum - 1;
        for (int baseLength = 1; baseLength <= supplyBaseMaximum; baseLength++) {
            int lineLength = serpentineVarioLength(baseLength, extension);
            if (baseLength == 1) {
                lineLength = Math.max(lineLength, snakeLength);
            }
            if (baseLength < returnBaseMaximum) {
                appendRepeated(commands, 'i', lineLength);
                commands.append('r');
            }
            appendRepeated(commands, 'I', lineLength);
            commands.append('R');
        }
        appendRepeated(commands, 'I', serpentineVarioFinalLength(supplyBaseMaximum, extension));
        appendRepeated(commands, 'i', serpentineVarioFinalLength(returnBaseMaximum, extension));
        return commands.toString();
    }

    private void appendMeanderPipe(
            StringBuilder commands,
            char lineCommand,
            char firstTurnCommand,
            char secondTurnCommand,
            int initialSteps,
            int firstLineSteps,
            int lineSteps,
            int turnCount
    ) {
        appendRepeated(commands, lineCommand, initialSteps);
        appendMeanderRows(commands, lineCommand, firstTurnCommand, secondTurnCommand, turnCount, firstLineSteps, lineSteps, lineSteps, 2);
    }

    private void appendMeanderRows(
            StringBuilder commands,
            char lineCommand,
            char firstTurnCommand,
            char secondTurnCommand,
            int turnCount,
            int firstLineSteps,
            int lineSteps,
            int lastLineSteps,
            int firstTurnRepetitions
    ) {
        char turnCommand = firstTurnCommand;
        for (int turn = 0; turn < turnCount; turn++) {
            appendRepeated(commands, turnCommand, turn == 0 ? firstTurnRepetitions : 2);
            int currentLineSteps = turn == 0 ? firstLineSteps : turn == turnCount - 1 ? lastLineSteps : lineSteps;
            appendRepeated(commands, lineCommand, currentLineSteps);
            turnCommand = turnCommand == firstTurnCommand ? secondTurnCommand : firstTurnCommand;
        }
    }

    private void appendMeanderReturnSnake(StringBuilder commands, int extension, int lineSteps, boolean oddLongSide) {
        int groupCount = meanderSnakeGroupCount(lineSteps, 5);
        commands.append('r');
        appendRepeated(commands, "llrr", groupCount);
        if (oddLongSide) {
            commands.append('i');
            return;
        }
        commands.append('l');
        if (groupCount == extension / 2) {
            commands.append('i');
        }
    }

    private void appendMeanderSupplySnake(StringBuilder commands, int lineSteps, boolean oddLongSide) {
        int groupCount = meanderSnakeGroupCount(lineSteps, 6);
        if (oddLongSide) {
            commands.append('I');
        }
        commands.append('L');
        appendRepeated(commands, "RRLL", groupCount);
        commands.append(oddLongSide ? "RRI" : "RRL");
    }

    private int meanderSnakeGroupCount(int lineSteps, int fixedSnakeCommandsIncludingClosingArcs) {
        int targetCommandsIncludingClosingArcs = lineSteps + 2;
        return Math.max(0, Math.round((targetCommandsIncludingClosingArcs - fixedSnakeCommandsIncludingClosingArcs) / 4.0f));
    }

    private int serpentineVarioSnakeLength(int extension) {
        int minimumLength = Math.max(2, extension + 2);
        return minimumLength % 2 == 0 ? minimumLength : minimumLength + 1;
    }

    private int serpentineVarioLength(int baseLength, int extension) {
        return rectangularVarioLength(baseLength, extension) + 1 + oddLongSideCorrection(baseLength, extension);
    }

    private int serpentineVarioFinalLength(int baseLength, int extension) {
        return baseLength + extension + 1 + oddExtensionCorrection(extension);
    }

    private int oddLongSideCorrection(int baseLength, int extension) {
        return baseLength % 2 == 1 ? oddExtensionCorrection(extension) : 0;
    }

    private int oddExtensionCorrection(int extension) {
        return extension % 2 == 1 ? 1 : 0;
    }

    private int largestEvenGridSteps(int steps) {
        return steps % 2 == 0 ? steps : steps - 1;
    }

    private int rectangularVarioLength(int baseLength, int extension) {
        return baseLength % 2 == 1 ? baseLength + extension : baseLength;
    }

    private void appendRepeated(StringBuilder target, char character, int repetitions) {
        for (int index = 0; index < repetitions; index++) {
            target.append(character);
        }
    }

    private void appendRepeated(StringBuilder target, String text, int repetitions) {
        for (int index = 0; index < repetitions; index++) {
            target.append(text);
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

    private PipeCursor removeLastPrimitive(List<PipePrimitive> primitives, PipeCursor cursor) {
        if (primitives.isEmpty()) {
            return cursor;
        }
        PipePrimitive removed = primitives.remove(primitives.size() - 1);
        CardinalDirection direction = removed instanceof QuarterArc arc ? arc.startDirection() : cursor.direction();
        return new PipeCursor(removed.startPoint(), direction);
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

        public RoutingResult rotatedCounterClockwise() {
            return rotatedClockwise().rotatedClockwise().rotatedClockwise();
        }

        public RoutingResult mirroredHorizontally() {
            return new RoutingResult(
                    widthMillimeters,
                    heightMillimeters,
                    spacingMillimeters,
                    supplyPath.mirroredHorizontally(),
                    returnPath.mirroredHorizontally()
            );
        }

        public RoutingResult mirroredVertically() {
            return new RoutingResult(
                    widthMillimeters,
                    heightMillimeters,
                    spacingMillimeters,
                    supplyPath.mirroredVertically(),
                    returnPath.mirroredVertically()
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

        PipePath mirroredHorizontally() {
            List<PipePrimitive> mirroredPrimitives = primitives.stream()
                    .map(PipePrimitive::mirroredHorizontally)
                    .toList();
            return new PipePath(
                    startPoint.mirroredHorizontally(),
                    endPoint.mirroredHorizontally(),
                    endDirection.mirroredHorizontally(),
                    mirroredPrimitives
            );
        }

        PipePath mirroredVertically() {
            List<PipePrimitive> mirroredPrimitives = primitives.stream()
                    .map(PipePrimitive::mirroredVertically)
                    .toList();
            return new PipePath(
                    startPoint.mirroredVertically(),
                    endPoint.mirroredVertically(),
                    endDirection.mirroredVertically(),
                    mirroredPrimitives
            );
        }
    }

    public sealed interface PipePrimitive permits LineSegment, QuarterArc {

        RoutingPoint startPoint();

        RoutingPoint endPoint();

        PipePrimitive translatedBy(double xMillimeters, double yMillimeters);

        PipePrimitive rotatedClockwise();

        PipePrimitive mirroredHorizontally();

        PipePrimitive mirroredVertically();
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

        @Override
        public LineSegment mirroredHorizontally() {
            return new LineSegment(startPoint.mirroredHorizontally(), endPoint.mirroredHorizontally());
        }

        @Override
        public LineSegment mirroredVertically() {
            return new LineSegment(startPoint.mirroredVertically(), endPoint.mirroredVertically());
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

        @Override
        public QuarterArc mirroredHorizontally() {
            return new QuarterArc(
                    startPoint.mirroredHorizontally(),
                    endPoint.mirroredHorizontally(),
                    centerPoint.mirroredHorizontally(),
                    radiusMillimeters,
                    turn.opposite(),
                    startDirection.mirroredHorizontally(),
                    endDirection.mirroredHorizontally()
            );
        }

        @Override
        public QuarterArc mirroredVertically() {
            return new QuarterArc(
                    startPoint.mirroredVertically(),
                    endPoint.mirroredVertically(),
                    centerPoint.mirroredVertically(),
                    radiusMillimeters,
                    turn.opposite(),
                    startDirection.mirroredVertically(),
                    endDirection.mirroredVertically()
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

        public RoutingPoint mirroredHorizontally() {
            return new RoutingPoint(-xMillimeters, yMillimeters);
        }

        public RoutingPoint mirroredVertically() {
            return new RoutingPoint(xMillimeters, -yMillimeters);
        }
    }

    public enum Turn {
        LEFT,
        RIGHT;

        Turn opposite() {
            return this == LEFT ? RIGHT : LEFT;
        }
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

        CardinalDirection mirroredHorizontally() {
            return switch (this) {
                case UP -> UP;
                case RIGHT -> LEFT;
                case DOWN -> DOWN;
                case LEFT -> RIGHT;
            };
        }

        CardinalDirection mirroredVertically() {
            return switch (this) {
                case UP -> DOWN;
                case RIGHT -> RIGHT;
                case DOWN -> UP;
                case LEFT -> LEFT;
            };
        }
    }
}
