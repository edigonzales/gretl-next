package ch.so.agi.gretl.internal.interlis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonValidatorImplTest {

    @TempDir
    Path tempDir;

    @Test
    void wrapsSingleJsonObjectAndAddsValidatorAttributes() throws Exception {
        Path source = tempDir.resolve("object.json");
        Files.writeString(source, """
                {
                  "@type": "Test2.TopicA.ClassA",
                  "name": "alpha"
                }
                """, StandardCharsets.UTF_8);

        Path preprocessed = new JsonValidatorImpl().preprocessJsonFile(source);

        try {
            String json = Files.readString(preprocessed, StandardCharsets.UTF_8);
            assertTrue(json.trim().startsWith("["));
            assertTrue(json.contains("\"@topic\" : \"Test2.TopicA\""));
            assertTrue(json.contains("\"@id\" : \"o1\""));
            assertTrue(json.contains("\"@bid\" : \"b1\""));
        } finally {
            deleteTree(preprocessed.getParent());
        }
    }

    @Test
    void preprocessesJsonArraysWithoutReplacingExistingIds() throws Exception {
        Path source = tempDir.resolve("array.json");
        Files.writeString(source, """
                [
                  { "@type": "Test2.TopicA.ClassA" },
                  { "@type": "Test2.TopicA.ClassA", "@id": "existing" }
                ]
                """, StandardCharsets.UTF_8);

        Path preprocessed = new JsonValidatorImpl().preprocessJsonFile(source);

        try {
            String json = Files.readString(preprocessed, StandardCharsets.UTF_8);
            assertTrue(json.contains("\"@id\" : \"o1\""));
            assertTrue(json.contains("\"@id\" : \"existing\""));
            assertTrue(json.contains("\"@bid\" : \"b1\""));
        } finally {
            deleteTree(preprocessed.getParent());
        }
    }

    @Test
    void rejectsObjectsWithoutTypeAttribute() throws Exception {
        Path source = tempDir.resolve("missing-type.json");
        Files.writeString(source, "{ \"name\": \"alpha\" }", StandardCharsets.UTF_8);
        long tempDirCountBefore = jsonValidatorTempDirCount();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new JsonValidatorImpl().preprocessJsonFile(source));

        assertTrue(exception.getMessage().contains("@type"));
        assertFalse(jsonValidatorTempDirCount() > tempDirCountBefore);
    }

    private long jsonValidatorTempDirCount() throws Exception {
        Path systemTemp = Path.of(System.getProperty("java.io.tmpdir"));
        try (var stream = Files.list(systemTemp)) {
            return stream
                    .filter(path -> path.getFileName().toString().startsWith("jsonvalidator_"))
                    .count();
        }
    }

    private void deleteTree(Path root) throws Exception {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }
    }
}
