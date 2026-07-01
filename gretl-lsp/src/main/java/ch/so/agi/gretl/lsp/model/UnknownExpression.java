package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

public record UnknownExpression(Range range, String sourceText) implements GretlExpression {
}
