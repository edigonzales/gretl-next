package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

public record MapEntryExpression(String key, GretlExpression value, Range range) {
}
