package ch.so.agi.gretl.testkit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishedArtifactTestConfigurationTest {
    private static final String[] PROPERTY_NAMES = {
            GretlTestSystemProperties.PUBLISHED_REPOSITORY,
            GretlTestSystemProperties.PLUGIN_VERSION,
            GretlTestSystemProperties.TEST_KIT_DIRECTORY
    };

    @TempDir
    Path tempDirectory;
    private final Map<String, String> originalProperties = new HashMap<>();

    @BeforeEach
    void captureSystemProperties() {
        for (String propertyName : PROPERTY_NAMES) {
            originalProperties.put(propertyName, System.getProperty(propertyName));
        }
    }

    @AfterEach
    void restoreSystemProperties() {
        for (String propertyName : PROPERTY_NAMES) {
            String originalValue = originalProperties.get(propertyName);
            if (originalValue == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, originalValue);
            }
        }
    }

    @Test
    void reportsMissingRepositoryProperty() {
        System.setProperty(GretlTestSystemProperties.PLUGIN_VERSION, "5.0.0-SNAPSHOT");
        System.setProperty(GretlTestSystemProperties.TEST_KIT_DIRECTORY,
                tempDirectory.resolve("test-kit").toString());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, PublishedArtifactTestConfiguration::fromSystemProperties);

        assertTrue(exception.getMessage().contains(GretlTestSystemProperties.PUBLISHED_REPOSITORY));
    }

    @Test
    void reportsMissingVersionProperty() throws IOException {
        Path repository = Files.createDirectory(tempDirectory.resolve("repository"));
        System.setProperty(GretlTestSystemProperties.PUBLISHED_REPOSITORY, repository.toString());
        System.setProperty(GretlTestSystemProperties.TEST_KIT_DIRECTORY,
                tempDirectory.resolve("test-kit").toString());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, PublishedArtifactTestConfiguration::fromSystemProperties);

        assertTrue(exception.getMessage().contains(GretlTestSystemProperties.PLUGIN_VERSION));
    }

    @Test
    void reportsMissingTestKitProperty() throws IOException {
        Path repository = Files.createDirectory(tempDirectory.resolve("repository"));
        System.setProperty(GretlTestSystemProperties.PUBLISHED_REPOSITORY, repository.toString());
        System.setProperty(GretlTestSystemProperties.PLUGIN_VERSION, "5.0.0-SNAPSHOT");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, PublishedArtifactTestConfiguration::fromSystemProperties);

        assertTrue(exception.getMessage().contains(GretlTestSystemProperties.TEST_KIT_DIRECTORY));
    }

    @Test
    void rejectsRepositoryFile() throws IOException {
        Path repositoryFile = Files.createFile(tempDirectory.resolve("repository"));
        configure(repositoryFile, "5.0.0-SNAPSHOT", tempDirectory.resolve("test-kit"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, PublishedArtifactTestConfiguration::fromSystemProperties);

        assertTrue(exception.getMessage().contains("not a directory"));
    }

    @Test
    void normalizesRepositoryAndCreatesTestKitDirectory() throws IOException {
        Path repository = Files.createDirectory(tempDirectory.resolve("repository"));
        Path testKitDirectory = tempDirectory.resolve("nested/test-kit");
        configure(repository.resolve(".").resolve("child").resolve(".."),
                "5.0.0-SNAPSHOT", testKitDirectory);

        PublishedArtifactTestConfiguration configuration =
                PublishedArtifactTestConfiguration.fromSystemProperties();

        assertEquals(repository.toAbsolutePath().normalize(), configuration.repository());
        assertEquals(testKitDirectory.toAbsolutePath().normalize(), configuration.testKitDirectory());
        assertTrue(Files.isDirectory(testKitDirectory));
        assertEquals(repository.toUri(), configuration.repositoryUri());
        assertFalse(configuration.pluginVersion().isBlank());
    }

    private void configure(Path repository, String version, Path testKitDirectory) {
        System.setProperty(GretlTestSystemProperties.PUBLISHED_REPOSITORY, repository.toString());
        System.setProperty(GretlTestSystemProperties.PLUGIN_VERSION, version);
        System.setProperty(GretlTestSystemProperties.TEST_KIT_DIRECTORY, testKitDirectory.toString());
    }
}
