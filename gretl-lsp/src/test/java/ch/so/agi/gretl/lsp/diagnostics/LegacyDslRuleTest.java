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

class LegacyDslRuleTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("detects legacy assignment style with modern alternative")
    void detectsLegacyAssignment() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    database = ['url', 'usr', 'pwd']\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        LegacyDslRule rule = new LegacyDslRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        var diag = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Information, diag.getSeverity());
        assertTrue(diag.getMessage().contains("Alte GRETL-DSL"));
        assertEquals("GRETL1201", diag.getCode().getLeft());
    }

    @Test
    @DisplayName("no diagnostic for modern method-call style")
    void noDiagnosticForModernStyle() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        LegacyDslRule rule = new LegacyDslRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("detects legacy assignment for property with modern form")
    void detectsLegacyAssignmentWithModernForm() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    sqlParameters = someMap\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        LegacyDslRule rule = new LegacyDslRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        assertEquals("GRETL1201", diagnostics.get(0).getCode().getLeft());
    }
}
