package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.model.GretlDependency;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ch.so.agi.gretl.lsp.diagnostics.UnknownPropertyRule.levenshteinDistance;
import static ch.so.agi.gretl.lsp.diagnostics.UnknownPropertyRule.suggestClosest;

public final class UnknownDependencyRule implements GretlDiagnosticRule {

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Set<String> knownTaskNames = input.script().taskNames();
        for (GretlTaskBlock task : input.script().tasks()) {
            for (GretlDependency dep : task.dependencies()) {
                if (knownTaskNames.contains(dep.targetTaskName())) {
                    continue;
                }
                Optional<String> suggestion = suggestClosest(dep.targetTaskName(), knownTaskNames);
                String message;
                if (suggestion.isPresent()) {
                    message = DiagnosticCode.UNKNOWN_DEPENDENCY.format(
                            dep.targetTaskName(), suggestion.get());
                } else {
                    message = DiagnosticCode.UNKNOWN_DEPENDENCY_NO_SUGGESTION.format(dep.targetTaskName());
                }
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Warning);
                diag.setRange(dep.range());
                diag.setCode(DiagnosticCode.UNKNOWN_DEPENDENCY.code());
                diag.setMessage(message);
                diag.setSource("gretl-lsp");
                diagnostics.add(diag);
            }
        }
        return diagnostics;
    }
}
