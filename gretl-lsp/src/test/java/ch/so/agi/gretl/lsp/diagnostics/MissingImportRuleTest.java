package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.document.LineIndex;
import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.MetadataLoader;
import ch.so.agi.gretl.lsp.parser.GroovyAstGretlParser;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class MissingImportRuleTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("info when task used without explicit import")
    void infoForMissingImport() {
        String text = "tasks.register('x', SqlExecutor) {\n    database 'url'\n}";
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        var rule = new MissingImportRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertFalse(diagnostics.isEmpty(), "should warn about missing import");
        var diag = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Information, diag.getSeverity());
        assertTrue(diag.getMessage().contains("SqlExecutor"));
        assertEquals("GRETL1401", diag.getCode().getLeft());
    }

    @Test
    @DisplayName("no diagnostic when fully qualified name is used")
    void noDiagnosticForFullyQualifiedName() {
        String text = "tasks.register('x', ch.so.agi.gretl.tasks.SqlExecutor) {\n    database 'url'\n}";
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        var rule = new MissingImportRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty(), "fully qualified name should not need import");
    }

    @Test
    @DisplayName("no diagnostic when import is present")
    void noDiagnosticWhenImportPresent() {
        String text = "import ch.so.agi.gretl.tasks.SqlExecutor\n\n"
                + "tasks.register('x', SqlExecutor) {\n    database 'url'\n}";
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        var rule = new MissingImportRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty(), "explicit import should suppress diagnostic");
    }

    @Test
    @DisplayName("no diagnostic when star import covers the task")
    void noDiagnosticWhenStarImportCoversTask() {
        String text = "import ch.so.agi.gretl.tasks.*\n\n"
                + "tasks.register('x', SqlExecutor) {\n    database 'url'\n}";
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        var rule = new MissingImportRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty(), "star import should suppress diagnostic");
    }

    @Test
    @DisplayName("no diagnostic for unknown task types")
    void noDiagnosticForUnknownType() {
        String text = "tasks.register('x', UnknownType) {\n}";
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle", text);
        var doc = new TextDocument("test.gradle", "groovy", 1, text, LineIndex.from(text));

        var rule = new MissingImportRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty(), "unknown type is not in metadata, skip");
    }
}
