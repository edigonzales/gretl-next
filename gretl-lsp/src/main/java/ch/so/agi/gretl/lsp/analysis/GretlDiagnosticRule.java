package ch.so.agi.gretl.lsp.analysis;

import org.eclipse.lsp4j.Diagnostic;

import java.util.List;

public interface GretlDiagnosticRule {
    List<Diagnostic> evaluate(AnalysisInput input);
}
