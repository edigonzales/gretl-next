package ch.so.agi.gretl.test.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerRunCommandBuilderTest {
    @TempDir
    Path temp;

    @Test
    void buildsOfflineCommandWithImmutableImageAndIsolatedHomes() throws Exception {
        Path project = Files.createDirectory(temp.resolve("project"));
        Path gradleHome = Files.createDirectory(temp.resolve("gradle"));
        List<String> command = new DockerRunCommandBuilder().build(new DockerRunRequest(
                "sha256:" + "a".repeat(64), "gretl-test", project, gradleHome,
                List.of("--offline", "task with spaces"), Map.of("SECRET", "value"), Optional.empty(), true,
                Optional.of("1000:1000"), Duration.ofMinutes(1), Set.of("value"), Map.of(), Map.of()));

        assertTrue(command.contains("--pull=never"));
        assertTrue(command.contains("--network"));
        assertTrue(command.contains("none"));
        assertTrue(command.contains("sha256:" + "a".repeat(64)));
        assertTrue(command.contains("task with spaces"));
        assertFalse(command.stream().anyMatch(value -> value.equals("--no-daemon")));
    }

    @Test
    void rejectsConflictingNetworkOptions() throws Exception {
        Path project = Files.createDirectory(temp.resolve("project"));
        Path gradleHome = Files.createDirectory(temp.resolve("gradle"));
        assertThrows(IllegalArgumentException.class, () -> new DockerRunRequest(
                "image", "name", project, gradleHome, List.of(), Map.of(), Optional.of("network"), true,
                Optional.empty(), Duration.ofSeconds(1), Set.of(), Map.of(), Map.of()));
    }
}
