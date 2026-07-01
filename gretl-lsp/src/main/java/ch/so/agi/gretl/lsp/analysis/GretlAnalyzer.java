package ch.so.agi.gretl.lsp.analysis;

import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.parser.GretlScriptParser;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GretlAnalyzer {

    private final GretlScriptParser parser;
    private final GretlMetadata metadata;
    private final List<GretlDiagnosticRule> rules;

    public GretlAnalyzer(GretlScriptParser parser, GretlMetadata metadata,
                         List<GretlDiagnosticRule> rules) {
        this.parser = parser;
        this.metadata = metadata;
        this.rules = List.copyOf(rules);
    }

    public AnalysisResult analyze(TextDocument document) {
        GretlScript script = parse(document.uri(), document.text());
        AnalysisInput input = new AnalysisInput(document, script, metadata);
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (GretlDiagnosticRule rule : rules) {
            diagnostics.addAll(rule.evaluate(input));
        }
        if (!script.parseProblems().isEmpty()) {
            for (var problem : script.parseProblems()) {
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(problem.isError()
                        ? org.eclipse.lsp4j.DiagnosticSeverity.Error
                        : org.eclipse.lsp4j.DiagnosticSeverity.Warning);
                diag.setRange(problem.range());
                diag.setMessage(problem.message());
                diag.setSource("gretl-lsp");
                diagnostics.add(diag);
            }
        }
        return new AnalysisResult(document, script, metadata, diagnostics);
    }

    public GretlScript parse(String uri, String text) {
        return parser.parse(uri, text);
    }
}
