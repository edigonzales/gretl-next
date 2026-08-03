package ch.so.agi.gretl.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStylingDefinitionTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsEmptyFile() throws Exception {
        Path file = Files.createFile(temporaryDirectory.resolve("empty.sql"));

        assertDoesNotThrow(() -> FileStylingDefinition.checkForUtf8(file.toFile()));
        assertDoesNotThrow(() -> FileStylingDefinition.checkForBOMInFile(file.toFile()));
    }

    @Test
    void validatesLargeUtf8StreamAcrossBufferBoundaries() throws Exception {
        Path file = temporaryDirectory.resolve("large.sql");
        String content = "a".repeat(8191) + "€" + "ö".repeat(1_000_000);
        Files.writeString(file, content, StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> FileStylingDefinition.checkForUtf8(file.toFile()));
        assertDoesNotThrow(() -> FileStylingDefinition.checkForBOMInFile(file.toFile()));
    }

    @Test
    void rejectsMalformedUtf8() throws Exception {
        Path file = temporaryDirectory.resolve("malformed.sql");
        Files.write(file, new byte[] {(byte) 0xC3, 0x28});

        GretlException exception = assertThrows(GretlException.class,
                () -> FileStylingDefinition.checkForUtf8(file.toFile()));

        assertEquals("Wrong encoding (not UTF-8) detected in File " + file.toAbsolutePath(), exception.getMessage());
    }

    @Test
    void rejectsUtf8Bom() throws Exception {
        Path file = temporaryDirectory.resolve("bom.sql");
        Files.write(file, new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'x'});

        GretlException exception = assertThrows(GretlException.class,
                () -> FileStylingDefinition.checkForBOMInFile(file.toFile()));

        assertEquals(GretlException.TYPE_FILE_WITH_BOM, exception.getType());
    }
}
