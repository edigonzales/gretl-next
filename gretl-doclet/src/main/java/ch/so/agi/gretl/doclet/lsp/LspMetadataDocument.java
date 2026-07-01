package ch.so.agi.gretl.doclet.lsp;

import java.util.List;

public record LspMetadataDocument(
        String schemaVersion,
        String generatedAt,
        String gretlVersion,
        LspMetadataSource source,
        List<LspTaskMetadata> tasks) {
}
