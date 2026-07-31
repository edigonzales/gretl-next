package ch.so.agi.gretl.test.execution;

import java.util.Locale;

public enum GretlExecutionMode {
    TESTKIT_CLASSPATH,
    PUBLISHED_ARTIFACT,
    RUNTIME_IMAGE;

    public static final String SYSTEM_PROPERTY = "gretl.test.executionMode";

    public static GretlExecutionMode current() {
        String configured = System.getProperty(SYSTEM_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return TESTKIT_CLASSPATH;
        }
        return parse(configured);
    }

    public static GretlExecutionMode parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "unknown value '<blank>' for property '" + SYSTEM_PROPERTY
                            + "'. Allowed values: TESTKIT_CLASSPATH, PUBLISHED_ARTIFACT, RUNTIME_IMAGE");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "unknown value '" + value + "' for property '" + SYSTEM_PROPERTY
                            + "'. Allowed values: TESTKIT_CLASSPATH, PUBLISHED_ARTIFACT, RUNTIME_IMAGE", e);
        }
    }

    public boolean isRuntimeImage() {
        return this == RUNTIME_IMAGE;
    }
}
