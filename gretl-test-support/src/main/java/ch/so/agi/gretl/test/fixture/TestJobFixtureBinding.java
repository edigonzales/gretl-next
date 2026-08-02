package ch.so.agi.gretl.test.fixture;

import java.util.Objects;

public record TestJobFixtureBinding(String source, TestJobBindingTarget target, String name) {
    public TestJobFixtureBinding {
        requireNonBlank(source, "source");
        Objects.requireNonNull(target, "target must not be null");
        requireNonBlank(name, "name");
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
