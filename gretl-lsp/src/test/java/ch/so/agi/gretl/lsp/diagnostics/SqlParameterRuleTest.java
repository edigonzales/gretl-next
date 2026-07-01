package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.document.LineIndex;
import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.DslCallStyle;
import ch.so.agi.gretl.lsp.model.GretlArgument;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import ch.so.agi.gretl.lsp.model.MapEntryExpression;
import ch.so.agi.gretl.lsp.model.MapExpression;
import ch.so.agi.gretl.lsp.model.MethodCallExpression;
import ch.so.agi.gretl.lsp.model.StringLiteralExpression;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SqlParameterRuleTest {

    private SqlParameterRule rule;
    private GretlMetadata metadata;

    @BeforeEach
    void setUp() {
        PropertyMetadata dbProp = new PropertyMetadata("database", "database",
                "dsl-method-and-property", "Connector", "Property<String>",
                true, false, "Datenbank.", null, List.of(), null, false, null);

        PropertyMetadata sqlFilesProp = new PropertyMetadata("sqlFiles", "sqlFiles",
                "dsl-method-and-property", "FileCollection", "Property<FileCollection>",
                true, false, "SQL-Dateien.", null, List.of(), null, false, null);

        PropertyMetadata paramsProp = new PropertyMetadata("sqlParameters", "sqlParameters",
                "dsl-method-and-property", "Object", "Property<Object>",
                false, false, "SQL-Parameter.", null, List.of(), null, true, null);

        TaskMetadata sqlTask = new TaskMetadata("SqlExecutor",
                "ch.so.agi.gretl.tasks.SqlExecutor", "SqlExecutor",
                "database", "stable", "Fuhrt SQL aus.",
                null, List.of(), List.of(dbProp, sqlFilesProp, paramsProp));

        metadata = new GretlMetadata("1.0.0", null, "test", null, List.of(sqlTask));
        rule = new SqlParameterRule();
    }

    @Test
    @DisplayName("detects missing SQL parameter")
    void detectsMissingParameter(@TempDir Path tempDir) throws IOException {
        createSqlFile(tempDir, "data.sql", "SELECT * FROM t WHERE dataset = ${dataset};");

        GretlDslCall filesCall = createFilesCall(tempDir, "data.sql");
        GretlDslCall paramsCall = createMapParamsCall(Map.of("key1", "val"));
        GretlTaskBlock task = createTaskWithCalls("executeSql", "SqlExecutor",
                List.of(filesCall, paramsCall));

        List<Diagnostic> diagnostics = evaluate(tempDir, task);
        assertTrue(diagnostics.size() >= 1);
        Optional<Diagnostic> missingDiag = diagnostics.stream()
                .filter(d -> d.getMessage().contains("dataset")
                        && d.getMessage().contains("GRETL1301"))
                .findFirst();
        assertTrue(missingDiag.isPresent());
        assertEquals(DiagnosticSeverity.Warning, missingDiag.get().getSeverity());
    }

    @Test
    @DisplayName("detects unused SQL parameter")
    void detectsUnusedParameter(@TempDir Path tempDir) throws IOException {
        createSqlFile(tempDir, "data.sql", "SELECT 1;");

        GretlDslCall filesCall = createFilesCall(tempDir, "data.sql");
        GretlDslCall paramsCall = createMapParamsCall(Map.of("dataset", "Olten"));
        GretlTaskBlock task = createTaskWithCalls("executeSql", "SqlExecutor",
                List.of(filesCall, paramsCall));

        List<Diagnostic> diagnostics = evaluate(tempDir, task);
        assertEquals(1, diagnostics.size());
        Diagnostic d = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Information, d.getSeverity());
        assertTrue(d.getMessage().contains("dataset"));
        assertTrue(d.getMessage().contains("GRETL1302"));
    }

    @Test
    @DisplayName("reports no diagnostics when all parameters match")
    void noDiagnosticsWhenAllMatch(@TempDir Path tempDir) throws IOException {
        createSqlFile(tempDir, "data.sql", "SELECT * FROM t WHERE dataset = ${dataset};");

        GretlDslCall filesCall = createFilesCall(tempDir, "data.sql");
        GretlDslCall paramsCall = createMapParamsCall(Map.of("dataset", "Olten"));
        GretlTaskBlock task = createTaskWithCalls("executeSql", "SqlExecutor",
                List.of(filesCall, paramsCall));

        List<Diagnostic> diagnostics = evaluate(tempDir, task);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("handles map entries in sqlParameters")
    void handlesMapEntries(@TempDir Path tempDir) throws IOException {
        createSqlFile(tempDir, "transform.sql",
                "DELETE FROM t WHERE a = ${param_a} AND b = ${param_b};");

        GretlDslCall filesCall = createFilesCall(tempDir, "transform.sql");
        GretlDslCall paramsCall = createMapParamsCall(
                new LinkedHashMap<>() {{ put("param_a", "x"); put("param_b", "y"); }});
        GretlTaskBlock task = createTaskWithCalls("executeSql", "SqlExecutor",
                List.of(filesCall, paramsCall));

        List<Diagnostic> diagnostics = evaluate(tempDir, task);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("task without sqlFiles is skipped")
    void skipsWithoutSqlFiles(@TempDir Path tempDir) throws IOException {
        createSqlFile(tempDir, "data.sql", "SELECT ${x};");

        GretlDslCall paramsCall = createMapParamsCall(Map.of("x", "val"));
        GretlTaskBlock task = createTaskWithCalls("executeSql", "SqlExecutor",
                List.of(paramsCall));

        List<Diagnostic> diagnostics = evaluate(tempDir, task);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("task without sqlParameters is skipped")
    void skipsWithoutSqlParameters(@TempDir Path tempDir) throws IOException {
        createSqlFile(tempDir, "data.sql", "SELECT ${x};");

        GretlDslCall filesCall = createFilesCall(tempDir, "data.sql");
        GretlTaskBlock task = createTaskWithCalls("executeSql", "SqlExecutor",
                List.of(filesCall));

        List<Diagnostic> diagnostics = evaluate(tempDir, task);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("returns empty for null workspace root")
    void returnsEmptyForNullRoot() {
        GretlTaskBlock task = createTaskWithCalls("executeSql", "SqlExecutor",
                List.of());

        TextDocument doc = new TextDocument("file:///test.gradle", "groovy", 1,
                "tasks.register('x', SqlExecutor) {\n}", LineIndex.from(""));
        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);

        AnalysisInput input = new AnalysisInput(doc, script, metadata, null);
        List<Diagnostic> diagnostics = rule.evaluate(input);
        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("warns when SQL file not found")
    void warnsWhenSqlFileNotFound(@TempDir Path tempDir) throws IOException {
        GretlDslCall filesCall = createFilesCall(tempDir, "nonexistent.sql");
        GretlDslCall paramsCall = createMapParamsCall(Map.of("dataset", "Olten"));
        GretlTaskBlock task = createTaskWithCalls("executeSql", "SqlExecutor",
                List.of(filesCall, paramsCall));

        List<Diagnostic> diagnostics = evaluate(tempDir, task);
        assertTrue(diagnostics.stream().anyMatch(d ->
                d.getMessage().contains("nicht gefunden")));
    }

    private List<Diagnostic> evaluate(Path workspaceRoot, GretlTaskBlock task) {
        TextDocument doc = new TextDocument("file:///test.gradle", "groovy", 1,
                "tasks.register('x', SqlExecutor) {\n}", LineIndex.from(""));
        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);
        AnalysisInput input = new AnalysisInput(doc, script, metadata, workspaceRoot);
        return rule.evaluate(input);
    }

    private GretlTaskBlock createTaskWithCalls(String name, String type,
                                                List<GretlDslCall> calls) {
        return new GretlTaskBlock(name, Optional.of(type),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 0), new Position(5, 1)),
                new Range(new Position(1, 4), new Position(5, 1)),
                calls, List.of(), List.of());
    }

    private GretlDslCall createFilesCall(Path tempDir, String sqlFile) {
        int pathCol = 20;
        Range pathRange = new Range(new Position(1, pathCol),
                new Position(1, pathCol + sqlFile.length() + 2));
        GretlArgument strArg = new GretlArgument(
                new StringLiteralExpression(sqlFile, pathRange,
                        "'" + sqlFile + "'"),
                pathRange, Optional.empty());
        Range filesRange = new Range(new Position(1, 14),
                new Position(1, 14 + 7 + sqlFile.length() + 2));
        GretlArgument filesArg = new GretlArgument(
                new MethodCallExpression("files", List.of(strArg),
                        filesRange, "files('" + sqlFile + "')"),
                filesRange, Optional.empty());

        return new GretlDslCall("sqlFiles", DslCallStyle.METHOD_CALL,
                new Range(new Position(1, 4), new Position(1, 12)),
                filesRange, List.of(filesArg),
                "sqlFiles files('" + sqlFile + "')");
    }

    private GretlDslCall createMapParamsCall(Map<String, String> entries) {
        int line = 2;
        int col = 4;
        Range nameRange = new Range(new Position(line, col),
                new Position(line, col + 13));
        Range fullRange = new Range(new Position(line, col),
                new Position(line, col + 13 + 20));

        List<MapEntryExpression> mapEntries = new java.util.ArrayList<>();
        for (var e : entries.entrySet()) {
            Range keyRange = new Range(new Position(line, col), new Position(line, col + 20));
            mapEntries.add(new MapEntryExpression(e.getKey(),
                    new StringLiteralExpression(e.getValue(), keyRange, "'" + e.getValue() + "'"),
                    keyRange));
        }

        MapExpression mapExpr = new MapExpression(mapEntries, fullRange, "");
        GretlArgument mapArg = new GretlArgument(mapExpr, fullRange, Optional.empty());
        return new GretlDslCall("sqlParameters", DslCallStyle.METHOD_CALL,
                nameRange, fullRange, List.of(mapArg), "");
    }

    private void createSqlFile(Path directory, String filename, String content) throws IOException {
        Path file = directory.resolve(filename);
        Files.writeString(file, content);
    }
}
