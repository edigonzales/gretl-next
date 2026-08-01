package ch.so.agi.gretl.testkit;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

abstract class AbstractGradleBuildExecutor implements GretlBuildExecutor {
    static final String DEFAULT_TEST_KIT_JVM_ARGS = GretlTestSystemProperties.DEFAULT_GRADLE_JVM_ARGS;

    @Override
    public final BuildResult run(Path projectDirectory, String... arguments) {
        return baseRunner(projectDirectory, arguments).build();
    }

    @Override
    public final BuildResult runAndFail(Path projectDirectory, String... arguments) {
        return baseRunner(projectDirectory, arguments).buildAndFail();
    }

    protected abstract GradleRunner customize(GradleRunner runner);

    protected final GradleRunner baseRunner(Path projectDirectory, String... arguments) {
        Objects.requireNonNull(projectDirectory, "projectDirectory must not be null");
        if (!Files.exists(projectDirectory) || !Files.isDirectory(projectDirectory)) {
            throw new IllegalArgumentException(
                    "Gradle test project must be an existing directory: " + projectDirectory);
        }
        Map<String, String> environment = new HashMap<>(System.getenv());

        return customize(GradleRunner.create()
                .withProjectDir(projectDirectory.toFile())
                .withArguments(normalizeArguments(arguments))
                .withEnvironment(environment)
                .forwardOutput());
    }

    static List<String> normalizeArguments(String... arguments) {
        if (arguments == null) {
            throw new IllegalArgumentException("Gradle arguments must not be null.");
        }
        List<String> normalized = new ArrayList<>(arguments.length + 1);
        for (String argument : arguments) {
            if (argument == null) {
                throw new IllegalArgumentException("Gradle arguments must not contain null.");
            }
            normalized.add(argument);
        }
        if (normalized.stream().noneMatch(argument -> argument.startsWith("-Dorg.gradle.jvmargs="))) {
            String configuredJvmArgs = System.getProperty(
                    GretlTestSystemProperties.GRADLE_JVM_ARGS, DEFAULT_TEST_KIT_JVM_ARGS).trim();
            if (configuredJvmArgs.isEmpty()) {
                throw new IllegalStateException(
                        "TestKit JVM arguments must not be empty. Configure "
                                + GretlTestSystemProperties.GRADLE_JVM_ARGS + ".");
            }
            normalized.add(0, "-Dorg.gradle.jvmargs=" + configuredJvmArgs);
        }
        if (normalized.stream().noneMatch(argument -> argument.equals("-Dorg.gradle.daemon=false"))) {
            normalized.add(0, "-Dorg.gradle.daemon=false");
        }
        if (!normalized.contains("--stacktrace") && !normalized.contains("-s")) {
            normalized.add("--stacktrace");
        }
        return List.copyOf(normalized);
    }
}
