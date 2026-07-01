package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

import java.util.Collections;
import java.util.List;

public record MethodCallExpression(String name, List<GretlArgument> arguments, Range range, String sourceText)
        implements GretlExpression {

    public MethodCallExpression {
        arguments = arguments != null ? List.copyOf(arguments) : List.of();
    }
}
