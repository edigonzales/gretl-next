package ch.so.agi.gretl.test.offline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record OfflineArtifactManifest(String gretlVersion, String gradleVersion, Instant generatedAt,
        List<OfflineArtifact> artifacts, List<OfflinePluginMarker> pluginMarkers,
        List<DuckDbExtensionArtifact> duckDbExtensions, String generatorVersion) {
    public OfflineArtifactManifest {
        gretlVersion = Objects.requireNonNull(gretlVersion, "gretlVersion must not be null");
        gradleVersion = Objects.requireNonNull(gradleVersion, "gradleVersion must not be null");
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        artifacts = List.copyOf(Objects.requireNonNull(artifacts, "artifacts must not be null"));
        pluginMarkers = List.copyOf(Objects.requireNonNull(pluginMarkers, "pluginMarkers must not be null"));
        duckDbExtensions = List.copyOf(Objects.requireNonNull(duckDbExtensions, "duckDbExtensions must not be null"));
        generatorVersion = Objects.requireNonNull(generatorVersion, "generatorVersion must not be null");
    }

    public static OfflineArtifactManifest read(Path path) {
        try {
            String json = Files.readString(path);
            String gretlVersion = value(json, "gretlVersion");
            String gradleVersion = value(json, "gradleVersion");
            String generatedAt = value(json, "generatedAt");
            String generatorVersion = value(json, "generatorVersion");
            List<OfflineArtifact> artifacts = new java.util.ArrayList<>();
            for (String object : objects(array(json, "artifacts"))) {
                if (!object.contains("relativePath")) continue;
                artifacts.add(new OfflineArtifact(value(object, "group"), value(object, "module"),
                        value(object, "version"), value(object, "classifier"), value(object, "extension"),
                        value(object, "relativePath"), value(object, "sha256"),
                        Long.parseLong(value(object, "size")), ArtifactRole.valueOf(value(object, "role"))));
            }
            List<OfflinePluginMarker> markers = new java.util.ArrayList<>();
            for (String object : objects(array(json, "pluginMarkers"))) {
                if (object.contains("id")) {
                    markers.add(new OfflinePluginMarker(value(object, "id"), value(object, "group"),
                            value(object, "module"), value(object, "version")));
                }
            }
            return new OfflineArtifactManifest(gretlVersion, gradleVersion, Instant.parse(generatedAt), artifacts,
                    markers, List.of(), generatorVersion);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read offline artifact manifest " + path, e);
        }
    }

    public void validate() {
        if (gretlVersion.isBlank() || gradleVersion.isBlank() || generatorVersion.isBlank()) {
            throw new IllegalArgumentException("Offline artifact manifest has a blank version or generator.");
        }
        Set<String> coordinates = new HashSet<>();
        Set<String> paths = new HashSet<>();
        for (OfflineArtifact artifact : artifacts) {
            String identity = artifact.coordinate() + ":" + artifact.extension();
            if (artifact.group().isBlank()) identity = artifact.relativePath();
            if (!coordinates.add(identity)) {
                throw new IllegalArgumentException("Duplicate offline artifact coordinate: " + identity);
            }
            if (!paths.add(artifact.relativePath()) || artifact.relativePath().startsWith("/")
                    || artifact.relativePath().contains("..")) {
                throw new IllegalArgumentException("Invalid or duplicate offline artifact path: " + artifact.relativePath());
            }
            if (artifact.size() <= 0 || !artifact.sha256().matches("[0-9a-fA-F]{64}")) {
                throw new IllegalArgumentException("Invalid size or SHA-256 for " + artifact.coordinate());
            }
            if (artifact.relativePath().endsWith("-sources.jar") || artifact.relativePath().endsWith("-javadoc.jar")) {
                throw new IllegalArgumentException("Source and Javadoc artifacts are not allowed: " + artifact.relativePath());
            }
            if (isDynamic(artifact.version())) {
                throw new IllegalArgumentException("Dynamic version in offline artifact manifest: " + artifact.version());
            }
        }
        for (DuckDbExtensionArtifact extension : duckDbExtensions) {
            if (extension.size() <= 0 || !extension.sha256().matches("[0-9a-fA-F]{64}")) {
                throw new IllegalArgumentException("Invalid DuckDB extension manifest entry: " + extension.name());
            }
        }
    }

    public Optional<OfflineArtifact> find(String group, String module, String version) {
        return artifacts.stream().filter(artifact -> artifact.group().equals(group)
                && artifact.module().equals(module) && artifact.version().equals(version)).findFirst();
    }

    private static boolean isDynamic(String version) {
        return version.contains("+") || version.contains("[") || version.contains("]")
                || version.equals("latest.release") || version.equals("latest.integration");
    }

    private static String value(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|(-?\\d+))").matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Manifest is missing field " + key);
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private static String array(String json, String key) {
        int keyPosition = json.indexOf('"' + key + '"');
        if (keyPosition < 0) throw new IllegalArgumentException("Manifest is missing array " + key);
        int start = json.indexOf('[', keyPosition);
        if (start < 0) throw new IllegalArgumentException("Manifest is missing array " + key);
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            char character = json.charAt(i);
            if (character == '[') depth++;
            if (character == ']' && --depth == 0) return json.substring(start + 1, i);
        }
        throw new IllegalArgumentException("Manifest has an unterminated array " + key);
    }

    private static List<String> objects(String array) {
        Matcher matcher = Pattern.compile("\\{([^{}]*)\\}", Pattern.DOTALL).matcher(array);
        List<String> objects = new java.util.ArrayList<>();
        while (matcher.find()) objects.add(matcher.group(1));
        return objects;
    }
}
