package ch.so.agi.gretl.testkit;

import java.util.Locale;

public enum GretlTestExecutionMode {
    PLUGIN_CLASSPATH,
    PUBLISHED_ARTIFACT;

    public static GretlTestExecutionMode current() {
        String configured = System.getProperty(GretlTestSystemProperties.EXECUTION_MODE);
        if (configured == null || configured.isBlank()) {
            return PLUGIN_CLASSPATH;
        }

        String normalized = configured.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown value '" + configured + "' for system property '"
                            + GretlTestSystemProperties.EXECUTION_MODE + "'. Allowed values: "
                            + PLUGIN_CLASSPATH + ", " + PUBLISHED_ARTIFACT,
                    e);
        }
    }
}
