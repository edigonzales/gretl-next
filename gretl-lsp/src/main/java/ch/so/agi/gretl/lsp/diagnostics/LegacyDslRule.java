package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.metadata.AcceptedForm;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.DslCallStyle;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class LegacyDslRule implements GretlDiagnosticRule {

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (GretlTaskBlock task : input.script().tasks()) {
            Optional<TaskMetadata> taskMeta = findTaskMetadata(input, task);
            if (taskMeta.isEmpty()) {
                continue;
            }
            for (GretlDslCall call : task.calls()) {
                if (call.style() != DslCallStyle.ASSIGNMENT) {
                    continue;
                }
                Optional<PropertyMetadata> propMeta = taskMeta.get().findProperty(call.name());
                if (propMeta.isEmpty()) {
                    continue;
                }
                Optional<AcceptedForm> modernForm = findModernForm(propMeta.get());
                if (modernForm.isEmpty()) {
                    continue;
                }
                String message = DiagnosticCode.LEGACY_DSL.format(modernForm.get().signature());
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Information);
                diag.setRange(call.fullRange());
                diag.setCode(DiagnosticCode.LEGACY_DSL.code());
                diag.setMessage(message);
                diag.setSource("gretl-lsp");
                diagnostics.add(diag);
            }
        }
        return diagnostics;
    }

    private Optional<TaskMetadata> findTaskMetadata(AnalysisInput input, GretlTaskBlock task) {
        return task.typeName().flatMap(tn -> input.metadata().findTask(tn));
    }

    private Optional<AcceptedForm> findModernForm(PropertyMetadata propMeta) {
        for (AcceptedForm form : propMeta.acceptedForms()) {
            if ("method-call".equals(form.style()) && !form.legacy()) {
                return Optional.of(form);
            }
        }
        return Optional.empty();
    }
}
