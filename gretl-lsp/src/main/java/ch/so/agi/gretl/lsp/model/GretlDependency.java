package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

public record GretlDependency(DependencyKind kind, String targetTaskName, Range range) {
}
