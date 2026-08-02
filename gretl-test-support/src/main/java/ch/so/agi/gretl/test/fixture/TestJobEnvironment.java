package ch.so.agi.gretl.test.fixture;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record TestJobEnvironment(
        Map<String, String> gradleProperties,
        Map<String, String> environmentVariables,
        Set<String> secretValues,
        Optional<String> dockerNetwork) {
    public TestJobEnvironment {
        gradleProperties = cleanMap(gradleProperties, "Gradle property");
        environmentVariables = cleanMap(environmentVariables, "Environment variable");
        secretValues = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(secretValues, "secretValues must not be null")));
        if (secretValues.stream().anyMatch(value -> value == null || value.isEmpty())) {
            throw new IllegalArgumentException("secret values must not be empty");
        }
        dockerNetwork = dockerNetwork == null ? Optional.empty() : dockerNetwork;
        dockerNetwork.ifPresent(value -> {
            if (value.isBlank()) throw new IllegalArgumentException("dockerNetwork must not be blank");
        });
    }

    public static TestJobEnvironment empty() {
        return new TestJobEnvironment(Map.of(), Map.of(), Set.of(), Optional.empty());
    }

    public TestJobEnvironment merge(TestJobEnvironment other) {
        Objects.requireNonNull(other, "other must not be null");
        Map<String, String> properties = mergeMaps(gradleProperties, other.gradleProperties, "Gradle property");
        Map<String, String> environment = mergeMaps(environmentVariables, other.environmentVariables,
                "Environment variable");
        Optional<String> network = mergeNetwork(dockerNetwork, other.dockerNetwork);
        Set<String> secrets = new LinkedHashSet<>(secretValues);
        secrets.addAll(other.secretValues);
        return new TestJobEnvironment(properties, environment, secrets, network);
    }

    @Override
    public String toString() {
        return "TestJobEnvironment[gradleProperties=" + redact(gradleProperties)
                + ", environmentVariables=" + redact(environmentVariables)
                + ", secretValues=<redacted>, dockerNetwork=" + dockerNetwork + "]";
    }

    private static Map<String, String> cleanMap(Map<String, String> values, String label) {
        Map<String, String> copy = new LinkedHashMap<>();
        Objects.requireNonNull(values, label + "s must not be null").forEach((key, value) -> {
            if (key == null || key.isBlank()) throw new IllegalArgumentException(label + " name must not be blank");
            if (value == null || value.isEmpty()) throw new IllegalArgumentException(label + " value must not be empty");
            copy.put(key, value);
        });
        return Map.copyOf(copy);
    }

    private static Map<String, String> mergeMaps(Map<String, String> first, Map<String, String> second, String label) {
        Map<String, String> merged = new LinkedHashMap<>(first);
        second.forEach((key, value) -> {
            String previous = merged.putIfAbsent(key, value);
            if (previous != null && !previous.equals(value)) {
                throw new IllegalArgumentException(label + " '" + key + "' has conflicting values");
            }
        });
        return merged;
    }

    private static Optional<String> mergeNetwork(Optional<String> first, Optional<String> second) {
        if (first.isPresent() && second.isPresent() && !first.get().equals(second.get())) {
            throw new IllegalArgumentException("Fixture environments require different Docker networks");
        }
        return first.isPresent() ? first : second;
    }

    private static Map<String, String> redact(Map<String, String> values) {
        return values.keySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(key -> key, key -> "<redacted>"));
    }
}
