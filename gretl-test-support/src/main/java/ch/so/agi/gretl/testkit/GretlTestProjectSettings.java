package ch.so.agi.gretl.testkit;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class GretlTestProjectSettings {
    private static final String CORE_PLUGIN_ID = "ch.so.agi.gretl";
    private static final String GEOTOOLS_PLUGIN_ID = "ch.so.agi.gretl.geotools";

    public static void write(Path projectDirectory, String rootProjectName) throws IOException {
        Objects.requireNonNull(projectDirectory, "projectDirectory must not be null");
        if (!Files.exists(projectDirectory) || !Files.isDirectory(projectDirectory)) {
            throw new IllegalArgumentException(
                    "Gradle test project must be an existing directory: " + projectDirectory);
        }
        Files.writeString(
                projectDirectory.resolve("settings.gradle"),
                render(rootProjectName),
                StandardCharsets.UTF_8);
    }

    public static String render(String rootProjectName) {
        Objects.requireNonNull(rootProjectName, "rootProjectName must not be null");
        if (rootProjectName.isBlank()) {
            throw new IllegalArgumentException("rootProjectName must not be blank");
        }

        if (GretlTestExecutionMode.current() == GretlTestExecutionMode.PLUGIN_CLASSPATH) {
            return "rootProject.name = '" + escapeGroovyString(rootProjectName) + "'\n";
        }

        PublishedArtifactTestConfiguration configuration = PublishedArtifactTestConfiguration.fromSystemProperties();
        return renderPublished(rootProjectName, configuration.repositoryUri(), configuration.pluginVersion());
    }

    public static String renderPublished(String rootProjectName, URI repositoryUri, String pluginVersion) {
        Objects.requireNonNull(repositoryUri, "repositoryUri must not be null");
        Objects.requireNonNull(pluginVersion, "pluginVersion must not be null");
        if (pluginVersion.isBlank()) throw new IllegalArgumentException("pluginVersion must not be blank");
        String repository = escapeGroovyString(repositoryUri.toString());
        String version = escapeGroovyString(pluginVersion);
        String projectName = escapeGroovyString(rootProjectName);

        return """
                pluginManagement {
                    repositories {
                        maven { url = uri('%s') }
                        maven { url = uri('https://jars.sogeo.services/mirror') }
                        maven { url = uri('https://repo.osgeo.org/repository/release/') }
                        maven { url = uri('https://maven.geo-solutions.it') }
                        mavenCentral()
                        gradlePluginPortal()
                    }
                    plugins {
                        id '%s' version '%s'
                        id '%s' version '%s'
                    }
                }

                dependencyResolutionManagement {
                    repositoriesMode.set(org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories {
                        maven { url = uri('%s') }
                        maven { url = uri('https://jars.sogeo.services/mirror') }
                        maven { url = uri('https://repo.osgeo.org/repository/release/') }
                        maven { url = uri('https://maven.geo-solutions.it') }
                        mavenCentral()
                    }
                }

                rootProject.name = '%s'
                """.formatted(
                repository, CORE_PLUGIN_ID, version, GEOTOOLS_PLUGIN_ID, version, repository, projectName);
    }

    private static String escapeGroovyString(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private GretlTestProjectSettings() {
    }
}
