package ch.so.agi.gretl.lsp.server;

import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.MetadataLoader;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class GretlTextDocumentServiceLspTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    private GretlLanguageServer createServer() {
        var config = GretlServerConfig.parse("--stdio");
        var logger = new ServerLogger("WARN");
        var server = new GretlLanguageServer(config, metadata, logger);
        return server;
    }

    @Test
    @DisplayName("completion in empty task body shows properties")
    void completionInEmptyTaskBody() throws Exception {
        var server = createServer();
        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        String uri = "file:///completion-test.gradle";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1,
                        "tasks.register('x', SqlExecutor) {\n    \n}")));

        CompletableFuture<Either<List<CompletionItem>, CompletionList>> future =
                server.getTextDocumentService().completion(
                        new CompletionParams(new TextDocumentIdentifier(uri), new Position(1, 0)));

        Either<List<CompletionItem>, CompletionList> result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        List<CompletionItem> items = result.getLeft();
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(i -> "database".equals(i.getLabel())));
        assertTrue(items.stream().anyMatch(i -> "sqlFiles".equals(i.getLabel())));
    }

    @Test
    @DisplayName("completion suggests task types after tasks.register")
    void completionSuggestsTaskTypes() throws Exception {
        var server = createServer();
        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        String uri = "file:///task-type-test.gradle";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1,
                        "tasks.register('x', Sql\n}")));

        CompletableFuture<Either<List<CompletionItem>, CompletionList>> future =
                server.getTextDocumentService().completion(
                        new CompletionParams(new TextDocumentIdentifier(uri), new Position(0, 23)));

        Either<List<CompletionItem>, CompletionList> result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        List<CompletionItem> items = result.getLeft();
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(i -> "SqlExecutor".equals(i.getLabel())));
        assertTrue(items.stream().anyMatch(i -> "DuckDbSqlExecutor".equals(i.getLabel())));
    }

    @Test
    @DisplayName("completion in dependency context shows task names")
    void completionInDependencyContext() throws Exception {
        var server = createServer();
        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        String uri = "file:///dep-test.gradle";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1,
                        "tasks.register('a', SqlExecutor) {\n" +
                                "    database 'url', 'usr', 'pwd'\n" +
                                "    sqlFiles files('x.sql')\n" +
                                "    dependsOn 'c'\n" +
                                "}\n" +
                                "tasks.register('c', SqlExecutor) {\n" +
                                "    database 'url', 'usr', 'pwd'\n" +
                                "    sqlFiles files('x.sql')\n" +
                                "}")));

        CompletableFuture<Either<List<CompletionItem>, CompletionList>> future =
                server.getTextDocumentService().completion(
                        new CompletionParams(new TextDocumentIdentifier(uri), new Position(3, 15)));

        Either<List<CompletionItem>, CompletionList> result = future.get(5, TimeUnit.SECONDS);
        assertNotNull(result);
        List<CompletionItem> items = result.getLeft();
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(i -> "a".equals(i.getLabel())));
        assertTrue(items.stream().anyMatch(i -> "c".equals(i.getLabel())));
    }

    @Test
    @DisplayName("hover over DSL property returns documentation")
    void hoverOverPropertyReturnsDocs() throws Exception {
        var server = createServer();
        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        String uri = "file:///hover-test.gradle";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1,
                        "tasks.register('x', SqlExecutor) {\n" +
                                "    database 'url', 'usr', 'pwd'\n" +
                                "}")));

        CompletableFuture<Hover> future = server.getTextDocumentService().hover(
                new HoverParams(new TextDocumentIdentifier(uri), new Position(1, 6)));

        Hover hover = future.get(5, TimeUnit.SECONDS);
        assertNotNull(hover);
        assertNotNull(hover.getContents().getRight());
        String content = hover.getContents().getRight().getValue();
        assertTrue(content.contains("database"));
        assertTrue(content.contains("Pflicht: ja"));
    }

    @Test
    @DisplayName("hover over task type returns task description")
    void hoverOverTaskType() throws Exception {
        var server = createServer();
        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        String uri = "file:///hover-type-test.gradle";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1,
                        "tasks.register('x', SqlExecutor) {\n" +
                                "    database 'url', 'usr', 'pwd'\n" +
                                "}")));

        CompletableFuture<Hover> future = server.getTextDocumentService().hover(
                new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 24)));

        Hover hover = future.get(5, TimeUnit.SECONDS);
        assertNotNull(hover);
        String content = hover.getContents().getRight().getValue();
        assertTrue(content.contains("SqlExecutor"));
        assertTrue(content.contains("ch.so.agi.gretl.tasks.SqlExecutor"));
    }

    @Test
    @DisplayName("signature help for database call shows parameters")
    void signatureHelpForDatabaseCall() throws Exception {
        var server = createServer();
        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        String uri = "file:///sig-test.gradle";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1,
                        "tasks.register('x', SqlExecutor) {\n" +
                                "    database dbUri, \n" +
                                "}")));

        CompletableFuture<SignatureHelp> future = server.getTextDocumentService().signatureHelp(
                new SignatureHelpParams(new TextDocumentIdentifier(uri), new Position(1, 19)));

        SignatureHelp help = future.get(5, TimeUnit.SECONDS);
        assertNotNull(help);
        assertFalse(help.getSignatures().isEmpty());
        assertTrue(help.getSignatures().get(0).getLabel().contains("database"));
        assertEquals(1, help.getActiveParameter());
    }

    @Test
    @DisplayName("signature help for sqlFiles call shows parameters")
    void signatureHelpForSqlFilesCall() throws Exception {
        var server = createServer();
        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        String uri = "file:///sig-sqlfiles-test.gradle";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1,
                        "tasks.register('x', SqlExecutor) {\n" +
                                "    database 'url', 'usr', 'pwd'\n" +
                                "    sqlFiles files(\n" +
                                "}")));

        CompletableFuture<SignatureHelp> future = server.getTextDocumentService().signatureHelp(
                new SignatureHelpParams(new TextDocumentIdentifier(uri), new Position(2, 17)));

        SignatureHelp help = future.get(5, TimeUnit.SECONDS);
        assertNotNull(help);
        assertFalse(help.getSignatures().isEmpty());
    }

    @Test
    @DisplayName("returns null hover for empty script")
    void returnsNullHoverForEmptyScript() throws Exception {
        var server = createServer();
        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        String uri = "file:///empty-hover.gradle";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1, "")));

        CompletableFuture<Hover> future = server.getTextDocumentService().hover(
                new HoverParams(new TextDocumentIdentifier(uri), new Position(0, 0)));

        Hover hover = future.get(5, TimeUnit.SECONDS);
        assertNull(hover);
    }

    @Test
    @DisplayName("returns null signature help for empty script")
    void returnsNullSignatureHelpForEmptyScript() throws Exception {
        var server = createServer();
        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        String uri = "file:///empty-sig.gradle";
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1, "")));

        CompletableFuture<SignatureHelp> future = server.getTextDocumentService().signatureHelp(
                new SignatureHelpParams(new TextDocumentIdentifier(uri), new Position(0, 0)));

        SignatureHelp help = future.get(5, TimeUnit.SECONDS);
        assertNull(help);
    }
}
