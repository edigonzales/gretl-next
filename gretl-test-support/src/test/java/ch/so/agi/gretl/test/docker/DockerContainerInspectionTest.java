package ch.so.agi.gretl.test.docker;

import ch.so.agi.gretl.test.runtime.RuntimeImageDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DockerContainerInspectionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsRuntimeImageInspectionOnDefaultNetwork() throws Exception {
        RuntimeImageDescriptor descriptor = descriptor();
        DockerContainerInspection inspection = new DockerContainerInspection("container", descriptor.imageId(),
                "bridge", "1001:0", false,
                Map.of("/home/gradle/project", new DockerMountInspection("/tmp/project", "/home/gradle/project", "bind", true),
                        "/home/gradle/.gradle", new DockerMountInspection("/tmp/gradle", "/home/gradle/.gradle", "bind", true)),
                Map.of("GRADLE_USER_HOME", "/home/gradle/.gradle"));

        assertDoesNotThrow(() -> inspection.assertRuntimeImage(descriptor));
    }

    @Test
    void acceptsNamedNetworkForRuntimeJobs() throws Exception {
        RuntimeImageDescriptor descriptor = descriptor();
        DockerContainerInspection inspection = new DockerContainerInspection("container", descriptor.imageId(),
                "gretl-postgis", "1001:0", false,
                Map.of("/home/gradle/project", new DockerMountInspection("/tmp/project", "/home/gradle/project", "bind", true),
                        "/home/gradle/.gradle", new DockerMountInspection("/tmp/gradle", "/home/gradle/.gradle", "bind", true)),
                Map.of("GRADLE_USER_HOME", "/home/gradle/.gradle"));

        assertDoesNotThrow(() -> inspection.assertRuntimeImage(descriptor));
    }

    private RuntimeImageDescriptor descriptor() throws Exception {
        Path id = Files.writeString(temporaryDirectory.resolve("image-id.txt"), "sha256:" + "a".repeat(64));
        return new RuntimeImageDescriptor(id.toString().trim(), "test:image", "5.0.0", "7.6.4", "17", id);
    }
}
