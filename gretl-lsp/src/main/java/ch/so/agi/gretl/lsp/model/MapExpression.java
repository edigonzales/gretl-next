package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

import java.util.Collections;
import java.util.List;

public record MapExpression(List<MapEntryExpression> entries, Range range, String sourceText) implements GretlExpression {

    public MapExpression {
        entries = entries != null ? List.copyOf(entries) : List.of();
    }
}
