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

class UnknownDependencyRuleTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("warns for unknown dependency target")
    void warnsForUnknownDependency() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('executeSql', SqlExecutor) {\n" +
                        "    dependsOn 'importDat'\n" +
                        "}\n" +
                        "tasks.register('importData', SqlExecutor) {\n" +
                        "    dependsOn 'unknownTask'\n" +
                        "}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownDependencyRule rule = new UnknownDependencyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.stream().anyMatch(d ->
                d.getMessage().contains("importDat") &&
                        d.getMessage().contains("importData")));
        assertTrue(diagnostics.stream().anyMatch(d ->
                d.getMessage().contains("unknownTask") &&
                        !d.getMessage().contains("Meintest")));
    }

    @Test
    @DisplayName("no diagnostic for known dependency")
    void noDiagnosticForKnownDependency() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('importData', SqlExecutor) { }\n" +
                        "tasks.register('executeSql', SqlExecutor) {\n" +
                        "    dependsOn 'importData'\n" +
                        "}");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownDependencyRule rule = new UnknownDependencyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }

    @Test
    @DisplayName("no diagnostics when no dependencies")
    void noDiagnosticsForNoDependencies() {
        var parser = new GroovyAstGretlParser();
        var script = parser.parse("test.gradle",
                "tasks.register('x', SqlExecutor) { }");
        var doc = new TextDocument("test.gradle", "groovy", 1, "", LineIndex.from(""));

        UnknownDependencyRule rule = new UnknownDependencyRule();
        var diagnostics = rule.evaluate(new AnalysisInput(doc, script, metadata));

        assertTrue(diagnostics.isEmpty());
    }
}
