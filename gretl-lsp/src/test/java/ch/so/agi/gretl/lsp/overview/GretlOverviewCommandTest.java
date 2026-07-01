package ch.so.agi.gretl.lsp.overview;

import ch.so.agi.gretl.lsp.analysis.AnalysisResult;
import ch.so.agi.gretl.lsp.document.LineIndex;
import ch.so.agi.gretl.lsp.document.TextDocument;
import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.model.GretlScript;
import ch.so.agi.gretl.lsp.server.GretlServerConfig;
import ch.so.agi.gretl.lsp.server.GretlLanguageServer;
import ch.so.agi.gretl.lsp.server.ServerLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GretlOverviewCommandTest {

    @Test
    @DisplayName("executeCommand returns error for unknown command")
    void unknownCommandReturnsError() throws Exception {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        var params = new org.eclipse.lsp4j.ExecuteCommandParams();
        params.setCommand("gretl.unknownCommand");
        params.setArguments(java.util.List.of());

        CompletableFuture<Object> result = server.getWorkspaceService().executeCommand(params);
        assertNull(result.get(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("executeCommand returns error for missing arguments")
    void missingArgumentsReturnsError() throws Exception {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        var params = new org.eclipse.lsp4j.ExecuteCommandParams();
        params.setCommand("gretl.getOverview");
        params.setArguments(null);

        CompletableFuture<Object> result = server.getWorkspaceService().executeCommand(params);
        Object value = result.get(5, TimeUnit.SECONDS);

        assertInstanceOf(Map.class, value);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        assertTrue(map.containsKey("error"));
    }

    @Test
    @DisplayName("executeCommand returns error for empty uri")
    void emptyUriReturnsError() throws Exception {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        var params = new org.eclipse.lsp4j.ExecuteCommandParams();
        params.setCommand("gretl.getOverview");
        params.setArguments(java.util.List.of(""));

        CompletableFuture<Object> result = server.getWorkspaceService().executeCommand(params);
        Object value = result.get(5, TimeUnit.SECONDS);

        assertInstanceOf(Map.class, value);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        assertTrue(map.containsKey("error"));
    }

    @Test
    @DisplayName("executeCommand with uri map argument parses correctly")
    void uriAsMapArgument() throws Exception {
        GretlServerConfig config = GretlServerConfig.parse("--stdio");
        ServerLogger logger = new ServerLogger("WARN");
        GretlLanguageServer server = new GretlLanguageServer(config, GretlMetadata.empty(), logger);

        var params = new org.eclipse.lsp4j.ExecuteCommandParams();
        params.setCommand("gretl.getOverview");
        params.setArguments(java.util.List.of(Map.of("uri", "file:///unknown.gradle")));

        CompletableFuture<Object> result = server.getWorkspaceService().executeCommand(params);
        Object value = result.get(5, TimeUnit.SECONDS);

        assertInstanceOf(Map.class, value);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        assertTrue(map.containsKey("error"));
        assertTrue(map.get("error").toString().contains("Document not found"));
    }
}
