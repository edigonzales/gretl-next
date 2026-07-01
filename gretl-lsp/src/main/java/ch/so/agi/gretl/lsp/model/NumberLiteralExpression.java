package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

public record NumberLiteralExpression(String value, Range range, String sourceText) implements GretlExpression {
}
