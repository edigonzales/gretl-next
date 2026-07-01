package ch.so.agi.gretl.lsp.overview;

import org.eclipse.lsp4j.Range;

import java.util.List;

public record TaskGraphNode(String taskName, String taskType, Range range, NodeStatus status,
                             List<String> missingRequiredProperties, int diagnosticCount) {

    public TaskGraphNode {
        missingRequiredProperties = missingRequiredProperties != null
                ? List.copyOf(missingRequiredProperties) : List.of();
    }
}
