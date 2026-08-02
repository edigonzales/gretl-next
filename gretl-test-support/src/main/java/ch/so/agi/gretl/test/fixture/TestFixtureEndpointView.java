package ch.so.agi.gretl.test.fixture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record TestFixtureEndpointView(Map<String, TestFixtureValue> values,
                                      Optional<String> dockerNetwork) {
    public TestFixtureEndpointView {
        Objects.requireNonNull(values, "values must not be null");
        Map<String, TestFixtureValue> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("fixture value name must not be blank");
            copy.put(key, Objects.requireNonNull(value, "fixture value must not be null"));
        });
        values = Map.copyOf(copy);
        dockerNetwork = dockerNetwork == null ? Optional.empty() : dockerNetwork;
        dockerNetwork.ifPresent(value -> {
            if (value.isBlank()) throw new IllegalArgumentException("dockerNetwork must not be blank");
        });
    }

    public TestFixtureValue require(String name) {
        TestFixtureValue value = values.get(name);
        if (value == null) throw new IllegalArgumentException("Fixture endpoint has no value '" + name + "'");
        return value;
    }

    public Set<String> secretValues() {
        return values.values().stream()
                .filter(value -> value.sensitivity() == TestFixtureValueSensitivity.SECRET)
                .map(TestFixtureValue::value)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Map<String, String> publicValues() {
        return values.entrySet().stream()
                .filter(entry -> entry.getValue().sensitivity() == TestFixtureValueSensitivity.PUBLIC)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().value()));
    }
}
