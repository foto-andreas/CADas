package de.andreas.cadas.application.layers;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WallSurfaceTargetKey {

    private static final String ROOM_SEPARATOR = "@";

    private WallSurfaceTargetKey() {
    }

    public static String interior(UUID wallId, UUID roomId) {
        return interior(wallId.toString(), roomId.toString());
    }

    public static String interior(String wallId, String roomId) {
        return Objects.requireNonNull(wallId, "wallId darf nicht null sein.")
                + ROOM_SEPARATOR
                + Objects.requireNonNull(roomId, "roomId darf nicht null sein.");
    }

    public static String wallId(String targetKey) {
        Objects.requireNonNull(targetKey, "targetKey darf nicht null sein.");
        int separatorIndex = targetKey.indexOf(ROOM_SEPARATOR);
        return separatorIndex >= 0 ? targetKey.substring(0, separatorIndex) : targetKey;
    }

    public static Optional<UUID> roomId(String targetKey) {
        Objects.requireNonNull(targetKey, "targetKey darf nicht null sein.");
        int separatorIndex = targetKey.indexOf(ROOM_SEPARATOR);
        if (separatorIndex < 0 || separatorIndex + 1 >= targetKey.length()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(targetKey.substring(separatorIndex + 1)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public static boolean matchesWall(String targetKey, UUID wallId) {
        return wallId(targetKey).equals(wallId.toString());
    }
}
