package ch.so.agi.gretl.test.execution;

import ch.so.agi.gretl.test.runtime.RuntimeImageRunOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record GretlBuildRequest(
        Path projectDirectory,
        List<String> arguments,
        Map<String, String> environment,
        Set<String> secretValues,
        Duration timeout,
        RuntimeImageRunOptions runtimeImageOptions) {

    public GretlBuildRequest {
        projectDirectory = projectDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(projectDirectory)) {
            throw new IllegalArgumentException("projectDirectory is not a directory: " + projectDirectory);
        }
        arguments = List.copyOf(arguments);
        if (arguments.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("arguments must not contain null");
        }
        environment = Map.copyOf(environment);
        secretValues = Set.copyOf(secretValues);
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        runtimeImageOptions = runtimeImageOptions == null
                ? RuntimeImageRunOptions.offline() : runtimeImageOptions;
    }

    @Override
    public String toString() {
        return "GretlBuildRequest[projectDirectory=" + projectDirectory
                + ", arguments=" + arguments
                + ", environment=" + environment.keySet()
                + ", secretValues=<redacted>, timeout=" + timeout
                + ", runtimeImageOptions=" + runtimeImageOptions + "]";
    }

    public static Builder builder(Path projectDirectory) {
        return new Builder(projectDirectory);
    }

    public static final class Builder {
        private final Path projectDirectory;
        private final List<String> arguments = new ArrayList<>();
        private final Map<String, String> environment = new HashMap<>();
        private final Set<String> secretValues = new HashSet<>();
        private Duration timeout = Duration.ofMinutes(2);
        private RuntimeImageRunOptions runtimeImageOptions = RuntimeImageRunOptions.offline();

        private Builder(Path projectDirectory) {
            this.projectDirectory = projectDirectory;
        }

        public Builder argument(String value) {
            arguments.add(value);
            return this;
        }

        public Builder arguments(String... values) {
            return arguments(List.of(values));
        }

        public Builder arguments(Collection<String> values) {
            arguments.addAll(values);
            return this;
        }

        public Builder environment(String name, String value) {
            environment.put(name, value);
            return this;
        }

        public Builder secret(String value) {
            secretValues.add(value);
            return this;
        }

        public Builder timeout(Duration value) {
            timeout = value;
            return this;
        }

        public Builder runtimeImageOptions(RuntimeImageRunOptions value) {
            runtimeImageOptions = value;
            return this;
        }

        public GretlBuildRequest build() {
            return new GretlBuildRequest(projectDirectory, arguments, environment, secretValues, timeout,
                    runtimeImageOptions);
        }
    }
}
