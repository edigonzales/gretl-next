package ch.so.agi.gretl.lsp.server;

import ch.so.agi.gretl.lsp.metadata.GretlMetadata;
import ch.so.agi.gretl.lsp.metadata.MetadataLoader;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.IOException;
import java.util.logging.LogManager;

public final class GretlServerLauncher {

    private GretlServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        GretlServerConfig config = GretlServerConfig.parse(args);

        if (config.help()) {
            System.out.print(GretlServerConfig.usage());
            return;
        }

        disableJavaUtilLogging();

        ServerLogger logger = new ServerLogger(config.logLevel());
        logger.info("gretl-lsp starting, log-level=" + config.logLevel());

        GretlMetadata metadata;
        MetadataLoader loader = new MetadataLoader();
        try {
            if (config.metadataPath() != null) {
                logger.info("loading metadata from " + config.metadataPath());
                metadata = loader.load(config.metadataPath());
            } else {
                logger.info("loading metadata from default classpath resource");
                metadata = loader.loadDefault();
            }
        } catch (IOException e) {
            logger.warn("failed to load metadata, using empty set: " + e.getMessage());
            metadata = GretlMetadata.empty();
        }
        logger.info("loaded " + metadata.tasks().size() + " tasks from metadata v" + metadata.schemaVersion());

        if (config.stdio()) {
            launchStdio(config, metadata, logger);
        } else {
            logger.info("no transport mode specified, defaulting to --stdio");
            launchStdio(config, metadata, logger);
        }
    }

    private static void launchStdio(GretlServerConfig config, GretlMetadata metadata,
                                    ServerLogger logger) throws Exception {
        GretlLanguageServer server = new GretlLanguageServer(config, metadata, logger);

        Launcher<LanguageClient> launcher = Launcher.createLauncher(
                server,
                LanguageClient.class,
                System.in,
                System.out);

        server.connect(launcher.getRemoteProxy());
        logger.info("LSP server listening on stdio");

        launcher.startListening().get();
        logger.info("LSP server stopped");
    }

    private static void disableJavaUtilLogging() {
        try {
            LogManager.getLogManager().reset();
        } catch (Exception ignore) {
        }
    }
}
