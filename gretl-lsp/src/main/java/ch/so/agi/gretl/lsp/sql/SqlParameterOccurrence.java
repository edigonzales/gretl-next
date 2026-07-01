package ch.so.agi.gretl.lsp.sql;

import org.eclipse.lsp4j.Range;

public record SqlParameterOccurrence(String name, Range range) {
}
