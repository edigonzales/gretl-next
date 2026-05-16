package ch.so.agi.gretl.doclet.model;

import java.util.List;

public record TaskDescriptor(
        String name,
        String className,
        String packageName,
        String description,
        List<DslMethodDescriptor> methods) {
}
