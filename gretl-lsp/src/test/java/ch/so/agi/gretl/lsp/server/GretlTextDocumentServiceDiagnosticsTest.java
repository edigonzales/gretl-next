package ch.so.agi.gretl.lsp.server;

import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.MetadataLoader;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GretlTextDocumentServiceDiagnosticsTest {

    private static GretlMetadata metadata;

    @BeforeAll
    static void loadMetadata() throws IOException {
        MetadataLoader loader = new MetadataLoader();
        metadata = loader.loadDefault();
    }

    @Test
    @DisplayName("didOpen publishes diagnostics for missing required property")
    void didOpenPublishesDiagnostics() throws Exception {
        var config = GretlServerConfig.parse("--stdio");
        var logger = new ServerLogger("WARN");
        var server = new GretlLanguageServer(config, metadata, logger);

        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem("file:///test.gradle", "groovy", 1,
                        "tasks.register('executeSql', SqlExecutor) {\n" +
                                "    database 'url', 'usr', 'pwd'\n" +
                                "}")
        ));

        assertFalse(client.publishedDiagnostics.isEmpty());
        var last = client.publishedDiagnostics.get(client.publishedDiagnostics.size() - 1);
        assertEquals("file:///test.gradle", last.getUri());
        assertFalse(last.getDiagnostics().isEmpty());

        assertTrue(last.getDiagnostics().stream()
                .anyMatch(d -> "GRETL1001".equals(d.getCode().getLeft()) && d.getMessage().contains("sqlFiles")));
    }

    @Test
    @DisplayName("didClose clears diagnostics")
    void didCloseClearsDiagnostics() throws Exception {
        var config = GretlServerConfig.parse("--stdio");
        var logger = new ServerLogger("WARN");
        var server = new GretlLanguageServer(config, metadata, logger);

        TestLanguageClient client = new TestLanguageClient();
        server.connect(client);
        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);

        var service = server.getTextDocumentService();
        String uri = "file:///test.gradle";

        service.didOpen(new DidOpenTextDocumentParams(
                new TextDocumentItem(uri, "groovy", 1,
                        "tasks.register('executeSql', SqlExecutor) {\n" +
                                "    database 'url', 'usr', 'pwd'\n" +
                                "}")
        ));

        int countAfterOpen = client.publishedDiagnostics.size();
        var lastOpen = client.publishedDiagnostics.get(countAfterOpen - 1);
        assertFalse(lastOpen.getDiagnostics().isEmpty());

        service.didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));

        int countAfterClose = client.publishedDiagnostics.size();
        var lastClose = client.publishedDiagnostics.get(countAfterClose - 1);
        assertEquals(uri, lastClose.getUri());
        assertTrue(lastClose.getDiagnostics().isEmpty());
        assertTrue(countAfterClose > countAfterOpen);
    }

    @Test
    @DisplayName("text document service returns non-null")
    void textDocumentServiceIsNotNull() {
        var config = GretlServerConfig.parse("--stdio");
        var logger = new ServerLogger("WARN");
        var server = new GretlLanguageServer(config, metadata, logger);

        assertNotNull(server.getTextDocumentService());
    }
}
