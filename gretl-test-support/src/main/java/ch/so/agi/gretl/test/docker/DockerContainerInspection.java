package ch.so.agi.gretl.test.docker;

import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;

import java.util.Map;
import java.util.Objects;

public record DockerContainerInspection(
        String id,
        String imageId,
        String networkMode,
        String user,
        boolean readOnlyRootFilesystem,
        Map<String, DockerMountInspection> mounts,
        Map<String, String> environment) {

    public DockerContainerInspection {
        id = Objects.requireNonNull(id, "id must not be null");
        imageId = Objects.requireNonNull(imageId, "imageId must not be null");
        networkMode = Objects.requireNonNull(networkMode, "networkMode must not be null");
        mounts = Map.copyOf(mounts == null ? Map.of() : mounts);
        environment = Map.copyOf(environment == null ? Map.of() : environment);
    }

    public void assertRuntimeImage(RuntimeImageDescriptor descriptor) {
        if (!descriptor.imageId().equals(imageId)) {
            throw new AssertionError("Expected image " + descriptor.imageId() + " but container was created from "
                    + imageId + ".");
        }
        if (user == null || user.isBlank() || "0".equals(user) || "root".equalsIgnoreCase(user)) {
            throw new AssertionError("Expected a non-root container user but was '" + user + "'.");
        }
        DockerMountInspection project = mounts.get("/home/gradle/project");
        if (project == null || !project.readWrite()) {
            throw new AssertionError("Expected /home/gradle/project as a read-write bind mount; mounts=" + mounts);
        }
        DockerMountInspection gradleHome = mounts.get("/home/gradle/.gradle");
        if (gradleHome == null || !gradleHome.readWrite()) {
            throw new AssertionError("Expected a writable service or fresh Gradle home; mounts=" + mounts);
        }
        for (DockerMountInspection mount : mounts.values()) {
            String source = mount.source() == null ? "" : mount.source().toLowerCase();
            String destination = mount.destination().toLowerCase();
            if (source.contains(".gradle") || source.contains(".m2")
                    || destination.contains("/.gradle") || destination.contains("/.m2")) {
                if (!"/home/gradle/.gradle".equals(destination)) {
                    throw new AssertionError("Forbidden host cache or checkout mount: " + mount);
                }
            }
        }
        if (!"/home/gradle/.gradle".equals(environment.get("GRADLE_USER_HOME"))) {
            throw new AssertionError("GRADLE_USER_HOME is not the container Gradle home: " + environment);
        }
    }
}
