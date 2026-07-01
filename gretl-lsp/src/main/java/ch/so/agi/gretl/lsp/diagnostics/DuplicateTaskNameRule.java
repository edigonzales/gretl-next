package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DuplicateTaskNameRule implements GretlDiagnosticRule {

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (GretlTaskBlock task : input.script().tasks()) {
            seen.merge(task.name(), 1, Integer::sum);
        }
        Map<String, Integer> counted = new HashMap<>(seen);
        for (GretlTaskBlock task : input.script().tasks()) {
            if (counted.get(task.name()) > 1) {
                counted.put(task.name(), counted.get(task.name()) - 1);
                if (counted.get(task.name()) > 0) {
                    String message = DiagnosticCode.DUPLICATE_TASK_NAME.format(task.name());
                    Diagnostic diag = new Diagnostic();
                    diag.setSeverity(DiagnosticSeverity.Warning);
                    diag.setRange(task.nameRange());
                    diag.setCode(DiagnosticCode.DUPLICATE_TASK_NAME.code());
                    diag.setMessage(message);
                    diag.setSource("gretl-lsp");
                    diagnostics.add(diag);
                }
            }
        }
        return diagnostics;
    }
}
