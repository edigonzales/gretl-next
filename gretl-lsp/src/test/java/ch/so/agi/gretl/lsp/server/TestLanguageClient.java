package ch.so.agi.gretl.lsp.server;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class TestLanguageClient implements LanguageClient {

    final List<PublishDiagnosticsParams> publishedDiagnostics = new ArrayList<>();

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams params) {
        publishedDiagnostics.add(params);
    }

    @Override
    public void logMessage(MessageParams message) {
    }

    @Override
    public void showMessage(MessageParams messageParams) {
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void telemetryEvent(Object object) {
    }
}
