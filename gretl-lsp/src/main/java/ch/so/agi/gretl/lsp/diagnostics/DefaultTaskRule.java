package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.model.DefaultTaskDeclaration;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ch.so.agi.gretl.lsp.diagnostics.UnknownPropertyRule.suggestClosest;

public final class DefaultTaskRule implements GretlDiagnosticRule {

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Set<String> knownTaskNames = input.script().taskNames();
        for (DefaultTaskDeclaration dtd : input.script().defaultTasks()) {
            if (knownTaskNames.contains(dtd.taskName())) {
                continue;
            }
            Optional<String> suggestion = suggestClosest(dtd.taskName(), knownTaskNames);
            String message;
            if (suggestion.isPresent()) {
                message = DiagnosticCode.UNKNOWN_DEFAULT_TASK.format(dtd.taskName(), suggestion.get());
            } else {
                message = DiagnosticCode.UNKNOWN_DEFAULT_TASK_NO_SUGGESTION.format(dtd.taskName());
            }
            Diagnostic diag = new Diagnostic();
            diag.setSeverity(DiagnosticSeverity.Warning);
            diag.setRange(dtd.range());
            diag.setCode(DiagnosticCode.UNKNOWN_DEFAULT_TASK.code());
            diag.setMessage(message);
            diag.setSource("gretl-lsp");
            diagnostics.add(diag);
        }
        return diagnostics;
    }
}
