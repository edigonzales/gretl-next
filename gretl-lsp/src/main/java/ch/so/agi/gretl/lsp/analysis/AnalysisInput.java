package ch.so.agi.gretl.lsp.analysis;

import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.model.GretlScript;

import java.nio.file.Path;

public record AnalysisInput(TextDocument document, GretlScript script, GretlMetadata metadata,
                            Path workspaceRoot) {

    public AnalysisInput(TextDocument document, GretlScript script, GretlMetadata metadata) {
        this(document, script, metadata, null);
    }
}
