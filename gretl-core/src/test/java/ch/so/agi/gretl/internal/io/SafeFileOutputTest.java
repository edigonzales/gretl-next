package ch.so.agi.gretl.internal.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeFileOutputTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesOnlyDescendants() {
        assertEquals(temporaryDirectory.resolve("nested/file.txt").toAbsolutePath().normalize(),
                SafeFileOutput.resolveDescendant(temporaryDirectory, "nested/file.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> SafeFileOutput.resolveDescendant(temporaryDirectory, "../outside.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> SafeFileOutput.resolveDescendant(temporaryDirectory, temporaryDirectory.resolve("absolute.txt").toString()));
    }

    @Test
    void preservesExistingTargetAndRemovesTemporaryFileWhenWritingFails() throws Exception {
        Path target = temporaryDirectory.resolve("output.txt");
        Files.writeString(target, "previous", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> SafeFileOutput.writeAtomically(target, temporary -> {
            Files.writeString(temporary, "partial", StandardCharsets.UTF_8);
            throw new IOException("expected failure");
        }));

        assertEquals("previous", Files.readString(target, StandardCharsets.UTF_8));
        try (var files = Files.list(temporaryDirectory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void replacesTargetOnlyAfterSuccessfulWrite() throws Exception {
        Path target = temporaryDirectory.resolve("output.txt");
        Files.writeString(target, "previous", StandardCharsets.UTF_8);

        SafeFileOutput.writeAtomically(target,
                temporary -> Files.writeString(temporary, "complete", StandardCharsets.UTF_8));

        assertEquals("complete", Files.readString(target, StandardCharsets.UTF_8));
    }
}
