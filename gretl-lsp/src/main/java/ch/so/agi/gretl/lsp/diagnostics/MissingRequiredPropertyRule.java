package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class MissingRequiredPropertyRule implements GretlDiagnosticRule {

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (GretlTaskBlock task : input.script().tasks()) {
            Optional<TaskMetadata> taskMeta = findTaskMetadata(input, task);
            if (taskMeta.isEmpty()) {
                continue;
            }
            Set<String> presentCallNames = presentCallNames(task);
            for (PropertyMetadata prop : taskMeta.get().requiredProperties()) {
                if (!presentCallNames.contains(prop.name())) {
                    String message = DiagnosticCode.MISSING_REQUIRED_PROPERTY.format(prop.name(), task.name());
                    Diagnostic diag = new Diagnostic();
                    diag.setSeverity(DiagnosticSeverity.Error);
                    diag.setRange(task.nameRange());
                    diag.setCode(DiagnosticCode.MISSING_REQUIRED_PROPERTY.code());
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

    private Set<String> presentCallNames(GretlTaskBlock task) {
        return task.calls().stream()
                .map(c -> c.name())
                .collect(Collectors.toSet());
    }
}
