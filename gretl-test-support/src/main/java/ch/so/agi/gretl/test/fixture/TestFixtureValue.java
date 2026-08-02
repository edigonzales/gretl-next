package ch.so.agi.gretl.test.fixture;

import java.util.Objects;

public record TestFixtureValue(String value, TestFixtureValueSensitivity sensitivity) {
    public TestFixtureValue {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("fixture value must not be empty");
        Objects.requireNonNull(sensitivity, "sensitivity must not be null");
    }

    public static TestFixtureValue publicValue(String value) {
        return new TestFixtureValue(value, TestFixtureValueSensitivity.PUBLIC);
    }

    public static TestFixtureValue secretValue(String value) {
        return new TestFixtureValue(value, TestFixtureValueSensitivity.SECRET);
    }
}
