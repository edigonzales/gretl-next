package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

import java.util.Collections;
import java.util.List;

public record ListExpression(List<GretlExpression> values, Range range, String sourceText) implements GretlExpression {

    public ListExpression {
        values = values != null ? List.copyOf(values) : List.of();
    }
}
