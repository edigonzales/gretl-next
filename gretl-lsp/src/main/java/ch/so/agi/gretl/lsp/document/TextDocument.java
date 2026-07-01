package ch.so.agi.gretl.lsp.document;

public record TextDocument(String uri, String languageId, int version, String text, LineIndex lineIndex) {

    public TextDocument {
        if (lineIndex == null) {
            lineIndex = LineIndex.from(text != null ? text : "");
        }
    }
}
