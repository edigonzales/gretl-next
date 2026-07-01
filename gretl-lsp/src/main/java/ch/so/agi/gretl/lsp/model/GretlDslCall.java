package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

import java.util.Collections;
import java.util.List;

public record GretlDslCall(String name, DslCallStyle style, Range nameRange, Range fullRange,
                           List<GretlArgument> arguments, String sourceText) {

    public GretlDslCall {
        arguments = arguments != null ? List.copyOf(arguments) : List.of();
    }
}
