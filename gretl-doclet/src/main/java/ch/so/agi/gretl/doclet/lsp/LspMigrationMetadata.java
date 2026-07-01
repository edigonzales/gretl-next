package ch.so.agi.gretl.doclet.lsp;

import java.util.List;

public record LspMigrationMetadata(
        List<String> from,
        String to,
        String codeActionTitle) {
}
