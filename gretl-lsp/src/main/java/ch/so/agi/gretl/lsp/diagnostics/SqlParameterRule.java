package ch.so.agi.gretl.lsp.diagnostics;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.metadata.PropertyMetadata;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import ch.so.agi.gretl.lsp.sql.SqlParameterExtractor;
import ch.so.agi.gretl.lsp.util.FileReferenceUtil;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SqlParameterRule implements GretlDiagnosticRule {

    private final SqlParameterExtractor sqlParameterExtractor = new SqlParameterExtractor();

    @Override
    public List<Diagnostic> evaluate(AnalysisInput input) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Path workspaceRoot = input.workspaceRoot();
        if (workspaceRoot == null) {
            return diagnostics;
        }

        GretlScript script = input.script();
        for (GretlTaskBlock task : script.tasks()) {
            Optional<TaskMetadata> taskMetaOpt = input.metadata().findTask(task.typeName().orElse(""));
            if (taskMetaOpt.isEmpty()) {
                continue;
            }

            if (!task.hasCall("sqlFiles") || !task.hasCall("sqlParameters")) {
                continue;
            }

            List<GretlDslCall> sqlFilesCalls = task.callsByName("sqlFiles");
            List<GretlDslCall> sqlParamsCalls = task.callsByName("sqlParameters");

            Set<String> usedParams = new HashSet<>();
            for (GretlDslCall filesCall : sqlFilesCalls) {
                List<String> paths = FileReferenceUtil.extractFilePaths(filesCall);
                for (String relativePath : paths) {
                    Path sqlFile = workspaceRoot.resolve(relativePath).normalize();
                    if (Files.exists(sqlFile) && Files.isReadable(sqlFile)) {
                        try {
                            String sqlText = Files.readString(sqlFile);
                            usedParams.addAll(sqlParameterExtractor.extractNames(sqlText));
                        } catch (IOException e) {
                            Diagnostic diag = new Diagnostic();
                            diag.setSeverity(DiagnosticSeverity.Warning);
                            diag.setRange(filesCall.fullRange());
                            diag.setMessage("Kann SQL-Datei nicht lesen: " + relativePath);
                            diag.setSource("gretl-lsp");
                            diagnostics.add(diag);
                        }
                    } else if (!relativePath.isEmpty()) {
                        Diagnostic diag = new Diagnostic();
                        diag.setSeverity(DiagnosticSeverity.Warning);
                        diag.setRange(filesCall.fullRange());
                        diag.setMessage("SQL-Datei nicht gefunden: " + relativePath);
                        diag.setSource("gretl-lsp");
                        diagnostics.add(diag);
                    }
                }
            }

            Set<String> providedParams = new HashSet<>();
            for (GretlDslCall paramsCall : sqlParamsCalls) {
                List<String> keys = FileReferenceUtil.extractMapKeys(paramsCall);
                providedParams.addAll(keys);
            }

            Set<String> missing = new HashSet<>(usedParams);
            missing.removeAll(providedParams);

            Set<String> unused = new HashSet<>(providedParams);
            unused.removeAll(usedParams);

            for (String missingParam : missing) {
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Warning);
                diag.setRange(task.bodyRange());
                diag.setMessage(DiagnosticCode.MISSING_SQL_PARAMETER.format(missingParam, task.name()));
                diag.setSource("gretl-lsp");
                diagnostics.add(diag);
            }

            for (String unusedParam : unused) {
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Information);
                diag.setRange(task.bodyRange());
                diag.setMessage(DiagnosticCode.UNUSED_SQL_PARAMETER.format(unusedParam, task.name()));
                diag.setSource("gretl-lsp");
                diagnostics.add(diag);
            }
        }
        return diagnostics;
    }
}
