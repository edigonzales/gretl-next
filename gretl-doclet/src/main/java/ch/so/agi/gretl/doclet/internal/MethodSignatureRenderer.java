package ch.so.agi.gretl.doclet.internal;

import ch.so.agi.gretl.doclet.model.DslMethodDescriptor;
import ch.so.agi.gretl.doclet.model.ParameterDescriptor;

import java.util.stream.Collectors;

public final class MethodSignatureRenderer {
    public String render(DslMethodDescriptor method) {
        return method.name() + "(" + renderParameters(method) + ")";
    }

    String renderParameters(DslMethodDescriptor method) {
        return method.parameters().stream()
                .map(this::renderParameter)
                .collect(Collectors.joining(", "));
    }

    private String renderParameter(ParameterDescriptor parameter) {
        return parameter.type() + " " + parameter.name();
    }
}
