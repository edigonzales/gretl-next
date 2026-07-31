package ch.so.agi.gretl.test.docker;

public record DockerImageInspection(
        String imageId,
        String entrypoint,
        String workingDirectory,
        String user,
        String labels,
        String raw) {
}
