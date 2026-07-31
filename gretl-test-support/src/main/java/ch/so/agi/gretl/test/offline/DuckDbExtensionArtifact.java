package ch.so.agi.gretl.test.offline;

public record DuckDbExtensionArtifact(String name, String duckDbVersion, String platform,
        String relativePath, String sha256, long size) {
}
