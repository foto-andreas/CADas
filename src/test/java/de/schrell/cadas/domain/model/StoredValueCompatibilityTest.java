package de.schrell.cadas.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StoredValueCompatibilityTest {

    @Test
    void liestMontagemodusUnabhängigVonSchreibweise() {
        assertEquals(
                RoomObjectMountingMode.WALL_MOUNTED,
                RoomObjectMountingMode.fromStoredValue("  wall_mounted  ", false)
        );
    }

    @Test
    void verwendetAltenAussparungswertAlsFallback() {
        assertEquals(
                RoomObjectMountingMode.CUTS_FLOOR_COVERING,
                RoomObjectMountingMode.fromStoredValue(null, true)
        );
        assertEquals(
                RoomObjectMountingMode.STANDS_ON_COVERING,
                RoomObjectMountingMode.fromStoredValue("unbekannt", false)
        );
    }

    @Test
    void liestSchnittbeschränkungAusNameOderLabel() {
        assertEquals(
                SurfaceCutRestriction.OUTER_CUTS_ROTATABLE,
                SurfaceCutRestriction.fromStoredValue("outer_cuts_rotatable")
        );
        assertEquals(
                SurfaceCutRestriction.OUTER_CUTS_ROTATABLE,
                SurfaceCutRestriction.fromStoredValue("Schnitt nur außen")
        );
    }

    @Test
    void verwendetSichereSchnittbeschränkungFürUnbekannteWerte() {
        assertEquals(SurfaceCutRestriction.fallback(), SurfaceCutRestriction.fromStoredValue(""));
        assertEquals(SurfaceCutRestriction.fallback(), SurfaceCutRestriction.fromStoredValue("unbekannt"));
    }
}
