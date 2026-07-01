package ch.so.agi.gretl.lsp.server;

import ch.so.agi.gretl.lsp.analysis.AnalysisResult;
import ch.so.agi.gretl.lsp.analysis.GretlAnalyzer;
import ch.so.agi.gretl.lsp.codeaction.GretlCodeActionProvider;
import ch.so.agi.gretl.lsp.completion.CompletionProvider;
import ch.so.agi.gretl.lsp.document.DocumentStore;
import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.hover.HoverProvider;
import ch.so.agi.gretl.lsp.links.DocumentLinkProvider;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.signature.SignatureHelpProvider;
import ch.so.agi.gretl.lsp.symbol.DocumentSymbolProvider;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class GretlTextDocumentService implements TextDocumentService {

    private final DocumentStore documentStore;
    private final GretlAnalyzer analyzer;
    private final GretlMetadata metadata;
    private final CompletionProvider completionProvider;
    private final HoverProvider hoverProvider;
    private final SignatureHelpProvider signatureHelpProvider;
    private final DocumentSymbolProvider documentSymbolProvider;
    private final DocumentLinkProvider documentLinkProvider;
    private final GretlCodeActionProvider codeActionProvider;
    private final ServerLogger logger;
    private final Map<String, AnalysisResult> lastAnalysis = new ConcurrentHashMap<>();
    private LanguageClient client;
    private Path workspaceRoot;

    public GretlTextDocumentService(DocumentStore documentStore, GretlAnalyzer analyzer,
                                    GretlMetadata metadata, ServerLogger logger) {
        this.documentStore = documentStore;
        this.analyzer = analyzer;
        this.metadata = metadata;
        this.completionProvider = new CompletionProvider(metadata);
        this.hoverProvider = new HoverProvider(metadata);
        this.signatureHelpProvider = new SignatureHelpProvider(metadata);
        this.documentSymbolProvider = new DocumentSymbolProvider(metadata);
        this.documentLinkProvider = new DocumentLinkProvider(metadata);
        this.codeActionProvider = new GretlCodeActionProvider(metadata);
        this.logger = logger;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    public void setWorkspaceRoot(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
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
        lastAnalysis.remove(uri);
        publishEmpty(uri);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        String uri = params.getTextDocument().getUri();
        logger.debug("didSave: " + uri);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String uri = params.getTextDocument().getUri();
            Position position = params.getPosition();

            Optional<TextDocument> docOpt = documentStore.get(uri);
            if (docOpt.isEmpty()) {
                return Either.forLeft(List.of());
            }

            TextDocument doc = docOpt.get();
            GretlScript script = parseOrGetCached(uri, doc);
            String currentLineText = doc.lineIndex().lineText(position.getLine());

            return completionProvider.complete(script, position, currentLineText);
        });
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String uri = params.getTextDocument().getUri();
            Position position = params.getPosition();

            Optional<TextDocument> docOpt = documentStore.get(uri);
            if (docOpt.isEmpty()) {
                return null;
            }

            TextDocument doc = docOpt.get();
            GretlScript script = parseOrGetCached(uri, doc);

            return hoverProvider.hover(script, position).orElse(null);
        });
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String uri = params.getTextDocument().getUri();
            Position position = params.getPosition();

            Optional<TextDocument> docOpt = documentStore.get(uri);
            if (docOpt.isEmpty()) {
                return null;
            }

            TextDocument doc = docOpt.get();
            GretlScript script = parseOrGetCached(uri, doc);

            return signatureHelpProvider.signatureHelp(script, position).orElse(null);
        });
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
            DocumentSymbolParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String uri = params.getTextDocument().getUri();

            Optional<TextDocument> docOpt = documentStore.get(uri);
            if (docOpt.isEmpty()) {
                return List.of();
            }

            TextDocument doc = docOpt.get();
            GretlScript script = parseOrGetCached(uri, doc);

            return documentSymbolProvider.symbols(script);
        });
    }

    @Override
    public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String uri = params.getTextDocument().getUri();

            Optional<TextDocument> docOpt = documentStore.get(uri);
            if (docOpt.isEmpty()) {
                return List.of();
            }

            TextDocument doc = docOpt.get();
            GretlScript script = parseOrGetCached(uri, doc);

            return documentLinkProvider.links(script, workspaceRoot);
        });
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String uri = params.getTextDocument().getUri();

            Optional<TextDocument> docOpt = documentStore.get(uri);
            if (docOpt.isEmpty()) {
                return List.of();
            }

            AnalysisResult cached = lastAnalysis.get(uri);
            if (cached == null) {
                return List.of();
            }

            return codeActionProvider.codeActions(params, cached);
        });
    }

    private GretlScript parseOrGetCached(String uri, TextDocument doc) {
        AnalysisResult cached = lastAnalysis.get(uri);
        if (cached != null && cached.document().version() == doc.version()) {
            return cached.script();
        }
        return analyzer.parse(doc.uri(), doc.text());
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
        lastAnalysis.put(uri, result);
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, result.diagnostics()));
    }

    private void publishEmpty(String uri) {
        if (client == null) {
            return;
        }
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, List.of()));
    }
}
