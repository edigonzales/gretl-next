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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GretlTestProjectSettingsTest {
    private static final String[] PROPERTY_NAMES = {
            GretlTestSystemProperties.EXECUTION_MODE,
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
    void rendersPublishedRepositoryFirstWithBothPluginMarkers() throws IOException {
        Path repository = Files.createDirectory(tempDirectory.resolve("maven repo"));
        configure(repository, "5.0.0-SNAPSHOT");

        String settings = GretlTestProjectSettings.render("core-test");

        String repositoryUri = repository.toAbsolutePath().normalize().toUri().toString();
        assertTrue(settings.contains("maven { url = uri('" + repositoryUri + "') }"));
        assertTrue(settings.indexOf(repositoryUri) < settings.indexOf("https://jars.sogeo.services/mirror"));
        assertTrue(settings.contains("id 'ch.so.agi.gretl' version '5.0.0-SNAPSHOT'"));
        assertTrue(settings.contains("id 'ch.so.agi.gretl.geotools' version '5.0.0-SNAPSHOT'"));
        assertTrue(settings.contains("FAIL_ON_PROJECT_REPOS"));
        assertFalse(settings.contains("mavenLocal()"));
        assertFalse(settings.contains("withPluginClasspath"));
        assertTrue(settings.endsWith("\n"));
    }

    @Test
    void escapesGroovyProjectNames() throws IOException {
        Path repository = Files.createDirectory(tempDirectory.resolve("repository"));
        configure(repository, "5.0.0-SNAPSHOT");

        String settings = GretlTestProjectSettings.render("name'with\\slash");

        assertTrue(settings.contains("rootProject.name = 'name\\'with\\\\slash'"));
    }

    @Test
    void rendersMinimalSettingsForPluginClasspathMode() {
        System.clearProperty(GretlTestSystemProperties.EXECUTION_MODE);

        assertTrue(GretlTestProjectSettings.render("core-test").equals("rootProject.name = 'core-test'\n"));
    }

    private void configure(Path repository, String version) {
        System.setProperty(GretlTestSystemProperties.EXECUTION_MODE, "PUBLISHED_ARTIFACT");
        System.setProperty(GretlTestSystemProperties.PUBLISHED_REPOSITORY, repository.toString());
        System.setProperty(GretlTestSystemProperties.PLUGIN_VERSION, version);
        System.setProperty(GretlTestSystemProperties.TEST_KIT_DIRECTORY,
                tempDirectory.resolve("test-kit").toString());
    }
}
