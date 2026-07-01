package ch.so.agi.gretl.lsp.symbol;

import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.DslCallStyle;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DocumentSymbolProviderTest {

    private DocumentSymbolProvider provider;
    private GretlMetadata metadata;

    @BeforeEach
    void setUp() {
        TaskMetadata sqlTask = new TaskMetadata("SqlExecutor",
                "ch.so.agi.gretl.tasks.SqlExecutor", "SqlExecutor",
                "database", "stable", "Fuhrt SQL aus.",
                null, List.of(), List.of());

        TaskMetadata duckDbTask = new TaskMetadata("DuckDbSqlExecutor",
                "ch.so.agi.gretl.tasks.DuckDbSqlExecutor", "DuckDbSqlExecutor",
                "database", "stable", "Fuhrt DuckDB SQL aus.",
                null, List.of(), List.of());

        metadata = new GretlMetadata("1.0.0", null, "test", null,
                List.of(sqlTask, duckDbTask));
        provider = new DocumentSymbolProvider(metadata);
    }

    @Test
    @DisplayName("three tasks produce three symbols")
    void threeTasksProduceThreeSymbols() {
        GretlTaskBlock task1 = createTask("importData", "SqlExecutor", 0, 0, 0, 30);
        GretlTaskBlock task2 = createTask("executeSql", "SqlExecutor", 4, 0, 4, 30);
        GretlTaskBlock task3 = createTask("analyse", "DuckDbSqlExecutor", 8, 0, 8, 40);

        GretlScript script = new GretlScript("test.gradle",
                List.of(task1, task2, task3), List.of(), List.of(), List.of(), true, false);

        List<Either<SymbolInformation, DocumentSymbol>> symbols = provider.symbols(script);
        assertEquals(3, symbols.size());

        DocumentSymbol sym1 = symbols.get(0).getRight();
        DocumentSymbol sym2 = symbols.get(1).getRight();
        DocumentSymbol sym3 = symbols.get(2).getRight();

        assertTrue(sym1.getName().contains("importData"));
        assertTrue(sym1.getName().contains("SqlExecutor"));
        assertTrue(sym2.getName().contains("executeSql"));
        assertTrue(sym2.getName().contains("SqlExecutor"));
        assertTrue(sym3.getName().contains("analyse"));
        assertTrue(sym3.getName().contains("DuckDbSqlExecutor"));
    }

    @Test
    @DisplayName("empty script produces empty symbol list")
    void emptyScriptProducesEmptyList() {
        GretlScript script = new GretlScript("empty.gradle",
                List.of(), List.of(), List.of(), List.of(), true, false);
        List<Either<SymbolInformation, DocumentSymbol>> symbols = provider.symbols(script);
        assertTrue(symbols.isEmpty());
    }

    @Test
    @DisplayName("task with DSL calls produces child symbols")
    void taskWithDslCallsProducesChildSymbols() {
        Range fullTaskRange = new Range(new Position(0, 0), new Position(3, 1));
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        Range nameRange = new Range(new Position(0, 17), new Position(0, 18));
        Range typeRange = new Range(new Position(0, 30), new Position(0, 41));

        GretlDslCall dbCall = new GretlDslCall("database", DslCallStyle.METHOD_CALL,
                new Range(new Position(1, 4), new Position(1, 12)),
                new Range(new Position(1, 4), new Position(1, 30)),
                List.of(), "database 'url', 'usr', 'pwd'");

        GretlDslCall sqlCall = new GretlDslCall("sqlFiles", DslCallStyle.METHOD_CALL,
                new Range(new Position(2, 4), new Position(2, 13)),
                new Range(new Position(2, 4), new Position(2, 30)),
                List.of(), "sqlFiles files('x.sql')");

        GretlTaskBlock task = new GretlTaskBlock("executeSql",
                Optional.of("SqlExecutor"), nameRange, typeRange,
                fullTaskRange, bodyRange,
                List.of(dbCall, sqlCall), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);

        List<Either<SymbolInformation, DocumentSymbol>> symbols = provider.symbols(script);
        assertEquals(1, symbols.size());
        DocumentSymbol sym = symbols.get(0).getRight();
        List<DocumentSymbol> children = sym.getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());
        assertEquals("database", children.get(0).getName());
        assertEquals("sqlFiles", children.get(1).getName());
    }

    @Test
    @DisplayName("task without DSL calls has empty children")
    void taskWithoutCallsHasEmptyChildren() {
        GretlTaskBlock task = createTask("simpleTask", "SqlExecutor", 0, 0, 0, 30);

        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);

        List<Either<SymbolInformation, DocumentSymbol>> symbols = provider.symbols(script);
        assertEquals(1, symbols.size());
        DocumentSymbol sym = symbols.get(0).getRight();
        assertNotNull(sym.getChildren());
        assertTrue(sym.getChildren().isEmpty());
    }

    @Test
    @DisplayName("symbol uses correct ranges")
    void symbolUsesCorrectRanges() {
        Range fullRange = new Range(new Position(0, 0), new Position(3, 1));
        Range nameRange = new Range(new Position(0, 17), new Position(0, 18));
        Range typeRange = new Range(new Position(0, 30), new Position(0, 41));

        GretlTaskBlock task = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                nameRange, typeRange, fullRange,
                new Range(new Position(1, 4), new Position(3, 1)),
                List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle",
                List.of(task), List.of(), List.of(), List.of(), true, false);

        List<Either<SymbolInformation, DocumentSymbol>> symbols = provider.symbols(script);
        DocumentSymbol sym = symbols.get(0).getRight();
        assertEquals(fullRange.getStart(), sym.getRange().getStart());
        assertEquals(fullRange.getEnd(), sym.getRange().getEnd());
        assertEquals(nameRange.getStart(), sym.getSelectionRange().getStart());
        assertEquals(nameRange.getEnd(), sym.getSelectionRange().getEnd());
    }

    private GretlTaskBlock createTask(String name, String typeName, int startLine,
                                       int startChar, int endLine, int endChar) {
        return new GretlTaskBlock(name, Optional.of(typeName),
                new Range(new Position(startLine, startChar), new Position(startLine, startChar)),
                new Range(new Position(startLine, startChar + 10), new Position(startLine, startChar + 20)),
                new Range(new Position(startLine, startChar), new Position(endLine, endChar)),
                new Range(new Position(startLine + 1, 4), new Position(endLine, 1)),
                List.of(), List.of(), List.of());
    }
}
