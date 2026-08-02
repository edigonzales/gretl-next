package ch.so.agi.gretl.test.fixture;

import java.util.Locale;

public enum TestJobBindingTarget {
    GRADLE_PROPERTY,
    ENVIRONMENT_VARIABLE;

    public static TestJobBindingTarget fromYaml(String value) {
        if (value == null) throw new IllegalArgumentException("binding target must not be null");
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "gradle-property", "gradle_property" -> GRADLE_PROPERTY;
            case "environment-variable", "environment_variable", "env" -> ENVIRONMENT_VARIABLE;
            default -> throw new IllegalArgumentException("Unknown fixture binding target: " + value);
        };
    }
}
