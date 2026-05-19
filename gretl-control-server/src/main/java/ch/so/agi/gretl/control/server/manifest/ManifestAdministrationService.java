package ch.so.agi.gretl.control.server.manifest;

import ch.so.agi.gretl.control.server.config.GretlControlProperties;
import ch.so.agi.gretl.control.server.run.RunService;
import ch.so.agi.gretl.control.server.schedule.QuartzScheduleSynchronizer;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManifestAdministrationService {
    private static final String MANIFEST_RELOAD_SKIP_MESSAGE = "Job was removed or disabled by manifest reload.";

    private final ManifestCatalog catalog;
    private final QuartzScheduleSynchronizer scheduleSynchronizer;
    private final RunService runService;
    private final GretlControlProperties properties;

    public ManifestAdministrationService(
            ManifestCatalog catalog,
            QuartzScheduleSynchronizer scheduleSynchronizer,
            RunService runService,
            GretlControlProperties properties) {
        this.catalog = catalog;
        this.scheduleSynchronizer = scheduleSynchronizer;
        this.runService = runService;
        this.properties = properties;
    }

    public synchronized ManifestReloadResponse reload() {
        ManifestCatalog.ManifestReload reload = catalog.reload();
        if (!reload.success()) {
            return response(reload, List.of(), reload.error());
        }
        try {
            scheduleSynchronizer.synchronize(catalog.jobs());
            List<String> skippedQueuedRuns = runService.skipQueuedRunsWithoutRunnableJob(MANIFEST_RELOAD_SKIP_MESSAGE);
            return response(reload, skippedQueuedRuns, null);
        } catch (SchedulerException e) {
            return response(reload, List.of(), "Manifest reloaded, but Quartz schedule reconciliation failed: " + e.getMessage());
        }
    }

    public ManifestStatusResponse status() {
        ManifestCatalog.ManifestStatus status = catalog.status();
        return new ManifestStatusResponse(
                status.path().toString(),
                status.loadedAt(),
                status.lastReloadAttemptAt(),
                status.lastReloadError(),
                properties.getManifest().isWatchEnabled(),
                status.jobIds());
    }

    private ManifestReloadResponse response(ManifestCatalog.ManifestReload reload, List<String> skippedQueuedRuns, String error) {
        ManifestCatalog.ManifestChangeSet changeSet = reload.changeSet();
        return new ManifestReloadResponse(
                reload.success() && error == null,
                reload.path().toString(),
                reload.loadedAt(),
                reload.attemptedAt(),
                changeSet.addedJobs(),
                changeSet.removedJobs(),
                changeSet.updatedJobs(),
                changeSet.unchangedJobs(),
                skippedQueuedRuns,
                error);
    }
}
