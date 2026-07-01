package ch.so.agi.gretl.lsp.analysis;

import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.model.GretlScript;

public record AnalysisInput(TextDocument document, GretlScript script, GretlMetadata metadata) {
}
