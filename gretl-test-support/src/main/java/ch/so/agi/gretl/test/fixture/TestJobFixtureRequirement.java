package ch.so.agi.gretl.test.fixture;

import java.util.List;
import java.util.Objects;

public record TestJobFixtureRequirement(
        String id,
        TestFixtureType type,
        List<TestJobFixtureBinding> bindings) {
    public TestJobFixtureRequirement {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("fixture id must not be blank");
        Objects.requireNonNull(type, "type must not be null");
        bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings must not be null"));
    }
}
