package ch.so.agi.gretl.doclet.lsp;

public record LspAcceptedForm(
        String style,
        String signature,
        String insertText,
        Integer argumentCount,
        boolean legacy) {
}
