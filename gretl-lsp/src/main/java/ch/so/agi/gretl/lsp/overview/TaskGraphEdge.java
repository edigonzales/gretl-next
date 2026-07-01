package ch.so.agi.gretl.lsp.overview;

import ch.so.agi.gretl.lsp.model.DependencyKind;

public record TaskGraphEdge(String fromTask, String toTask, DependencyKind kind) {
}
