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

public final class WrongArgumentCountRule implements GretlDiagnosticRule {

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (GretlTaskBlock task : input.script().tasks()) {
            Optional<TaskMetadata> taskMeta = findTaskMetadata(input, task);
            if (taskMeta.isEmpty()) {
                continue;
            }
            for (GretlDslCall call : task.calls()) {
                if (call.style() != DslCallStyle.METHOD_CALL) {
                    continue;
                }
                Optional<PropertyMetadata> propMeta = taskMeta.get().findProperty(call.name());
                if (propMeta.isEmpty()) {
                    continue;
                }
                Optional<AcceptedForm> matchingForm = findMatchingForm(call, propMeta.get());
                if (matchingForm.isEmpty()) {
                    continue;
                }
                AcceptedForm form = matchingForm.get();
                if (form.argumentCount() == null) {
                    continue;
                }
                int expected = form.argumentCount();
                int actual = call.arguments().size();
                if (actual != expected) {
                    String message = DiagnosticCode.WRONG_ARGUMENT_COUNT.format(
                            call.name(), expected, form.signature());
                    Diagnostic diag = new Diagnostic();
                    diag.setSeverity(DiagnosticSeverity.Warning);
                    diag.setRange(call.fullRange());
                    diag.setCode(DiagnosticCode.WRONG_ARGUMENT_COUNT.code());
                    diag.setMessage(message);
                    diag.setSource("gretl-lsp");
                    diagnostics.add(diag);
                }
            }
        }
        return diagnostics;
    }

    private Optional<TaskMetadata> findTaskMetadata(AnalysisInput input, GretlTaskBlock task) {
        return task.typeName().flatMap(tn -> input.metadata().findTask(tn));
    }

    private Optional<AcceptedForm> findMatchingForm(GretlDslCall call, PropertyMetadata propMeta) {
        String targetStyle = "method-call";
        for (AcceptedForm form : propMeta.acceptedForms()) {
            if (targetStyle.equals(form.style()) && !form.legacy()) {
                return Optional.of(form);
            }
        }
        if (!propMeta.acceptedForms().isEmpty()) {
            return Optional.of(propMeta.acceptedForms().get(0));
        }
        return Optional.empty();
    }
}
