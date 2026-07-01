package ch.so.agi.gretl.lsp.analysis;

import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.model.GretlScript;
import org.eclipse.lsp4j.Diagnostic;

import java.util.Collections;
import java.util.List;

public record AnalysisResult(TextDocument document, GretlScript script, List<Diagnostic> diagnostics) {

    public AnalysisResult {
        diagnostics = diagnostics != null ? List.copyOf(diagnostics) : List.of();
    }
}
