package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

public record GretlParseProblem(String message, Range range, boolean isError) {
}
