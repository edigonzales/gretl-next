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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
                List<AcceptedForm> methodForms = findMethodCallForms(propMeta.get());
                if (methodForms.isEmpty()) {
                    continue;
                }
                int actual = call.arguments().size();
                if (methodForms.stream().anyMatch(f -> f.argumentCount() != null && f.argumentCount() == actual)) {
                    continue;
                }

                String message = buildWrongArgumentMessage(call.name(), methodForms);
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Warning);
                diag.setRange(call.fullRange());
                diag.setCode(DiagnosticCode.WRONG_ARGUMENT_COUNT.code());
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

    private List<AcceptedForm> findMethodCallForms(PropertyMetadata propMeta) {
        List<AcceptedForm> result = new ArrayList<>();
        for (AcceptedForm form : propMeta.acceptedForms()) {
            if ("method-call".equals(form.style()) && !form.legacy()) {
                result.add(form);
            }
        }
        return result;
    }

    private String buildWrongArgumentMessage(String name, List<AcceptedForm> forms) {
        List<AcceptedForm> counted = forms.stream()
                .filter(f -> f.argumentCount() != null)
                .toList();
        if (counted.isEmpty()) {
            return DiagnosticCode.WRONG_ARGUMENT_COUNT.code() + ": `" + name + "` akzeptiert keine Argumente.";
        }
        if (counted.size() == 1) {
            return DiagnosticCode.WRONG_ARGUMENT_COUNT.format(
                    name, counted.get(0).argumentCount(), counted.get(0).signature());
        }
        List<Integer> counts = counted.stream()
                .map(AcceptedForm::argumentCount)
                .distinct()
                .sorted()
                .toList();
        String countsStr = counts.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" oder "));
        String sigs = counted.stream()
                .map(AcceptedForm::signature)
                .collect(Collectors.joining(", "));
        return DiagnosticCode.WRONG_ARGUMENT_COUNT.code() + ": `" + name
                + "` erwartet " + countsStr + " Argumente: " + sigs + ".";
    }
}
