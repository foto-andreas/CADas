package de.schrell.cadas.infrastructure.dxf;

import de.schrell.cadas.domain.geometry.Length;
import de.schrell.cadas.domain.geometry.PlanPoint;
import de.schrell.cadas.domain.model.Room;
import de.schrell.cadas.domain.model.SlopedCeilingProfile;
import de.schrell.cadas.domain.model.SlopedCeilingSide;
import de.schrell.cadas.domain.model.Wall;
import de.schrell.cadas.domain.model.WallProfilePoint;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kodiert und dekodiert wiederkehrende Metadatenwerte wie Punkte, Profile, Zahlen und UUIDs konsistent.
 * Ungültige optionale Werte werden lokal verworfen, damit andere rettbare DXF-Datensätze erhalten bleiben.
 */
final class DxfMetadataCodec {

    static final String MARKER_TYPE = "CADAS_DXF";
    static final String CURRENT_VERSION = "6";
    static final String CURRENT_MARKER = MARKER_TYPE + "|" + CURRENT_VERSION;

    private DxfMetadataCodec() {
    }

    static boolean usesCurrentEncoding(List<String> metadataEntries) {
        return metadataEntries.stream()
                .map(DxfMetadataCodec::split)
                .anyMatch(parts -> parts.length >= 2 && MARKER_TYPE.equals(parts[0]) && encodedMarkerVersion(parts[1]));
    }

    static boolean usesObjectRotationDegrees(List<String> metadataEntries) {
        return markerVersion(metadataEntries) >= 4;
    }

    private static int markerVersion(List<String> metadataEntries) {
        return metadataEntries.stream()
                .map(DxfMetadataCodec::split)
                .filter(parts -> parts.length >= 2 && MARKER_TYPE.equals(parts[0]))
                .mapToInt(parts -> parseVersion(parts[1]))
                .max()
                .orElse(0);
    }

    private static int parseVersion(String version) {
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static boolean encodedMarkerVersion(String version) {
        return parseVersion(version) >= 2;
    }

    static String[] split(String metadataEntry) {
        return metadataEntry.split("\\|", -1);
    }

    static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    static String decode(String value, boolean encoded) {
        if (encoded) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        return value.replace('/', '|');
    }

    static boolean isMarker(String[] parts) {
        return parts.length >= 2 && MARKER_TYPE.equals(parts[0]);
    }

    static boolean isUuid(String text) {
        if (text == null || text.length() != 36) {
            return false;
        }
        try {
            UUID.fromString(text);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    static String serializePoints(List<PlanPoint> points) {
        return points.stream()
                .map(point -> String.format(Locale.US, "%.3f,%.3f", point.xMillimeters(), point.yMillimeters()))
                .collect(Collectors.joining(";"));
    }

    static List<PlanPoint> deserializePoints(String value) {
        return Arrays.stream(value.split(";"))
                .map(entry -> entry.split(","))
                .map(coordinates -> new PlanPoint(parseDouble(coordinates[0]), parseDouble(coordinates[1])))
                .toList();
    }

    static String serializeSlopedCeiling(Room room) {
        if (room.slopedCeilingProfiles().isEmpty()) {
            return "NONE";
        }
        return "SLOPES;" + room.slopedCeilingProfiles().stream()
                .map(profile -> String.format(Locale.US, "%s,%.3f,%.3f",
                        profile.lowSide().name(),
                        profile.kneeWallHeight().toMillimeters(),
                        profile.horizontalRun().toMillimeters()))
                .collect(Collectors.joining(";"));
    }

    static String serializeCeilingVertexHeights(Room room) {
        return room.ceilingVertexHeightsProfile()
                .map(heights -> heights.stream()
                        .map(height -> String.format(Locale.US, "%.3f", height.toMillimeters()))
                        .collect(Collectors.joining(";")))
                .orElse("NONE");
    }

    static List<SlopedCeilingProfile> deserializeSlopedCeilings(String value) {
        if (value == null || value.isBlank() || value.equals("NONE")) {
            return List.of();
        }
        if (value.startsWith("SLOPES;")) {
            return Arrays.stream(value.substring("SLOPES;".length()).split(";"))
                    .map(DxfMetadataCodec::deserializeSlopeValues)
                    .toList();
        }
        String[] parts = value.split(",");
        if ((parts.length != 3 && parts.length != 4) || !parts[0].equals("SLOPE")) {
            return List.of();
        }
        return List.of(new SlopedCeilingProfile(
                SlopedCeilingSide.valueOf(parts[1]),
                Length.ofMillimeters(parseDouble(parts[2])),
                parts.length == 4 ? Length.ofMillimeters(parseDouble(parts[3])) : Length.zero()
        ));
    }

    static List<Length> deserializeCeilingVertexHeights(String value) {
        if (value == null || value.isBlank() || value.equals("NONE")) {
            return null;
        }
        return Arrays.stream(value.split(";"))
                .map(DxfMetadataCodec::parseDouble)
                .map(Length::ofMillimeters)
                .toList();
    }

    static String serializeWallProfile(Wall wall) {
        if (!wall.hasPolygonalProfile()) {
            return "NONE";
        }
        return wall.profile().stream()
                .map(point -> String.format(Locale.US, "%.3f,%.3f",
                        point.offset().toMillimeters(), point.height().toMillimeters()))
                .collect(Collectors.joining(";"));
    }

    static List<WallProfilePoint> deserializeWallProfile(String value) {
        if (value == null || value.isBlank() || value.equals("NONE")) {
            return List.of();
        }
        List<WallProfilePoint> profile = new ArrayList<>();
        for (String serializedPoint : value.split(";")) {
            String[] coordinates = serializedPoint.split(",");
            if (coordinates.length != 2) {
                throw new IllegalArgumentException("Ungültiger Wandprofilpunkt: " + serializedPoint);
            }
            profile.add(new WallProfilePoint(
                    Length.ofMillimeters(parseDouble(coordinates[0])),
                    Length.ofMillimeters(parseDouble(coordinates[1]))
            ));
        }
        return List.copyOf(profile);
    }

    private static SlopedCeilingProfile deserializeSlopeValues(String value) {
        String[] parts = value.split(",");
        return new SlopedCeilingProfile(
                SlopedCeilingSide.valueOf(parts[0]),
                Length.ofMillimeters(parseDouble(parts[1])),
                Length.ofMillimeters(parseDouble(parts[2]))
        );
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value);
    }
}
