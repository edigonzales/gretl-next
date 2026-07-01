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

class UnknownPropertyRuleTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("detects unknown property and suggests closest match")
    void suggestsClosestProperty() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    sqlFile files('x.sql')\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownPropertyRule rule = new UnknownPropertyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        var diag = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Warning, diag.getSeverity());
        assertTrue(diag.getMessage().contains("sqlFile"));
        assertTrue(diag.getMessage().contains("sqlFiles"));
        assertEquals("GRETL1002", diag.getCode().getLeft());
    }

    @Test
    @DisplayName("no diagnostic for known properties")
    void noDiagnosticForKnown() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n    sqlFiles files('x.sql')\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownPropertyRule rule = new UnknownPropertyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("no suggestion for far miss")
    void noSuggestionForFarMiss() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) {\n    completelyUnknownThing 'val'\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownPropertyRule rule = new UnknownPropertyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        var diag = diagnostics.get(0);
        assertFalse(diag.getMessage().contains("Meintest"));
    }

    @Test
    @DisplayName("levenshteinDistance computes correct distance")
    void levenshteinDistance() {
        assertEquals(0, UnknownPropertyRule.levenshteinDistance("abc", "abc"));
        assertEquals(1, UnknownPropertyRule.levenshteinDistance("abc", "ab"));
        assertEquals(1, UnknownPropertyRule.levenshteinDistance("sqlFile", "sqlFiles"));
        assertEquals(3, UnknownPropertyRule.levenshteinDistance("kitten", "sitting"));
    }
}
