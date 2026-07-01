package ch.so.agi.gretl.lsp.server;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

public final class GretlWorkspaceService implements WorkspaceService {

    private final ServerLogger logger;

    public GretlWorkspaceService(ServerLogger logger) {
        this.logger = logger;
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        logger.debug("didChangeConfiguration");
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        logger.debug("didChangeWatchedFiles");
    }
}
