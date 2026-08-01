package ch.so.agi.gretl.test.job;

import java.util.Locale;

public enum TestJobExecutionRequirement {
    REQUIRED,
    OPTIONAL,
    NOT_APPLICABLE;

    public static TestJobExecutionRequirement fromYaml(String value) {
        if (value == null) {
            throw new IllegalArgumentException("execution requirement must not be null");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "required" -> REQUIRED;
            case "optional" -> OPTIONAL;
            case "not-applicable", "not_applicable" -> NOT_APPLICABLE;
            default -> throw new IllegalArgumentException("Unknown execution requirement: " + value);
        };
    }
}
