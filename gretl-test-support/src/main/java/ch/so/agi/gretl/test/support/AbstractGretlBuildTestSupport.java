package ch.so.agi.gretl.test.support;

import ch.so.agi.gretl.test.execution.GretlBuildExecutor;
import ch.so.agi.gretl.test.execution.GretlBuildExecutors;
import ch.so.agi.gretl.test.execution.GretlBuildRequest;
import ch.so.agi.gretl.test.execution.GretlBuildResult;
import ch.so.agi.gretl.test.runtime.RuntimeImageRunOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractGretlBuildTestSupport {
    protected Path projectDir;

    protected GretlBuildExecutor executor() {
        return GretlBuildExecutors.forCurrentMode();
    }

    protected GretlBuildResult run(String... arguments) {
        return executor().execute(buildRequest(arguments));
    }

    protected GretlBuildResult runAndFail(String... arguments) {
        return executor().executeAndExpectFailure(buildRequest(arguments));
    }

    protected GretlBuildRequest buildRequest(String... arguments) {
        return GretlBuildRequest.builder(projectDir)
                .arguments(arguments)
                .timeout(runtimeImageTimeout())
                .runtimeImageOptions(runtimeImageRunOptions())
                .build();
    }

    protected Duration runtimeImageTimeout() {
        String configuredSeconds = System.getProperty("gretl.test.runtimeImage.timeoutSeconds");
        if (configuredSeconds == null || configuredSeconds.isBlank()) {
            return Duration.ofMinutes(2);
        }
        try {
            long seconds = Long.parseLong(configuredSeconds);
            if (seconds <= 0) {
                throw new NumberFormatException("not positive");
            }
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "gretl.test.runtimeImage.timeoutSeconds must be a positive integer: " + configuredSeconds, e);
        }
    }

    protected List<String> defaultArguments() {
        return List.of("--console=plain", "--stacktrace", "--rerun-tasks");
    }

    protected Map<String, String> defaultEnvironment() {
        return Map.of("TZ", "Europe/Zurich", "LANG", "C.UTF-8", "LC_ALL", "C.UTF-8");
    }

    protected Set<String> secretValues() {
        return Set.of();
    }

    protected RuntimeImageRunOptions runtimeImageRunOptions() {
        return RuntimeImageRunOptions.defaults();
    }

    protected void writeSettings(String content) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), content, StandardCharsets.UTF_8);
    }

    protected void writeGroovyBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), content, StandardCharsets.UTF_8);
    }

    protected void writeKotlinBuild(String content) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), content, StandardCharsets.UTF_8);
    }

    protected Path copyResource(String source, String target) throws IOException {
        Path resource = Path.of(getClass().getClassLoader().getResource(source).getPath());
        Path destination = projectDir.resolve(target);
        Files.createDirectories(destination.getParent());
        return Files.copy(resource, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    protected void copyResourceTree(String source, Path target) throws IOException {
        Path resource = Path.of(getClass().getClassLoader().getResource(source).getPath());
        try (var paths = Files.walk(resource)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                Path destination = target.resolve(resource.relativize(path));
                try {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }
}
