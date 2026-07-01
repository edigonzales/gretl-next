package ch.so.agi.gretl.lsp.overview;

import ch.so.agi.gretl.lsp.analysis.AnalysisInput;
import ch.so.agi.gretl.lsp.analysis.AnalysisResult;
import ch.so.agi.gretl.lsp.metadata.TaskMetadata;
import ch.so.agi.gretl.lsp.model.GretlDslCall;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import ch.so.agi.gretl.lsp.sql.SqlParameterExtractor;
import ch.so.agi.gretl.lsp.util.FileReferenceUtil;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class TaskOverviewService {

    private final TaskGraphBuilder graphBuilder = new TaskGraphBuilder();
    private final SqlParameterExtractor sqlParameterExtractor = new SqlParameterExtractor();

    public GretlOverview overview(AnalysisResult analysis, Path workspaceRoot) {
        String uri = analysis.document().uri();
        List<OverviewTask> tasks = buildOverviewTasks(analysis);
        List<OverviewDiagnostic> diagnostics = buildOverviewDiagnostics(analysis);
        TaskGraph graph = graphBuilder.build(analysis.script(), diagnostics);
        SqlParameterReport sqlParamReport = buildSqlParameterReport(analysis, workspaceRoot);

        return new GretlOverview(uri, tasks, graph, diagnostics, sqlParamReport);
    }

    private List<OverviewTask> buildOverviewTasks(AnalysisResult analysis) {
        List<OverviewTask> result = new ArrayList<>();
        for (GretlTaskBlock task : analysis.script().tasks()) {
            String typeName = task.typeName().orElse("unknown");
            int line = task.fullRange().getStart().getLine();
            List<String> requiredProps = getRequiredPropertyNames(task.typeName(), analysis);
            boolean allPresent = true;
            for (String req : requiredProps) {
                if (!task.hasCall(req)) {
                    allPresent = false;
                    break;
                }
            }
            result.add(new OverviewTask(task.name(), typeName, line, allPresent, requiredProps));
        }
        return result;
    }

    private List<String> getRequiredPropertyNames(Optional<String> typeName, AnalysisResult analysis) {
        if (typeName.isEmpty()) {
            return List.of();
        }
        Optional<TaskMetadata> taskMeta = analysis.metadata().findTask(typeName.get());
        if (taskMeta.isEmpty()) {
            return List.of();
        }
        return taskMeta.get().requiredProperties().stream()
                .map(p -> p.name())
                .toList();
    }

    private List<OverviewDiagnostic> buildOverviewDiagnostics(AnalysisResult analysis) {
        List<OverviewDiagnostic> result = new ArrayList<>();
        for (Diagnostic diag : analysis.diagnostics()) {
            String severity = switch (diag.getSeverity()) {
                case Error -> "Error";
                case Warning -> "Warning";
                case Information -> "Info";
                case Hint -> "Hint";
                default -> "Unknown";
            };
            String taskName = findTaskNameForRange(diag.getRange(), analysis);
            result.add(new OverviewDiagnostic(
                    diag.getMessage(),
                    severity,
                    diag.getRange(),
                    taskName
            ));
        }
        return result;
    }

    private String findTaskNameForRange(Range range, AnalysisResult analysis) {
        if (range == null) {
            return "";
        }
        for (GretlTaskBlock task : analysis.script().tasks()) {
            if (isInside(range, task.fullRange())) {
                return task.name();
            }
        }
        return "";
    }

    private boolean isInside(Range inner, Range outer) {
        return inner.getStart().getLine() >= outer.getStart().getLine()
                && inner.getStart().getCharacter() >= outer.getStart().getCharacter()
                && inner.getEnd().getLine() <= outer.getEnd().getLine()
                && inner.getEnd().getCharacter() <= outer.getEnd().getCharacter();
    }

    private SqlParameterReport buildSqlParameterReport(AnalysisResult analysis, Path workspaceRoot) {
        List<String> sqlFiles = new ArrayList<>();
        List<MissingParam> missingParams = new ArrayList<>();
        List<UnusedParam> unusedParams = new ArrayList<>();

        if (workspaceRoot == null) {
            return new SqlParameterReport(sqlFiles, missingParams, unusedParams);
        }

        for (GretlTaskBlock task : analysis.script().tasks()) {
            if (!task.hasCall("sqlFiles") || !task.hasCall("sqlParameters")) {
                continue;
            }

            Optional<TaskMetadata> taskMetaOpt = analysis.metadata().findTask(
                    task.typeName().orElse(""));
            if (taskMetaOpt.isEmpty()) {
                continue;
            }

            List<GretlDslCall> sqlFilesCalls = task.callsByName("sqlFiles");
            List<GretlDslCall> sqlParamsCalls = task.callsByName("sqlParameters");

            Set<String> usedParams = new HashSet<>();
            for (GretlDslCall filesCall : sqlFilesCalls) {
                List<String> paths = FileReferenceUtil.extractFilePaths(filesCall);
                for (String relativePath : paths) {
                    if (!relativePath.isEmpty()) {
                        sqlFiles.add(relativePath);
                    }
                    Path sqlFile = workspaceRoot.resolve(relativePath).normalize();
                    if (Files.exists(sqlFile) && Files.isReadable(sqlFile)) {
                        try {
                            String sqlText = Files.readString(sqlFile);
                            usedParams.addAll(sqlParameterExtractor.extractNames(sqlText));
                        } catch (IOException ignored) {
                        }
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
            for (String paramName : missing) {
                for (GretlDslCall filesCall : sqlFilesCalls) {
                    List<String> paths = FileReferenceUtil.extractFilePaths(filesCall);
                    for (String path : paths) {
                        missingParams.add(new MissingParam(paramName, path));
                    }
                }
            }

            Set<String> unused = new HashSet<>(providedParams);
            unused.removeAll(usedParams);
            for (String paramName : unused) {
                unusedParams.add(new UnusedParam(paramName, task.name()));
            }
        }

        return new SqlParameterReport(sqlFiles, missingParams, unusedParams);
    }
}
