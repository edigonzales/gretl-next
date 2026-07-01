package ch.so.agi.gretl.lsp.model;

import org.eclipse.lsp4j.Range;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record GretlTaskBlock(String name, Optional<String> typeName, Range nameRange, Range typeRange,
                              Range fullRange, Range bodyRange, List<GretlDslCall> calls,
                              List<GretlDependency> dependencies, List<GretlExpression> rawExpressions) {

    public GretlTaskBlock {
        calls = calls != null ? List.copyOf(calls) : List.of();
        dependencies = dependencies != null ? List.copyOf(dependencies) : List.of();
        rawExpressions = rawExpressions != null ? List.copyOf(rawExpressions) : List.of();
    }

    public List<GretlDslCall> callsByName(String name) {
        return calls.stream().filter(c -> c.name().equals(name)).toList();
    }

    public boolean hasCall(String name) {
        return calls.stream().anyMatch(c -> c.name().equals(name));
    }
}
