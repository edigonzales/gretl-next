package ch.so.agi.gretl.test.docker;

public record DockerMountInspection(String source, String destination, String type, boolean readWrite) {
}
