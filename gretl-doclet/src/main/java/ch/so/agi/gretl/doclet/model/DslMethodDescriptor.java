package ch.so.agi.gretl.doclet.model;

import java.util.List;

public record DslMethodDescriptor(
        String name,
        String returnType,
        List<ParameterDescriptor> parameters,
        boolean required,
        String defaultValue,
        String description) {
}
