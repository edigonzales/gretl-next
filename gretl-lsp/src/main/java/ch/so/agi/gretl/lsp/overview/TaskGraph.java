package ch.so.agi.gretl.lsp.overview;

import java.util.List;
import java.util.Optional;

public record TaskGraph(List<TaskGraphNode> nodes, List<TaskGraphEdge> edges,
                         List<TaskGraphProblem> problems) {

    public TaskGraph {
        nodes = nodes != null ? List.copyOf(nodes) : List.of();
        edges = edges != null ? List.copyOf(edges) : List.of();
        problems = problems != null ? List.copyOf(problems) : List.of();
    }

    public Optional<TaskGraphNode> findNode(String taskName) {
        return nodes.stream().filter(n -> n.taskName().equals(taskName)).findFirst();
    }
}
