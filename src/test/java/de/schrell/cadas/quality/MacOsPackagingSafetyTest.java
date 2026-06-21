package de.schrell.cadas.quality;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MacOsPackagingSafetyTest {

    @Test
    void paketierungUndInstallationVerwendenKeineExternenVerzeichnislinks() throws Exception {
        String buildScript = Files.readString(Path.of("build.gradle.kts"));

        assertFalse(buildScript.contains("createSymbolicLink"));
        assertFalse(buildScript.contains("dmgStagingDirectory.deleteRecursively"));
        assertTrue(buildScript.contains("deleteTreeWithoutFollowingLinks"));
        assertTrue(buildScript.contains("Path.of(\"/Applications\", \"CADas.app\")"));
        assertTrue(buildScript.contains("target == allowedTarget"));
        assertTrue(buildScript.contains("!Files.isSymbolicLink(target)"));
        assertTrue(buildScript.contains("hasExpectedBundleIdentifier"));
    }
}
