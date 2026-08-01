package ch.so.agi.gretl.combined;

import ch.so.agi.gretl.testkit.GretlBuildExecutor;
import ch.so.agi.gretl.testkit.GretlBuildExecutors;
import ch.so.agi.gretl.testkit.GretlTestProjectSettings;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class CombinedPluginTestSupport {
    @TempDir
    Path projectDir;

    protected GretlBuildExecutor executor() {
        return GretlBuildExecutors.current();
    }

    protected BuildResult run(String... arguments) {
        return executor().run(projectDir, arguments);
    }

    protected BuildResult runAndFail(String... arguments) {
        return executor().runAndFail(projectDir, arguments);
    }

    protected void writeSettings() throws IOException {
        writeSettings("combined-consumer");
    }

    protected void writeSettings(String projectName) throws IOException {
        GretlTestProjectSettings.write(projectDir, projectName);
    }

    protected void writeSettingsWithIncludes(String projectName, String... projectNames) throws IOException {
        StringBuilder settings = new StringBuilder(GretlTestProjectSettings.render(projectName));
        settings.append("\ninclude ");
        for (int i = 0; i < projectNames.length; i++) {
            if (i > 0) {
                settings.append(", ");
            }
            settings.append("'").append(projectNames[i].replace("'", "\\'")).append("'");
        }
        settings.append("\n");
        Files.writeString(projectDir.resolve("settings.gradle"), settings, StandardCharsets.UTF_8);
    }

    protected void writeGroovyBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), content, StandardCharsets.UTF_8);
    }

    protected void writeKotlinBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), content, StandardCharsets.UTF_8);
    }

    protected Path copyResource(String source, String target) throws IOException, URISyntaxException {
        Path targetPath = projectDir.resolve(target).normalize();
        if (!targetPath.startsWith(projectDir)) {
            throw new IllegalArgumentException("Fixture target escapes project directory: " + target);
        }
        Files.createDirectories(targetPath.getParent());
        Files.copy(resourcePath(source), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath;
    }

    protected void copyResourceTree(String source, Path target) throws IOException, URISyntaxException {
        Path sourcePath = resourcePath(source);
        Files.createDirectories(target);
        try (Stream<Path> paths = Files.walk(sourcePath)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                Path destination = target.resolve(sourcePath.relativize(file));
                try {
                    Files.createDirectories(destination.getParent());
                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot copy fixture " + file, e);
                }
            });
        }
    }

    protected Path projectPath(String relative) {
        return projectDir.resolve(relative);
    }

    protected void assertTaskOutcome(BuildResult result, String taskPath, TaskOutcome expected) {
        CombinedBuildResultAssertions.assertOutcome(result, taskPath, expected);
    }

    protected void assertTaskNotExecuted(BuildResult result, String taskPath) {
        CombinedBuildResultAssertions.assertNotExecuted(result, taskPath);
    }

    protected void assertNoCombinedPluginWarnings(BuildResult result) {
        CombinedBuildResultAssertions.assertNoClassloaderFailure(result);
        CombinedBuildResultAssertions.assertNoWorkerProtocolLeak(result);
    }

    private Path resourcePath(String resource) throws URISyntaxException {
        URL url = getClass().getClassLoader().getResource(resource);
        assertNotNull(url, "Missing combined test resource: " + resource);
        return Path.of(url.toURI());
    }
}
