package ch.so.agi.gretl.test.docker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record DockerRunRequest(
        String imageReference,
        String containerName,
        Path projectDirectory,
        Path gradleUserHome,
        List<String> commandArguments,
        Map<String, String> environment,
        Optional<String> network,
        boolean networkDisabled,
        Optional<String> user,
        Duration timeout,
        Set<String> secretValues,
        Map<Path, String> additionalReadOnlyMounts,
        Map<Path, String> additionalReadWriteMounts) {

    public DockerRunRequest {
        if (imageReference == null || imageReference.isBlank()) {
            throw new IllegalArgumentException("imageReference must not be blank");
        }
        if (containerName == null || !containerName.matches("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}")) {
            throw new IllegalArgumentException("Invalid Docker container name: " + containerName);
        }
        projectDirectory = existingDirectory(projectDirectory, "projectDirectory");
        gradleUserHome = Objects.requireNonNull(gradleUserHome, "gradleUserHome must not be null")
                .toAbsolutePath().normalize();
        if (projectDirectory.equals(gradleUserHome)) {
            throw new IllegalArgumentException("projectDirectory and gradleUserHome must be different");
        }
        commandArguments = List.copyOf(commandArguments);
        environment = Map.copyOf(environment);
        network = network == null ? Optional.empty() : network;
        user = user == null ? Optional.empty() : user;
        if (network.isPresent() == networkDisabled) {
            throw new IllegalArgumentException("exactly one Docker network mode must be selected");
        }
        timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        secretValues = Set.copyOf(secretValues);
        additionalReadOnlyMounts = normalizeMounts(additionalReadOnlyMounts);
        additionalReadWriteMounts = normalizeMounts(additionalReadWriteMounts);
        Set<String> targets = new java.util.HashSet<>();
        targets.add("/home/gradle/project");
        targets.add("/home/gradle/.gradle");
        additionalReadOnlyMounts.values().forEach(target -> addTarget(target, targets));
        additionalReadWriteMounts.values().forEach(target -> addTarget(target, targets));
    }

    private static void addTarget(String target, Set<String> targets) {
        if (!Path.of(target).isAbsolute() || !targets.add(target)) {
            throw new IllegalArgumentException("Duplicate or non-absolute Docker mount target: " + target);
        }
    }

    private static Path existingDirectory(Path path, String name) {
        Objects.requireNonNull(path, name + " must not be null");
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException(name + " is not a directory: " + normalized);
        }
        return normalized;
    }

    private static Path existingPath(Path path, String name) {
        Objects.requireNonNull(path, name + " must not be null");
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            throw new IllegalArgumentException(name + " does not exist: " + normalized);
        }
        return normalized;
    }

    private static Map<Path, String> normalizeMounts(Map<Path, String> mounts) {
        Map<Path, String> normalized = new java.util.LinkedHashMap<>();
        if (mounts != null) {
            mounts.forEach((host, container) -> {
                Path normalizedHost = existingPath(host, "mount host");
                if (container == null || !Path.of(container).isAbsolute()) {
                    throw new IllegalArgumentException("Mount target must be absolute: " + container);
                }
                normalized.put(normalizedHost, container);
            });
        }
        return Map.copyOf(normalized);
    }
}
