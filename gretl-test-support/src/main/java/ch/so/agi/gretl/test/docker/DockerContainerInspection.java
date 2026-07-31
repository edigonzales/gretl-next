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

    public void assertStrictOffline(RuntimeImageDescriptor descriptor) {
        if (!"none".equals(networkMode)) {
            throw new AssertionError("Expected Docker NetworkMode 'none' but was '" + networkMode
                    + "' for container " + id + ".");
        }
        if (!descriptor.imageId().equals(imageId)) {
            throw new AssertionError("Expected image " + descriptor.imageId() + " but container was created from "
                    + imageId + ".");
        }
        if (!readOnlyRootFilesystem) {
            throw new AssertionError("Expected a read-only root filesystem for container " + id + ".");
        }
        if (user == null || user.isBlank() || "0".equals(user) || "root".equalsIgnoreCase(user)) {
            throw new AssertionError("Expected a non-root container user but was '" + user + "'.");
        }
        DockerMountInspection project = mounts.get("/work/project");
        if (project == null || !project.readWrite()) {
            throw new AssertionError("Expected /work/project as a read-write bind mount; mounts=" + mounts);
        }
        DockerMountInspection gradleHome = mounts.get("/work/gradle-home");
        if (gradleHome == null || !"tmpfs".equalsIgnoreCase(gradleHome.type())) {
            throw new AssertionError("Expected a fresh tmpfs Gradle home; mounts=" + mounts);
        }
        for (DockerMountInspection mount : mounts.values()) {
            String source = mount.source() == null ? "" : mount.source().toLowerCase();
            String destination = mount.destination().toLowerCase();
            if (source.contains(".gradle") || source.contains(".m2") || source.contains("gretl-modular")
                    || destination.contains("/.gradle") || destination.contains("/.m2")) {
                if (!"/work/gradle-home".equals(destination)) {
                    throw new AssertionError("Forbidden host cache or checkout mount: " + mount);
                }
            }
        }
        if (!"/work/gradle-home".equals(environment.get("GRADLE_USER_HOME"))) {
            throw new AssertionError("GRADLE_USER_HOME is not the fresh test mount: " + environment);
        }
        if (!"true".equalsIgnoreCase(environment.get("GRETL_IMAGE_OFFLINE"))) {
            throw new AssertionError("GRETL_IMAGE_OFFLINE is not enabled: " + environment);
        }
    }
}
