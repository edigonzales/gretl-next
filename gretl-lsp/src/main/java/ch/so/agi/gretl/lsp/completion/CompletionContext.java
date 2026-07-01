package ch.so.agi.gretl.lsp.completion;

import ch.so.agi.gretl.lsp.model.GretlTaskBlock;

public record CompletionContext(CompletionContextKind kind, GretlTaskBlock taskBlock) {

    public static CompletionContext of(CompletionContextKind kind) {
        return new CompletionContext(kind, null);
    }

    public static CompletionContext of(CompletionContextKind kind, GretlTaskBlock taskBlock) {
        return new CompletionContext(kind, taskBlock);
    }
}
