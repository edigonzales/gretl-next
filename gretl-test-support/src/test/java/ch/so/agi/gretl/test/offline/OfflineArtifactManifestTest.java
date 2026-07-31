package ch.so.agi.gretl.test.offline;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OfflineArtifactManifestTest {
    private static final String SHA = "a".repeat(64);

    @Test
    void validatesChecksummedNonSourceArtifacts() {
        OfflineArtifact artifact = new OfflineArtifact("org.example", "demo", "1.0", "", "jar",
                "org/example/demo/1.0/demo-1.0.jar", SHA, 1, ArtifactRole.RUNTIME_DEPENDENCY);
        OfflineArtifactManifest manifest = new OfflineArtifactManifest("5.0.0", "7.6.4", Instant.now(),
                List.of(artifact), List.of(), List.of(), "test");

        assertDoesNotThrow(manifest::validate);
    }

    @Test
    void rejectsSourceArtifacts() {
        OfflineArtifact artifact = new OfflineArtifact("org.example", "demo", "1.0", "", "jar",
                "org/example/demo/1.0/demo-1.0-sources.jar", SHA, 1, ArtifactRole.RUNTIME_DEPENDENCY);
        OfflineArtifactManifest manifest = new OfflineArtifactManifest("5.0.0", "7.6.4", Instant.now(),
                List.of(artifact), List.of(), List.of(), "test");

        assertThrows(IllegalArgumentException.class, manifest::validate);
    }
}
