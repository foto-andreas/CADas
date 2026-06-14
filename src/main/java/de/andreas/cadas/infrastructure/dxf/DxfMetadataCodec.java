package de.andreas.cadas.infrastructure.dxf;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class DxfMetadataCodec {

    static final String MARKER_TYPE = "CADAS_DXF";
    static final String CURRENT_VERSION = "3";
    static final String CURRENT_MARKER = MARKER_TYPE + "|" + CURRENT_VERSION;

    private DxfMetadataCodec() {
    }

    static boolean usesCurrentEncoding(List<String> metadataEntries) {
        return metadataEntries.stream()
                .map(DxfMetadataCodec::split)
                .anyMatch(parts -> parts.length >= 2 && MARKER_TYPE.equals(parts[0]) && encodedMarkerVersion(parts[1]));
    }

    private static boolean encodedMarkerVersion(String version) {
        try {
            return Integer.parseInt(version) >= 2;
        } catch (NumberFormatException exception) {
            return false;
        }
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
}
