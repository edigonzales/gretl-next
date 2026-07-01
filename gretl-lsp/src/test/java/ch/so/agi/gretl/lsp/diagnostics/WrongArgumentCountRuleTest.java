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

class WrongArgumentCountRuleTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("detects wrong argument count for database method call")
    void detectsWrongArgumentCount() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr'\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        WrongArgumentCountRule rule = new WrongArgumentCountRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        var diag = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Warning, diag.getSeverity());
        assertTrue(diag.getMessage().contains("database"));
        assertTrue(diag.getMessage().contains("3"));
        assertEquals("GRETL1003", diag.getCode().getLeft());
    }

    @Test
    @DisplayName("no diagnostic when argument count matches")
    void noDiagnosticWhenMatching() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        WrongArgumentCountRule rule = new WrongArgumentCountRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("ignores assignment-style calls")
    void ignoresAssignmentStyle() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    database = ['url', 'usr', 'pwd']\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        WrongArgumentCountRule rule = new WrongArgumentCountRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }
}
