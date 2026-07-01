package ch.so.agi.gretl.lsp.server;

import ch.so.agi.gretl.lsp.analysis.GretlAnalyzer;
import ch.so.agi.gretl.lsp.document.DocumentStore;
import ch.so.agi.gretl.lsp.document.TextDocument;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.util.List;

public final class GretlTextDocumentService implements TextDocumentService {

    private final DocumentStore documentStore;
    private final GretlAnalyzer analyzer;
    private final ServerLogger logger;
    private LanguageClient client;

    public GretlTextDocumentService(DocumentStore documentStore, GretlAnalyzer analyzer,
                                    ServerLogger logger) {
        this.documentStore = documentStore;
        this.analyzer = analyzer;
        this.logger = logger;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        var doc = params.getTextDocument();
        String uri = doc.getUri();
        String languageId = doc.getLanguageId();
        int version = doc.getVersion();
        String text = doc.getText();

        logger.debug("didOpen: " + uri);
        documentStore.open(uri, languageId, version, text);
        analyzeAndPublish(uri);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        var doc = params.getTextDocument();
        String uri = doc.getUri();
        int version = doc.getVersion();
        String text = params.getContentChanges().get(0).getText();

        logger.debug("didChange: " + uri);
        documentStore.changeFull(uri, text, version);
        analyzeAndPublish(uri);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        logger.debug("didClose: " + uri);
        documentStore.close(uri);
        publishEmpty(uri);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        logger.debug("didSave: " + uri);
    }

    private void analyzeAndPublish(String uri) {
        if (client == null) {
            return;
        }
        var docOpt = documentStore.get(uri);
        if (docOpt.isEmpty()) {
            return;
        }
        TextDocument document = docOpt.get();
        var result = analyzer.analyze(document);
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, result.diagnostics()));
    }

    private void publishEmpty(String uri) {
        if (client == null) {
            return;
        }
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of()));
    }
}
