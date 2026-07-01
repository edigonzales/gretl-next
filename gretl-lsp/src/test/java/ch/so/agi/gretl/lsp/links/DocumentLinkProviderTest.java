package ch.so.agi.gretl.lsp.links;

import ch.so.agi.gretl.lsp.metadata.FileMetadata;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.DslCallStyle;
import ch.so.agi.gretl.lsp.model.GretlArgument;
import ch.so.agi.gretl.lsp.model.GretlDependency;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import ch.so.agi.gretl.lsp.model.MethodCallExpression;
import ch.so.agi.gretl.lsp.model.StringLiteralExpression;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DocumentLinkProviderTest {

    private DocumentLinkProvider provider;
    private GretlMetadata metadata;

    @BeforeEach
    void setUp() {
        FileMetadata fileMeta = new FileMetadata("input", List.of(".sql"), true, true);
        PropertyMetadata sqlFilesProp = new PropertyMetadata("sqlFiles", "sqlFiles",
                "dsl-method-and-property", "FileCollection", "Property<FileCollection>",
                true, false, "SQL-Dateien.", fileMeta,
                List.of(), null, false, null);

        PropertyMetadata dbProp = new PropertyMetadata("database", "database",
                "dsl-method-and-property", "Connector", "Property<String>",
                true, false, "Datenbank.", null,
                List.of(), null, false, null);

        TaskMetadata sqlTask = new TaskMetadata("SqlExecutor",
                "ch.so.agi.gretl.tasks.SqlExecutor", "SqlExecutor",
                "database", "stable", "Fuhrt SQL aus.",
                null, List.of(), List.of(dbProp, sqlFilesProp));

        metadata = new GretlMetadata("1.0.0", null, "test", null, List.of(sqlTask));
        provider = new DocumentLinkProvider(metadata);
    }

    @Test
    @DisplayName("creates link for sqlFiles with files() call")
    void createsLinkForSqlFiles(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("x.sql"));
        createSqlFile(tempDir, "data.sql");

        Range filePathRange = new Range(new Position(1, 20), new Position(1, 29));
        GretlArgument fileArg = new GretlArgument(
                new StringLiteralExpression("data.sql", filePathRange, "'data.sql'"),
                filePathRange, Optional.empty());

        GretlArgument filesArg = new GretlArgument(
                new MethodCallExpression("files", List.of(fileArg),
                        new Range(new Position(1, 14), new Position(1, 30)),
                        "files('data.sql')"),
                new Range(new Position(1, 14), new Position(1, 30)), Optional.empty());

        GretlDslCall sqlCall = new GretlDslCall("sqlFiles", DslCallStyle.METHOD_CALL,
                new Range(new Position(1, 4), new Position(1, 14)),
                new Range(new Position(1, 4), new Position(1, 30)),
                List.of(filesArg), "sqlFiles files('data.sql')");

        GretlTaskBlock task = createTask("x", "SqlExecutor", 0, 0, 2, 1,
                List.of(sqlCall));

        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);

        List<DocumentLink> links = provider.links(script, tempDir);
        assertEquals(1, links.size());
        DocumentLink link = links.get(0);
        assertNotNull(link.getTarget());
        assertTrue(link.getTarget().contains("data.sql"));
        assertEquals(filePathRange.getStart(), link.getRange().getStart());
        assertEquals(filePathRange.getEnd(), link.getRange().getEnd());
    }

    @Test
    @DisplayName("no link for property without file metadata")
    void noLinkForNonFileProperty(@TempDir Path tempDir) {
        GretlDslCall dbCall = new GretlDslCall("database", DslCallStyle.METHOD_CALL,
                new Range(new Position(1, 4), new Position(1, 12)),
                new Range(new Position(1, 4), new Position(1, 30)),
                List.of(), "database 'url', 'usr', 'pwd'");

        GretlTaskBlock task = createTask("x", "SqlExecutor", 0, 0, 2, 1,
                List.of(dbCall));

        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);

        List<DocumentLink> links = provider.links(script, tempDir);
        assertTrue(links.isEmpty());
    }

    @Test
    @DisplayName("returns empty for null workspace root")
    void returnsEmptyForNullRoot() {
        GretlDslCall sqlCall = new GretlDslCall("sqlFiles", DslCallStyle.METHOD_CALL,
                new Range(new Position(1, 4), new Position(1, 14)),
                new Range(new Position(1, 4), new Position(1, 30)),
                List.of(), "sqlFiles files('x.sql')");

        GretlTaskBlock task = createTask("x", "SqlExecutor", 0, 0, 2, 1,
                List.of(sqlCall));

        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);

        List<DocumentLink> links = provider.links(script, null);
        assertTrue(links.isEmpty());
    }

    @Test
    @DisplayName("multiple files in files() call produce multiple links")
    void multipleFilesProduceMultipleLinks(@TempDir Path tempDir) throws IOException {
        createSqlFile(tempDir, "a.sql");
        createSqlFile(tempDir, "b.sql");

        Range range1 = new Range(new Position(1, 20), new Position(1, 26));
        GretlArgument arg1 = new GretlArgument(
                new StringLiteralExpression("a.sql", range1, "'a.sql'"), range1, Optional.empty());

        Range range2 = new Range(new Position(1, 28), new Position(1, 34));
        GretlArgument arg2 = new GretlArgument(
                new StringLiteralExpression("b.sql", range2, "'b.sql'"), range2, Optional.empty());

        GretlArgument filesArg = new GretlArgument(
                new MethodCallExpression("files", List.of(arg1, arg2),
                        new Range(new Position(1, 14), new Position(1, 35)),
                        "files('a.sql','b.sql')"),
                new Range(new Position(1, 14), new Position(1, 35)), Optional.empty());

        GretlDslCall sqlCall = new GretlDslCall("sqlFiles", DslCallStyle.METHOD_CALL,
                new Range(new Position(1, 4), new Position(1, 14)),
                new Range(new Position(1, 4), new Position(1, 35)),
                List.of(filesArg), "sqlFiles files('a.sql','b.sql')");

        GretlTaskBlock task = createTask("x", "SqlExecutor", 0, 0, 2, 1,
                List.of(sqlCall));

        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);

        List<DocumentLink> links = provider.links(script, tempDir);
        assertEquals(2, links.size());
    }

    @Test
    @DisplayName("task without file properties produces no links")
    void taskWithoutFilePropsNoLinks(@TempDir Path tempDir) {
        GretlTaskBlock task = createTask("x", "SqlExecutor", 0, 0, 2, 1, List.of());

        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);

        List<DocumentLink> links = provider.links(script, tempDir);
        assertTrue(links.isEmpty());
    }

    private GretlTaskBlock createTask(String name, String type, int startLine, int startChar,
                                       int endLine, int endChar, List<GretlDslCall> calls) {
        return new GretlTaskBlock(name, Optional.of(type),
                new Range(new Position(startLine, startChar), new Position(startLine, startChar)),
                new Range(new Position(startLine, startChar + 10), new Position(startLine, startChar + 20)),
                new Range(new Position(startLine, startChar), new Position(endLine, endChar)),
                new Range(new Position(startLine + 1, 4), new Position(endLine, 1)),
                calls, List.of(), List.of());
    }

    private void createSqlFile(Path directory, String filename) throws IOException {
        Path file = directory.resolve(filename);
        Files.writeString(file, "SELECT * FROM t WHERE dataset = ${dataset};");
    }
}
