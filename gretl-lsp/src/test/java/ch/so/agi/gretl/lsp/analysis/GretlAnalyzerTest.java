package ch.so.agi.gretl.lsp.analysis;

import ch.so.agi.gretl.lsp.diagnostics.*;
import ch.so.agi.gretl.lsp.document.LineIndex;
import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.MetadataLoader;
import ch.so.agi.gretl.lsp.scanner.HybridGretlScriptParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GretlAnalyzerTest {

    private static GretlMetadata metadata;
    private static GretlAnalyzer analyzer;

    @BeforeAll
    static void setUp() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
        List<GretlDiagnosticRule> rules = List.of(
                new MissingRequiredPropertyRule(),
                new UnknownPropertyRule(),
                new WrongArgumentCountRule(),
                new UnknownTaskTypeRule(),
                new UnknownDependencyRule(),
                new DefaultTaskRule(),
                new DuplicateTaskNameRule(),
                new LegacyDslRule()
        );
        HybridGretlScriptParser parser = new HybridGretlScriptParser();
        analyzer = new GretlAnalyzer(parser, metadata, rules);
    }

    @Test
    @DisplayName("produces missing required property diagnostic")
    void producesMissingRequiredProperty() {
        var doc = new TextDocument("test.gradle", "groovy", 1,
                "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n}",
                LineIndex.from("tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n}"));

        AnalysisResult result = analyzer.analyze(doc);

        assertTrue(result.diagnostics().stream()
                .anyMatch(d -> "GRETL1001".equals(d.getCode().getLeft())));
    }

    @Test
    @DisplayName("produces legacy DSL diagnostic")
    void producesLegacyDsl() {
        var doc = new TextDocument("test.gradle", "groovy", 1,
                "tasks.register('x', SqlExecutor) {\n    database = ['url', 'usr', 'pwd']\n}",
                LineIndex.from("tasks.register('x', SqlExecutor) {\n    database = ['url', 'usr', 'pwd']\n}"));

        AnalysisResult result = analyzer.analyze(doc);

        assertTrue(result.diagnostics().stream()
                .anyMatch(d -> "GRETL1201".equals(d.getCode().getLeft())));
    }

    @Test
    @DisplayName("produces unknown property diagnostic")
    void producesUnknownProperty() {
        var doc = new TextDocument("test.gradle", "groovy", 1,
                "tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n    sqlFile files('x.sql')\n}",
                LineIndex.from("tasks.register('x', SqlExecutor) {\n    database 'url', 'usr', 'pwd'\n    sqlFile files('x.sql')\n}"));

        AnalysisResult result = analyzer.analyze(doc);

        assertTrue(result.diagnostics().stream()
                .anyMatch(d -> "GRETL1002".equals(d.getCode().getLeft())));
    }

    @Test
    @DisplayName("produces duplicate task name diagnostic")
    void producesDuplicateTaskName() {
        var doc = new TextDocument("test.gradle", "groovy", 1,
                "tasks.register('x', SqlExecutor) { database 'url', 'usr', 'pwd' }\n" +
                        "tasks.register('x', SqlExecutor) { database 'url', 'usr', 'pwd' }",
                LineIndex.from("tasks.register('x', SqlExecutor) { database 'url', 'usr', 'pwd' }\n" +
                        "tasks.register('x', SqlExecutor) { database 'url', 'usr', 'pwd' }"));

        AnalysisResult result = analyzer.analyze(doc);

        assertTrue(result.diagnostics().stream()
                .anyMatch(d -> "GRETL1103".equals(d.getCode().getLeft())));
    }

    @Test
    @DisplayName("no diagnostics for clean script")
    void noDiagnosticsForCleanScript() {
        var doc = new TextDocument("test.gradle", "groovy", 1,
                "tasks.register('x', SqlExecutor) {\n" +
                        "    database 'url', 'usr', 'pwd'\n" +
                        "    sqlFiles files('x.sql')\n" +
                        "}",
                LineIndex.from("tasks.register('x', SqlExecutor) {\n" +
                        "    database 'url', 'usr', 'pwd'\n" +
                        "    sqlFiles files('x.sql')\n" +
                        "}"));

        AnalysisResult result = analyzer.analyze(doc);

        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    @DisplayName("analysis result contains script and document")
    void analysisResultContainsScriptAndDocument() {
        var doc = new TextDocument("test.gradle", "groovy", 1,
                "tasks.register('x', SqlExecutor) { database 'url', 'usr', 'pwd' }",
                LineIndex.from("tasks.register('x', SqlExecutor) { database 'url', 'usr', 'pwd' }"));

        AnalysisResult result = analyzer.analyze(doc);

        assertNotNull(result.script());
        assertSame(doc, result.document());
        assertEquals("test.gradle", result.script().uri());
    }
}
