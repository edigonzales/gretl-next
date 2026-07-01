package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

public record DefaultTaskDeclaration(String taskName, Range range) {
}
