package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlImport;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MissingImportRule implements GretlDiagnosticRule {

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        List<GretlImport> imports = input.imports();

        for (GretlTaskBlock task : input.script().tasks()) {
            if (task.typeName().isEmpty()) {
                continue;
            }
            String typeName = task.typeName().get();
            if (isFullyQualified(task, input)) {
                continue;
            }

            Optional<TaskMetadata> taskMeta = input.metadata().findTask(typeName);
            if (taskMeta.isEmpty()) {
                continue;
            }

            String fqn = taskMeta.get().qualifiedClassName();
            if (isImported(fqn, imports)) {
                continue;
            }

            Diagnostic diag = new Diagnostic();
            diag.setSeverity(DiagnosticSeverity.Information);
            diag.setRange(task.typeRange());
            diag.setCode(DiagnosticCode.MISSING_IMPORT.code());
            diag.setMessage(DiagnosticCode.MISSING_IMPORT.format(typeName));
            diag.setSource("gretl-lsp");
            diagnostics.add(diag);
        }

        return diagnostics;
    }

    private boolean isImported(String fqn, List<GretlImport> imports) {
        for (GretlImport imp : imports) {
            if (imp.isStarImport()) {
                if (fqn.startsWith(imp.packagePrefix() + ".")) {
                    return true;
                }
            } else if (imp.importPath().equals(fqn)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFullyQualified(GretlTaskBlock task, AnalysisInput input) {
        if (task.typeRange() == null) {
            return false;
        }
        int startOffset = input.document().lineIndex().offsetAt(task.typeRange().getStart());
        int endOffset = input.document().lineIndex().offsetAt(task.typeRange().getEnd());
        String sourceText = input.document().text().substring(startOffset, endOffset);
        return sourceText.contains(".");
    }
}
