package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class UnknownTaskTypeRule implements GretlDiagnosticRule {

    private static final Set<String> KNOWN_EXTERNAL = Set.of(
            "Copy", "Delete", "Sync", "Zip", "Exec", "JavaExec"
    );

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (GretlTaskBlock task : input.script().tasks()) {
            if (task.typeName().isEmpty()) {
                String message = DiagnosticCode.DYNAMIC_TASK_TYPE.format();
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Information);
                diag.setRange(task.nameRange());
                diag.setCode(DiagnosticCode.DYNAMIC_TASK_TYPE.code());
                diag.setMessage(message);
                diag.setSource("gretl-lsp");
                diagnostics.add(diag);
                continue;
            }
            String typeName = task.typeName().get();
            if (KNOWN_EXTERNAL.contains(typeName)) {
                continue;
            }
            if (input.metadata().findTask(typeName).isEmpty()) {
                String message = DiagnosticCode.UNKNOWN_TASK_TYPE.format(typeName);
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Warning);
                diag.setRange(task.typeRange() != null ? task.typeRange() : task.nameRange());
                diag.setCode(DiagnosticCode.UNKNOWN_TASK_TYPE.code());
                diag.setMessage(message);
                diag.setSource("gretl-lsp");
                diagnostics.add(diag);
            }
        }
        return diagnostics;
    }
}
