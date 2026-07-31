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
    void acceptsStrictOfflineInspection() throws Exception {
        RuntimeImageDescriptor descriptor = descriptor();
        DockerContainerInspection inspection = new DockerContainerInspection("container", descriptor.imageId(),
                "none", "1001:0", true,
                Map.of("/work/project", new DockerMountInspection("/tmp/project", "/work/project", "bind", true),
                        "/work/gradle-home", new DockerMountInspection("tmpfs", "/work/gradle-home", "tmpfs", true)),
                Map.of("GRADLE_USER_HOME", "/work/gradle-home", "GRETL_IMAGE_OFFLINE", "true"));

        assertDoesNotThrow(() -> inspection.assertStrictOffline(descriptor));
    }

    @Test
    void rejectsNetworkedContainer() throws Exception {
        RuntimeImageDescriptor descriptor = descriptor();
        DockerContainerInspection inspection = new DockerContainerInspection("container", descriptor.imageId(),
                "bridge", "1001:0", true, Map.of(),
                Map.of("GRADLE_USER_HOME", "/work/gradle-home", "GRETL_IMAGE_OFFLINE", "true"));

        assertThrows(AssertionError.class, () -> inspection.assertStrictOffline(descriptor));
    }

    private RuntimeImageDescriptor descriptor() throws Exception {
        Path id = Files.writeString(temporaryDirectory.resolve("image-id.txt"), "sha256:" + "a".repeat(64));
        return new RuntimeImageDescriptor(id.toString().trim(), "test:image", "5.0.0", "7.6.4", "17", id);
    }
}
