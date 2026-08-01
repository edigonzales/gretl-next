package ch.so.agi.gretl.test.job;

import java.util.Locale;

public enum TestJobExecutionTarget {
    PLUGIN_CLASSPATH,
    PUBLISHED_ARTIFACT,
    RUNTIME_IMAGE_ONE_SHOT,
    RUNTIME_IMAGE_SERVICE;

    public static TestJobExecutionTarget fromYaml(String value) {
        if (value == null) {
            throw new IllegalArgumentException("execution target must not be null");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "pluginclasspath" -> PLUGIN_CLASSPATH;
            case "publishedartifact" -> PUBLISHED_ARTIFACT;
            case "runtimeimageoneshot" -> RUNTIME_IMAGE_ONE_SHOT;
            case "runtimeimageservice" -> RUNTIME_IMAGE_SERVICE;
            default -> throw new IllegalArgumentException("Unknown execution target: " + value);
        };
    }

    public String yamlName() {
        return switch (this) {
            case PLUGIN_CLASSPATH -> "pluginClasspath";
            case PUBLISHED_ARTIFACT -> "publishedArtifact";
            case RUNTIME_IMAGE_ONE_SHOT -> "runtimeImageOneShot";
            case RUNTIME_IMAGE_SERVICE -> "runtimeImageService";
        };
    }
}
