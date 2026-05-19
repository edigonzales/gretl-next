package ch.so.agi.gretl.control.server.manifest;

import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

@Component
public class ManifestAutoReloadWatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestAutoReloadWatcher.class);

    private final GretlControlProperties properties;
    private final ManifestCatalog catalog;
    private final ManifestAdministrationService administrationService;
    private FileTime lastSeenModifiedAt;

    public ManifestAutoReloadWatcher(
            GretlControlProperties properties,
            ManifestCatalog catalog,
            ManifestAdministrationService administrationService) {
        this.properties = properties;
        this.catalog = catalog;
        this.administrationService = administrationService;
    }

    @PostConstruct
    public void initialize() {
        Path manifestPath = catalog.resolvedManifestPath();
        if (!Files.exists(manifestPath)) {
            return;
        }
        try {
            lastSeenModifiedAt = Files.getLastModifiedTime(manifestPath);
        } catch (IOException e) {
            LOGGER.warn("Could not inspect GRETL control manifest for automatic reload initialization: {}",
                    manifestPath.toAbsolutePath(), e);
        }
    }

    @Scheduled(fixedDelay = 5_000)
    public void reloadChangedManifest() {
        if (!properties.getManifest().isWatchEnabled()) {
            return;
        }
        Path manifestPath = catalog.resolvedManifestPath();
        if (!Files.exists(manifestPath)) {
            return;
        }
        try {
            FileTime modifiedAt = Files.getLastModifiedTime(manifestPath);
            if (lastSeenModifiedAt == null || modifiedAt.compareTo(lastSeenModifiedAt) > 0) {
                lastSeenModifiedAt = modifiedAt;
                ManifestReloadResponse response = administrationService.reload();
                if (!response.success()) {
                    LOGGER.warn("Automatic GRETL manifest reload failed: {}", response.error());
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not inspect GRETL control manifest for automatic reload: {}",
                    manifestPath.toAbsolutePath(), e);
        }
    }
}
