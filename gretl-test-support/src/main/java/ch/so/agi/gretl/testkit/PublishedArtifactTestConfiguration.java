package ch.so.agi.gretl.testkit;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public record PublishedArtifactTestConfiguration(
        Path repository,
        String pluginVersion,
        Path testKitDirectory) {

    public static PublishedArtifactTestConfiguration fromSystemProperties() {
        String repositoryValue = required(GretlTestSystemProperties.PUBLISHED_REPOSITORY);
        String version = required(GretlTestSystemProperties.PLUGIN_VERSION);
        String testKitValue = required(GretlTestSystemProperties.TEST_KIT_DIRECTORY);

        Path repository = Path.of(repositoryValue).toAbsolutePath().normalize();
        if (!Files.exists(repository)) {
            throw new IllegalStateException(
                    "Published GRETL repository does not exist: " + repository);
        }
        if (!Files.isDirectory(repository)) {
            throw new IllegalStateException(
                    "Published GRETL repository is not a directory: " + repository);
        }

        Path testKitDirectory = Path.of(testKitValue).toAbsolutePath().normalize();
        try {
            Files.createDirectories(testKitDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot create TestKit directory '" + testKitDirectory + "'.", e);
        }
        if (!Files.isDirectory(testKitDirectory)) {
            throw new IllegalStateException(
                    "TestKit path is not a directory: " + testKitDirectory);
        }

        return new PublishedArtifactTestConfiguration(repository, version, testKitDirectory);
    }

    public URI repositoryUri() {
        return repository.toAbsolutePath().normalize().toUri();
    }

    private static String required(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required system property '" + propertyName
                            + "' for PUBLISHED_ARTIFACT test execution.");
        }
        return value.trim();
    }
}
