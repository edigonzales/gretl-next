package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

public record GretlVariableDeclaration(String name, GretlExpression initializer, Range range) {
}
