package ch.so.agi.gretl;

import ch.so.agi.gretl.testkit.GretlBuildExecutors;
import ch.so.agi.gretl.testkit.GretlTestExecutionMode;
import ch.so.agi.gretl.testkit.GretlTestProjectSettings;
import ch.so.agi.gretl.testkit.PublishedArtifactTestConfiguration;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("published-artifact-only")
class PublishedArtifactIsolationFunctionalTest extends CoreFunctionalTestSupport {

    @Test
    void doesNotResolvePluginWithoutPublishedRepository() throws Exception {
        assertEquals(GretlTestExecutionMode.PUBLISHED_ARTIFACT, GretlTestExecutionMode.current());
        PublishedArtifactTestConfiguration configuration =
                PublishedArtifactTestConfiguration.fromSystemProperties();

        Path emptyRepository = Files.createDirectories(projectDir.resolve("empty-maven-repository"));
        Path emptyTestKitDirectory = Files.createDirectories(projectDir.resolve("empty-test-kit"));
        writeEmptyRepositorySettings(emptyRepository, configuration.pluginVersion());
        Files.writeString(projectDir.resolve("build.gradle"), """
                plugins {
                    id 'ch.so.agi.gretl'
                }
                """, StandardCharsets.UTF_8);

        BuildResult result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withTestKitDir(emptyTestKitDirectory.toFile())
                .withArguments("tasks", "--stacktrace")
                .forwardOutput()
                .buildAndFail();

        assertTrue(result.getOutput().contains("ch.so.agi.gretl"));
        assertTrue(result.getOutput().contains(configuration.pluginVersion()));
        assertTrue(result.getOutput().toLowerCase().contains("not found")
                || result.getOutput().toLowerCase().contains("could not resolve"));
    }

    @Test
    void resolvesBothPluginMarkersFromPublishedRepository() throws IOException {
        assertEquals(GretlTestExecutionMode.PUBLISHED_ARTIFACT, GretlTestExecutionMode.current());
        GretlTestProjectSettings.write(projectDir, "published-marker-test");
        writeBuild("""
                plugins {
                    id 'ch.so.agi.gretl'
                    id 'ch.so.agi.gretl.geotools'
                }

                import ch.so.agi.gretl.tasks.Gzip

                tasks.register('coreCanary', Gzip)
                """);

        BuildResult result = GretlBuildExecutors.current().run(projectDir, "tasks", "--all");

        assertTrue(result.getOutput().contains("coreCanary"));
        assertTrue(result.getOutput().contains("readShapefile"));
        assertFalse(result.getOutput().contains("Plugin [id: 'ch.so.agi.gretl'] was not found"));
        assertFalse(result.getOutput().contains("Plugin [id: 'ch.so.agi.gretl.geotools'] was not found"));
    }

    private void writeEmptyRepositorySettings(Path repository, String version) throws IOException {
        String repositoryUri = repository.toAbsolutePath().normalize().toUri().toString()
                .replace("\\", "\\\\")
                .replace("'", "\\'");
        String escapedVersion = version.replace("\\", "\\\\").replace("'", "\\'");
        Files.writeString(projectDir.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        maven { url = uri('%s') }
                    }
                    plugins {
                        id 'ch.so.agi.gretl' version '%s'
                    }
                }
                rootProject.name = 'empty-repository-test'
                """.formatted(repositoryUri, escapedVersion), StandardCharsets.UTF_8);
    }
}
