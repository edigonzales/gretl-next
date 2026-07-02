package ch.so.agi.gretl.lsp.completion;

import ch.so.agi.gretl.lsp.metadata.AcceptedForm;
import ch.so.agi.gretl.lsp.metadata.CompletionMetadata;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.DependencyKind;
import ch.so.agi.gretl.lsp.model.DslCallStyle;
import ch.so.agi.gretl.lsp.model.GretlDependency;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CompletionProviderTest {

    private CompletionProvider provider;
    private GretlMetadata metadata;

    @BeforeEach
    void setUp() {
        AcceptedForm form1 = new AcceptedForm("method-call", "database url, user, password",
                "database ${1:url}, ${2:user}, ${3:password}", 3, false);
        AcceptedForm form2 = new AcceptedForm("assignment", "database = [url, user, password]",
                null, null, true);
        CompletionMetadata completion1 = new CompletionMetadata("database", "Pflicht \u00b7 Connector", "0100_database");

        AcceptedForm form3 = new AcceptedForm("method-call", "sqlFiles files('...')",
                "sqlFiles files('${1:script.sql}')", 1, false);
        AcceptedForm form4 = new AcceptedForm("assignment", "sqlFiles = files('...')",
                null, null, true);
        CompletionMetadata completion2 = new CompletionMetadata("sqlFiles", "Pflicht \u00b7 FileCollection", "0100_sqlFiles");

        AcceptedForm form5 = new AcceptedForm("method-call", "sqlParameters key: 'value'",
                "sqlParameters ${1:key}: ${2:'value'}", 1, false);
        CompletionMetadata completion3 = new CompletionMetadata("sqlParameters", "Optional \u00b7 Object", "0200_sqlParameters");

        PropertyMetadata dbProp = new PropertyMetadata("database", "database",
                "dsl-method-and-property", "Connector", "Property<String>",
                true, false, "Datenbankverbindung.", null,
                List.of(form1, form2), null, false, completion1);

        PropertyMetadata sqlProp = new PropertyMetadata("sqlFiles", "sqlFiles",
                "dsl-method-and-property", "FileCollection", "Property<FileCollection>",
                true, false, "SQL-Dateien.", null,
                List.of(form3, form4), null, false, completion2);

        PropertyMetadata paramsProp = new PropertyMetadata("sqlParameters", "sqlParameters",
                "dsl-method-and-property", "Object", "Property<Object>",
                false, false, "Map mit SQL-Parametern.", null,
                List.of(form5), null, true, completion3);

        TaskMetadata sqlTask = new TaskMetadata("SqlExecutor", "ch.so.agi.gretl.tasks.SqlExecutor",
                "SqlExecutor", "database", "stable", "Fuhrt SQL-Dateien aus.",
                null, List.of(), List.of(dbProp, sqlProp, paramsProp));

        TaskMetadata duckDbTask = new TaskMetadata("DuckDbSqlExecutor",
                "ch.so.agi.gretl.tasks.DuckDbSqlExecutor",
                "DuckDbSqlExecutor", "database", "stable", "Fuhrt SQL-Dateien mit DuckDB aus.",
                null, List.of(), List.of());

        metadata = new GretlMetadata("1.0.0", null, "test", null,
                List.of(sqlTask, duckDbTask));

        provider = new CompletionProvider(metadata);
    }

    @Test
    @DisplayName("task type completion returns all tasks from metadata")
    void taskTypeCompletionReturnsAllTasks() {
        Range typeRange = new Range(new Position(0, 30), new Position(0, 41));
        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                typeRange,
                new Range(new Position(0, 1), new Position(2, 1)),
                null, List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(0, 35), "");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);
        assertEquals(2, items.size());
        assertEquals("DuckDbSqlExecutor", items.get(0).getLabel());
        assertEquals("SqlExecutor", items.get(1).getLabel());
    }

    @Test
    @DisplayName("task type completion uses text edit for correct insert after comma")
    void taskTypeCompletionUsesTextEditAfterComma() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(0, 30),
                        "tasks.register('x', SqlE");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);
        assertFalse(items.isEmpty());

        CompletionItem sql = items.stream()
                .filter(i -> "SqlExecutor".equals(i.getLabel()))
                .findFirst().orElseThrow();
        assertNotNull(sql.getTextEdit());
        TextEdit edit = sql.getTextEdit().getLeft();
        assertEquals(" SqlExecutor", edit.getNewText(), "should include leading space after comma");
        assertTrue(edit.getRange().getStart().getCharacter() < edit.getRange().getEnd().getCharacter(),
                "edit range should cover typed prefix after comma");
    }

    @Test
    @DisplayName("task type completion inserts space after comma even with trailing whitespace")
    void taskTypeCompletionInsertsSpaceAfterComma() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        // Cursor right after comma+space, no type characters typed
        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(0, 30),
                        "tasks.register('x', ");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);
        assertFalse(items.isEmpty());

        CompletionItem sql = items.stream()
                .filter(i -> "SqlExecutor".equals(i.getLabel()))
                .findFirst().orElseThrow();
        assertNotNull(sql.getTextEdit());
        TextEdit edit = sql.getTextEdit().getLeft();
        assertTrue(edit.getNewText().startsWith(" "),
                "insert text should start with space after comma");
        assertEquals("SqlExecutor", sql.getLabel(),
                "label should remain the simple name without leading space");
    }

    @Test
    @DisplayName("property completion shows missing required properties first")
    void propertyCompletionShowsMissingRequiredFirst() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(2, 0), "");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);
        assertEquals(3, items.size());

        assertTrue(items.get(0).getSortText().startsWith("0100"));
        assertTrue(items.get(1).getSortText().startsWith("0100"));
        assertTrue(items.get(2).getSortText().startsWith("0200"));
    }

    @Test
    @DisplayName("property completion excludes already-set properties")
    void propertyCompletionExcludesAlreadySet() {
        Range bodyRange = new Range(new Position(2, 4), new Position(4, 1));
        Range dbCallRange = new Range(new Position(2, 4), new Position(2, 30));
        GretlDslCall dbCall = new GretlDslCall("database", DslCallStyle.METHOD_CALL,
                new Range(new Position(2, 4), new Position(2, 12)), dbCallRange,
                List.of(), "database 'url', 'usr', 'pwd'");

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(4, 1)),
                bodyRange,
                List.of(dbCall), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(3, 0), "");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);

        boolean hasDatabase = items.stream().anyMatch(i -> "database".equals(i.getLabel()));
        assertFalse(hasDatabase, "database should be excluded because it's already set");

        boolean hasSqlFiles = items.stream().anyMatch(i -> "sqlFiles".equals(i.getLabel()));
        assertTrue(hasSqlFiles, "sqlFiles should be present because it's not yet set");
    }

    @Test
    @DisplayName("insert text is taken from non-legacy accepted form")
    void insertTextFromNonLegacyForm() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(2, 0), "");

        List<CompletionItem> items = result.getLeft();

        var sqlFilesItem = items.stream()
                .filter(i -> "sqlFiles".equals(i.getLabel()))
                .findFirst();
        assertTrue(sqlFilesItem.isPresent());
        assertEquals("sqlFiles files('${1:script.sql}')", sqlFilesItem.get().getInsertText());
        assertEquals(InsertTextFormat.Snippet, sqlFilesItem.get().getInsertTextFormat());
    }

    @Test
    @DisplayName("dependency completion shows task names from script")
    void dependencyCompletionShowsTaskNames() {
        Range depRange = new Range(new Position(1, 4), new Position(1, 25));
        GretlDependency dep = new GretlDependency(DependencyKind.DEPENDS_ON, "importData", depRange);
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(4, 1)),
                bodyRange,
                List.of(), List.of(dep), List.of());

        GretlTaskBlock block2 = new GretlTaskBlock("importData", Optional.of("CsvImport"),
                new Range(new Position(2, 17), new Position(2, 27)),
                null,
                new Range(new Position(2, 1), new Position(5, 1)),
                null,
                List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block, block2),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(1, 10), "");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);
        assertTrue(items.stream().anyMatch(i -> "importData".equals(i.getLabel())));
        assertTrue(items.stream().anyMatch(i -> "x".equals(i.getLabel())));
    }

    @Test
    @DisplayName("returns empty for unknown context")
    void returnsEmptyForUnknownContext() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(0, 0), "");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("returns empty for task block with no type name")
    void returnsEmptyForUnknownType() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        GretlTaskBlock block = new GretlTaskBlock("x", Optional.empty(),
                new Range(new Position(0, 17), new Position(0, 18)),
                null,
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(2, 0), "");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    @DisplayName("import completion returns fully qualified class names")
    void importCompletionReturnsFqns() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(0, 40),
                        "import ch.so.agi.gretl.tasks.");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(
                i -> i.getLabel().equals("ch.so.agi.gretl.tasks.SqlExecutor")));
        assertTrue(items.stream().anyMatch(
                i -> i.getLabel().equals("ch.so.agi.gretl.tasks.DuckDbSqlExecutor")));
    }

    @Test
    @DisplayName("import completion filters by prefix")
    void importCompletionFiltersByPrefix() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(0, 34),
                        "import ch.so.agi.gretl.tasks.Sql");

        List<CompletionItem> items = result.getLeft();
        assertNotNull(items);
        assertTrue(items.stream().anyMatch(
                i -> i.getLabel().equals("ch.so.agi.gretl.tasks.SqlExecutor")));
        assertFalse(items.stream().anyMatch(
                i -> i.getLabel().equals("ch.so.agi.gretl.tasks.DuckDbSqlExecutor")));
    }

    @Test
    @DisplayName("import completion items have class kind and task name detail")
    void importCompletionItemsHaveKindAndDetail() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(0, 40),
                        "import ch.so.agi.gretl.tasks.");

        List<CompletionItem> items = result.getLeft();
        var sql = items.stream()
                .filter(i -> i.getLabel().equals("ch.so.agi.gretl.tasks.SqlExecutor"))
                .findFirst();
        assertTrue(sql.isPresent());
        assertEquals(CompletionItemKind.Class, sql.get().getKind());
        assertEquals("SqlExecutor", sql.get().getDetail());
    }

    @Test
    @DisplayName("import completion text edit covers only the import path")
    void importCompletionTextEditCoversImportPath() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        Either<List<CompletionItem>, CompletionList> result =
                provider.complete(script, new Position(0, 40),
                        "import ch.so.agi.gretl.tasks.");

        List<CompletionItem> items = result.getLeft();
        var sql = items.stream()
                .filter(i -> i.getLabel().equals("ch.so.agi.gretl.tasks.SqlExecutor"))
                .findFirst();
        assertTrue(sql.isPresent());

        TextEdit edit = sql.get().getTextEdit().getLeft();
        assertEquals("ch.so.agi.gretl.tasks.SqlExecutor", edit.getNewText());

        int importKeywordLen = "import ".length();
        assertEquals(importKeywordLen, edit.getRange().getStart().getCharacter(),
                "edit range should start after 'import '");
        assertEquals(40, edit.getRange().getEnd().getCharacter(),
                "edit range should end at cursor position");
    }
}
