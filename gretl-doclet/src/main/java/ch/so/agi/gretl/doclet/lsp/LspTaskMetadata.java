package ch.so.agi.gretl.doclet.lsp;

import java.util.List;

public record LspTaskMetadata(
        String name,
        String qualifiedClassName,
        String simpleClassName,
        String category,
        String status,
        String description,
        String longDescription,
        List<LspExample> examples,
        List<LspPropertyMetadata> properties) {
}
