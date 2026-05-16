package ch.so.agi.gretl.internal.sql;

import ch.so.agi.gretl.util.EmptyFileException;
import ch.so.agi.gretl.util.GretlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlExecutionEngineTest {

    @TempDir
    Path tempDir;

    @Test
    void requestRejectsEmptySqlFileList() {
        DatabaseSpec database = new DatabaseSpec("jdbc:sqlite:" + tempDir.resolve("test.db"), null, null);

        assertThrows(IllegalArgumentException.class,
                () -> new SqlExecutionRequest("sql", database, List.of(), List.of(Map.of())));
    }

    @Test
    void rejectsWrongFileExtension() throws Exception {
        Path file = tempDir.resolve("query.txt");
        Files.writeString(file, "select 1;", StandardCharsets.UTF_8);

        GretlException exception = assertThrows(GretlException.class,
                () -> new SqlExecutionEngine().execute(request(file)));

        assertEquals(GretlException.TYPE_WRONG_EXTENSION, exception.getType());
    }

    @Test
    void rejectsMissingSqlFile() {
        Path file = tempDir.resolve("missing.sql");

        GretlException exception = assertThrows(GretlException.class,
                () -> new SqlExecutionEngine().execute(request(file)));

        assertEquals(GretlException.TYPE_FILE_NOT_READABLE, exception.getType());
    }

    @Test
    void rejectsEmptySqlFile() throws Exception {
        Path file = tempDir.resolve("empty.sql");
        Files.writeString(file, "", StandardCharsets.UTF_8);

        assertThrows(EmptyFileException.class,
                () -> new SqlExecutionEngine().execute(request(file)));
    }

    @Test
    void rejectsSqlFileWithBom() throws Exception {
        Path file = resourcePath("original-gretl/sql/query_with_bom.sql");

        GretlException exception = assertThrows(GretlException.class,
                () -> new SqlExecutionEngine().execute(request(file)));

        assertEquals(GretlException.TYPE_FILE_WITH_BOM, exception.getType());
    }

    @Test
    void rejectsNonUtf8SqlFile() throws Exception {
        Path file = tempDir.resolve("latin1.sql");
        Files.write(file, new byte[] {(byte) 0xC3, 0x28});

        GretlException exception = assertThrows(GretlException.class,
                () -> new SqlExecutionEngine().execute(request(file)));

        assertEquals("Wrong encoding (not UTF-8) detected in File " + file.toAbsolutePath(), exception.getMessage());
    }

    @Test
    void propagatesInvalidSql() throws Exception {
        Path file = tempDir.resolve("invalid.sql");
        Files.writeString(file, "select missing_column from missing_table;", StandardCharsets.UTF_8);

        assertThrows(SQLException.class, () -> new SqlExecutionEngine().execute(request(file)));
    }

    private SqlExecutionRequest request(Path... files) throws Exception {
        Path database = tempDir.resolve("sql-executor.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             var statement = connection.createStatement()) {
            statement.execute("create table if not exists colors (id integer primary key, name text)");
        }
        return new SqlExecutionRequest(
                "sql",
                new DatabaseSpec("jdbc:sqlite:" + database, null, null),
                List.of(files),
                List.of(Map.of())
        );
    }

    private Path resourcePath(String resourcePath) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing test resource: " + resourcePath);
        }
        return Path.of(resource.toURI());
    }
}
