package ch.so.agi.gretl.lsp.server;

import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class GretlLanguageServerInitializeTest {

    @Test
    @DisplayName("initialize returns expected server capabilities")
    void initializeReturnsExpectedCapabilities() throws ExecutionException, InterruptedException, TimeoutException {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        InitializeParams params = new InitializeParams();
        CompletableFuture<InitializeResult> future = server.initialize(params);
        InitializeResult result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result);
        assertNotNull(result.getCapabilities());

        ServerCapabilities caps = result.getCapabilities();
        assertEquals(TextDocumentSyncKind.Full, caps.getTextDocumentSync().getLeft());
        assertTrue(caps.getHoverProvider().getLeft());
        assertTrue(caps.getDocumentSymbolProvider().getLeft());
    }

    @Test
    @DisplayName("initialize returns server info")
    void initializeReturnsServerInfo() throws ExecutionException, InterruptedException, TimeoutException {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        InitializeParams params = new InitializeParams();
        CompletableFuture<InitializeResult> future = server.initialize(params);
        InitializeResult result = future.get(5, TimeUnit.SECONDS);

        assertNotNull(result.getServerInfo());
        assertEquals("gretl-lsp", result.getServerInfo().getName());
        assertEquals("0.1.0", result.getServerInfo().getVersion());
    }

    @Test
    @DisplayName("shutdown completes successfully")
    void shutdownCompletesSuccessfully() throws ExecutionException, InterruptedException, TimeoutException {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        server.initialize(new InitializeParams()).get(5, TimeUnit.SECONDS);
        Object result = server.shutdown().get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @DisplayName("exit does not throw")
    void exitDoesNotThrow() {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        assertDoesNotThrow(server::exit);
    }

    @Test
    @DisplayName("getTextDocumentService returns non-null")
    void textDocumentServiceReturnsNonNull() {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        assertNotNull(server.getTextDocumentService());
    }

    @Test
    @DisplayName("getWorkspaceService returns non-null")
    void workspaceServiceReturnsNonNull() {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        assertNotNull(server.getWorkspaceService());
    }

    @Test
    @DisplayName("capabilities include hover provider")
    void capabilitiesIncludeHoverProvider() throws ExecutionException, InterruptedException, TimeoutException {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        InitializeParams params = new InitializeParams();
        InitializeResult result = server.initialize(params).get(5, TimeUnit.SECONDS);

        assertTrue(result.getCapabilities().getHoverProvider().getLeft());
    }

    @Test
    @DisplayName("capabilities include document symbol provider")
    void capabilitiesIncludeDocumentSymbolProvider() throws ExecutionException, InterruptedException, TimeoutException {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        InitializeParams params = new InitializeParams();
        InitializeResult result = server.initialize(params).get(5, TimeUnit.SECONDS);

        assertTrue(result.getCapabilities().getDocumentSymbolProvider().getLeft());
    }
}
