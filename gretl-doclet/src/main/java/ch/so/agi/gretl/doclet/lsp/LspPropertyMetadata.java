package ch.so.agi.gretl.doclet.lsp;

import java.util.List;

public record LspPropertyMetadata(
        String name,
        String displayName,
        String kind,
        String valueType,
        String javaType,
        boolean required,
        boolean deprecated,
        String description,
        LspFileMetadata file,
        List<LspAcceptedForm> acceptedForms,
        LspMigrationMetadata migration,
        boolean sqlParameterProvider,
        LspCompletionMetadata completion) {
}
