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
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MissingRequiredPropertyRuleTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("detects missing required property sqlFiles")
    void detectsMissingRequiredProperty() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, script.uri(), LineIndex.from(""));

        MissingRequiredPropertyRule rule = new MissingRequiredPropertyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        var diag = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Error, diag.getSeverity());
        assertTrue(diag.getMessage().contains("sqlFiles"));
        assertTrue(diag.getMessage().contains("x"));
        assertEquals("GRETL1001", diag.getCode().getLeft());
    }

    @Test
    @DisplayName("no diagnostic when all required properties present")
    void noDiagnosticWhenAllPresent() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n" +
                        "    database 'url', 'usr', 'pwd'\n" +
                        "    sqlFiles files('x.sql')\n" +
                        "}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        MissingRequiredPropertyRule rule = new MissingRequiredPropertyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("skips tasks with unknown type")
    void skipsUnknownTaskType() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', UnknownTask) { }");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        MissingRequiredPropertyRule rule = new MissingRequiredPropertyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("no tasks in script returns no diagnostics")
    void emptyScript() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle", "println 'hello'");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        MissingRequiredPropertyRule rule = new MissingRequiredPropertyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }
}
