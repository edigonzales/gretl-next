package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

import java.util.Optional;

public record GretlArgument(GretlExpression expression, Range range, Optional<String> name) {
}
