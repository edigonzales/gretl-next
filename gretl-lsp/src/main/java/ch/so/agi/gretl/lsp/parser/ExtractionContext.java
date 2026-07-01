package ch.so.agi.gretl.lsp.parser;

import ch.so.agi.gretl.lsp.document.LineIndex;

public record ExtractionContext(String uri, String text, LineIndex lineIndex) {
}
