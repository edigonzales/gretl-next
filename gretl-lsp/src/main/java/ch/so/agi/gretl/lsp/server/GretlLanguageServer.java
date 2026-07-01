package ch.so.agi.gretl.lsp.server;

import ch.so.agi.gretl.lsp.analysis.GretlAnalyzer;
import ch.so.agi.gretl.lsp.analysis.GretlDiagnosticRule;
import ch.so.agi.gretl.lsp.diagnostics.DefaultTaskRule;
import ch.so.agi.gretl.lsp.diagnostics.DuplicateTaskNameRule;
import ch.so.agi.gretl.lsp.diagnostics.LegacyDslRule;
import ch.so.agi.gretl.lsp.diagnostics.MissingRequiredPropertyRule;
import ch.so.agi.gretl.lsp.diagnostics.UnknownDependencyRule;
import ch.so.agi.gretl.lsp.diagnostics.UnknownPropertyRule;
import ch.so.agi.gretl.lsp.diagnostics.UnknownTaskTypeRule;
import ch.so.agi.gretl.lsp.diagnostics.WrongArgumentCountRule;
import ch.so.agi.gretl.lsp.document.DocumentStore;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.scanner.HybridGretlScriptParser;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.SignatureHelpOptions;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GretlLanguageServer implements LanguageServer, LanguageClientAware {

    private final GretlTextDocumentService textDocumentService;
    private final GretlWorkspaceService workspaceService;
    private final ServerLifecycle lifecycle;
    private final ServerLogger logger;
    private LanguageClient client;

    public GretlLanguageServer(GretlServerConfig config, GretlMetadata metadata, ServerLogger logger) {
        this.lifecycle = new ServerLifecycle();
        this.logger = logger;

        DocumentStore documentStore = new DocumentStore();
        HybridGretlScriptParser parser = new HybridGretlScriptParser();
        List<GretlDiagnosticRule> rules = List.of(
                new MissingRequiredPropertyRule(),
                new UnknownPropertyRule(),
                new WrongArgumentCountRule(),
                new UnknownTaskTypeRule(),
                new UnknownDependencyRule(),
                new DefaultTaskRule(),
                new DuplicateTaskNameRule(),
                new LegacyDslRule()
        );
        GretlAnalyzer analyzer = new GretlAnalyzer(parser, metadata, rules);

        this.textDocumentService = new GretlTextDocumentService(documentStore, analyzer, metadata, logger);
        this.workspaceService = new GretlWorkspaceService(logger);
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        logger.info("initialize: workspaceRoot="
                + (params.getWorkspaceFolders() != null ? params.getWorkspaceFolders() : "none"));

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        capabilities.setCompletionProvider(new CompletionOptions(true, List.of()));
        capabilities.setHoverProvider(true);
        capabilities.setSignatureHelpProvider(new SignatureHelpOptions(List.of()));
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
        textDocumentService.setClient(client);
        logger.info("client connected");
    }
}
