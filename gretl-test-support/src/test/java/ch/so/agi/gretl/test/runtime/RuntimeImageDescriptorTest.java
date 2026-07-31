package ch.so.agi.gretl.test.runtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeImageDescriptorTest {
    private static final String VALID_ID = "sha256:" + "a".repeat(64);

    @TempDir
    Path temp;

    @AfterEach
    void clearProperties() {
        System.clearProperty(RuntimeImageDescriptor.ID_FILE);
        System.clearProperty(RuntimeImageDescriptor.TAG);
        System.clearProperty(RuntimeImageDescriptor.VERSION);
        System.clearProperty(RuntimeImageDescriptor.GRADLE_VERSION);
        System.clearProperty(RuntimeImageDescriptor.JAVA_VERSION);
    }

    @Test
    void readsValidImageIdFile() throws IOException {
        Path idFile = Files.writeString(temp.resolve("image-id.txt"), VALID_ID + "\n");
        setProperties(idFile);

        RuntimeImageDescriptor descriptor = RuntimeImageDescriptor.fromSystemProperties();

        assertEquals(VALID_ID, descriptor.imageId());
        assertEquals(idFile.toAbsolutePath().normalize(), descriptor.imageIdFile());
        descriptor.verify();
    }

    @Test
    void rejectsMissingImageIdFile() {
        setProperties(temp.resolve("missing-image-id.txt"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                RuntimeImageDescriptor::fromSystemProperties);

        assertTrue(error.getMessage().contains("image ID file"));
    }

    @Test
    void rejectsEmptyImageId() throws IOException {
        Path idFile = Files.writeString(temp.resolve("image-id.txt"), "\n");
        setProperties(idFile);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> RuntimeImageDescriptor.fromSystemProperties().verify());

        assertTrue(error.getMessage().contains("immutable sha256"));
    }

    @Test
    void rejectsNonSha256Reference() throws IOException {
        Path idFile = Files.writeString(temp.resolve("image-id.txt"), "latest\n");
        setProperties(idFile);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> RuntimeImageDescriptor.fromSystemProperties().verify());

        assertTrue(error.getMessage().contains("immutable sha256"));
    }

    private void setProperties(Path idFile) {
        System.setProperty(RuntimeImageDescriptor.ID_FILE, idFile.toString());
        System.setProperty(RuntimeImageDescriptor.TAG, "gretl-next-e2e:test");
        System.setProperty(RuntimeImageDescriptor.VERSION, "5.0.0-SNAPSHOT");
        System.setProperty(RuntimeImageDescriptor.GRADLE_VERSION, "7.6.4");
        System.setProperty(RuntimeImageDescriptor.JAVA_VERSION, "17");
    }
}
