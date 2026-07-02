package ch.so.agi.gretl.lsp.analysis;

import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.model.GretlImport;
import ch.so.agi.gretl.lsp.model.GretlScript;

import java.nio.file.Path;
import java.util.List;

public record AnalysisInput(TextDocument document, GretlScript script, GretlMetadata metadata,
                            Path workspaceRoot, List<GretlImport> imports) {

    public AnalysisInput(TextDocument document, GretlScript script, GretlMetadata metadata) {
        this(document, script, metadata, null, GretlImport.parseAll(document.text()));
    }

    public AnalysisInput(TextDocument document, GretlScript script, GretlMetadata metadata,
                         Path workspaceRoot) {
        this(document, script, metadata, workspaceRoot, GretlImport.parseAll(document.text()));
    }
}
