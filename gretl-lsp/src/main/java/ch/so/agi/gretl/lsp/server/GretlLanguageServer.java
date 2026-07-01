package ch.so.agi.gretl.lsp.server;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.concurrent.CompletableFuture;

public final class GretlLanguageServer implements LanguageServer, LanguageClientAware {

    private final GretlTextDocumentService textDocumentService;
    private final GretlWorkspaceService workspaceService;
    private final ServerLifecycle lifecycle;
    private final ServerLogger logger;
    private LanguageClient client;

    public GretlLanguageServer(GretlServerConfig config, ServerLogger logger) {
        this.lifecycle = new ServerLifecycle();
        this.logger = logger;
        this.textDocumentService = new GretlTextDocumentService(logger);
        this.workspaceService = new GretlWorkspaceService(logger);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        logger.info("initialize: workspaceRoot="
                + (params.getWorkspaceFolders() != null ? params.getWorkspaceFolders() : "none"));

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        capabilities.setCompletionProvider(null);
        capabilities.setHoverProvider(true);
        capabilities.setSignatureHelpProvider(null);
        capabilities.setDocumentSymbolProvider(true);
        capabilities.setDocumentLinkProvider(null);

        InitializeResult result = new InitializeResult(capabilities);
        result.setServerInfo(new ServerInfo("gretl-lsp", "0.1.0"));
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        logger.info("shutdown requested");
        lifecycle.transitionToShuttingDown();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        logger.info("exit requested");
        lifecycle.transitionToShutdown();
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
        logger.info("client connected");
    }
}
