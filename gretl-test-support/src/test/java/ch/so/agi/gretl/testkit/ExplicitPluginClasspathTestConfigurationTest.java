package ch.so.agi.gretl.testkit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExplicitPluginClasspathTestConfigurationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsAbsoluteDuplicateFreePluginArtifacts() throws Exception {
        Path core = Files.createFile(temporaryDirectory.resolve("gretl-core-1.jar"));
        Path geotools = Files.createFile(temporaryDirectory.resolve("gretl-geotools-1.jar"));
        Path classpath = Files.writeString(temporaryDirectory.resolve("classpath.txt"),
                core + System.lineSeparator() + geotools + System.lineSeparator());

        assertDoesNotThrow(() -> new ExplicitPluginClasspathTestConfiguration(
                classpath, temporaryDirectory.resolve("test-kit")).validate());
    }

    @Test
    void rejectsRelativeConfigurationPaths() {
        assertThrows(IllegalArgumentException.class, () -> new ExplicitPluginClasspathTestConfiguration(
                Path.of("classpath.txt"), temporaryDirectory));
        assertThrows(IllegalArgumentException.class, () -> new ExplicitPluginClasspathTestConfiguration(
                temporaryDirectory.resolve("classpath.txt"), Path.of("test-kit")));
    }

    @Test
    void rejectsHostTestArtifactsAndRawGeoToolsLibraries() throws Exception {
        Path core = Files.createFile(temporaryDirectory.resolve("gretl-core-1.jar"));
        Path geotools = Files.createFile(temporaryDirectory.resolve("gretl-geotools-1.jar"));
        Path support = Files.createFile(temporaryDirectory.resolve("gretl-test-support-1.jar"));
        Path classpath = Files.writeString(temporaryDirectory.resolve("classpath.txt"),
                core + "\n" + geotools + "\n" + support + "\n");

        assertThrows(IllegalStateException.class, () -> new ExplicitPluginClasspathTestConfiguration(
                classpath, temporaryDirectory.resolve("test-kit")).validate());
    }
}
