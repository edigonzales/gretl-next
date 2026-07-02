package ch.so.agi.gretl.lsp.hover;

import ch.so.agi.gretl.lsp.metadata.AcceptedForm;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.DslCallStyle;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HoverProviderTest {

    private HoverProvider provider;
    private GretlMetadata metadata;

    @BeforeEach
    void setUp() {
        AcceptedForm form1 = new AcceptedForm("method-call", "database url, user, password",
                "database ${1:url}, ${2:user}, ${3:password}", 3, false);
        AcceptedForm form2 = new AcceptedForm("assignment", "database = [url, user, password]",
                null, null, true);
        AcceptedForm form3 = new AcceptedForm("method-call", "sqlFiles files('...')",
                "sqlFiles files('${1:script.sql}')", 1, false);

        PropertyMetadata dbProp = new PropertyMetadata("database", "database",
                "dsl-method-and-property", "Connector", "Property<String>",
                true, false, "Datenbankverbindung.", null,
                List.of(form1, form2), null, false, null);

        PropertyMetadata sqlProp = new PropertyMetadata("sqlFiles", "sqlFiles",
                "dsl-method-and-property", "FileCollection", "Property<FileCollection>",
                true, false, "SQL-Dateien, deren Statements gelesen und ausgefuhrt werden.", null,
                List.of(form3), null, false, null);

        PropertyMetadata paramsProp = new PropertyMetadata("sqlParameters", "sqlParameters",
                "dsl-method-and-property", "Object", "Property<Object>",
                false, false, "Map mit SQL-Parametern.", null,
                List.of(), null, true, null);

        TaskMetadata sqlTask = new TaskMetadata("SqlExecutor", "ch.so.agi.gretl.tasks.SqlExecutor",
                "SqlExecutor", "database", "stable", "Fuhrt SQL-Dateien in angegebener Reihenfolge aus.",
                null, List.of(), List.of(dbProp, sqlProp, paramsProp));

        metadata = new GretlMetadata("1.0.0", null, "test", null,
                List.of(sqlTask));

        provider = new HoverProvider(metadata);
    }

    @Test
    @DisplayName("hover over task type name shows task description")
    void hoverOverTaskTypeShowsDescription() {
        Range typeRange = new Range(new Position(0, 30), new Position(0, 41));
        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                typeRange,
                new Range(new Position(0, 1), new Position(3, 1)),
                new Range(new Position(1, 4), new Position(3, 1)),
                List.of(), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Optional<Hover> hover = provider.hover(script, new Position(0, 35));

        assertTrue(hover.isPresent());
        String content = hover.get().getContents().getRight().getValue();
        assertTrue(content.contains("**SqlExecutor**"));
        assertTrue(content.contains("Fuhrt SQL-Dateien"));
        assertTrue(content.contains("ch.so.agi.gretl.tasks.SqlExecutor"));
        assertTrue(content.contains("- `database`\n- `sqlFiles`"),
                "required properties should be separated by newlines");
        assertTrue(content.contains("- `sqlParameters`"), "optional properties should appear in hover");
        assertTrue(content.contains("Object (Optional)"), "property types should appear with status");
    }

    @Test
    @DisplayName("hover over DSL property shows type and required status")
    void hoverOverPropertyShowsTypeAndRequired() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        Range callNameRange = new Range(new Position(1, 4), new Position(1, 12));
        Range callFullRange = new Range(new Position(1, 4), new Position(1, 30));
        GretlDslCall call = new GretlDslCall("database", DslCallStyle.METHOD_CALL,
                callNameRange, callFullRange,
                List.of(), "database 'url', 'usr', 'pwd'");

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(call), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Optional<Hover> hover = provider.hover(script, new Position(1, 6));

        assertTrue(hover.isPresent());
        String content = hover.get().getContents().getRight().getValue();
        assertTrue(content.contains("**database**"));
        assertTrue(content.contains("Property<String>"));
        assertTrue(content.contains("Pflicht: ja"));
        assertTrue(content.contains("Datenbankverbindung"));
    }

    @Test
    @DisplayName("hover shows signature in code block")
    void hoverShowsSignature() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        Range callNameRange = new Range(new Position(1, 4), new Position(1, 12));
        Range callFullRange = new Range(new Position(1, 4), new Position(1, 30));
        GretlDslCall call = new GretlDslCall("sqlFiles", DslCallStyle.METHOD_CALL,
                callNameRange, callFullRange,
                List.of(), "sqlFiles files('x.sql')");

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(call), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Optional<Hover> hover = provider.hover(script, new Position(1, 6));

        assertTrue(hover.isPresent());
        String content = hover.get().getContents().getRight().getValue();
        assertTrue(content.contains("```groovy"));
        assertTrue(content.contains("sqlFiles files('...')"));
    }

    @Test
    @DisplayName("returns empty for position with no hover target")
    void returnsEmptyForNoTarget() {
        GretlScript script = new GretlScript("test.gradle", List.of(),
                List.of(), List.of(), List.of(), true, false);

        Optional<Hover> hover = provider.hover(script, new Position(0, 0));

        assertFalse(hover.isPresent());
    }

    @Test
    @DisplayName("hover shows deprecated status for deprecated properties")
    void hoverShowsDeprecatedStatus() {
        AcceptedForm form = new AcceptedForm("method-call", "oldProp value",
                "oldProp ${1:value}", 1, false);
        PropertyMetadata oldProp = new PropertyMetadata("oldProp", "oldProp",
                "dsl-method-and-property", "String", "Property<String>",
                false, true, "Veraltete Property.", null,
                List.of(form), null, false, null);

        TaskMetadata taskMeta = new TaskMetadata("TestTask", "ch.so.agi.gretl.tasks.TestTask",
                "TestTask", "other", "deprecated", "Ein Test-Task.",
                null, List.of(), List.of(oldProp));

        metadata = new GretlMetadata("1.0.0", null, "test", null, List.of(taskMeta));
        provider = new HoverProvider(metadata);

        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        Range callNameRange = new Range(new Position(1, 4), new Position(1, 11));
        Range callFullRange = new Range(new Position(1, 4), new Position(1, 20));
        GretlDslCall call = new GretlDslCall("oldProp", DslCallStyle.METHOD_CALL,
                callNameRange, callFullRange, List.of(), "oldProp 'val'");

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("TestTask"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 38)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(call), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Optional<Hover> hover = provider.hover(script, new Position(1, 6));

        assertTrue(hover.isPresent());
        String content = hover.get().getContents().getRight().getValue();
        assertTrue(content.contains("deprecated"));
    }

    @Test
    @DisplayName("hover shows sqlParameterProvider info")
    void hoverShowsSqlParameterProvider() {
        Range bodyRange = new Range(new Position(1, 4), new Position(3, 1));
        Range callNameRange = new Range(new Position(1, 4), new Position(1, 17));
        Range callFullRange = new Range(new Position(1, 4), new Position(1, 30));
        GretlDslCall call = new GretlDslCall("sqlParameters", DslCallStyle.METHOD_CALL,
                callNameRange, callFullRange, List.of(), "sqlParameters key: 'val'");

        GretlTaskBlock block = new GretlTaskBlock("x", Optional.of("SqlExecutor"),
                new Range(new Position(0, 17), new Position(0, 18)),
                new Range(new Position(0, 30), new Position(0, 41)),
                new Range(new Position(0, 1), new Position(3, 1)),
                bodyRange,
                List.of(call), List.of(), List.of());

        GretlScript script = new GretlScript("test.gradle", List.of(block),
                List.of(), List.of(), List.of(), true, false);

        Optional<Hover> hover = provider.hover(script, new Position(1, 6));

        assertTrue(hover.isPresent());
        String content = hover.get().getContents().getRight().getValue();
        assertTrue(content.contains("SQL-Parameter-Provider"));
    }
}
