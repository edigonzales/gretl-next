package ch.so.agi.gretl.test.offline;

import java.util.Objects;

public record OfflineArtifact(String group, String module, String version, String classifier,
        String extension, String relativePath, String sha256, long size, ArtifactRole role) {
    public OfflineArtifact {
        group = Objects.requireNonNull(group, "group must not be null");
        module = Objects.requireNonNull(module, "module must not be null");
        version = Objects.requireNonNull(version, "version must not be null");
        classifier = classifier == null ? "" : classifier;
        extension = Objects.requireNonNull(extension, "extension must not be null");
        relativePath = Objects.requireNonNull(relativePath, "relativePath must not be null");
        sha256 = Objects.requireNonNull(sha256, "sha256 must not be null");
        role = Objects.requireNonNull(role, "role must not be null");
    }

    public String coordinate() {
        return group + ":" + module + ":" + version + (classifier.isBlank() ? "" : ":" + classifier);
    }
}
