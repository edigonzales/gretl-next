package ch.so.agi.gretl.doclet.lsp;

import ch.so.agi.gretl.doclet.model.DslMethodDescriptor;
import ch.so.agi.gretl.doclet.model.ParameterDescriptor;
import ch.so.agi.gretl.doclet.model.TaskDescriptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LspMetadataWriterTest {

    private final LspMetadataWriter writer = new LspMetadataWriter();

    @Test
    void writesValidJson() throws Exception {
        LspMetadataDocument doc = minimalDocument();
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("lsp-metadata-", ".json");
        try {
            writer.write(doc, tempFile);
            String json = java.nio.file.Files.readString(tempFile);

            assertTrue(json.contains("\"schemaVersion\""));
            assertTrue(json.contains("\"1.0.0\""));
            assertTrue(json.contains("\"tasks\""));
            assertTrue(json.startsWith("{"));
            assertTrue(json.trim().endsWith("}"));
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void omitsNullValues() throws Exception {
        LspTaskMetadata task = new LspTaskMetadata(
                "TestTask", "pkg.TestTask", "TestTask",
                "database", "stable", "desc", null,
                List.of(), List.of());
        LspMetadataDocument doc = documentWithTasks(List.of(task));

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("lsp-metadata-", ".json");
        try {
            writer.write(doc, tempFile);
            String json = java.nio.file.Files.readString(tempFile);

            assertFalse(json.contains("longDescription"));
            assertFalse(json.contains("\"file\""));
            assertFalse(json.contains("\"migration\""));
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void preservesInsertionOrder() throws Exception {
        LspTaskMetadata taskB = new LspTaskMetadata(
                "BbbTask", "pkg.BbbTask", "BbbTask",
                "database", "stable", "desc", null,
                List.of(), List.of());
        LspTaskMetadata taskA = new LspTaskMetadata(
                "AaaTask", "pkg.AaaTask", "AaaTask",
                "database", "stable", "desc", null,
                List.of(), List.of());
        LspMetadataDocument doc = documentWithTasks(List.of(taskB, taskA));

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("lsp-metadata-", ".json");
        try {
            writer.write(doc, tempFile);
            String json = java.nio.file.Files.readString(tempFile);

            int posA = json.indexOf("\"AaaTask\"");
            int posB = json.indexOf("\"BbbTask\"");
            assertTrue(posA > posB, "Tasks should preserve insertion order (BbbTask before AaaTask)");
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void escapesSpecialJsonCharacters() throws Exception {
        LspTaskMetadata task = new LspTaskMetadata(
                "Test", "pkg.Test", "Test",
                "database", "stable", "desc with \"quotes\" and\nnewline", null,
                List.of(), List.of());
        LspMetadataDocument doc = documentWithTasks(List.of(task));

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("lsp-metadata-", ".json");
        try {
            writer.write(doc, tempFile);
            String json = java.nio.file.Files.readString(tempFile);

            assertTrue(json.contains("\\\""));
            assertTrue(json.contains("\\n"));
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void writesFileMetadataWhenPresent() throws Exception {
        LspFileMetadata file = new LspFileMetadata("input", List.of(".sql"), true, true);
        LspPropertyMetadata prop = new LspPropertyMetadata(
                "sqlFiles", "sqlFiles", "dsl-method-and-property",
                "FileCollection", "Property<FileCollection>",
                true, false, "SQL files to execute.",
                file,
                List.of(new LspAcceptedForm("method-call", "sqlFiles files('...')", "sqlFiles files('${1:file}')", 1, false)),
                null, false,
                new LspCompletionMetadata("sqlFiles", "Pflicht \u00b7 FileCollection", "0100_sqlFiles"));

        LspTaskMetadata task = new LspTaskMetadata(
                "SqlExecutor", "ch.so.agi.gretl.tasks.SqlExecutor", "SqlExecutor",
                "database", "stable", "Executes SQL.", null,
                List.of(), List.of(prop));
        LspMetadataDocument doc = documentWithTasks(List.of(task));

        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("lsp-metadata-", ".json");
        try {
            writer.write(doc, tempFile);
            String json = java.nio.file.Files.readString(tempFile);

            assertTrue(json.contains("\"file\""));
            assertTrue(json.contains("\"role\""));
            assertTrue(json.contains("\"input\""));
            assertTrue(json.contains("\".sql\""));
            assertTrue(json.contains("\"multiple\""));
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void escapeJsonHandlesNullAndControlChars() {
        assertEquals("", LspMetadataWriter.escapeJson(null));
        assertEquals("", LspMetadataWriter.escapeJson(""));
        assertEquals("abc", LspMetadataWriter.escapeJson("abc"));
        assertEquals("\\\"hello\\\"", LspMetadataWriter.escapeJson("\"hello\""));
        assertEquals("a\\\\b", LspMetadataWriter.escapeJson("a\\b"));
        assertEquals("hello\\nworld", LspMetadataWriter.escapeJson("hello\nworld"));
        assertEquals("a\\tb", LspMetadataWriter.escapeJson("a\tb"));
    }

    private static LspMetadataDocument minimalDocument() {
        return documentWithTasks(List.of());
    }

    private static LspMetadataDocument documentWithTasks(List<LspTaskMetadata> tasks) {
        return new LspMetadataDocument(
                "1.0.0",
                "2026-07-01T12:00:00Z",
                "5.0.0-SNAPSHOT",
                new LspMetadataSource("https://github.com/sogis/gretl", "gretl-doclet", null),
                tasks);
    }
}
