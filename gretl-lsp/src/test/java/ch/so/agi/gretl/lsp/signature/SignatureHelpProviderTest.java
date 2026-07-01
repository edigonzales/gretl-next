package ch.so.agi.gretl.lsp.signature;

import ch.so.agi.gretl.lsp.metadata.AcceptedForm;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.DslCallStyle;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SignatureHelp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SignatureHelpProviderTest {

    private SignatureHelpProvider provider;
    private GretlMetadata metadata;

    @BeforeEach
    void setUp() {
        AcceptedForm form1 = new AcceptedForm("method-call", "database url, user, password",
                "database ${1:url}, ${2:user}, ${3:password}", 3, false);
        AcceptedForm form1Legacy = new AcceptedForm("assignment", "database = [url, user, password]",
                null, null, true);

        AcceptedForm form2 = new AcceptedForm("method-call", "sqlFiles files('...')",
                "sqlFiles files('${1:script.sql}')", 1, false);

        AcceptedForm form3 = new AcceptedForm("method-call", "sqlParameters key: 'value'",
                "sqlParameters ${1:key}: ${2:'value'}", 1, false);

        PropertyMetadata dbProp = new PropertyMetadata("database", "database",
                "dsl-method-and-property", "Connector", "Property<String>",
                true, false, "Datenbankverbindung.", null,
                List.of(form1, form1Legacy), null, false, null);

        PropertyMetadata sqlProp = new PropertyMetadata("sqlFiles", "sqlFiles",
                "dsl-method-and-property", "FileCollection", "Property<FileCollection>",
                true, false, "SQL-Dateien.", null,
                List.of(form2), null, false, null);

        TaskMetadata sqlTask = new TaskMetadata("SqlExecutor", "ch.so.agi.gretl.tasks.SqlExecutor",
                "SqlExecutor", "database", "stable", "Fuhrt SQL-Dateien aus.",
                null, List.of(), List.of(dbProp, sqlProp));

        metadata = new GretlMetadata("1.0.0", null, "test", null, List.of(sqlTask));
        provider = new SignatureHelpProvider(metadata);
    }

    @Test
    @DisplayName("signature help returns signatures for multi-argument method call")
    void signatureHelpForMultiArgumentCall() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        Range callFullRange = new Range(new Position(1, 4), new Position(1, 30));
        GretlDslCall call = new GretlDslCall("database", DslCallStyle.METHOD_CALL,
                new Range(new Position(1, 4), new Position(1, 12)),
                callFullRange,
                List.of(), "database dbUri, ");

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(call), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Optional<SignatureHelp> help = provider.signatureHelp(script, new Position(1, 18));

        assertTrue(help.isPresent());
        assertFalse(help.get().getSignatures().isEmpty());
        assertEquals("database url, user, password", help.get().getSignatures().get(0).getLabel());
    }

    @Test
    @DisplayName("active parameter is based on comma count before cursor in sourceText")
    void activeParameterFromCommaCount() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        Range callFullRange = new Range(new Position(1, 4), new Position(1, 30));
        GretlDslCall call = new GretlDslCall("database", DslCallStyle.METHOD_CALL,
                new Range(new Position(1, 4), new Position(1, 12)),
                callFullRange,
                List.of(), "database dbUri, usr, ");

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(call), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Optional<SignatureHelp> help = provider.signatureHelp(script, new Position(1, 23));

        assertTrue(help.isPresent());
        assertEquals(2, help.get().getActiveParameter());
    }

    @Test
    @DisplayName("returns empty when not inside a task block")
    void returnsEmptyOutsideTaskBlock() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        Optional<SignatureHelp> help = provider.signatureHelp(script, new Position(0, 0));

        assertFalse(help.isPresent());
    }

    @Test
    @DisplayName("returns empty when task block has no type name")
    void returnsEmptyForUnknownType() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        Range callFullRange = new Range(new Position(1, 4), new Position(1, 30));
        GretlDslCall call = new GretlDslCall("database", DslCallStyle.METHOD_CALL,
                new Range(new Position(1, 4), new Position(1, 12)),
                callFullRange, List.of(), "database dbUri, ");

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.empty(),
                new Range(new Position(0, 17), new Position(0, 18)),
                null,
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(call), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Optional<SignatureHelp> help = provider.signatureHelp(script, new Position(1, 18));

        assertFalse(help.isPresent());
    }

    @Test
    @DisplayName("activeParameterIndex counts commas correctly")
    void activeParameterIndexCountsCommas() {
        assertEquals(0, provider.activeParameterIndex("database ", new Position(0, 9)));
        assertEquals(1, provider.activeParameterIndex("database dbUri, ", new Position(0, 17)));
        assertEquals(2, provider.activeParameterIndex("database dbUri, usr, ", new Position(0, 22)));
    }

    @Test
    @DisplayName("activeParameterIndex returns 0 for empty text")
    void activeParameterIndexEmptyText() {
        assertEquals(0, provider.activeParameterIndex(null, new Position(0, 5)));
        assertEquals(0, provider.activeParameterIndex("", new Position(0, 5)));
    }
}
