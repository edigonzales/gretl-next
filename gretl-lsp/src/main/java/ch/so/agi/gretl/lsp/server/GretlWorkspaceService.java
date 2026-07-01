package ch.so.agi.gretl.lsp.server;

import ch.so.agi.gretl.lsp.analysis.AnalysisResult;
import ch.so.agi.gretl.lsp.analysis.GretlAnalyzer;
import ch.so.agi.gretl.lsp.document.DocumentStore;
import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.overview.GretlOverview;
import ch.so.agi.gretl.lsp.overview.MissingParam;
import ch.so.agi.gretl.lsp.overview.OverviewDiagnostic;
import ch.so.agi.gretl.lsp.overview.OverviewTask;
import ch.so.agi.gretl.lsp.overview.TaskGraph;
import ch.so.agi.gretl.lsp.overview.TaskGraphEdge;
import ch.so.agi.gretl.lsp.overview.TaskGraphNode;
import ch.so.agi.gretl.lsp.overview.TaskGraphProblem;
import ch.so.agi.gretl.lsp.overview.TaskOverviewService;
import ch.so.agi.gretl.lsp.overview.UnusedParam;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class GretlWorkspaceService implements WorkspaceService {

    private final ServerLogger logger;
    private final DocumentStore documentStore;
    private final GretlAnalyzer analyzer;
    private Path workspaceRoot;

    GretlWorkspaceService(ServerLogger logger, DocumentStore documentStore,
                           GretlAnalyzer analyzer) {
        this.logger = logger;
        this.documentStore = documentStore;
        this.analyzer = analyzer;
    }

    void setWorkspaceRoot(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        logger.debug("didChangeConfiguration");
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        logger.debug("didChangeWatchedFiles");
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        return CompletableFutures.computeAsync(cancelToken -> {
            String command = params.getCommand();
            logger.info("executeCommand: " + command);

            if ("gretl.getOverview".equals(command)) {
                return executeGetOverview(params.getArguments());
            }

            logger.warn("Unknown command: " + command);
            return null;
        });
    }

    private Object executeGetOverview(List<Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of("error", "Missing argument: uri");
        }

        Object firstArg = arguments.get(0);
        String uri;
        if (firstArg instanceof String) {
            uri = (String) firstArg;
        } else if (firstArg instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> argMap = (Map<String, Object>) firstArg;
            uri = (String) argMap.getOrDefault("uri", "");
        } else {
            return Map.of("error", "Invalid argument: expected uri string or object with uri field");
        }

        if (uri == null || uri.isEmpty()) {
            return Map.of("error", "Missing uri argument");
        }

        Optional<TextDocument> docOpt = documentStore.get(uri);
        if (docOpt.isEmpty()) {
            return Map.of("error", "Document not found: " + uri);
        }

        TextDocument document = docOpt.get();
        AnalysisResult analysis = analyzer.analyze(document);

        TaskOverviewService overviewService = new TaskOverviewService();
        GretlOverview overview = overviewService.overview(analysis, workspaceRoot);

        return overviewToMap(overview);
    }

    private Map<String, Object> overviewToMap(GretlOverview overview) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uri", overview.uri());
        result.put("tasks", tasksToMaps(overview.tasks()));
        result.put("graph", graphToMap(overview.graph()));
        result.put("diagnostics", diagnosticsToMaps(overview.diagnostics()));
        result.put("sqlParameterReport", sqlParamReportToMap(overview.sqlParameterReport()));
        return result;
    }

    private List<Map<String, Object>> tasksToMaps(List<OverviewTask> tasks) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OverviewTask task : tasks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", task.name());
            m.put("typeName", task.typeName());
            m.put("line", task.line());
            m.put("allRequiredPresent", task.allRequiredPresent());
            m.put("requiredProperties", task.requiredProperties());
            list.add(m);
        }
        return list;
    }

    private Map<String, Object> graphToMap(TaskGraph graph) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nodes", nodesToMaps(graph.nodes()));
        m.put("edges", edgesToMaps(graph.edges()));
        m.put("problems", problemsToMaps(graph.problems()));
        return m;
    }

    private List<Map<String, Object>> nodesToMaps(List<TaskGraphNode> nodes) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (TaskGraphNode node : nodes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskName", node.taskName());
            m.put("taskType", node.taskType());
            m.put("range", rangeToMap(node.range()));
            m.put("status", node.status().name());
            m.put("missingRequiredProperties", node.missingRequiredProperties());
            m.put("diagnosticCount", node.diagnosticCount());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> edgesToMaps(List<TaskGraphEdge> edges) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (TaskGraphEdge edge : edges) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fromTask", edge.fromTask());
            m.put("toTask", edge.toTask());
            m.put("kind", edge.kind().name());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> problemsToMaps(List<TaskGraphProblem> problems) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (TaskGraphProblem problem : problems) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("message", problem.message());
            m.put("severity", problem.severity());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> diagnosticsToMaps(List<OverviewDiagnostic> diagnostics) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OverviewDiagnostic diag : diagnostics) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("message", diag.message());
            m.put("severity", diag.severity());
            m.put("range", rangeToMap(diag.range()));
            m.put("taskName", diag.taskName());
            list.add(m);
        }
        return list;
    }

    private Map<String, Object> rangeToMap(Range range) {
        if (range == null) {
            return Map.of("start", Map.of("line", 0, "character", 0),
                    "end", Map.of("line", 0, "character", 0));
        }
        return Map.of(
                "start", Map.of("line", range.getStart().getLine(),
                        "character", range.getStart().getCharacter()),
                "end", Map.of("line", range.getEnd().getLine(),
                        "character", range.getEnd().getCharacter())
        );
    }

    private Map<String, Object> sqlParamReportToMap(
            ch.so.agi.gretl.lsp.overview.SqlParameterReport report) {
        if (report == null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sqlFiles", List.of());
            m.put("missingParams", List.of());
            m.put("unusedParams", List.of());
            return m;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sqlFiles", report.sqlFiles());

        List<Map<String, Object>> missingList = new ArrayList<>();
        for (MissingParam mp : report.missingParams()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("paramName", mp.paramName());
            entry.put("sqlFile", mp.sqlFile());
            missingList.add(entry);
        }
        m.put("missingParams", missingList);

        List<Map<String, Object>> unusedList = new ArrayList<>();
        for (UnusedParam up : report.unusedParams()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("paramName", up.paramName());
            entry.put("taskName", up.taskName());
            unusedList.add(entry);
        }
        m.put("unusedParams", unusedList);

        return m;
    }
}
