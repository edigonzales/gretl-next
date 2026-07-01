package ch.so.agi.gretl.doclet.lsp;

import java.util.List;

public record LspFileMetadata(
        String role,
        List<String> extensions,
        boolean multiple,
        boolean mustExist) {
}
