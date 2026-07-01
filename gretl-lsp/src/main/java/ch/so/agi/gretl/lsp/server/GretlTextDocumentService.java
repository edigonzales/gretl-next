package ch.so.agi.gretl.lsp.server;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.services.TextDocumentService;

public final class GretlTextDocumentService implements TextDocumentService {

    private final ServerLogger logger;

    public GretlTextDocumentService(ServerLogger logger) {
        this.logger = logger;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        logger.debug("didOpen: " + params.getTextDocument().getUri());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        logger.debug("didChange: " + params.getTextDocument().getUri());
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        logger.debug("didClose: " + params.getTextDocument().getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        logger.debug("didSave: " + params.getTextDocument().getUri());
    }
}
