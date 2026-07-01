package ch.so.agi.gretl.lsp.overview;

import ch.so.agi.gretl.lsp.model.DependencyKind;
import ch.so.agi.gretl.lsp.model.GretlDependency;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.model.GretlTaskBlock;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TaskGraphBuilder {

    private static final Map<DependencyKind, String> EDGE_LABELS = createEdgeLabels();

    private static Map<DependencyKind, String> createEdgeLabels() {
        Map<DependencyKind, String> labels = new EnumMap<>(DependencyKind.class);
        labels.put(DependencyKind.DEPENDS_ON, "dependsOn");
        labels.put(DependencyKind.FINALIZED_BY, "finalizedBy");
        labels.put(DependencyKind.MUST_RUN_AFTER, "mustRunAfter");
        labels.put(DependencyKind.SHOULD_RUN_AFTER, "shouldRunAfter");
        return Map.copyOf(labels);
    }

    public TaskGraph build(GretlScript script) {
        return build(script, List.of());
    }

    public TaskGraph build(GretlScript script, List<OverviewDiagnostic> diagnostics) {
        List<TaskGraphNode> nodes = new ArrayList<>();
        List<TaskGraphEdge> edges = new ArrayList<>();
        List<TaskGraphProblem> problems = new ArrayList<>();
        Set<String> taskNames = script.taskNames();
        Map<String, List<OverviewDiagnostic>> diagsByTask = groupDiagnosticsByTask(diagnostics);

        for (GretlTaskBlock task : script.tasks()) {
            List<OverviewDiagnostic> taskDiags = diagsByTask.getOrDefault(task.name(), List.of());
            boolean hasErrors = taskDiags.stream()
                    .anyMatch(d -> "Error".equals(d.severity()));
            boolean hasWarnings = taskDiags.stream()
                    .anyMatch(d -> "Warning".equals(d.severity()));

            NodeStatus status;
            if (hasErrors) {
                status = NodeStatus.ERROR;
            } else if (hasWarnings) {
                status = NodeStatus.WARNING;
            } else {
                status = NodeStatus.OK;
            }

            List<String> missingRequired = taskDiags.stream()
                    .filter(d -> d.message().contains("Pflichtparameter"))
                    .map(d -> {
                        String msg = d.message();
                        int start = msg.indexOf("`");
                        int end = msg.indexOf("`", start + 1);
                        if (start >= 0 && end > start) {
                            return msg.substring(start + 1, end);
                        }
                        return "";
                    })
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList();

            nodes.add(new TaskGraphNode(
                    task.name(),
                    task.typeName().orElse("unknown"),
                    task.fullRange(),
                    status,
                    missingRequired,
                    taskDiags.size()
            ));

            for (GretlDependency dep : task.dependencies()) {
                String target = dep.targetTaskName();
                if (!taskNames.contains(target)) {
                    problems.add(new TaskGraphProblem(
                            "Task \"" + task.name() + "\" verweist auf unbekannten Task \""
                                    + target + "\" (" + EDGE_LABELS.getOrDefault(dep.kind(), dep.kind().name()) + ")",
                            "Warning"
                    ));
                }
                edges.add(new TaskGraphEdge(task.name(), target, dep.kind()));
            }
        }

        return new TaskGraph(List.copyOf(nodes), List.copyOf(edges), List.copyOf(problems));
    }

    private static Map<String, List<OverviewDiagnostic>> groupDiagnosticsByTask(
            List<OverviewDiagnostic> diagnostics) {
        Map<String, List<OverviewDiagnostic>> grouped = new java.util.HashMap<>();
        for (OverviewDiagnostic diag : diagnostics) {
            if (diag.taskName() != null && !diag.taskName().isEmpty()) {
                grouped.computeIfAbsent(diag.taskName(), k -> new ArrayList<>()).add(diag);
            }
        }
        return grouped;
    }
}
