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

class DefaultTaskRuleTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("warns for unknown default task")
    void warnsForUnknownDefaultTask() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "defaultTasks 'nonexistent'");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        DefaultTaskRule rule = new DefaultTaskRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        var diag = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Warning, diag.getSeverity());
        assertTrue(diag.getMessage().contains("nonexistent"));
        assertEquals("GRETL1102", diag.getCode().getLeft());
    }

    @Test
    @DisplayName("suggests closest match for default task")
    void suggestsClosestMatch() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('executeSql', SqlExecutor) { }\n" +
                        "defaultTasks 'executSql'");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        DefaultTaskRule rule = new DefaultTaskRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.get(0).getMessage().contains("Meintest"));
        assertTrue(diagnostics.get(0).getMessage().contains("executeSql"));
    }

    @Test
    @DisplayName("no diagnostic for valid default task")
    void noDiagnosticForValid() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('executeSql', SqlExecutor) { }\n" +
                        "defaultTasks 'executeSql'");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        DefaultTaskRule rule = new DefaultTaskRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("no diagnostics when no defaultTasks")
    void noDiagnosticsWhenNoDefaultTasks() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) { }");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        DefaultTaskRule rule = new DefaultTaskRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }
}
