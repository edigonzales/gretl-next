package ch.so.agi.gretl.combined.assertions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class GzipAssertions {
    public static byte[] decompress(Path gzip) {
        try (InputStream input = new GZIPInputStream(Files.newInputStream(gzip))) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("Cannot decompress GZIP file " + gzip, e);
        }
    }

    public static void assertDecompressesToFile(Path gzip, Path expectedFile) {
        try {
            assertArrayEquals(Files.readAllBytes(expectedFile), decompress(gzip));
        } catch (IOException e) {
            throw new AssertionError("Cannot read expected GZIP payload " + expectedFile, e);
        }
    }

    public static void assertHeaderIsValid(Path gzip) {
        try {
            byte[] header = Files.readAllBytes(gzip);
            assertTrue(header.length >= 10, "GZIP file is too short");
            assertEquals(0x1f, header[0] & 0xff);
            assertEquals(0x8b, header[1] & 0xff);
            assertEquals(8, header[2] & 0xff, "GZIP must use the deflate method");
        } catch (IOException e) {
            throw new AssertionError("Cannot inspect GZIP file " + gzip, e);
        }
    }

    private GzipAssertions() {
    }
}
