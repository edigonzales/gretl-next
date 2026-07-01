package ch.so.agi.gretl.lsp.metadata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class MetadataLoaderTest {

    private final MetadataLoader loader = new MetadataLoader();

    @Test
    @DisplayName("loads valid metadata from test resource")
    void loadsValidMetadataFromResource() throws IOException {
        GretlMetadata metadata = loadTestMetadata();

        assertEquals("1.0.0", metadata.schemaVersion());
        assertEquals("test", metadata.gretlVersion());
        assertEquals(2, metadata.tasks().size());
    }

    @Test
    @DisplayName("finds SqlExecutor task in test metadata")
    void findsSqlExecutorTask() throws IOException {
        GretlMetadata metadata = loadTestMetadata();

        TaskMetadata sqlExecutor = metadata.findTask("SqlExecutor").orElseThrow();
        assertEquals("SqlExecutor", sqlExecutor.name());
        assertEquals("ch.so.agi.gretl.tasks.SqlExecutor", sqlExecutor.qualifiedClassName());
        assertEquals("database", sqlExecutor.category());
        assertEquals("stable", sqlExecutor.status());
    }

    @Test
    @DisplayName("SqlExecutor has required properties database and sqlFiles")
    void sqlExecutorHasRequiredProperties() throws IOException {
        GretlMetadata metadata = loadTestMetadata();
        TaskMetadata sqlExecutor = metadata.findTask("SqlExecutor").orElseThrow();

        assertEquals(2, sqlExecutor.requiredProperties().size());
        assertTrue(sqlExecutor.requiredProperties().stream()
                .anyMatch(p -> p.name().equals("database")));
        assertTrue(sqlExecutor.requiredProperties().stream()
                .anyMatch(p -> p.name().equals("sqlFiles")));
    }

    @Test
    @DisplayName("sqlParameters has sqlParameterProvider=true")
    void sqlParametersProviderTrue() throws IOException {
        GretlMetadata metadata = loadTestMetadata();
        TaskMetadata sqlExecutor = metadata.findTask("SqlExecutor").orElseThrow();

        PropertyMetadata sqlParams = sqlExecutor.findProperty("sqlParameters").orElseThrow();
        assertTrue(sqlParams.sqlParameterProvider());
        assertFalse(sqlParams.required());
    }

    @Test
    @DisplayName("sqlFiles has file metadata with .sql extension")
    void sqlFilesHasFileMetadata() throws IOException {
        GretlMetadata metadata = loadTestMetadata();
        TaskMetadata sqlExecutor = metadata.findTask("SqlExecutor").orElseThrow();

        PropertyMetadata sqlFiles = sqlExecutor.findProperty("sqlFiles").orElseThrow();
        assertNotNull(sqlFiles.file());
        assertEquals("input", sqlFiles.file().role());
        assertTrue(sqlFiles.file().extensions().contains(".sql"));
        assertTrue(sqlFiles.file().multiple());
    }

    @Test
    @DisplayName("returns empty metadata for invalid schema version")
    void returnsEmptyForInvalidSchemaVersion() throws IOException {
        try (InputStream in = resource("invalid-version.json")) {
            GretlMetadata metadata = loader.load(in);
            assertTrue(metadata.tasks().isEmpty());
        }
    }

    @Test
    @DisplayName("throws IOException for broken JSON")
    void throwsExceptionForBrokenJson() throws IOException {
        try (InputStream in = resource("broken.json")) {
            assertThrows(IOException.class, () -> loader.load(in));
        }
    }

    @Test
    @DisplayName("loadDefault returns empty metadata when resource not found")
    void loadDefaultReturnsEmpty() {
        GretlMetadata metadata = loader.loadDefault();
        assertNotNull(metadata);
    }

    @Test
    @DisplayName("tasksSortedByName returns alphabetically sorted tasks")
    void tasksSortedByName() throws IOException {
        GretlMetadata metadata = loadTestMetadata();
        var sorted = metadata.tasksSortedByName();

        assertEquals(2, sorted.size());
        assertTrue(sorted.get(0).name().compareTo(sorted.get(1).name()) < 0);
    }

    @Test
    @DisplayName("database property has two accepted forms and is required")
    void databasePropertyForms() throws IOException {
        GretlMetadata metadata = loadTestMetadata();
        TaskMetadata sqlExecutor = metadata.findTask("SqlExecutor").orElseThrow();

        PropertyMetadata database = sqlExecutor.findProperty("database").orElseThrow();
        assertTrue(database.required());
        assertEquals(2, database.acceptedForms().size());
        assertTrue(database.acceptedForms().stream()
                .anyMatch(f -> "method-call".equals(f.style()) && !f.legacy()));
        assertTrue(database.acceptedForms().stream()
                .anyMatch(f -> "assignment".equals(f.style()) && f.legacy()));
    }

    private GretlMetadata loadTestMetadata() throws IOException {
        try (InputStream in = resource("gretl-lsp-metadata.json")) {
            return loader.load(in);
        }
    }

    private InputStream resource(String name) {
        String resourcePath = "ch/so/agi/gretl/lsp/metadata/" + name;
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            fail("Test resource not found: " + resourcePath);
        }
        return in;
    }
}
