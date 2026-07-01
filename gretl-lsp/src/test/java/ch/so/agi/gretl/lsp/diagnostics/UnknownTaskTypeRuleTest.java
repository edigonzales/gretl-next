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

class UnknownTaskTypeRuleTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("warns for unknown task type")
    void warnsForUnknownTaskType() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', UnknownCustomTask) { }");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownTaskTypeRule rule = new UnknownTaskTypeRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        var diag = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Warning, diag.getSeverity());
        assertTrue(diag.getMessage().contains("UnknownCustomTask"));
        assertEquals("GRETL1004", diag.getCode().getLeft());
    }

    @Test
    @DisplayName("no warning for known task type SqlExecutor")
    void noWarningForKnownType() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) { }");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownTaskTypeRule rule = new UnknownTaskTypeRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("no warning for known external Gradle task Copy")
    void noWarningForCopy() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', Copy) { }");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownTaskTypeRule rule = new UnknownTaskTypeRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("warns for unknown type when variable used as type")
    void warnsForVariableType() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "def t = SqlExecutor\n" +
                        "tasks.register('x', t) { }");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownTaskTypeRule rule = new UnknownTaskTypeRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.stream().anyMatch(d ->
                d.getSeverity() == DiagnosticSeverity.Warning &&
                        "GRETL1004".equals(d.getCode().getLeft())));
    }
}
