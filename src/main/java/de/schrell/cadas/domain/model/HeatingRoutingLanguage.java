package de.schrell.cadas.domain.model;

public final class HeatingRoutingLanguage {

    public static final char SUPPLY_LINE = '=';
    public static final char RETURN_LINE = '-';
    public static final char SUPPLY_TURN_RIGHT = 'R';
    public static final char RETURN_TURN_RIGHT = 'r';
    public static final char SUPPLY_TURN_LEFT = 'L';
    public static final char RETURN_TURN_LEFT = 'l';
    public static final char SUPPLY_DELETE = 'X';
    public static final char RETURN_DELETE = 'x';
    public static final char CONNECTOR_SEPARATOR = '+';
    public static final char LEGACY_SUPPLY_LINE = 'I';
    public static final char LEGACY_RETURN_LINE = 'i';

    private HeatingRoutingLanguage() {
    }

    public static boolean isIgnoredCharacter(char character) {
        return Character.isWhitespace(character);
    }

    public static boolean isCommandCharacter(char character) {
        char normalized = normalizeCharacter(character);
        return isSeparator(normalized) || isSupplyCommand(normalized) || isReturnCommand(normalized);
    }

    public static boolean isSeparator(char character) {
        return normalizeCharacter(character) == CONNECTOR_SEPARATOR;
    }

    public static boolean isSupplyCommand(char character) {
        char normalized = normalizeCharacter(character);
        return normalized == SUPPLY_LINE
                || normalized == SUPPLY_TURN_RIGHT
                || normalized == SUPPLY_TURN_LEFT
                || normalized == SUPPLY_DELETE;
    }

    public static boolean isReturnCommand(char character) {
        char normalized = normalizeCharacter(character);
        return normalized == RETURN_LINE
                || normalized == RETURN_TURN_RIGHT
                || normalized == RETURN_TURN_LEFT
                || normalized == RETURN_DELETE;
    }

    public static boolean isLineCommand(char character) {
        char normalized = normalizeCharacter(character);
        return normalized == SUPPLY_LINE || normalized == RETURN_LINE;
    }

    public static char normalizeCharacter(char character) {
        return switch (character) {
            case LEGACY_SUPPLY_LINE -> SUPPLY_LINE;
            case LEGACY_RETURN_LINE -> RETURN_LINE;
            default -> character;
        };
    }

    public static String normalizeCommands(String commands) {
        if (commands == null || commands.isBlank()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(commands.length());
        for (int index = 0; index < commands.length(); index++) {
            char character = commands.charAt(index);
            if (isIgnoredCharacter(character)) {
                continue;
            }
            char normalizedCharacter = normalizeCharacter(character);
            if (!isCommandCharacter(normalizedCharacter)) {
                throw new IllegalArgumentException("Unbekannter Routing-Befehl `" + character + "`.");
            }
            normalized.append(normalizedCharacter);
        }
        return normalized.toString();
    }

    public static String stripWhitespaceAndNormalizeAliases(String commands) {
        if (commands == null || commands.isBlank()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(commands.length());
        for (int index = 0; index < commands.length(); index++) {
            char character = commands.charAt(index);
            if (!isIgnoredCharacter(character)) {
                normalized.append(normalizeCharacter(character));
            }
        }
        return normalized.toString();
    }

    public static String ensureConnectorSeparator(String commands) {
        String normalized = normalizeCommands(commands);
        if (normalized.isBlank() || normalized.indexOf(CONNECTOR_SEPARATOR) >= 0) {
            return normalized;
        }
        return normalized + CONNECTOR_SEPARATOR;
    }

    public static String fieldCommands(String commands) {
        String normalized = normalizeCommands(commands);
        int separatorIndex = normalized.indexOf(CONNECTOR_SEPARATOR);
        return separatorIndex >= 0 ? normalized.substring(0, separatorIndex) : normalized;
    }
}
