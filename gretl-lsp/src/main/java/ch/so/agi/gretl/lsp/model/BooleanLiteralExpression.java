package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

public record BooleanLiteralExpression(boolean value, Range range, String sourceText) implements GretlExpression {
}
