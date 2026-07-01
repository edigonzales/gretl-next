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

class DuplicateTaskNameRuleTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("detects duplicate task names")
    void detectsDuplicateTaskName() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('importData', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n}\n" +
                        "tasks.register('importData', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        DuplicateTaskNameRule rule = new DuplicateTaskNameRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(1, diagnostics.size());
        var diag = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.Warning, diag.getSeverity());
        assertTrue(diag.getMessage().contains("importData"));
        assertEquals("GRETL1103", diag.getCode().getLeft());
    }

    @Test
    @DisplayName("flags second duplicate but not first")
    void flagsOneDuplicate() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('a', SqlExecutor) { }\n" +
                        "tasks.register('a', SqlExecutor) { }\n" +
                        "tasks.register('a', SqlExecutor) { }");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        DuplicateTaskNameRule rule = new DuplicateTaskNameRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertEquals(2, diagnostics.size());
    }

    @Test
    @DisplayName("no diagnostic for unique names")
    void noDiagnosticForUniqueNames() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('a', SqlExecutor) { }\n" +
                        "tasks.register('b', SqlExecutor) { }");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        DuplicateTaskNameRule rule = new DuplicateTaskNameRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }
}
